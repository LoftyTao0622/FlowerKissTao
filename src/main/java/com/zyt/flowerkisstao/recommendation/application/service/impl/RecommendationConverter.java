package com.zyt.flowerkisstao.recommendation.application.service.impl;

import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.recommendation.domain.entity.RecResult;
import com.zyt.flowerkisstao.recommendation.domain.entity.RecResultItem;
import com.zyt.flowerkisstao.recommendation.domain.model.FilterRule;
import com.zyt.flowerkisstao.recommendation.domain.model.ScoreDimension;
import com.zyt.flowerkisstao.recommendation.web.vo.FilterDiagnosticVO;
import com.zyt.flowerkisstao.recommendation.web.vo.RecItemVO;
import com.zyt.flowerkisstao.recommendation.web.vo.RecommendationVO;
import com.zyt.flowerkisstao.recommendation.web.vo.ScoreItemVO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 实体到展示对象的映射。集中放一处，免得同一份文案在服务与控制器里各写一遍。
 */
public final class RecommendationConverter {

    private RecommendationConverter() {
    }

    public static RecommendationVO toVO(RecResult result,
                                 List<RecResultItem> items,
                                 Map<Long, CatalogSpecies> speciesById,
                                 Map<Long, CatalogSku> skuById,
                                 List<String> suggestions) {
        Map<String, Integer> weights = result.getWeightSnapshot();

        return RecommendationVO.builder()
                .resultId(result.getId())
                .profileId(result.getProfileId())
                .sceneName(result.getSceneNameSnapshot())
                .createdAt(result.getCreatedAt())
                .totalCount(result.getTotalCount())
                .candidateCount(result.getCandidateCount())
                .items(items.stream()
                        .map(item -> toItemVO(item, speciesById.get(item.getSpeciesId()),
                                skuById.get(item.getSkuId()), weights))
                        .toList())
                .diagnostics(toDiagnostics(result.getFilteredJson()))
                .suggestions(suggestions)
                .weights(toWeightVOs(weights))
                .build();
    }

    private static RecItemVO toItemVO(RecResultItem item, CatalogSpecies species,
                                      CatalogSku sku, Map<String, Integer> weights) {
        RecItemVO.RecItemVOBuilder builder = RecItemVO.builder()
                .itemId(item.getId())
                .rankNo(item.getRankNo())
                .speciesId(item.getSpeciesId())
                .skuId(item.getSkuId())
                .totalScore(item.getTotalScore())
                .scores(toScoreVOs(item.getScoreJson(), weights))
                .reasons(item.getReasonsJson())
                .risk(item.getRisk());

        // 品种或 SKU 可能在生成推荐之后被下架删除。这时仍返回分数与理由，
        // 只是商品信息为空——历史快照是事实，不该因为商品没了就整条消失
        if (species != null) {
            builder.slug(species.getCode())
                    .name(species.getName())
                    .latinName(species.getLatinName())
                    .careLevel(species.getCareLevel())
                    .careLevelLabel(careLevelLabel(species.getCareLevel()))
                    .lightNote(species.getLightNote())
                    .waterNote(species.getWaterNote())
                    .petFriendly(isNo(species.getToxicCat()) && isNo(species.getToxicDog()));
        }
        if (sku != null) {
            builder.spec(sku.getSpec())
                    .price(sku.getPrice())
                    .image(sku.getImage())
                    .imageAlt(sku.getImageAlt())
                    .stock(sku.getStock());
        }
        return builder.build();
    }

    /** 分项得分配上快照里的权重，前端才能解释"光照 100 分 × 30%" */
    private static List<ScoreItemVO> toScoreVOs(Map<String, Integer> scores, Map<String, Integer> weights) {
        List<ScoreItemVO> result = new ArrayList<>();
        for (ScoreDimension dimension : ScoreDimension.values()) {
            result.add(new ScoreItemVO(
                    dimension.code(),
                    dimension.label(),
                    scores == null ? 0 : scores.getOrDefault(dimension.code(), 0),
                    weights == null ? 0 : weights.getOrDefault(dimension.code(), 0)));
        }
        return result;
    }

    private static List<ScoreItemVO> toWeightVOs(Map<String, Integer> weights) {
        List<ScoreItemVO> result = new ArrayList<>();
        for (ScoreDimension dimension : ScoreDimension.values()) {
            int weight = weights == null ? 0 : weights.getOrDefault(dimension.code(), 0);
            // 权重列表里 score 无意义，置 null 免得前端误当成得分画进度条
            result.add(new ScoreItemVO(dimension.code(), dimension.label(), null, weight));
        }
        return result;
    }

    private static List<FilterDiagnosticVO> toDiagnostics(Map<String, Integer> filtered) {
        List<FilterDiagnosticVO> result = new ArrayList<>();
        for (FilterRule rule : FilterRule.values()) {
            result.add(new FilterDiagnosticVO(
                    rule.code(),
                    rule.label(),
                    filtered == null ? 0 : filtered.getOrDefault(rule.code(), 0),
                    rule.relaxable()));
        }
        return result;
    }

    public static String careLevelLabel(Integer careLevel) {
        if (careLevel == null) {
            return null;
        }
        return switch (careLevel) {
            case 1 -> "新手友好";
            case 2 -> "需要关注";
            case 3 -> "进阶养护";
            default -> null;
        };
    }

    public static BigDecimal scale2(double value) {
        return BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static boolean isNo(Integer flag) {
        return flag == null || flag == 0;
    }
}
