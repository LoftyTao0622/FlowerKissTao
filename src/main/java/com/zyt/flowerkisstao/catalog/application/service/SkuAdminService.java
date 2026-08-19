package com.zyt.flowerkisstao.catalog.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zyt.flowerkisstao.catalog.web.dto.SkuSaveDTO;
import com.zyt.flowerkisstao.catalog.web.vo.SkuVO;

/**
 * 面向运营的 SKU 管理。不过滤 status，已下架 SKU 在后台仍需可见可编辑。
 */
public interface SkuAdminService {

    /** speciesId 为 null 时查全部，否则只查该品种下的 SKU */
    IPage<SkuVO> page(IPage<?> page, Long speciesId, String keyword, Integer status);

    SkuVO get(Long id);

    Long create(SkuSaveDTO dto);

    void update(Long id, SkuSaveDTO dto);

    void remove(Long id);

    /** 上架或下架 */
    void changeStatus(Long id, Integer status);
}
