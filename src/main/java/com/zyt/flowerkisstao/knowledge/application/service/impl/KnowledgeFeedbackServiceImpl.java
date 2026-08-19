package com.zyt.flowerkisstao.knowledge.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.knowledge.application.service.KnowledgeFeedbackService;
import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeArticle;
import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeFeedback;
import com.zyt.flowerkisstao.knowledge.infrastructure.mapper.KnowledgeArticleMapper;
import com.zyt.flowerkisstao.knowledge.infrastructure.mapper.KnowledgeFeedbackMapper;
import com.zyt.flowerkisstao.knowledge.web.vo.ArticleVO;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeFeedbackServiceImpl implements KnowledgeFeedbackService {

    private final KnowledgeFeedbackMapper feedbackMapper;
    private final KnowledgeArticleMapper articleMapper;

    public KnowledgeFeedbackServiceImpl(KnowledgeFeedbackMapper feedbackMapper,
                                        KnowledgeArticleMapper articleMapper) {
        this.feedbackMapper = feedbackMapper;
        this.articleMapper = articleMapper;
    }

    /**
     * 切换"有用"。
     *
     * <p>计数用 SQL 自增而不是"查一遍再写回"：并发下后者会丢计数。
     * 插入撞唯一键时按"已经点过"处理并返回 true——用户连点两次的结果应当是
     * 幂等的"已标记"，而不是一个报错弹窗。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleUseful(Long articleId) {
        return toggle(articleId, KnowledgeFeedback.TYPE_USEFUL, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long articleId) {
        return toggle(articleId, KnowledgeFeedback.TYPE_FAVORITE, false);
    }

    private boolean toggle(Long articleId, String type, boolean countsOnArticle) {
        Long userId = CurrentUser.requireUserId();
        requireArticle(articleId);

        KnowledgeFeedback existing = feedbackMapper.selectOne(
                Wrappers.<KnowledgeFeedback>lambdaQuery()
                        .eq(KnowledgeFeedback::getUserId, userId)
                        .eq(KnowledgeFeedback::getArticleId, articleId)
                        .eq(KnowledgeFeedback::getType, type)
                        .last("LIMIT 1"));

        if (existing != null) {
            feedbackMapper.deleteById(existing.getId());
            if (countsOnArticle) {
                articleMapper.addUsefulCount(articleId, -1);
            }
            return false;
        }

        KnowledgeFeedback feedback = new KnowledgeFeedback();
        feedback.setUserId(userId);
        feedback.setArticleId(articleId);
        feedback.setType(type);
        try {
            feedbackMapper.insert(feedback);
        } catch (DuplicateKeyException e) {
            // 并发下两个请求同时查到"没点过"，一个插成功另一个撞键。
            // 对用户而言结果一样是"已标记"，不必报错
            return true;
        }
        if (countsOnArticle) {
            articleMapper.addUsefulCount(articleId, 1);
        }
        return true;
    }

    @Override
    public List<ArticleVO> myFavorites() {
        Long userId = CurrentUser.requireUserId();

        List<Long> articleIds = feedbackMapper.selectList(
                        Wrappers.<KnowledgeFeedback>lambdaQuery()
                                .eq(KnowledgeFeedback::getUserId, userId)
                                .eq(KnowledgeFeedback::getType, KnowledgeFeedback.TYPE_FAVORITE)
                                .orderByDesc(KnowledgeFeedback::getId)).stream()
                .map(KnowledgeFeedback::getArticleId)
                .toList();
        if (articleIds.isEmpty()) {
            return List.of();
        }

        // 收藏后文章被下架时不再展示，但收藏记录保留——重新上线后还在
        return articleMapper.selectBatchIds(articleIds).stream()
                .filter(article -> article.getStatus() != null && article.getStatus() == 2)
                .map(KnowledgeConverter::toSummary)
                .peek(vo -> vo.setFavorited(true))
                .toList();
    }

    private KnowledgeArticle requireArticle(Long articleId) {
        KnowledgeArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BizException(ErrorCode.ARTICLE_NOT_FOUND, "文章不存在或已下架");
        }
        return article;
    }
}
