package com.zyt.flowerkisstao.catalog.web.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品 SKU 展示对象。
 *
 * <p>随 PlantVO 一起返回，供详情页的规格切换使用。第④步下单时按 {@code id} 提交。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String skuCode;

    /** 规格文案，如"中大型 · 约 75 厘米" */
    private String spec;

    private String pot;

    private BigDecimal price;

    /** 可售库存。为 0 时前端应禁用加购按钮 */
    private Integer stock;

    private String image;

    private String imageAlt;

    private Boolean featured;

    /** 1 上架 0 下架。管理端列表需要；公开接口恒为 1 */
    private Integer status;
}
