package com.zyt.flowerkisstao.catalog.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zyt.flowerkisstao.catalog.web.dto.PlantSaveDTO;
import com.zyt.flowerkisstao.catalog.web.vo.PlantVO;

/**
 * 面向运营的植物管理。与 PlantQueryService 的关键区别是不过滤 status，
 * 已下架商品在后台仍需可见可编辑。
 */
public interface PlantAdminService {

    IPage<PlantVO> page(IPage<?> page, String keyword, Integer status);

    PlantVO get(Long id);

    Long create(PlantSaveDTO dto);

    void update(Long id, PlantSaveDTO dto);

    void remove(Long id);

    /** 上下架 */
    void changeStatus(Long id, Integer status);
}
