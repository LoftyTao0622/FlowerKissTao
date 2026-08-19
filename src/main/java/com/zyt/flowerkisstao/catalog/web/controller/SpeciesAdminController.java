package com.zyt.flowerkisstao.catalog.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyt.flowerkisstao.catalog.application.service.SpeciesAdminService;
import com.zyt.flowerkisstao.catalog.web.dto.SpeciesSaveDTO;
import com.zyt.flowerkisstao.catalog.web.vo.PlantVO;
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
 * 植物品种管理，仅运营与管理员可用。
 *
 * <p>路径放在 /api/admin 下而不是 /api/catalog/admin：后者会落进
 * SecurityConfig 的 GET /api/catalog/** 白名单，等于把后台列表对游客敞开。
 *
 * <p>读接口用的是 CATALOG_PLANT_UPDATE 而非 _READ——catalog:plant:read
 * 授予了 ROLE_USER，拿它把关会让任何注册用户都能翻到已停用品种。
 *
 * <p>品种与 SKU 沿用同一组 catalog:plant:* 权限点：它们是同一件运营职责的两个
 * 侧面，拆成两套权限只会让角色配置更容易配错。
 */
@RestController
@RequestMapping("/api/admin/species")
@Validated
public class SpeciesAdminController {

    private final SpeciesAdminService speciesAdminService;

    public SpeciesAdminController(SpeciesAdminService speciesAdminService) {
        this.speciesAdminService = speciesAdminService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.CATALOG_PLANT_UPDATE + "')")
    public R<IPage<PlantVO>> page(@RequestParam(defaultValue = "1") long current,
                                  @RequestParam(defaultValue = "10") long size,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Integer status) {
        return R.ok(speciesAdminService.page(new Page<>(current, size), keyword, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.CATALOG_PLANT_UPDATE + "')")
    public R<PlantVO> get(@PathVariable Long id) {
        return R.ok(speciesAdminService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Perms.CATALOG_PLANT_CREATE + "')")
    public R<Long> create(@Valid @RequestBody SpeciesSaveDTO dto) {
        return R.ok(speciesAdminService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.CATALOG_PLANT_UPDATE + "')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SpeciesSaveDTO dto) {
        speciesAdminService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.CATALOG_PLANT_DELETE + "')")
    public R<Void> remove(@PathVariable Long id) {
        speciesAdminService.remove(id);
        return R.ok();
    }

    /** 启用或停用 */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + Perms.CATALOG_PLANT_PUBLISH + "')")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        speciesAdminService.changeStatus(id, status);
        return R.ok();
    }
}
