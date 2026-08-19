package com.zyt.flowerkisstao.catalog.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyt.flowerkisstao.catalog.application.service.SkuAdminService;
import com.zyt.flowerkisstao.catalog.web.dto.SkuSaveDTO;
import com.zyt.flowerkisstao.catalog.web.vo.SkuVO;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 植物商品 SKU 管理，仅运营与管理员可用。权限点与品种管理共用 catalog:plant:*。
 */
@RestController
@RequestMapping("/api/admin/skus")
@Validated
public class SkuAdminController {

    private final SkuAdminService skuAdminService;

    public SkuAdminController(SkuAdminService skuAdminService) {
        this.skuAdminService = skuAdminService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.CATALOG_PLANT_UPDATE + "')")
    public R<IPage<SkuVO>> page(@RequestParam(defaultValue = "1") long current,
                                @RequestParam(defaultValue = "10") long size,
                                @RequestParam(required = false) Long speciesId,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) Integer status) {
        return R.ok(skuAdminService.page(new Page<>(current, size), speciesId, keyword, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.CATALOG_PLANT_UPDATE + "')")
    public R<SkuVO> get(@PathVariable Long id) {
        return R.ok(skuAdminService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Perms.CATALOG_PLANT_CREATE + "')")
    public R<Long> create(@Valid @RequestBody SkuSaveDTO dto) {
        return R.ok(skuAdminService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.CATALOG_PLANT_UPDATE + "')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SkuSaveDTO dto) {
        skuAdminService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.CATALOG_PLANT_DELETE + "')")
    public R<Void> remove(@PathVariable Long id) {
        skuAdminService.remove(id);
        return R.ok();
    }

    /** 上架或下架 */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + Perms.CATALOG_PLANT_PUBLISH + "')")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        skuAdminService.changeStatus(id, status);
        return R.ok();
    }
}
