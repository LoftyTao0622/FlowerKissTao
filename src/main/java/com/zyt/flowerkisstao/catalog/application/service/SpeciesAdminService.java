package com.zyt.flowerkisstao.catalog.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zyt.flowerkisstao.catalog.web.dto.SpeciesSaveDTO;
import com.zyt.flowerkisstao.catalog.web.vo.PlantVO;

/**
 * 面向运营的品种管理。与 PlantQueryService 的关键区别是不过滤 status，
 * 已停用品种在后台仍需可见可编辑。
 */
public interface SpeciesAdminService {

    IPage<PlantVO> page(IPage<?> page, String keyword, Integer status);

    PlantVO get(Long id);

    Long create(SpeciesSaveDTO dto);

    void update(Long id, SpeciesSaveDTO dto);

    /** 逻辑删除。品种下仍有未删 SKU 时拒绝，抛 BizException(SPECIES_HAS_SKU) */
    void remove(Long id);

    /** 启用或停用 */
    void changeStatus(Long id, Integer status);
}
