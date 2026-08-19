package com.zyt.flowerkisstao.trade.domain.model;

import java.util.Set;

/**
 * 订单动作。每个动作声明"从哪些状态可以发起"以及"由谁发起"。
 *
 * <p>把允许的来源状态挂在动作自己身上，而不是散在各个 Service 方法里写 if：
 * 加一个状态时只需要改这个文件，漏改的风险就没了。
 */
public enum OrderAction {

    /** 支付。待付款 → 待发货 */
    PAY("支付", Actor.CUSTOMER, Set.of(OrderStatus.PENDING_PAY), OrderStatus.PAID),

    /**
     * 取消。只有未支付的订单能取消——已经付了钱的要走售后，那是另一条路。
     * 方案原文："取消未支付订单"。
     */
    CANCEL("取消订单", Actor.CUSTOMER, Set.of(OrderStatus.PENDING_PAY), OrderStatus.CANCELLED),

    /** 发货。方案原文："管理员负责发货和审核" */
    SHIP("发货", Actor.OPERATOR, Set.of(OrderStatus.PAID), OrderStatus.SHIPPED),

    /** 确认收货。只有运输中的订单能确认——没发货就说收到了不合逻辑 */
    RECEIVE("确认收货", Actor.CUSTOMER, Set.of(OrderStatus.SHIPPED), OrderStatus.COMPLETED),

    /**
     * 申请售后。付款之后、收货之后都可能出问题，所以三个状态都允许申请。
     * 待付款不允许——还没付钱，直接取消即可。
     */
    APPLY_AFTER_SALE("申请售后", Actor.CUSTOMER,
            Set.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED),
            OrderStatus.AFTER_SALE),

    /** 售后通过。退款并归还库存，订单走向已取消 */
    APPROVE_AFTER_SALE("售后通过", Actor.OPERATOR,
            Set.of(OrderStatus.AFTER_SALE), OrderStatus.CANCELLED),

    /**
     * 售后驳回。目标状态是动态的——回到申请售后之前的那个状态，
     * 所以这里的 target 为 null，由 {@code OrderTransition} 从 prevStatus 取。
     */
    REJECT_AFTER_SALE("售后驳回", Actor.OPERATOR, Set.of(OrderStatus.AFTER_SALE), null);

    /** 谁能发起这个动作 */
    public enum Actor {
        /** 下单人自己 */
        CUSTOMER,
        /** 运营或管理员 */
        OPERATOR
    }

    private final String label;
    private final Actor actor;
    private final Set<OrderStatus> allowedFrom;
    private final OrderStatus target;

    OrderAction(String label, Actor actor, Set<OrderStatus> allowedFrom, OrderStatus target) {
        this.label = label;
        this.actor = actor;
        this.allowedFrom = allowedFrom;
        this.target = target;
    }

    public String label() {
        return label;
    }

    public Actor actor() {
        return actor;
    }

    public Set<OrderStatus> allowedFrom() {
        return allowedFrom;
    }

    /** 固定目标状态。售后驳回返回 null，目标由原状态决定 */
    public OrderStatus target() {
        return target;
    }

    /** 这个动作会不会让库存回到货架上 */
    public boolean restoresStock() {
        return this == CANCEL || this == APPROVE_AFTER_SALE;
    }
}
