package com.zyt.flowerkisstao.recommendation.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 一次推荐的完整结果。
 *
 * <p>{@link #items} 为空时 {@link #diagnostics} 与 {@link #suggestions} 必然非空——
 * 方案要求"无候选时仅建议放宽非安全约束"，前端据此展示漏斗而不是一片空白。
 */
@Data
@Builder
public class RecommendationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** rec_result.id */
    private Long resultId;

    private Long profileId;

    /** 生成时的场景名。画像改名后历史记录仍显示当时那个名字 */
    private String sceneName;

    private LocalDateTime createdAt;

    /** 参与筛选的在售品种总数 */
    private Integer totalCount;

    /** 硬过滤后的候选数。注意它可能大于 items.size()，后者受 topN 限制 */
    private Integer candidateCount;

    /** Top-N 结果，已排好序 */
    private List<RecItemVO> items;

    /** 漏斗诊断，六条规则各排除了多少株。有候选时也返回，可用于"为什么只有 3 条" */
    private List<FilterDiagnosticVO> diagnostics;

    /** 无候选时的放宽建议，只含非安全约束。有候选时为空列表 */
    private List<String> suggestions;

    /** 本次使用的权重，取自快照。改过权重后历史推荐仍按当时的解释 */
    private List<ScoreItemVO> weights;
}
