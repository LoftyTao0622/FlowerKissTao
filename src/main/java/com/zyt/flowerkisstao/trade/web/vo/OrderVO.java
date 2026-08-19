package com.zyt.flowerkisstao.trade.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 订单。列表页与详情页共用一份，列表时 items 只带前几条 */
@Data
@Builder
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前角色在这个状态下能做的动作。
     *
     * <p>由后端算好而不是前端各自判断：状态机在后端，前端再实现一遍必然会有出入，
     * 界面上就会出现点下去必然报错的按钮。
     */
    @Data
    @AllArgsConstructor
    public static class ActionVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 动作名，如 PAY / CANCEL，前端据此调对应接口 */
        private String code;

        /** 按钮文案，如"确认收货" */
        private String label;
    }

    private Long id;

    private String orderNo;

    /** 0待付款 1待发货 2运输中 3已完成 4已取消 5售后中 */
    private Integer status;

    private String statusLabel;

    private BigDecimal totalAmount;

    private Integer itemCount;

    // ===== 地址快照 =====

    private String receiver;

    private String phone;

    private String addressSnapshot;

    // ===== 时间线 =====

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    private LocalDateTime shippedAt;

    private LocalDateTime receivedAt;

    private LocalDateTime closedAt;

    private String cancelReason;

    private String refundReason;

    private String refundRejectReason;

    private List<OrderItemVO> items;

    /** 当前角色可执行的动作 */
    private List<ActionVO> actions;
}
