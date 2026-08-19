package com.zyt.flowerkisstao.recommendation.domain.model;

import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;

import java.util.List;
import java.util.Map;

/**
 * 打分引擎给出的一条结果。
 *
 * @param species     推荐品种
 * @param sku         价格最贴合预算的那个 SKU，前端点击直达它的详情页
 * @param totalScore  加权总分 0-100，保留两位小数
 * @param scores      七项分项得分，键为 {@link ScoreDimension#code()}，各项 0-100
 * @param reasons     推荐理由，取得分最高的三维各一句
 * @param risk        主要风险提示，由得分最低那一维生成；七维都够高时为 null
 */
public record ScoredPlant(CatalogSpecies species,
                          CatalogSku sku,
                          double totalScore,
                          Map<String, Integer> scores,
                          List<String> reasons,
                          String risk) {
}
