package com.zyt.flowerkisstao.catalog.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyt.flowerkisstao.catalog.application.service.PlantQueryService;
import com.zyt.flowerkisstao.catalog.web.vo.PlantFacetsVO;
import com.zyt.flowerkisstao.catalog.web.vo.PlantVO;
import com.zyt.flowerkisstao.shared.web.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向顾客的植物浏览接口。
 *
 * <p>这里刻意不写 @PreAuthorize：SecurityConfig 已把 GET /api/catalog/** 放进白名单，
 * 游客本就该看得到商品。若挂上 catalog:plant:read，反而会把所有未登录访客挡在 403，
 * 因为那个权限点只授予了登录角色。
 *
 * <p>筛选项走 /api/catalog/plant-facets 而不是 /plants/facets，
 * 免得将来出现一株 slug 恰好叫 facets 的植物把它遮掉。
 */
@RestController
@RequestMapping("/api/catalog")
public class PlantController {

    private final PlantQueryService plantQueryService;

    public PlantController(PlantQueryService plantQueryService) {
        this.plantQueryService = plantQueryService;
    }

    @GetMapping("/plants")
    public R<IPage<PlantVO>> page(@RequestParam(defaultValue = "1") long current,
                                  @RequestParam(defaultValue = "12") long size,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(required = false) String light,
                                  @RequestParam(required = false) Boolean featured) {
        return R.ok(plantQueryService.page(new Page<>(current, size), keyword, category, light, featured));
    }

    @GetMapping("/plants/{slug}")
    public R<PlantVO> detail(@PathVariable String slug) {
        return R.ok(plantQueryService.getBySlug(slug));
    }

    @GetMapping("/plant-facets")
    public R<PlantFacetsVO> facets() {
        return R.ok(plantQueryService.facets());
    }
}
