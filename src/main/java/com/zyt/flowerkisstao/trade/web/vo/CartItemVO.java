package com.zyt.flowerkisstao.trade.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车条目。
 *
 * <p>价格与库存都是**每次实时从 catalog_sku 读出来的**，不是加购时存下的快照。
 * 用户昨天加购、今天商品调价，结算页必须显示今天的价格。
 */
@Data
@Builder
public class CartItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long skuId;

    private Long speciesId;

    /** 品种 code，前端跳详情页用 */
    private String slug;

    private String speciesName;

    private String spec;

    private String image;

    private String imageAlt;

    /** 实时单价，非快照 */
    private BigDecimal price;

    private Integer quantity;

    /** price × quantity */
    private BigDecimal subtotal;

    /** 实时可售库存 */
    private Integer stock;

    // ===== 失效标记：界面上要把这类条目标灰并排除在结算之外 =====

    /** 商品已下架或已删除 */
    private Boolean unavailable;

    /** 库存不足以支撑当前数量（含库存为 0） */
    private Boolean outOfStock;

    /** 失效原因文案，为空表示这条可以正常结算 */
    private String invalidReason;

    /** 可结算：既没下架也不缺货 */
    public boolean isPurchasable() {
        return !Boolean.TRUE.equals(unavailable) && !Boolean.TRUE.equals(outOfStock);
    }
}
