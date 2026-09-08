package com.zyt.flowerkisstao.knowledge;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zyt.flowerkisstao.knowledge.application.service.impl.KnowledgeFeedbackServiceImpl;
import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeArticle;
import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeFeedback;
import com.zyt.flowerkisstao.knowledge.infrastructure.mapper.KnowledgeArticleMapper;
import com.zyt.flowerkisstao.knowledge.infrastructure.mapper.KnowledgeFeedbackMapper;
import com.zyt.flowerkisstao.shared.config.RedisProperties;
import com.zyt.flowerkisstao.shared.redis.AfterCommitCacheInvalidator;
import com.zyt.flowerkisstao.shared.redis.RedisKey;
import com.zyt.flowerkisstao.shared.security.AppUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeFeedbackServiceTest {

    private KnowledgeFeedbackMapper feedbackMapper;
    private KnowledgeArticleMapper articleMapper;
    private KnowledgeFeedbackServiceImpl service;

    @BeforeEach
    void setUp() {
        feedbackMapper = mock(KnowledgeFeedbackMapper.class);
        articleMapper = mock(KnowledgeArticleMapper.class);
        service = new KnowledgeFeedbackServiceImpl(
                feedbackMapper, articleMapper, mock(AfterCommitCacheInvalidator.class),
                new RedisKey(new RedisProperties()));

        AppUserDetails principal = new AppUserDetails(
                7L, "tester", "", 1, Set.of("ROLE_USER"), Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void concurrentLosingDeleteDoesNotDecrementUsefulCountAgain() {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(11L);
        KnowledgeFeedback existing = new KnowledgeFeedback();
        existing.setId(23L);
        existing.setUserId(7L);
        existing.setArticleId(11L);
        existing.setType(KnowledgeFeedback.TYPE_USEFUL);

        when(articleMapper.selectById(11L)).thenReturn(article);
        when(feedbackMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(feedbackMapper.deleteById(23L)).thenReturn(0);

        assertThat(service.toggleUseful(11L)).isFalse();
        verify(articleMapper, never()).addUsefulCount(anyLong(), anyInt());
    }
}
