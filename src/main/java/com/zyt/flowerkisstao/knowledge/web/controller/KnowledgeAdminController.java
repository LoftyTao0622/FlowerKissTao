package com.zyt.flowerkisstao.knowledge.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyt.flowerkisstao.knowledge.application.service.KnowledgeAdminService;
import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeSearchMiss;
import com.zyt.flowerkisstao.knowledge.domain.service.ArticleTransition;
import com.zyt.flowerkisstao.knowledge.web.dto.ArticleSaveDTO;
import com.zyt.flowerkisstao.knowledge.web.vo.ArticleVO;
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

import java.util.List;

/**
 * 知识库管理端。方案原文："文章由管理员编辑、分类、打标签并审核发布。"
 *
 * <p>路径挂 /api/admin/knowledge，不落公开白名单前缀，自动要求登录。
 * 编辑类动作要 {@code knowledge:article:write}，审核发布类要 {@code publish}——
 * 两个权限点分开挂，将来加"只能写不能发"的角色时一行都不用改。
 */
@RestController
@RequestMapping("/api/admin/knowledge")
@Validated
public class KnowledgeAdminController {

    private final KnowledgeAdminService adminService;

    public KnowledgeAdminController(KnowledgeAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/articles")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_WRITE + "')")
    public R<IPage<ArticleVO>> page(@RequestParam(defaultValue = "1") long current,
                                    @RequestParam(defaultValue = "10") long size,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) String keyword) {
        return R.ok(adminService.page(new Page<>(current, size), status, keyword));
    }

    @GetMapping("/articles/{id}")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_WRITE + "')")
    public R<ArticleVO> get(@PathVariable Long id) {
        return R.ok(adminService.get(id));
    }

    @PostMapping("/articles")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_WRITE + "')")
    public R<Long> create(@Valid @RequestBody ArticleSaveDTO dto) {
        return R.ok(adminService.create(dto));
    }

    @PutMapping("/articles/{id}")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_WRITE + "')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleSaveDTO dto) {
        adminService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/articles/{id}")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_WRITE + "')")
    public R<Void> remove(@PathVariable Long id) {
        adminService.remove(id);
        return R.ok();
    }

    // ===== 作者动作 =====

    @PutMapping("/articles/{id}/submit")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_WRITE + "')")
    public R<Void> submit(@PathVariable Long id) {
        adminService.transition(id, ArticleTransition.Action.SUBMIT);
        return R.ok();
    }

    @PutMapping("/articles/{id}/withdraw")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_WRITE + "')")
    public R<Void> withdraw(@PathVariable Long id) {
        adminService.transition(id, ArticleTransition.Action.WITHDRAW);
        return R.ok();
    }

    // ===== 审核动作 =====

    @PutMapping("/articles/{id}/publish")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_PUBLISH + "')")
    public R<Void> publish(@PathVariable Long id) {
        adminService.transition(id, ArticleTransition.Action.PUBLISH);
        return R.ok();
    }

    @PutMapping("/articles/{id}/reject")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_PUBLISH + "')")
    public R<Void> reject(@PathVariable Long id) {
        adminService.transition(id, ArticleTransition.Action.REJECT);
        return R.ok();
    }

    @PutMapping("/articles/{id}/offline")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_PUBLISH + "')")
    public R<Void> offline(@PathVariable Long id) {
        adminService.transition(id, ArticleTransition.Action.OFFLINE);
        return R.ok();
    }

    /**
     * 无结果关键词清单。方案原文："高频搜索但无结果的关键词会进入后台内容需求清单。"
     *
     * <p>这份清单把"用户想知道什么而我们没写"变成可排序的待办。
     */
    @GetMapping("/search-misses")
    @PreAuthorize("hasAuthority('" + Perms.KNOWLEDGE_ARTICLE_WRITE + "')")
    public R<List<KnowledgeSearchMiss>> searchMisses(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(adminService.searchMisses(limit));
    }
}
