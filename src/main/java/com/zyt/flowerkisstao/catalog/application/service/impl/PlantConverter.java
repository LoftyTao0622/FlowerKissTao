package com.zyt.flowerkisstao.catalog.application.service.impl;

import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.catalog.web.vo.PlantVO;
import com.zyt.flowerkisstao.catalog.web.vo.SkuVO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 品种 + SKU 到 VO 的转换。多个 Service 都要用，抽出来避免各写一份后字段漏配。
 */
public final class PlantConverter {

    private PlantConverter() {
    }

    /**
     * 把品种与它的 SKU 列表拍平成一个 PlantVO。
     *
     * @param skus 该品种下的上架 SKU，按 sort 降序排列；第一个即默认 SKU。
     *             允许为空——管理端要能看到还没配 SKU 的品种，公开接口则在
     *             查询阶段就已经把这类品种排除了。
     */
    public static PlantVO toVO(CatalogSpecies species, List<CatalogSku> skus) {
        List<CatalogSku> safeSkus = skus == null ? Collections.emptyList() : skus;
        CatalogSku defaultSku = safeSkus.isEmpty() ? null : safeSkus.get(0);

        boolean toxicCat = isTrue(species.getToxicCat());
        boolean toxicDog = isTrue(species.getToxicDog());

        return PlantVO.builder()
                // ===== 与拆表前一致的字段 =====
                .id(species.getId())
                .slug(species.getCode())
                .name(species.getName())
                .latinName(species.getLatinName())
                .price(defaultSku == null ? null : defaultSku.getPrice())
                .image(defaultSku == null ? null : defaultSku.getImage())
                .imageAlt(defaultSku == null ? null : defaultSku.getImageAlt())
                .category(species.getCategory())
                .light(species.getLightNote())
                .watering(species.getWaterNote())
                .size(defaultSku == null ? null : defaultSku.getSpec())
                .petFriendly(!toxicCat && !toxicDog)
                .petNote(petNote(toxicCat, toxicDog))
                .difficulty(difficultyLabel(species.getCareLevel()))
                .matchTags(orEmpty(species.getMatchTags()))
                .recommendationReason(species.getRecommendationReason())
                .shortDescription(species.getShortDescription())
                .description(species.getDescription())
                .careTips(orEmpty(species.getCareTips()))
                // 任一上架 SKU 精选即算精选，与 PlantQueryServiceImpl 里 featured 筛选的
                // exists 子查询口径一致。若只看默认 SKU，会出现"筛得出来但显示不精选"
                .featured(safeSkus.stream().anyMatch(sku -> isTrue(sku.getFeatured())))
                .status(species.getStatus())
                // ===== 拆表后新增 =====
                .defaultSkuId(defaultSku == null ? null : defaultSku.getId())
                .skus(safeSkus.stream().map(PlantConverter::toSkuVO).collect(Collectors.toList()))
                .ornamentalType(species.getOrnamentalType())
                .bloomSeason(species.getBloomSeason())
                .bloomColor(species.getBloomColor())
                .fragrance(species.getFragrance())
                .lightMin(species.getLightMin())
                .lightMax(species.getLightMax())
                .tempMin(species.getTempMin())
                .tempMax(species.getTempMax())
                .humidityMin(species.getHumidityMin())
                .humidityMax(species.getHumidityMax())
                .waterIntervalDays(species.getWaterIntervalDays())
                .fertilizeIntervalDays(species.getFertilizeIntervalDays())
                .repotIntervalMonths(species.getRepotIntervalMonths())
                .pruneNeeded(isTrue(species.getPruneNeeded()))
                .careLevel(species.getCareLevel())
                .toxicCat(toxicCat)
                .toxicDog(toxicDog)
                .toxicChild(isTrue(species.getToxicChild()))
                .pollenRisk(isTrue(species.getPollenRisk()))
                .matureHeightCm(species.getMatureHeightCm())
                .footprintCm(species.getFootprintCm())
                .build();
    }

    public static SkuVO toSkuVO(CatalogSku sku) {
        return SkuVO.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .spec(sku.getSpec())
                .pot(sku.getPot())
                .price(sku.getPrice())
                .stock(sku.getStock())
                .image(sku.getImage())
                .imageAlt(sku.getImageAlt())
                .featured(isTrue(sku.getFeatured()))
                .status(sku.getStatus())
                .build();
    }

    /**
     * 拆表前 pet_note 是一个手填列，拆表后改由毒性组合派生。
     *
     * <p>分猫狗给出不同措辞，而不是一律"宠物需隔离"：只对猫有毒时，
     * 养狗的用户看到"宠物需隔离"会误以为自己也要隔离。
     */
    private static String petNote(boolean toxicCat, boolean toxicDog) {
        if (toxicCat && toxicDog) {
            return "宠物需隔离";
        }
        if (toxicCat) {
            return "猫需隔离";
        }
        if (toxicDog) {
            return "狗需隔离";
        }
        return null;
    }

    /** careLevel 数值转展示文案，取值与拆表前 difficulty 列的文案一致 */
    private static String difficultyLabel(Integer careLevel) {
        if (careLevel == null) {
            return null;
        }
        switch (careLevel) {
            case 1:
                return "新手友好";
            case 2:
                return "需要关注";
            case 3:
                return "进阶养护";
            default:
                return null;
        }
    }

    /** JSON 列可能为 NULL，给前端补成空数组，省掉一处 v-if */
    private static List<String> orEmpty(List<String> value) {
        return value == null ? Collections.emptyList() : value;
    }

    private static boolean isTrue(Integer value) {
        return value != null && value == 1;
    }
}
