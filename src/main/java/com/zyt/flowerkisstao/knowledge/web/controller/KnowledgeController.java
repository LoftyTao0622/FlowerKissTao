package com.zyt.flowerkisstao.knowledge.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyt.flowerkisstao.knowledge.application.service.KnowledgeFeedbackService;
import com.zyt.flowerkisstao.knowledge.application.service.KnowledgeQueryService;
import com.zyt.flowerkisstao.knowledge.web.dto.ArticleQueryDTO;
import com.zyt.flowerkisstao.knowledge.web.vo.ArticleFacetsVO;
import com.zyt.flowerkisstao.knowledge.web.vo.ArticleVO;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库，读者侧。
 *
 * <p>路径挂 /api/knowledge，其 GET 早已在 SecurityConfig 的公开白名单里——
 * 知识科普对未注册用户开放是既定设计，不加 {@code @PreAuthorize}。
 *
 * <p>需要身份的三个接口（推荐、收藏、有用）才挂 {@code knowledge:article:read}。
 */
@RestController
@RequestMapping("/api/knowledge")
@Validated
public class KnowledgeController {

    private final KnowledgeQueryService queryService;
    private final KnowledgeFeedbackService feedbackService;

    public KnowledgeController(KnowledgeQueryService queryService,
                               KnowledgeFeedbackService feedbackService) {
        this.queryService = queryService;
        this.feedbackService = feedbackService;
    }

    /** 多维检索。方案："按植物名称、问题症状、养护难度和季节检索" */
    @GetMapping("/articles")
    public R<IPage<ArticleVO>> page(@RequestParam(defaultValue = "1") long current,
                                    @RequestParam(defaultValue = "9") long size,
                                    ArticleQueryDTO query) {
        return R.ok(queryService.page(new Page<>(current, size), query));
    }

    /** 详情，按 slug 而非 id——与商品详情页同一约定，URL 可读 */
    @GetMapping("/articles/{slug}")
    public R<ArticleVO> get(@PathVariable String slug) {
        return R.ok(queryService.getBySlug(slug));
    }

    /** 筛选项，检索页下拉框数据源 */
    @GetMapping("/facets")
    public R<ArticleFacetsVO> facets() {
        return R.ok(queryService.facets());
    }

    /**
     * 依据已购植物与近期养护行为推荐。
     *
     * <p>不挂权限：游客也能调，只是拿到的是热门内容。挂上反而会让首页推荐位
     * 对未登录用户空一块。
     */
    @GetMapping("/recommended")
    public R<List<ArticleVO>> recommended(@RequestParam(defaultValue = "4") int limit) {
        return R.ok(queryService.recommended(limit));
    }

    /** 标记有用。再点一次取消 */
    @PostMapping("/articles/{id}/useful")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_READ + "')")
    public R<Boolean> toggleUseful(@PathVariable Long id) {
        return R.ok(feedbackService.toggleUseful(id));
    }

    /** 收藏。再点一次取消 */
    @PostMapping("/articles/{id}/favorite")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_READ + "')")
    public R<Boolean> toggleFavorite(@PathVariable Long id) {
        return R.ok(feedbackService.toggleFavorite(id));
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_READ + "')")
    public R<List<ArticleVO>> myFavorites() {
        return R.ok(feedbackService.myFavorites());
    }
}
