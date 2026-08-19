package com.zyt.flowerkisstao.knowledge.domain.service;

import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeArticle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 知识内容的个性化推荐。
 *
 * <p>方案原文："系统依据用户已购植物和近期养护行为推荐相关内容。"
 *
 * <p>与 {@code RecommendationEngine}、{@code CareTaskPlanner} 同一套路：不依赖 Spring、
 * 不碰数据库的纯函数。入参是候选文章 + 用户画像信号，出参是排好序的推荐列表，
 * 同样的输入必然得到同样的输出。
 */
public final class KnowledgeRecommender {

    /**
     * 用户侧的信号。
     *
     * @param ownedSpeciesCodes 已购品种的 code（来自 care_archive → catalog_species）
     * @param recentTaskTypes   近期养护任务类型（来自 care_task，含待办与刚做过的）
     * @param overdueTaskTypes  逾期或被跳过的任务类型——这些最需要解释"为什么要做"
     * @param readArticleIds    已经读过的文章，降权避免反复推同一篇
     */
    public record UserSignals(Set<String> ownedSpeciesCodes,
                              Set<String> recentTaskTypes,
                              Set<String> overdueTaskTypes,
                              Set<Long> readArticleIds) {

        /** 什么信号都没有：新用户，还没买过东西 */
        public boolean isEmpty() {
            return ownedSpeciesCodes.isEmpty() && recentTaskTypes.isEmpty()
                    && overdueTaskTypes.isEmpty();
        }
    }

    /**
     * 一条推荐结果。
     *
     * @param article 文章
     * @param score   相关度得分，仅用于排序
     * @param reason  为什么推它，界面上直接显示——不解释理由的推荐等于随机排列
     */
    public record Scored(KnowledgeArticle article, int score, String reason) {
    }

    // ===== 权重。数值本身不重要，重要的是它们的相对大小 =====

    /** 讲的正是你养的那个品种 */
    private static final int W_SPECIES = 50;
    /** 对应你逾期或跳过的任务——最需要看的就是这类 */
    private static final int W_OVERDUE_TASK = 40;
    /** 对应你近期的任务 */
    private static final int W_RECENT_TASK = 25;
    /** 入门难度加一点分：推荐给正在养的人，先给能立刻用上的 */
    private static final int W_EASY = 5;
    /** 已读过的降权，但不排除——复习也有价值 */
    private static final int W_ALREADY_READ = -30;

    private KnowledgeRecommender() {
    }

    /**
     * 挑出最相关的几篇。
     *
     * @param candidates 候选文章（调用方保证都是已发布的）
     * @param signals    用户信号
     * @param limit      返回条数
     */
    public static List<Scored> recommend(List<KnowledgeArticle> candidates,
                                         UserSignals signals, int limit) {
        if (candidates.isEmpty() || limit <= 0) {
            return List.of();
        }

        // 没有任何信号时退回热门：新用户也该看到内容，而不是一片空白
        if (signals.isEmpty()) {
            return candidates.stream()
                    .sorted(popularityOrder())
                    .limit(limit)
                    .map(article -> new Scored(article, 0, "热门养护知识"))
                    .toList();
        }

        List<Scored> scored = new ArrayList<>();
        for (KnowledgeArticle article : candidates) {
            Match match = match(article, signals);
            // 判定"要不要收"用的是命中信号后的相关度，不是最终排序分。
            // 两者分开是有意的：已读降权只该把文章往后排，不该让它凭空消失——
            // 若用最终分判定，一篇只命中一个信号又恰好读过的文章会被减成 0 而被丢掉
            if (match.relevance() > 0) {
                scored.add(new Scored(article, match.rankScore(), match.reason()));
            }
        }

        scored.sort(Comparator
                .comparingInt(Scored::score).reversed()
                // 同分时按热度，最后用 id 兜底——保证同样输入的输出顺序稳定可复现
                .thenComparingInt(s -> -safe(s.article().getUsefulCount()))
                .thenComparingInt(s -> -safe(s.article().getViewCount()))
                .thenComparing(s -> s.article().getId()));

        if (scored.size() > limit) {
            return new ArrayList<>(scored.subList(0, limit));
        }
        // 相关的不够时用热门补齐，免得推荐位空着
        if (scored.size() < limit) {
            Set<Long> picked = scored.stream().map(s -> s.article().getId())
                    .collect(java.util.stream.Collectors.toSet());
            candidates.stream()
                    .filter(article -> !picked.contains(article.getId()))
                    .sorted(popularityOrder())
                    .limit((long) limit - scored.size())
                    .forEach(article -> scored.add(new Scored(article, 0, "热门养护知识")));
        }
        return scored;
    }

    /**
     * 一次匹配的中间结果。
     *
     * @param relevance 命中信号得到的相关度。&gt; 0 才进推荐
     * @param rankScore 排序用的最终分，含已读降权，可能为负
     * @param reason    推荐理由
     */
    private record Match(int relevance, int rankScore, String reason) {
    }

    /** 给一篇文章打分，并说明为什么推它 */
    private static Match match(KnowledgeArticle article, UserSignals signals) {
        int relevance = 0;
        String reason = null;

        // 1. 品种命中最强：讲的正是你家那株
        String hitSpecies = firstHit(article.getRelatedSpecies(), signals.ownedSpeciesCodes());
        if (hitSpecies != null) {
            relevance += W_SPECIES;
            reason = "与你养的植物直接相关";
        }

        // 2. 逾期/跳过的任务次之：用户没做这件事，很可能是不知道为什么要做
        String hitOverdue = firstHit(article.getRelatedTaskTypes(), signals.overdueTaskTypes());
        if (hitOverdue != null) {
            relevance += W_OVERDUE_TASK;
            if (reason == null) {
                reason = "你有一项相关养护任务还没完成";
            }
        }

        // 3. 近期任务。与逾期命中同一个类型时不重复加分——那本来就是同一件事
        String hitRecent = firstHit(article.getRelatedTaskTypes(), signals.recentTaskTypes());
        if (hitRecent != null && !hitRecent.equals(hitOverdue)) {
            relevance += W_RECENT_TASK;
            if (reason == null) {
                reason = "对应你近期的养护安排";
            }
        }

        if (relevance > 0 && safe(article.getDifficulty()) <= 1) {
            relevance += W_EASY;
        }

        int rankScore = relevance;
        if (signals.readArticleIds().contains(article.getId())) {
            rankScore += W_ALREADY_READ;
            if (reason != null) {
                reason = reason + "（你读过）";
            }
        }

        return new Match(relevance, rankScore, reason == null ? "推荐阅读" : reason);
    }

    /** 热门排序：有用数优先，其次浏览数，最后 id 兜底保证稳定 */
    private static Comparator<KnowledgeArticle> popularityOrder() {
        return Comparator
                .comparingInt((KnowledgeArticle a) -> safe(a.getUsefulCount())).reversed()
                .thenComparing(Comparator.comparingInt(
                        (KnowledgeArticle a) -> safe(a.getViewCount())).reversed())
                .thenComparing(KnowledgeArticle::getId);
    }

    /** 两个集合的第一个交集元素，没有则 null */
    private static String firstHit(List<String> articleValues, Set<String> userValues) {
        if (articleValues == null || articleValues.isEmpty() || userValues.isEmpty()) {
            return null;
        }
        for (String value : articleValues) {
            if (userValues.contains(value)) {
                return value;
            }
        }
        return null;
    }

    private static int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
