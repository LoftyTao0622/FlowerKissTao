package com.zyt.flowerkisstao.catalog.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.catalog.application.service.PlantQueryService;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSkuMapper;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSpeciesMapper;
import com.zyt.flowerkisstao.catalog.web.vo.PlantFacetsVO;
import com.zyt.flowerkisstao.catalog.web.vo.PlantVO;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.redis.RedisCacheService;
import com.zyt.flowerkisstao.shared.redis.RedisKey;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlantQueryServiceImpl implements PlantQueryService {

    private final CatalogSpeciesMapper speciesMapper;
    private final CatalogSkuMapper skuMapper;
    private final RedisCacheService cacheService;
    private final RedisKey redisKey;

    public PlantQueryServiceImpl(CatalogSpeciesMapper speciesMapper,
                                 CatalogSkuMapper skuMapper,
                                 RedisCacheService cacheService,
                                 RedisKey redisKey) {
        this.speciesMapper = speciesMapper;
        this.skuMapper = skuMapper;
        this.cacheService = cacheService;
        this.redisKey = redisKey;
    }

    @Override
    public IPage<PlantVO> page(IPage<?> page, String keyword, String category, String light, Boolean featured) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String trimmed = hasKeyword ? keyword.trim() : null;

        LambdaQueryWrapper<CatalogSpecies> wrapper = Wrappers.<CatalogSpecies>lambdaQuery()
                // 启用状态在这里兜死，任何调用方都绕不过去
                .eq(CatalogSpecies::getStatus, 1)
                .eq(category != null && !category.isEmpty(), CatalogSpecies::getCategory, category)
                .eq(light != null && !light.isEmpty(), CatalogSpecies::getLightNote, light)
                .and(hasKeyword, w -> w
                        .like(CatalogSpecies::getName, trimmed)
                        .or().like(CatalogSpecies::getLatinName, trimmed)
                        .or().like(CatalogSpecies::getCategory, trimmed)
                        .or().like(CatalogSpecies::getLightNote, trimmed)
                        .or().like(CatalogSpecies::getShortDescription, trimmed)
                        // match_tags 是 JSON 列，走字段引用会被 typeHandler 接管，
                        // 这里用 apply 参数绑定转成字符串再模糊匹配，不拼 SQL
                        .or().apply("CAST(match_tags AS CHAR) LIKE CONCAT('%', {0}, '%')", trimmed))
                // 品种必须至少有一个上架 SKU 才出现在列表：否则用户点进详情页会看到
                // 一个没有价格、没法加购的空商品
                .exists(!Boolean.TRUE.equals(featured),
                        "SELECT 1 FROM catalog_sku k WHERE k.species_id = catalog_species.id "
                                + "AND k.status = 1 AND k.deleted = 0")
                // featured 落在 SKU 层，条件要写进 exists 内部。判定口径与
                // PlantConverter 里的 featured 字段一致：任一上架 SKU 精选即算精选
                .exists(Boolean.TRUE.equals(featured),
                        "SELECT 1 FROM catalog_sku k WHERE k.species_id = catalog_species.id "
                                + "AND k.status = 1 AND k.deleted = 0 AND k.featured = 1")
                .orderByDesc(CatalogSpecies::getSort)
                .orderByAsc(CatalogSpecies::getId);

        @SuppressWarnings("unchecked")
        IPage<CatalogSpecies> speciesPage = speciesMapper.selectPage((IPage<CatalogSpecies>) page, wrapper);

        // 一次批量查回本页所有品种的 SKU，避免每条记录查一次库
        Map<Long, List<CatalogSku>> skuMap = loadSkusBySpecies(speciesPage.getRecords().stream()
                .map(CatalogSpecies::getId)
                .collect(Collectors.toList()));

        return speciesPage.convert(species ->
                PlantConverter.toVO(species, skuMap.getOrDefault(species.getId(), Collections.emptyList())));
    }

    @Override
    public PlantVO getBySlug(String slug) {
        String cacheKey = redisKey.plantDetail(slug);
        PlantVO cached = cacheService.get(cacheKey, PlantVO.class).orElse(null);
        if (cached != null) {
            return cached;
        }
        CatalogSpecies species = speciesMapper.selectOne(Wrappers.<CatalogSpecies>lambdaQuery()
                .eq(CatalogSpecies::getCode, slug)
                .eq(CatalogSpecies::getStatus, 1));
        if (species == null) {
            // 已停用与不存在对外是同一种结果，不泄露"这株曾经存在"
            throw new BizException(ErrorCode.PLANT_NOT_FOUND, "植物不存在或已下架");
        }

        List<CatalogSku> skus = listOnSaleSkus(species.getId());
        if (skus.isEmpty()) {
            // 品种还在但 SKU 全部下架，对顾客而言与下架无异
            throw new BizException(ErrorCode.PLANT_NOT_FOUND, "植物不存在或已下架");
        }
        PlantVO result = PlantConverter.toVO(species, skus);
        cacheService.set(cacheKey, result, Duration.ofSeconds(300));
        return result;
    }

    @Override
    public PlantFacetsVO facets() {
        String cacheKey = redisKey.plantFacets();
        PlantFacetsVO cached = cacheService.get(cacheKey, PlantFacetsVO.class).orElse(null);
        if (cached != null) {
            return cached;
        }
        PlantFacetsVO result = PlantFacetsVO.builder()
                .categories(speciesMapper.selectDistinctCategories())
                .lights(speciesMapper.selectDistinctLights())
                .build();
        cacheService.set(cacheKey, result, Duration.ofMinutes(5));
        return result;
    }

    /**
     * 查某个品种的上架 SKU。
     *
     * <p>排序即"默认 SKU"的定义：sort 大者优先，同 sort 取价低者。
     * 价格作为次级键是为了让默认展示价成为该品种的起步价，
     * 而不是随自增 id 漂移。
     */
    private List<CatalogSku> listOnSaleSkus(Long speciesId) {
        return skuMapper.selectList(Wrappers.<CatalogSku>lambdaQuery()
                .eq(CatalogSku::getSpeciesId, speciesId)
                .eq(CatalogSku::getStatus, 1)
                .orderByDesc(CatalogSku::getSort)
                .orderByAsc(CatalogSku::getPrice)
                .orderByAsc(CatalogSku::getId));
    }

    /** 批量版本，排序规则与 listOnSaleSkus 一致，分组后各组内顺序得以保留 */
    private Map<Long, List<CatalogSku>> loadSkusBySpecies(List<Long> speciesIds) {
        if (speciesIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CatalogSku> skus = skuMapper.selectList(Wrappers.<CatalogSku>lambdaQuery()
                .in(CatalogSku::getSpeciesId, speciesIds)
                .eq(CatalogSku::getStatus, 1)
                .orderByDesc(CatalogSku::getSort)
                .orderByAsc(CatalogSku::getPrice)
                .orderByAsc(CatalogSku::getId));
        return skus.stream().collect(Collectors.groupingBy(CatalogSku::getSpeciesId));
    }
}
