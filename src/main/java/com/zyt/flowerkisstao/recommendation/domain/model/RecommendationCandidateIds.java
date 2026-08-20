package com.zyt.flowerkisstao.recommendation.domain.model;

import java.util.List;

/** 推荐候选缓存，只保存稳定的品种 ID，不缓存价格、库存等业务事实。 */
public record RecommendationCandidateIds(List<Long> speciesIds) {
}
