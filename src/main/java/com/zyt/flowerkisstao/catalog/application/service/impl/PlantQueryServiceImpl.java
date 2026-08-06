package com.zyt.flowerkisstao.catalog.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.catalog.application.service.PlantQueryService;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogPlant;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogPlantMapper;
import com.zyt.flowerkisstao.catalog.web.vo.PlantFacetsVO;
import com.zyt.flowerkisstao.catalog.web.vo.PlantVO;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class PlantQueryServiceImpl implements PlantQueryService {

    private final CatalogPlantMapper plantMapper;

    public PlantQueryServiceImpl(CatalogPlantMapper plantMapper) {
        this.plantMapper = plantMapper;
    }

    @Override
    public IPage<PlantVO> page(IPage<?> page, String keyword, String category, String light, Boolean featured) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String trimmed = hasKeyword ? keyword.trim() : null;

        LambdaQueryWrapper<CatalogPlant> wrapper = Wrappers.<CatalogPlant>lambdaQuery()
                // 上架状态在这里兜死，任何调用方都绕不过去
                .eq(CatalogPlant::getStatus, 1)
                .eq(category != null && !category.isEmpty(), CatalogPlant::getCategory, category)
                .eq(light != null && !light.isEmpty(), CatalogPlant::getLight, light)
                .eq(Boolean.TRUE.equals(featured), CatalogPlant::getFeatured, 1)
                .and(hasKeyword, w -> w
                        .like(CatalogPlant::getName, trimmed)
                        .or().like(CatalogPlant::getLatinName, trimmed)
                        .or().like(CatalogPlant::getCategory, trimmed)
                        .or().like(CatalogPlant::getLight, trimmed)
                        .or().like(CatalogPlant::getShortDescription, trimmed)
                        // match_tags 是 JSON 列，走字段引用会被 typeHandler 接管，
                        // 这里用 apply 参数绑定转成字符串再模糊匹配，不拼 SQL
                        .or().apply("CAST(match_tags AS CHAR) LIKE CONCAT('%', {0}, '%')", trimmed))
                .orderByDesc(CatalogPlant::getSort)
                .orderByAsc(CatalogPlant::getId);

        @SuppressWarnings("unchecked")
        IPage<CatalogPlant> plantPage = plantMapper.selectPage((IPage<CatalogPlant>) page, wrapper);
        return plantPage.convert(PlantConverter::toVO);
    }

    @Override
    public PlantVO getBySlug(String slug) {
        CatalogPlant plant = plantMapper.selectOne(Wrappers.<CatalogPlant>lambdaQuery()
                .eq(CatalogPlant::getSlug, slug)
                .eq(CatalogPlant::getStatus, 1));
        if (plant == null) {
            // 已下架与不存在对外是同一种结果，不泄露"这株曾经存在"
            throw new BizException(ErrorCode.PLANT_NOT_FOUND, "植物不存在或已下架");
        }
        return PlantConverter.toVO(plant);
    }

    @Override
    public PlantFacetsVO facets() {
        return PlantFacetsVO.builder()
                .categories(plantMapper.selectDistinctCategories())
                .lights(plantMapper.selectDistinctLights())
                .build();
    }
}
