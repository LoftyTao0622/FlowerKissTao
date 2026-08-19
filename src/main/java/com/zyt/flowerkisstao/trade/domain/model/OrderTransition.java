package com.zyt.flowerkisstao.trade.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单状态流转规则。
 *
 * <p><b>这个类不依赖 Spring，也不碰数据库。</b>入参是当前状态与要执行的动作，出参是
 * 目标状态或一条拒绝理由。与第③步的 {@code RecommendationEngine} 同样处理，理由也一样：
 * 状态流转是这个模块最容易出错、也最容易测的地方，把它抽成纯函数之后，出问题时能
 * 立刻分清是流转规则错了还是接线错了。
 *
 * <p>方案原文的六个状态与允许的动作见 {@link OrderStatus} 与 {@link OrderAction}。
 */
public final class OrderTransition {

    private OrderTransition() {
    }

    /**
     * 流转结果。{@code allowed} 为 false 时 {@code target} 无意义，{@code reason} 是给用户看的话。
     */
    public record Result(boolean allowed, OrderStatus target, String reason) {

        static Result ok(OrderStatus target) {
            return new Result(true, target, null);
        }

        static Result reject(String reason) {
            return new Result(false, null, reason);
        }
    }

    /**
     * 能不能从 {@code current} 执行 {@code action}，能的话落到哪个状态。
     *
     * @param current    订单当前状态
     * @param action     要执行的动作
     * @param prevStatus 申请售后前的原状态，只有售后驳回用得到；其余情况可传 null
     */
    public static Result check(OrderStatus current, OrderAction action, OrderStatus prevStatus) {
        if (!action.allowedFrom().contains(current)) {
            return Result.reject(rejectReason(current, action));
        }

        if (action == OrderAction.REJECT_AFTER_SALE) {
            // 驳回要回到申请之前的状态。数据缺了原状态就退回待发货——
            // 那是三个可申请售后的状态里最保守的一个：用户的钱还在，货还没发出去
            return Result.ok(prevStatus == null ? OrderStatus.PAID : prevStatus);
        }
        return Result.ok(action.target());
    }

    /** 便捷重载：不涉及售后驳回时用这个 */
    public static Result check(OrderStatus current, OrderAction action) {
        return check(current, action, null);
    }

    /**
     * 当前状态下这个角色能做哪些动作。前端据此决定按钮显示哪几个——
     * 让后端说了算，免得界面上出现一个点下去必然报错的按钮。
     */
    public static List<OrderAction> availableActions(OrderStatus current, OrderAction.Actor actor) {
        List<OrderAction> actions = new ArrayList<>();
        for (OrderAction action : OrderAction.values()) {
            if (action.actor() == actor && action.allowedFrom().contains(current)) {
                actions.add(action);
            }
        }
        return actions;
    }

    /** 拒绝理由写具体些。"状态不允许"这种话对用户没有任何帮助 */
    private static String rejectReason(OrderStatus current, OrderAction action) {
        if (current.isFinal()) {
            // 标签本身就以"已"开头（已完成/已取消），再拼一个"已"会变成"订单已已完成"
            return "订单" + current.label() + "，不能再" + action.label();
        }
        return "当前是" + current.label() + "状态，不能" + action.label();
    }
}
