package com.zyt.flowerkisstao.knowledge.web.vo;

import com.zyt.flowerkisstao.knowledge.domain.entity.ArticleStep;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章。列表与详情共用一份：列表时 {@link #steps} / {@link #mistakes} / {@link #risks}
 * 为空，只带摘要级字段。
 */
@Data
@Builder
public class ArticleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 详情页路由用的就是它 */
    private String slug;

    private String title;

    private String summary;

    private String cover;

    private String category;

    private String categoryLabel;

    private Integer difficulty;

    private String difficultyLabel;

    private List<String> seasons;

    private List<String> tags;

    // ===== 方案要求的步骤化四件套，详情才带 =====

    private String applicable;

    private String frequency;

    private List<ArticleStep> steps;

    private List<String> mistakes;

    private List<String> risks;

    // ===== 关联 =====

    private List<String> relatedTaskTypes;

    private List<String> relatedSpecies;

    private Integer viewCount;

    private Integer usefulCount;

    /** 当前登录人是否点过"有用"。游客恒为 false */
    private Boolean marked;

    /** 当前登录人是否收藏过 */
    private Boolean favorited;

    private LocalDateTime publishedAt;

    /**
     * 为什么推荐给你，只有个性化推荐接口会填。
     *
     * <p>不解释理由的推荐等于随机排列，所以这一条必须跟着结果一起返回。
     */
    private String recommendReason;

    // ===== 管理端才带 =====

    private Integer status;

    private String statusLabel;

    /** 当前角色在这个状态下能做的动作，由后端算好 */
    private List<ActionVO> actions;

    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class ActionVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /** SUBMIT / WITHDRAW / PUBLISH / REJECT / OFFLINE */
        private String code;

        private String label;
    }
}
