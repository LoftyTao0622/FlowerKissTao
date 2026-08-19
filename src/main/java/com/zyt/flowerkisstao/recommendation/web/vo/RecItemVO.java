package com.zyt.flowerkisstao.recommendation.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 推荐结果里的一条。
 *
 * <p>字段的取舍对着方案原文那句要求："每个结果展示总分、分项匹配度、适合场景、
 * 养护难度、主要风险及推荐理由。"少任何一项，这就退化成一个普通的商品列表。
 */
@Data
@Builder
public class RecItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** rec_result_item.id，记录点击时回传 */
    private Long itemId;

    private Integer rankNo;

    // ===== 商品信息，够前端直接渲染一张卡片并跳转详情页 =====

    private Long speciesId;

    /** 品种 code，前端详情页路由用的就是它 */
    private String slug;

    private String name;

    private String latinName;

    private Long skuId;

    private String spec;

    private BigDecimal price;

    private String image;

    private String imageAlt;

    private Integer stock;

    // ===== 推荐信息 =====

    /** 加权总分 0-100 */
    private BigDecimal totalScore;

    /** 七项分项得分，键为 light/temp/humidity/care/space/budget/preference */
    private List<ScoreItemVO> scores;

    /** 推荐理由，三条 */
    private List<String> reasons;

    /** 主要风险提示，没有明显短板时为 null */
    private String risk;

    // ===== 方案要求展示的植物属性 =====

    /** 1 新手友好 / 2 需要关注 / 3 进阶养护 */
    private Integer careLevel;

    private String careLevelLabel;

    /** 光照展示文案，如"明亮散射光" */
    private String lightNote;

    private String waterNote;

    /** 猫狗均无毒时为 true */
    private Boolean petFriendly;
}
