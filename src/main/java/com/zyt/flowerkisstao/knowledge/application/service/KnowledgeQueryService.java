package com.zyt.flowerkisstao.knowledge.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zyt.flowerkisstao.knowledge.web.dto.ArticleQueryDTO;
import com.zyt.flowerkisstao.knowledge.web.vo.ArticleFacetsVO;
import com.zyt.flowerkisstao.knowledge.web.vo.ArticleVO;

import java.util.List;

/**
 * 知识库的公开读取。
 *
 * <p>GET 已在 SecurityConfig 的公开白名单里，游客也能读——知识科普对未注册用户开放
 * 本身就是拉新的一环。个性化推荐与收藏状态需要登录，无登录时降级为通用内容。
 */
public interface KnowledgeQueryService {

    /**
     * 多维检索。方案原文："用户可按植物名称、问题症状、养护难度和季节检索。"
     *
     * <p>关键词无结果时会记进 {@code knowledge_search_miss}，作为后台内容需求清单。
     */
    IPage<ArticleVO> page(IPage<?> page, ArticleQueryDTO query);

    /** 详情，按 slug。浏览数 +1 */
    ArticleVO getBySlug(String slug);

    /** 筛选项，检索页的下拉框数据源 */
    ArticleFacetsVO facets();

    /**
     * 个性化推荐。方案原文："系统依据用户已购植物和近期养护行为推荐相关内容。"
     *
     * <p>未登录或没有养护记录时退回热门，不会返回空列表。
     */
    List<ArticleVO> recommended(int limit);
}
