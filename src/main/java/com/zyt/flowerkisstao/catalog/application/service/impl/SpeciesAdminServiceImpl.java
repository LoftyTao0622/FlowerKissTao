package com.zyt.flowerkisstao.catalog.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.catalog.application.service.SpeciesAdminService;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSkuMapper;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSpeciesMapper;
import com.zyt.flowerkisstao.catalog.web.dto.SpeciesSaveDTO;
import com.zyt.flowerkisstao.catalog.web.vo.PlantVO;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.redis.AfterCommitCacheInvalidator;
import com.zyt.flowerkisstao.shared.redis.RedisKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpeciesAdminServiceImpl implements SpeciesAdminService {

    private final CatalogSpeciesMapper speciesMapper;
    private final CatalogSkuMapper skuMapper;
    private final AfterCommitCacheInvalidator cacheInvalidator;
    private final RedisKey redisKey;

    public SpeciesAdminServiceImpl(CatalogSpeciesMapper speciesMapper,
                                   CatalogSkuMapper skuMapper,
                                   AfterCommitCacheInvalidator cacheInvalidator,
                                   RedisKey redisKey) {
        this.speciesMapper = speciesMapper;
        this.skuMapper = skuMapper;
        this.cacheInvalidator = cacheInvalidator;
        this.redisKey = redisKey;
    }

    @Override
    public IPage<PlantVO> page(IPage<?> page, String keyword, Integer status) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String trimmed = hasKeyword ? keyword.trim() : null;

        // 后台不过滤 status：已停用品种同样要能看到和编辑
        LambdaQueryWrapper<CatalogSpecies> wrapper = Wrappers.<CatalogSpecies>lambdaQuery()
                .eq(status != null, CatalogSpecies::getStatus, status)
                .and(hasKeyword, w -> w
                        .like(CatalogSpecies::getName, trimmed)
                        .or().like(CatalogSpecies::getLatinName, trimmed)
                        .or().like(CatalogSpecies::getCode, trimmed))
                .orderByDesc(CatalogSpecies::getSort)
                .orderByAsc(CatalogSpecies::getId);

        @SuppressWarnings("unchecked")
        IPage<CatalogSpecies> speciesPage = speciesMapper.selectPage((IPage<CatalogSpecies>) page, wrapper);

        Map<Long, List<CatalogSku>> skuMap = loadAllSkus(speciesPage.getRecords().stream()
                .map(CatalogSpecies::getId)
                .collect(Collectors.toList()));

        return speciesPage.convert(species ->
                PlantConverter.toVO(species, skuMap.getOrDefault(species.getId(), Collections.emptyList())));
    }

    @Override
    public PlantVO get(Long id) {
        CatalogSpecies species = requireSpecies(id);
        return PlantConverter.toVO(species, listAllSkus(id));
    }

    @Override
    public Long create(SpeciesSaveDTO dto) {
        requireCodeAvailable(dto.getCode(), null);
        requireRangesValid(dto);

        CatalogSpecies species = new CatalogSpecies();
        applyDto(species, dto);
        species.setStatus(1);
        speciesMapper.insert(species);
        cacheInvalidator.delete(redisKey.plantFacets(), redisKey.recommendCandidates());
        return species.getId();
    }

    @Override
    public void update(Long id, SpeciesSaveDTO dto) {
        CatalogSpecies existing = requireSpecies(id);
        requireCodeAvailable(dto.getCode(), id);
        requireRangesValid(dto);

        CatalogSpecies species = new CatalogSpecies();
        species.setId(id);
        applyDto(species, dto);
        // status 由启停接口单独管，避免编辑表单顺手把品种停用了
        speciesMapper.updateById(species);
        cacheInvalidator.delete(redisKey.plantDetail(existing.getCode()),
                redisKey.plantDetail(dto.getCode()), redisKey.plantFacets(), redisKey.recommendCandidates());
    }

    /**
     * 逻辑删除。
     *
     * <p>先拦下仍挂着 SKU 的品种：删掉品种但留下 SKU，会让那些 SKU 变成
     * 查不到品种属性的孤儿数据，而 catalog_sku 上没有外键能兜住这件事。
     *
     * <p>uk_species_code 是物理唯一索引，不认 deleted 标记：直接删掉后想用同一个
     * code 重建就会撞唯一键，最终落到 500 兜底。所以先把 code 改名腾位，再标记删除。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        CatalogSpecies existing = requireSpecies(id);

        long skuCount = skuMapper.selectCount(Wrappers.<CatalogSku>lambdaQuery()
                .eq(CatalogSku::getSpeciesId, id));
        if (skuCount > 0) {
            throw new BizException(ErrorCode.SPECIES_HAS_SKU,
                    "该品种下还有 " + skuCount + " 个商品，请先删除商品再删品种");
        }

        CatalogSpecies rename = new CatalogSpecies();
        rename.setId(id);
        rename.setCode(truncateCode(existing.getCode(), id));
        speciesMapper.updateById(rename);

        speciesMapper.deleteById(id);
        cacheInvalidator.delete(redisKey.plantDetail(existing.getCode()), redisKey.plantFacets(), redisKey.recommendCandidates());
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("status 只能是 0 或 1");
        }
        CatalogSpecies existing = requireSpecies(id);

        CatalogSpecies update = new CatalogSpecies();
        update.setId(id);
        update.setStatus(status);
        speciesMapper.updateById(update);
        cacheInvalidator.delete(redisKey.plantDetail(existing.getCode()), redisKey.plantFacets(), redisKey.recommendCandidates());
    }

    private CatalogSpecies requireSpecies(Long id) {
        CatalogSpecies species = speciesMapper.selectById(id);
        if (species == null) {
            throw new BizException(ErrorCode.PLANT_NOT_FOUND, "品种不存在");
        }
        return species;
    }

    /** 新建时 excludeId 传 null；编辑时传自身 id，否则会和自己撞车 */
    private void requireCodeAvailable(String code, Long excludeId) {
        long taken = speciesMapper.selectCount(Wrappers.<CatalogSpecies>lambdaQuery()
                .eq(CatalogSpecies::getCode, code)
                .ne(excludeId != null, CatalogSpecies::getId, excludeId));
        if (taken > 0) {
            throw new BizException(ErrorCode.PLANT_SLUG_TAKEN, "code 已被占用：" + code);
        }
    }

    /**
     * 区间字段的跨字段校验。
     *
     * <p>单字段的 @Min/@Max 拦不住 min 大于 max：光照区间写成 4-2 会让推荐算法的
     * 硬过滤永远命中不了任何等级，而这类错误在页面上完全看不出来。
     */
    private void requireRangesValid(SpeciesSaveDTO dto) {
        if (dto.getLightMin() > dto.getLightMax()) {
            throw new BizException("最低光照等级不能高于最高光照等级");
        }
        if (dto.getTempMin() >= dto.getTempMax()) {
            throw new BizException("最低耐受温度必须低于最高耐受温度");
        }
        if (dto.getHumidityMin() >= dto.getHumidityMax()) {
            throw new BizException("最低适宜湿度必须低于最高适宜湿度");
        }
    }

    /** code 列限长 80，拼后缀前先给原值留出空间 */
    private String truncateCode(String code, Long id) {
        String suffix = "-del-" + id;
        int maxBase = 80 - suffix.length();
        String base = code.length() > maxBase ? code.substring(0, maxBase) : code;
        return base + suffix;
    }

    /** 后台要看到全部 SKU（含已下架），排序规则与公开接口一致 */
    private List<CatalogSku> listAllSkus(Long speciesId) {
        return skuMapper.selectList(Wrappers.<CatalogSku>lambdaQuery()
                .eq(CatalogSku::getSpeciesId, speciesId)
                .orderByDesc(CatalogSku::getSort)
                .orderByAsc(CatalogSku::getPrice)
                .orderByAsc(CatalogSku::getId));
    }

    private Map<Long, List<CatalogSku>> loadAllSkus(List<Long> speciesIds) {
        if (speciesIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CatalogSku> skus = skuMapper.selectList(Wrappers.<CatalogSku>lambdaQuery()
                .in(CatalogSku::getSpeciesId, speciesIds)
                .orderByDesc(CatalogSku::getSort)
                .orderByAsc(CatalogSku::getPrice)
                .orderByAsc(CatalogSku::getId));
        return skus.stream().collect(Collectors.groupingBy(CatalogSku::getSpeciesId));
    }

    private void applyDto(CatalogSpecies species, SpeciesSaveDTO dto) {
        species.setCode(dto.getCode());
        species.setName(dto.getName());
        species.setLatinName(dto.getLatinName());
        species.setCategory(dto.getCategory());
        species.setOrnamentalType(dto.getOrnamentalType());
        species.setBloomSeason(dto.getBloomSeason());
        species.setBloomColor(dto.getBloomColor());
        species.setFragrance(dto.getFragrance());
        species.setLightMin(dto.getLightMin());
        species.setLightMax(dto.getLightMax());
        species.setLightNote(dto.getLightNote());
        species.setTempMin(dto.getTempMin());
        species.setTempMax(dto.getTempMax());
        species.setHumidityMin(dto.getHumidityMin());
        species.setHumidityMax(dto.getHumidityMax());
        species.setWaterIntervalDays(dto.getWaterIntervalDays());
        species.setWaterNote(dto.getWaterNote());
        species.setFertilizeIntervalDays(dto.getFertilizeIntervalDays() == null
                ? 30 : dto.getFertilizeIntervalDays());
        species.setRepotIntervalMonths(dto.getRepotIntervalMonths() == null
                ? 24 : dto.getRepotIntervalMonths());
        species.setPruneNeeded(toFlag(dto.getPruneNeeded()));
        species.setCareLevel(dto.getCareLevel());
        species.setToxicCat(toFlag(dto.getToxicCat()));
        species.setToxicDog(toFlag(dto.getToxicDog()));
        species.setToxicChild(toFlag(dto.getToxicChild()));
        species.setPollenRisk(toFlag(dto.getPollenRisk()));
        species.setMatureHeightCm(dto.getMatureHeightCm());
        species.setFootprintCm(dto.getFootprintCm());
        species.setMatchTags(dto.getMatchTags());
        species.setRecommendationReason(dto.getRecommendationReason());
        species.setShortDescription(dto.getShortDescription());
        species.setDescription(dto.getDescription());
        species.setCareTips(dto.getCareTips());
        species.setSort(dto.getSort() == null ? 0 : dto.getSort());
    }

    private Integer toFlag(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }
}
