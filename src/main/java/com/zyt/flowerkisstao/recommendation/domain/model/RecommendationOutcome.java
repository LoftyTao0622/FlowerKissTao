package com.zyt.flowerkisstao.recommendation.domain.model;

import java.util.List;
import java.util.Map;

/**
 * 打分引擎的完整产出。
 *
 * <p>候选为空时 {@link #items} 是空列表，但 {@link #filtered} 仍然完整——前端据此
 * 说出"是预算卡掉了 6 株"，而不是干巴巴一句"没有结果"。
 *
 * @param items          Top-N 结果，已按总分降序、同分按方案规定的次序排好
 * @param totalCount     参与筛选的品种总数
 * @param candidateCount 硬过滤后剩余的候选数。<b>与 {@code items.size()} 不是一回事</b>：
 *                       后者被 topN 截断过。诊断要说的是"筛出来 7 株，展示前 3 株"，
 *                       用 items.size() 会让漏斗的数字对不上总数
 * @param filtered       各条硬规则各排除了多少株，键为 {@link FilterRule#code()}
 * @param suggestions    无候选时的放宽建议，只含可放宽的规则；有候选时为空列表
 */
public record RecommendationOutcome(List<ScoredPlant> items,
                                    int totalCount,
                                    int candidateCount,
                                    Map<String, Integer> filtered,
                                    List<String> suggestions) {

    /**
     * 恒等式：总数 = 候选数 + 各规则排除数之和。
     *
     * <p>硬过滤每株只记第一条命中的规则，所以两边必然相等。不等就说明计数漏了
     * 或重了，那时前端展示的漏斗是错的。
     */
    public boolean isFunnelConsistent() {
        int excluded = filtered.values().stream().mapToInt(Integer::intValue).sum();
        return totalCount == candidateCount + excluded;
    }
}
