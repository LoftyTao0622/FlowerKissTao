package com.zyt.flowerkisstao.trade.domain.model;

/**
 * 订单状态。六个取值来自方案原文："订单状态覆盖待付款、待发货、运输中、已完成、
 * 已取消和售后中"。
 *
 * <p>数值一旦定下就不能改：它直接存进 {@code trade_order.status}，改了会让存量数据
 * 全部错位。新增状态只能往后追加。
 */
public enum OrderStatus {

    /** 待付款。下单即进入此态，库存此时已经扣掉 */
    PENDING_PAY(0, "待付款"),

    /** 待发货。支付成功 */
    PAID(1, "待发货"),

    /** 运输中。管理员已发货 */
    SHIPPED(2, "运输中"),

    /** 已完成。用户确认收货，第⑤步在这里建养护档案 */
    COMPLETED(3, "已完成"),

    /** 已取消。用户主动取消，或售后通过。两种情况都要归还库存 */
    CANCELLED(4, "已取消"),

    /** 售后中。等待管理员审核 */
    AFTER_SALE(5, "售后中");

    private final int code;
    private final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    /** 中文名，直接用于界面与状态时间线 */
    public String label() {
        return label;
    }

    /** 数据库里的值转回枚举。遇到未知值直接抛，比静默当成待付款安全得多 */
    public static OrderStatus of(Integer code) {
        if (code != null) {
            for (OrderStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("未知的订单状态：" + code);
    }

    /** 终态：不能再发生任何流转 */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
