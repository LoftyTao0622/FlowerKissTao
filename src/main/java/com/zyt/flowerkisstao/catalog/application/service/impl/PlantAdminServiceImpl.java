package com.zyt.flowerkisstao.catalog.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.catalog.application.service.PlantAdminService;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogPlant;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogPlantMapper;
import com.zyt.flowerkisstao.catalog.web.dto.PlantSaveDTO;
import com.zyt.flowerkisstao.catalog.web.vo.PlantVO;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlantAdminServiceImpl implements PlantAdminService {

    private final CatalogPlantMapper plantMapper;

    public PlantAdminServiceImpl(CatalogPlantMapper plantMapper) {
        this.plantMapper = plantMapper;
    }

    @Override
    public IPage<PlantVO> page(IPage<?> page, String keyword, Integer status) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String trimmed = hasKeyword ? keyword.trim() : null;

        // 后台不过滤 status：已下架商品同样要能看到和编辑
        LambdaQueryWrapper<CatalogPlant> wrapper = Wrappers.<CatalogPlant>lambdaQuery()
                .eq(status != null, CatalogPlant::getStatus, status)
                .and(hasKeyword, w -> w
                        .like(CatalogPlant::getName, trimmed)
                        .or().like(CatalogPlant::getLatinName, trimmed)
                        .or().like(CatalogPlant::getSlug, trimmed))
                .orderByDesc(CatalogPlant::getSort)
                .orderByAsc(CatalogPlant::getId);

        @SuppressWarnings("unchecked")
        IPage<CatalogPlant> plantPage = plantMapper.selectPage((IPage<CatalogPlant>) page, wrapper);
        return plantPage.convert(PlantConverter::toVO);
    }

    @Override
    public PlantVO get(Long id) {
        return PlantConverter.toVO(requirePlant(id));
    }

    @Override
    public Long create(PlantSaveDTO dto) {
        requireSlugAvailable(dto.getSlug(), null);

        CatalogPlant plant = new CatalogPlant();
        applyDto(plant, dto);
        plant.setStatus(1);
        plantMapper.insert(plant);
        return plant.getId();
    }

    @Override
    public void update(Long id, PlantSaveDTO dto) {
        requirePlant(id);
        requireSlugAvailable(dto.getSlug(), id);

        CatalogPlant plant = new CatalogPlant();
        plant.setId(id);
        applyDto(plant, dto);
        // status 由上下架接口单独管，避免编辑表单顺手把商品下架了
        plantMapper.updateById(plant);
    }

    /**
     * 逻辑删除。
     *
     * <p>uk_plant_slug 是物理唯一索引，不认 deleted 标记：直接删掉后想用同一个 slug
     * 重建就会撞唯一键，最终落到 500 兜底。所以先把 slug 改名腾位，再标记删除。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        CatalogPlant existing = requirePlant(id);

        CatalogPlant rename = new CatalogPlant();
        rename.setId(id);
        rename.setSlug(truncateSlug(existing.getSlug(), id));
        plantMapper.updateById(rename);

        plantMapper.deleteById(id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("status 只能是 0 或 1");
        }
        requirePlant(id);

        CatalogPlant update = new CatalogPlant();
        update.setId(id);
        update.setStatus(status);
        plantMapper.updateById(update);
    }

    private CatalogPlant requirePlant(Long id) {
        CatalogPlant plant = plantMapper.selectById(id);
        if (plant == null) {
            throw new BizException(ErrorCode.PLANT_NOT_FOUND, "植物不存在");
        }
        return plant;
    }

    /** 新建时 excludeId 传 null；编辑时传自身 id，否则会和自己撞车 */
    private void requireSlugAvailable(String slug, Long excludeId) {
        long taken = plantMapper.selectCount(Wrappers.<CatalogPlant>lambdaQuery()
                .eq(CatalogPlant::getSlug, slug)
                .ne(excludeId != null, CatalogPlant::getId, excludeId));
        if (taken > 0) {
            throw new BizException(ErrorCode.PLANT_SLUG_TAKEN, "slug 已被占用：" + slug);
        }
    }

    /** slug 列限长 80，拼后缀前先给原值留出空间 */
    private String truncateSlug(String slug, Long id) {
        String suffix = "-del-" + id;
        int maxBase = 80 - suffix.length();
        String base = slug.length() > maxBase ? slug.substring(0, maxBase) : slug;
        return base + suffix;
    }

    private void applyDto(CatalogPlant plant, PlantSaveDTO dto) {
        plant.setSlug(dto.getSlug());
        plant.setName(dto.getName());
        plant.setLatinName(dto.getLatinName());
        plant.setPrice(dto.getPrice());
        plant.setImage(dto.getImage());
        plant.setImageAlt(dto.getImageAlt());
        plant.setCategory(dto.getCategory());
        plant.setLight(dto.getLight());
        plant.setWatering(dto.getWatering());
        plant.setSize(dto.getSize());
        plant.setPetFriendly(Boolean.TRUE.equals(dto.getPetFriendly()) ? 1 : 0);
        plant.setPetNote(dto.getPetNote());
        plant.setDifficulty(dto.getDifficulty());
        plant.setMatchTags(dto.getMatchTags());
        plant.setRecommendationReason(dto.getRecommendationReason());
        plant.setShortDescription(dto.getShortDescription());
        plant.setDescription(dto.getDescription());
        plant.setCareTips(dto.getCareTips());
        plant.setFeatured(Boolean.TRUE.equals(dto.getFeatured()) ? 1 : 0);
        plant.setSort(dto.getSort() == null ? 0 : dto.getSort());
    }
}
