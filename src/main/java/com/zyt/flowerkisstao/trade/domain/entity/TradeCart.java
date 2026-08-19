package com.zyt.flowerkisstao.trade.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 购物车条目。
 *
 * <p><b>这里刻意不存价格和商品名。</b>价格必须每次从 {@code catalog_sku} 实时读——
 * 存一份快照就等于给了客户端一个可能过期、也可能被篡改的价格来源。改造前的前端
 * 购物车正是把价格存在 localStorage 里的，用户改一下浏览器存储就能按自己写的价格下单。
 *
 * <p>按 {@code skuId} 而非品种存：同一株琴叶榕 75 厘米款 168 元、110 厘米款 288 元，
 * 按品种存就表达不了"买的是哪个规格"。
 *
 * <p>没有 {@code deleted}：购物车条目删了就是删了，没有留痕的价值。
 */
@Data
@TableName("trade_cart")
public class TradeCart implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 商品 catalog_sku.id */
    private Long skuId;

    /** 数量，最小 1。同一 SKU 再次加购是累加而非新增一行 */
    private Integer quantity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
