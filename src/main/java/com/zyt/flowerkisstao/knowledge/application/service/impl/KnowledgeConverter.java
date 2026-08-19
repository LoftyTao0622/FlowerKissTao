package com.zyt.flowerkisstao.knowledge.application.service.impl;

import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeArticle;
import com.zyt.flowerkisstao.knowledge.domain.model.ArticleCategory;
import com.zyt.flowerkisstao.knowledge.domain.model.ArticleStatus;
import com.zyt.flowerkisstao.knowledge.domain.service.ArticleTransition;
import com.zyt.flowerkisstao.knowledge.web.vo.ArticleVO;

import java.util.List;

/**
 * 文章实体到展示对象的映射。集中放一处，免得同一份文案在服务与控制器里各写一遍。
 */
final class KnowledgeConverter {

    private KnowledgeConverter() {
    }

    /**
     * 列表用的摘要形态。
     *
     * <p>刻意不带 steps / mistakes / risks：一页 10 篇每篇 4 个步骤加两段误区，
     * 列表响应会比详情页还大，而卡片上一个字都用不到。
     */
    static ArticleVO toSummary(KnowledgeArticle article) {
        return baseBuilder(article).build();
    }

    /** 详情形态，带齐方案要求的四件套 */
    static ArticleVO toDetail(KnowledgeArticle article, boolean marked, boolean favorited) {
        return baseBuilder(article)
                .applicable(article.getApplicable())
                .frequency(article.getFrequency())
                .steps(article.getSteps())
                .mistakes(article.getMistakes())
                .risks(article.getRisks())
                .relatedTaskTypes(article.getRelatedTaskTypes())
                .relatedSpecies(article.getRelatedSpecies())
                .marked(marked)
                .favorited(favorited)
                .build();
    }

    /** 管理端形态：带状态与可执行动作，且不管发布与否都给全文 */
    static ArticleVO toAdmin(KnowledgeArticle article, ArticleTransition.Actor actor) {
        ArticleStatus status = ArticleStatus.of(article.getStatus());
        return baseBuilder(article)
                .applicable(article.getApplicable())
                .frequency(article.getFrequency())
                .steps(article.getSteps())
                .mistakes(article.getMistakes())
                .risks(article.getRisks())
                .relatedTaskTypes(article.getRelatedTaskTypes())
                .relatedSpecies(article.getRelatedSpecies())
                .status(status.code())
                .statusLabel(status.label())
                .updatedAt(article.getUpdatedAt())
                // 由后端算好当前能做什么，界面上就不会出现点下去必然报错的按钮
                .actions(ArticleTransition.availableActions(status, actor).stream()
                        .map(action -> ArticleVO.ActionVO.builder()
                                .code(action.name())
                                .label(action.label())
                                .build())
                        .toList())
                .build();
    }

    private static ArticleVO.ArticleVOBuilder baseBuilder(KnowledgeArticle article) {
        return ArticleVO.builder()
                .id(article.getId())
                .slug(article.getSlug())
                .title(article.getTitle())
                .summary(article.getSummary())
                .cover(article.getCover())
                .category(article.getCategory())
                .categoryLabel(ArticleCategory.labelOf(article.getCategory()))
                .difficulty(article.getDifficulty())
                .difficultyLabel(difficultyLabel(article.getDifficulty()))
                .seasons(article.getSeasons())
                .tags(article.getTags())
                .viewCount(article.getViewCount())
                .usefulCount(article.getUsefulCount())
                .marked(false)
                .favorited(false)
                .publishedAt(article.getPublishedAt());
    }

    static String difficultyLabel(Integer difficulty) {
        if (difficulty == null) {
            return null;
        }
        return switch (difficulty) {
            case 1 -> "入门";
            case 2 -> "进阶";
            case 3 -> "专业";
            default -> null;
        };
    }

    /** 季节 code 到中文，筛选框与详情页共用 */
    static String seasonLabel(String season) {
        return switch (season == null ? "" : season) {
            case "spring" -> "春季";
            case "summer" -> "夏季";
            case "autumn" -> "秋季";
            case "winter" -> "冬季";
            default -> season;
        };
    }

    static List<String> allSeasons() {
        return List.of("spring", "summer", "autumn", "winter");
    }
}
