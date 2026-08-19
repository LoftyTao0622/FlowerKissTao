package com.zyt.flowerkisstao.trade.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSkuMapper;
import com.zyt.flowerkisstao.trade.domain.entity.TradeOrderItem;
import com.zyt.flowerkisstao.trade.infrastructure.mapper.TradeOrderItemMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 库存归还。
 *
 * <p>单独抽成一个组件，是因为有两个入口会用到它：用户取消未支付订单，以及管理员
 * 审核通过售后。<b>两处各写一遍必然会漏掉一边</b>——将来改归还规则（比如加一条
 * "已发货的售后不还库存，货已经在路上了"）时，只改了一处的后果是库存悄悄对不上，
 * 而且要等到盘点时才会发现。
 */
@Component
class OrderStockSupport {

    private final TradeOrderItemMapper orderItemMapper;
    private final CatalogSkuMapper skuMapper;

    OrderStockSupport(TradeOrderItemMapper orderItemMapper, CatalogSkuMapper skuMapper) {
        this.orderItemMapper = orderItemMapper;
        this.skuMapper = skuMapper;
    }

    /**
     * 把这笔订单占用的库存全部还回货架。
     *
     * <p>按订单条目里的 {@code quantity} 还，而不是重新算——条目存的就是当时实际
     * 扣掉的数量，是唯一可信的依据。
     *
     * <p>调用方必须在事务里调它，否则还了一半失败会留下半笔账。
     */
    void restore(Long orderId) {
        for (TradeOrderItem item : itemsOf(orderId)) {
            skuMapper.restoreStock(item.getSkuId(), item.getQuantity());
        }
    }

    List<TradeOrderItem> itemsOf(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<TradeOrderItem>lambdaQuery()
                .eq(TradeOrderItem::getOrderId, orderId)
                .orderByAsc(TradeOrderItem::getId));
    }
}
