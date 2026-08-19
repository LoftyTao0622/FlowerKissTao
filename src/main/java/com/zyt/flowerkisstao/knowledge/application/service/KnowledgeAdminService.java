package com.zyt.flowerkisstao.knowledge.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeSearchMiss;
import com.zyt.flowerkisstao.knowledge.domain.service.ArticleTransition;
import com.zyt.flowerkisstao.knowledge.web.dto.ArticleSaveDTO;
import com.zyt.flowerkisstao.knowledge.web.vo.ArticleVO;

import java.util.List;

/**
 * 知识库管理端。方案原文："文章由管理员编辑、分类、打标签并审核发布。"
 *
 * <p>与公开读取分成两个 Service：公开侧把"已发布"兜死在查询条件里，管理侧要能看草稿。
 * 混在一个类里就得到处传"是不是管理员"，那是最容易漏判的写法。
 */
public interface KnowledgeAdminService {

    /** 全部文章，含草稿。status 传 null 表示全部 */
    IPage<ArticleVO> page(IPage<?> page, Integer status, String keyword);

    ArticleVO get(Long id);

    Long create(ArticleSaveDTO dto);

    void update(Long id, ArticleSaveDTO dto);

    void remove(Long id);

    /** 执行一次状态流转 */
    void transition(Long id, ArticleTransition.Action action);

    /** 无结果关键词清单，按搜索次数倒序。方案要求的"后台内容需求清单" */
    List<KnowledgeSearchMiss> searchMisses(int limit);
}
