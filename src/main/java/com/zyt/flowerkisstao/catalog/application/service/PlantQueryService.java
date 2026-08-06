package com.zyt.flowerkisstao.catalog.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zyt.flowerkisstao.catalog.web.vo.PlantFacetsVO;
import com.zyt.flowerkisstao.catalog.web.vo.PlantVO;

/**
 * 面向顾客的植物查询。所有方法只返回上架商品。
 */
public interface PlantQueryService {

    IPage<PlantVO> page(IPage<?> page, String keyword, String category, String light, Boolean featured);

    /** 按 slug 查详情，查不到抛 BizException(PLANT_NOT_FOUND) */
    PlantVO getBySlug(String slug);

    PlantFacetsVO facets();
}
