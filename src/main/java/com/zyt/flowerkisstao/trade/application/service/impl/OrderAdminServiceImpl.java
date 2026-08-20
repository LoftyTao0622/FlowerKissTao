package com.zyt.flowerkisstao.trade.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.operation.application.service.OperationLogService;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.trade.application.service.OrderAdminService;
import com.zyt.flowerkisstao.trade.domain.entity.TradeOrder;
import com.zyt.flowerkisstao.trade.domain.entity.TradeOrderItem;
import com.zyt.flowerkisstao.trade.domain.model.OrderAction;
import com.zyt.flowerkisstao.trade.domain.model.OrderStatus;
import com.zyt.flowerkisstao.trade.infrastructure.mapper.TradeOrderItemMapper;
import com.zyt.flowerkisstao.trade.infrastructure.mapper.TradeOrderMapper;
import com.zyt.flowerkisstao.trade.web.vo.OrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单，管理端。方案原文："管理员负责发货和审核。"
 *
 * <p>与 {@code OrderServiceImpl} 分开是有意的：这里**不做归属校验**，运营本来就要能看
 * 所有人的订单。把两套完全相反的归属规则放进同一个类，早晚会有一个方法漏判。
 * 权限由 Controller 上的 {@code @PreAuthorize} 把住。
 */
@Service
public class OrderAdminServiceImpl implements OrderAdminService {

    private final TradeOrderMapper orderMapper;
    private final TradeOrderItemMapper orderItemMapper;
    private final OrderStockSupport stockSupport;
    private final OperationLogService operationLogService;

    public OrderAdminServiceImpl(TradeOrderMapper orderMapper,
                                 TradeOrderItemMapper orderItemMapper,
                                 OrderStockSupport stockSupport,
                                 OperationLogService operationLogService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.stockSupport = stockSupport;
        this.operationLogService = operationLogService;
    }

    @Override
    public IPage<OrderVO> page(IPage<?> page, Integer status, String keyword) {
        @SuppressWarnings("unchecked")
        IPage<TradeOrder> orderPage = orderMapper.selectPage((IPage<TradeOrder>) page,
                Wrappers.<TradeOrder>lambdaQuery()
                        .eq(status != null, TradeOrder::getStatus, status)
                        // 按订单号或收件人搜。客服接到电话时手上多半只有这两样
                        .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                                .like(TradeOrder::getOrderNo, keyword)
                                .or().like(TradeOrder::getReceiver, keyword))
                        .orderByDesc(TradeOrder::getId));

        Map<Long, List<TradeOrderItem>> itemsByOrder = loadItems(orderPage.getRecords());
        return orderPage.convert(order -> TradeConverter.toVO(order,
                itemsByOrder.getOrDefault(order.getId(), List.of()),
                OrderAction.Actor.OPERATOR));
    }

    @Override
    public OrderVO get(Long id) {
        TradeOrder order = requireOrder(id);
        return TradeConverter.toVO(order, stockSupport.itemsOf(id), OrderAction.Actor.OPERATOR);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id) {
        TradeOrder order = requireOrder(id);
        OrderStatus target = OrderServiceImpl.requireTransition(order, OrderAction.SHIP);

        TradeOrder update = new TradeOrder();
        update.setId(id);
        update.setStatus(target.code());
        update.setShippedAt(LocalDateTime.now());
        if (orderMapper.markStatus(id, order.getStatus(), target.code()) == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "订单状态已变化，请刷新后重试");
        }
        orderMapper.updateById(update);
        operationLogService.record("trade", "SHIP", "trade_order", id,
                Map.of("status", order.getStatus()), Map.of("status", target.code()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveAfterSale(Long id) {
        TradeOrder order = requireOrder(id);
        OrderStatus target = OrderServiceImpl.requireTransition(order,
                OrderAction.APPROVE_AFTER_SALE);

        // 退款成立，货退回来了，库存要还上。与用户取消订单共用同一段逻辑
        if (orderMapper.markStatus(id, order.getStatus(), target.code()) == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "订单状态已变化，请刷新后重试");
        }
        stockSupport.restore(id);

        TradeOrder update = new TradeOrder();
        update.setId(id);
        update.setStatus(target.code());
        update.setClosedAt(LocalDateTime.now());
        orderMapper.updateById(update);
        operationLogService.record("trade", "REFUND", "trade_order", id,
                Map.of("status", order.getStatus()), Map.of("status", target.code(), "restoredStock", true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectAfterSale(Long id, String reason) {
        TradeOrder order = requireOrder(id);
        // 目标状态由 prevStatus 决定——从待发货申请的退回待发货，从已完成申请的退回已完成
        OrderStatus target = OrderServiceImpl.requireTransition(order,
                OrderAction.REJECT_AFTER_SALE);

        if (orderMapper.markStatus(id, order.getStatus(), target.code()) == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "订单状态已变化，请刷新后重试");
        }

        TradeOrder update = new TradeOrder();
        update.setId(id);
        update.setStatus(target.code());
        update.setRefundRejectReason(reason);
        orderMapper.updateById(update);
        // 驳回后 prevStatus 已无意义，清掉免得下次申请时读到陈旧值。
        // updateById 遇到 null 字段会跳过，只能显式置空
        orderMapper.update(null, Wrappers.<TradeOrder>lambdaUpdate()
                .eq(TradeOrder::getId, id)
                .set(TradeOrder::getPrevStatus, null));
        operationLogService.record("trade", "REFUND_REJECT", "trade_order", id,
                Map.of("status", order.getStatus()),
                Map.of("status", target.code(), "reason", reason));
    }

    // ================================================================

    /** 管理端不校归属，但订单本身得存在 */
    private TradeOrder requireOrder(Long id) {
        TradeOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private Map<Long, List<TradeOrderItem>> loadItems(List<TradeOrder> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<Long> orderIds = orders.stream().map(TradeOrder::getId).toList();
        return orderItemMapper.selectList(Wrappers.<TradeOrderItem>lambdaQuery()
                        .in(TradeOrderItem::getOrderId, orderIds)
                        .orderByAsc(TradeOrderItem::getId)).stream()
                .collect(Collectors.groupingBy(TradeOrderItem::getOrderId));
    }
}
