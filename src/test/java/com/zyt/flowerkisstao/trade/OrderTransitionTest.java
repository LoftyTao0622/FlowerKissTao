package com.zyt.flowerkisstao.trade;

import com.zyt.flowerkisstao.trade.domain.model.OrderAction;
import com.zyt.flowerkisstao.trade.domain.model.OrderStatus;
import com.zyt.flowerkisstao.trade.domain.model.OrderTransition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单状态机的单元测试。
 *
 * <p>不起 Spring 容器：流转规则是纯函数，测它不需要数据库。这也是把它独立出来的理由
 * ——订单出问题时能立刻和"接线出问题"区分开。
 *
 * <p>重点是 {@link #everyCombinationIsDecided()}：穷举 6 状态 × 7 动作全部 42 种组合，
 * 而不是只测几条 happy path。漏测的那一格往往就是线上出事的那一格。
 */
class OrderTransitionTest {

    // ================================================================
    // 合法流转：方案原文点名的那条主链路
    // ================================================================

    @Test
    @DisplayName("主链路：待付款 → 待发货 → 运输中 → 已完成")
    void happyPath() {
        OrderTransition.Result paid =
                OrderTransition.check(OrderStatus.PENDING_PAY, OrderAction.PAY);
        assertTrue(paid.allowed());
        assertEquals(OrderStatus.PAID, paid.target());

        OrderTransition.Result shipped =
                OrderTransition.check(OrderStatus.PAID, OrderAction.SHIP);
        assertTrue(shipped.allowed());
        assertEquals(OrderStatus.SHIPPED, shipped.target());

        OrderTransition.Result completed =
                OrderTransition.check(OrderStatus.SHIPPED, OrderAction.RECEIVE);
        assertTrue(completed.allowed());
        assertEquals(OrderStatus.COMPLETED, completed.target());
    }

    @Test
    @DisplayName("只有未支付的订单能取消")
    void onlyUnpaidCanBeCancelled() {
        assertTrue(OrderTransition.check(OrderStatus.PENDING_PAY, OrderAction.CANCEL).allowed());

        for (OrderStatus status : List.of(OrderStatus.PAID, OrderStatus.SHIPPED,
                OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.AFTER_SALE)) {
            assertFalse(OrderTransition.check(status, OrderAction.CANCEL).allowed(),
                    status.label() + "的订单不该能直接取消——付了钱的要走售后");
        }
    }

    @Test
    @DisplayName("付款后的三个状态都能申请售后，待付款不能")
    void afterSaleAppliesOnlyAfterPayment() {
        for (OrderStatus status : List.of(OrderStatus.PAID, OrderStatus.SHIPPED,
                OrderStatus.COMPLETED)) {
            OrderTransition.Result result =
                    OrderTransition.check(status, OrderAction.APPLY_AFTER_SALE);
            assertTrue(result.allowed(), status.label() + "应当能申请售后");
            assertEquals(OrderStatus.AFTER_SALE, result.target());
        }

        assertFalse(OrderTransition.check(OrderStatus.PENDING_PAY,
                        OrderAction.APPLY_AFTER_SALE).allowed(),
                "还没付钱就申请售后没有意义，应当走取消");
    }

    // ================================================================
    // 售后驳回：目标状态是动态的
    // ================================================================

    @Test
    @DisplayName("售后驳回回到申请之前的状态")
    void rejectReturnsToPreviousStatus() {
        for (OrderStatus previous : List.of(OrderStatus.PAID, OrderStatus.SHIPPED,
                OrderStatus.COMPLETED)) {
            OrderTransition.Result result = OrderTransition.check(
                    OrderStatus.AFTER_SALE, OrderAction.REJECT_AFTER_SALE, previous);
            assertTrue(result.allowed());
            assertEquals(previous, result.target(),
                    "从" + previous.label() + "申请的售后被驳回，应当回到" + previous.label());
        }
    }

    @Test
    @DisplayName("原状态缺失时，售后驳回退回待发货这个最保守的选择")
    void rejectFallsBackToPaidWhenPreviousUnknown() {
        OrderTransition.Result result = OrderTransition.check(
                OrderStatus.AFTER_SALE, OrderAction.REJECT_AFTER_SALE, null);
        assertTrue(result.allowed());
        assertEquals(OrderStatus.PAID, result.target(),
                "缺数据时应退回待发货：用户的钱还在，货也还没发出去");
    }

    @Test
    @DisplayName("售后通过走向已取消")
    void approveGoesToCancelled() {
        OrderTransition.Result result =
                OrderTransition.check(OrderStatus.AFTER_SALE, OrderAction.APPROVE_AFTER_SALE);
        assertTrue(result.allowed());
        assertEquals(OrderStatus.CANCELLED, result.target());
    }

    // ================================================================
    // 终态
    // ================================================================

    @Test
    @DisplayName("终态不接受任何动作")
    void finalStatesAcceptNothing() {
        for (OrderStatus status : List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED)) {
            assertTrue(status.isFinal());
            for (OrderAction action : OrderAction.values()) {
                if (action == OrderAction.APPLY_AFTER_SALE && status == OrderStatus.COMPLETED) {
                    // 已完成仍可申请售后——收到货发现是坏的，这是最常见的售后场景
                    continue;
                }
                assertFalse(OrderTransition.check(status, action).allowed(),
                        status.label() + "不该还能" + action.label());
            }
        }
    }

    @Test
    @DisplayName("已取消是彻底的终点，连售后都不能申请")
    void cancelledAcceptsNothingAtAll() {
        for (OrderAction action : OrderAction.values()) {
            assertFalse(OrderTransition.check(OrderStatus.CANCELLED, action).allowed(),
                    "已取消的订单不该能" + action.label());
        }
    }

    // ================================================================
    // 穷举：6 状态 × 7 动作 = 42 种组合逐一断言
    // ================================================================

    @Test
    @DisplayName("穷举 42 种组合，每一格的结论都与预期一致")
    void everyCombinationIsDecided() {
        // 预期允许的组合，逐条对应方案原文。不在这张表里的一律应当被拒
        Set<String> expectedAllowed = Set.of(
                key(OrderStatus.PENDING_PAY, OrderAction.PAY),
                key(OrderStatus.PENDING_PAY, OrderAction.CANCEL),
                key(OrderStatus.PAID, OrderAction.SHIP),
                key(OrderStatus.PAID, OrderAction.APPLY_AFTER_SALE),
                key(OrderStatus.SHIPPED, OrderAction.RECEIVE),
                key(OrderStatus.SHIPPED, OrderAction.APPLY_AFTER_SALE),
                key(OrderStatus.COMPLETED, OrderAction.APPLY_AFTER_SALE),
                key(OrderStatus.AFTER_SALE, OrderAction.APPROVE_AFTER_SALE),
                key(OrderStatus.AFTER_SALE, OrderAction.REJECT_AFTER_SALE));

        int checked = 0;
        for (OrderStatus status : OrderStatus.values()) {
            for (OrderAction action : OrderAction.values()) {
                boolean expected = expectedAllowed.contains(key(status, action));
                OrderTransition.Result result =
                        OrderTransition.check(status, action, OrderStatus.PAID);
                assertEquals(expected, result.allowed(),
                        String.format("%s + %s 的结论不对", status.label(), action.label()));
                if (result.allowed()) {
                    assertNotNull(result.target(), "允许流转就必须给出目标状态");
                    assertNull(result.reason(), "允许流转时不该有拒绝理由");
                } else {
                    assertNotNull(result.reason(), "拒绝时必须给出理由，用户要看得懂为什么");
                    assertFalse(result.reason().isBlank());
                }
                checked++;
            }
        }
        assertEquals(OrderStatus.values().length * OrderAction.values().length, checked);
        assertEquals(42, checked, "6 状态 × 7 动作");
    }

    // ================================================================
    // 库存归还与可用动作
    // ================================================================

    @Test
    @DisplayName("只有取消和售后通过会归还库存")
    void onlyCancelAndApproveRestoreStock() {
        assertTrue(OrderAction.CANCEL.restoresStock());
        assertTrue(OrderAction.APPROVE_AFTER_SALE.restoresStock());

        for (OrderAction action : List.of(OrderAction.PAY, OrderAction.SHIP,
                OrderAction.RECEIVE, OrderAction.APPLY_AFTER_SALE,
                OrderAction.REJECT_AFTER_SALE)) {
            assertFalse(action.restoresStock(),
                    action.label() + "不该归还库存——货还是要发出去的");
        }
    }

    @Test
    @DisplayName("按角色给出可用动作，用户看不到发货按钮")
    void availableActionsAreScopedByActor() {
        List<OrderAction> customerOnPaid =
                OrderTransition.availableActions(OrderStatus.PAID, OrderAction.Actor.CUSTOMER);
        assertEquals(List.of(OrderAction.APPLY_AFTER_SALE), customerOnPaid,
                "待发货时用户只能申请售后，发货是运营的事");

        List<OrderAction> operatorOnPaid =
                OrderTransition.availableActions(OrderStatus.PAID, OrderAction.Actor.OPERATOR);
        assertEquals(List.of(OrderAction.SHIP), operatorOnPaid);

        assertTrue(OrderTransition.availableActions(
                        OrderStatus.CANCELLED, OrderAction.Actor.CUSTOMER).isEmpty(),
                "已取消的订单不该给用户任何按钮");
    }

    // ================================================================
    // 状态码映射
    // ================================================================

    @Test
    @DisplayName("状态码与枚举双向对应")
    void statusCodesRoundTrip() {
        for (OrderStatus status : OrderStatus.values()) {
            assertEquals(status, OrderStatus.of(status.code()));
        }
        // 数值是存进数据库的，改了会让存量数据整体错位
        assertEquals(0, OrderStatus.PENDING_PAY.code());
        assertEquals(1, OrderStatus.PAID.code());
        assertEquals(2, OrderStatus.SHIPPED.code());
        assertEquals(3, OrderStatus.COMPLETED.code());
        assertEquals(4, OrderStatus.CANCELLED.code());
        assertEquals(5, OrderStatus.AFTER_SALE.code());
    }

    @Test
    @DisplayName("未知状态码直接抛，不静默当成待付款")
    void unknownStatusCodeThrows() {
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.of(9));
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.of(null));
    }

    private static String key(OrderStatus status, OrderAction action) {
        return status.name() + "|" + action.name();
    }
}
