package com.zyt.flowerkisstao.knowledge.application.service;

import com.zyt.flowerkisstao.knowledge.web.vo.ArticleVO;

import java.util.List;

/**
 * 收藏与有用性反馈。方案原文："支持收藏、评论和有用性反馈。"
 */
public interface KnowledgeFeedbackService {

    /**
     * 切换"有用"。已经点过就取消，没点过就加上。
     *
     * @return 操作后是否处于"已标记"状态
     */
    boolean toggleUseful(Long articleId);

    /** 切换收藏 */
    boolean toggleFavorite(Long articleId);

    /** 我的收藏 */
    List<ArticleVO> myFavorites();
}
