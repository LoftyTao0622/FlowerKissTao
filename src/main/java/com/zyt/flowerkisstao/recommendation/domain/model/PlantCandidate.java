package com.zyt.flowerkisstao.recommendation.domain.model;

import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;

import java.util.List;

/**
 * 送进打分引擎的一个待选项：一个品种连同它在售的全部 SKU。
 *
 * <p>为什么要带上 SKU 列表而不是只给品种：预算过滤与预算贴合度都发生在价格上，
 * 而价格属于 SKU。同一株琴叶榕 75 厘米款 268 元、110 厘米款 458 元，用户预算 300
 * 时前者进得来、后者进不来——只看品种就判不出来。
 *
 * @param species 品种，承载光照/温湿度/毒性/冠幅等知识属性
 * @param skus    该品种在售的 SKU，不可为空列表（无在售 SKU 的品种不进候选）
 */
public record PlantCandidate(CatalogSpecies species, List<CatalogSku> skus) {

    public Long speciesId() {
        return species.getId();
    }
}
