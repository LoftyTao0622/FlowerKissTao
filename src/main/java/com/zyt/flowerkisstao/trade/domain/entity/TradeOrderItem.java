package com.zyt.flowerkisstao.trade.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单条目。
 *
 * <p>商品名、规格、单价、图片全部存快照。商品会改名、调价、下架，历史订单必须
 * 显示当时买的到底是什么、花了多少钱——只存 {@code skuId} 的话，管理员明天调个价，
 * 用户上个月的订单金额就跟着变了。
 *
 * <p>{@link #speciesId} 冗余存（由 {@code skuId} 也能查出来）：第⑤步"确认收货 →
 * 自动建立我的植物"是按品种建档的，直接读这一列即可，不必再去关联 {@code catalog_sku}
 * ——而那时这个 SKU 可能已经下架甚至删除了。
 */
@Data
@TableName("trade_order_item")
public class TradeOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 下单时的 catalog_sku.id。归还库存按它 */
    private Long skuId;

    /** 品种 catalog_species.id，第⑤步建养护档案用 */
    private Long speciesId;

    // ===== 商品快照 =====

    private String speciesName;

    private String spec;

    private String image;

    /** 成交单价快照 */
    private BigDecimal unitPrice;

    private Integer quantity;

    /** unitPrice × quantity */
    private BigDecimal subtotal;
}
