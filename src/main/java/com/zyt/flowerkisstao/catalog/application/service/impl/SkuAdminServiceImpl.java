package com.zyt.flowerkisstao.catalog.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.catalog.application.service.SkuAdminService;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSkuMapper;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSpeciesMapper;
import com.zyt.flowerkisstao.catalog.web.dto.SkuSaveDTO;
import com.zyt.flowerkisstao.catalog.web.vo.SkuVO;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.redis.AfterCommitCacheInvalidator;
import com.zyt.flowerkisstao.shared.redis.RedisKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkuAdminServiceImpl implements SkuAdminService {

    private final CatalogSkuMapper skuMapper;
    private final CatalogSpeciesMapper speciesMapper;
    private final AfterCommitCacheInvalidator cacheInvalidator;
    private final RedisKey redisKey;

    public SkuAdminServiceImpl(CatalogSkuMapper skuMapper,
                               CatalogSpeciesMapper speciesMapper,
                               AfterCommitCacheInvalidator cacheInvalidator,
                               RedisKey redisKey) {
        this.skuMapper = skuMapper;
        this.speciesMapper = speciesMapper;
        this.cacheInvalidator = cacheInvalidator;
        this.redisKey = redisKey;
    }

    @Override
    public IPage<SkuVO> page(IPage<?> page, Long speciesId, String keyword, Integer status) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String trimmed = hasKeyword ? keyword.trim() : null;

        // 后台不过滤 status：已下架 SKU 同样要能看到和编辑
        LambdaQueryWrapper<CatalogSku> wrapper = Wrappers.<CatalogSku>lambdaQuery()
                .eq(speciesId != null, CatalogSku::getSpeciesId, speciesId)
                .eq(status != null, CatalogSku::getStatus, status)
                .and(hasKeyword, w -> w
                        .like(CatalogSku::getSkuCode, trimmed)
                        .or().like(CatalogSku::getSpec, trimmed))
                .orderByDesc(CatalogSku::getSort)
                .orderByAsc(CatalogSku::getId);

        @SuppressWarnings("unchecked")
        IPage<CatalogSku> skuPage = skuMapper.selectPage((IPage<CatalogSku>) page, wrapper);
        return skuPage.convert(PlantConverter::toSkuVO);
    }

    @Override
    public SkuVO get(Long id) {
        return PlantConverter.toSkuVO(requireSku(id));
    }

    @Override
    public Long create(SkuSaveDTO dto) {
        CatalogSpecies species = requireSpeciesExists(dto.getSpeciesId());
        requireSkuCodeAvailable(dto.getSkuCode(), null);

        CatalogSku sku = new CatalogSku();
        applyDto(sku, dto);
        sku.setStatus(1);
        skuMapper.insert(sku);
        cacheInvalidator.delete(redisKey.plantDetail(species.getCode()), redisKey.plantFacets(), redisKey.recommendCandidates());
        return sku.getId();
    }

    @Override
    public void update(Long id, SkuSaveDTO dto) {
        CatalogSku existing = requireSku(id);
        CatalogSpecies oldSpecies = requireSpeciesExists(existing.getSpeciesId());
        CatalogSpecies newSpecies = requireSpeciesExists(dto.getSpeciesId());
        requireSkuCodeAvailable(dto.getSkuCode(), id);

        CatalogSku sku = new CatalogSku();
        sku.setId(id);
        applyDto(sku, dto);
        // status 由上下架接口单独管，避免编辑表单顺手把商品下架了
        skuMapper.updateById(sku);
        cacheInvalidator.delete(redisKey.plantDetail(oldSpecies.getCode()),
                redisKey.plantDetail(newSpecies.getCode()), redisKey.plantFacets(), redisKey.recommendCandidates());
    }

    /**
     * 逻辑删除。
     *
     * <p>uk_sku_code 是物理唯一索引，不认 deleted 标记，处理方式与品种的 code 相同：
     * 先改名腾位再标记删除。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        CatalogSku existing = requireSku(id);
        CatalogSpecies species = requireSpeciesExists(existing.getSpeciesId());

        CatalogSku rename = new CatalogSku();
        rename.setId(id);
        rename.setSkuCode(truncateCode(existing.getSkuCode(), id));
        skuMapper.updateById(rename);

        skuMapper.deleteById(id);
        cacheInvalidator.delete(redisKey.plantDetail(species.getCode()), redisKey.plantFacets(), redisKey.recommendCandidates());
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("status 只能是 0 或 1");
        }
        CatalogSku existing = requireSku(id);
        CatalogSpecies species = requireSpeciesExists(existing.getSpeciesId());

        CatalogSku update = new CatalogSku();
        update.setId(id);
        update.setStatus(status);
        skuMapper.updateById(update);
        cacheInvalidator.delete(redisKey.plantDetail(species.getCode()), redisKey.plantFacets(), redisKey.recommendCandidates());
    }

    private CatalogSku requireSku(Long id) {
        CatalogSku sku = skuMapper.selectById(id);
        if (sku == null) {
            throw new BizException(ErrorCode.SKU_NOT_FOUND, "商品不存在");
        }
        return sku;
    }

    /**
     * catalog_sku 上没有到 catalog_species 的外键（品种走逻辑删除，外键拦不住
     * deleted=1 的脏引用），引用完整性只能在这里把住。
     */
    private CatalogSpecies requireSpeciesExists(Long speciesId) {
        CatalogSpecies species = speciesMapper.selectById(speciesId);
        if (species == null) {
            throw new BizException(ErrorCode.PLANT_NOT_FOUND, "所属品种不存在：" + speciesId);
        }
        return species;
    }

    private void requireSkuCodeAvailable(String skuCode, Long excludeId) {
        long taken = skuMapper.selectCount(Wrappers.<CatalogSku>lambdaQuery()
                .eq(CatalogSku::getSkuCode, skuCode)
                .ne(excludeId != null, CatalogSku::getId, excludeId));
        if (taken > 0) {
            throw new BizException(ErrorCode.SKU_CODE_TAKEN, "商品编码已被占用：" + skuCode);
        }
    }

    /** sku_code 列限长 80，拼后缀前先给原值留出空间 */
    private String truncateCode(String skuCode, Long id) {
        String suffix = "-del-" + id;
        int maxBase = 80 - suffix.length();
        String base = skuCode.length() > maxBase ? skuCode.substring(0, maxBase) : skuCode;
        return base + suffix;
    }

    private void applyDto(CatalogSku sku, SkuSaveDTO dto) {
        sku.setSpeciesId(dto.getSpeciesId());
        sku.setSkuCode(dto.getSkuCode());
        sku.setSpec(dto.getSpec());
        sku.setPot(dto.getPot());
        sku.setPrice(dto.getPrice());
        sku.setStock(dto.getStock());
        sku.setImage(dto.getImage());
        sku.setImageAlt(dto.getImageAlt());
        sku.setFeatured(Boolean.TRUE.equals(dto.getFeatured()) ? 1 : 0);
        sku.setSort(dto.getSort() == null ? 0 : dto.getSort());
    }
}
