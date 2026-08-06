package com.zyt.flowerkisstao.catalog.application.service.impl;

import com.zyt.flowerkisstao.catalog.domain.entity.CatalogPlant;
import com.zyt.flowerkisstao.catalog.web.vo.PlantVO;

import java.util.Collections;
import java.util.List;

/**
 * 实体到 VO 的转换。两个 Service 都要用，抽出来避免各写一份后字段漏配。
 */
final class PlantConverter {

    private PlantConverter() {
    }

    static PlantVO toVO(CatalogPlant plant) {
        return PlantVO.builder()
                .id(plant.getId())
                .slug(plant.getSlug())
                .name(plant.getName())
                .latinName(plant.getLatinName())
                .price(plant.getPrice())
                .image(plant.getImage())
                .imageAlt(plant.getImageAlt())
                .category(plant.getCategory())
                .light(plant.getLight())
                .watering(plant.getWatering())
                .size(plant.getSize())
                .petFriendly(isTrue(plant.getPetFriendly()))
                .petNote(plant.getPetNote())
                .difficulty(plant.getDifficulty())
                .matchTags(orEmpty(plant.getMatchTags()))
                .recommendationReason(plant.getRecommendationReason())
                .shortDescription(plant.getShortDescription())
                .description(plant.getDescription())
                .careTips(orEmpty(plant.getCareTips()))
                .featured(isTrue(plant.getFeatured()))
                .status(plant.getStatus())
                .build();
    }

    /** JSON 列可能为 NULL，给前端补成空数组，省掉一处 v-if */
    private static List<String> orEmpty(List<String> value) {
        return value == null ? Collections.emptyList() : value;
    }

    private static boolean isTrue(Integer value) {
        return value != null && value == 1;
    }
}
