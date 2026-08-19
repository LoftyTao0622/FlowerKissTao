package com.zyt.flowerkisstao.trade.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单。
 *
 * <p>{@code status} 六个取值来自方案原文："订单状态覆盖待付款、待发货、运输中、
 * 已完成、已取消和售后中"。流转规则在 {@code OrderTransition} 里，是个不依赖
 * Spring、可单测的纯函数。
 *
 * <p>{@link #payIdemKey} 的唯一索引是"防重复支付"的真正落点。方案要求"结合幂等
 * 标识避免重复付款"——靠应用层先查再插是有竞态的（两个请求可能同时查到"没付过"），
 * 唯一索引没有这个问题，第二个请求必然撞键。NULL 不参与唯一性判定，所以未支付
 * 订单的 NULL 不会互相冲突。
 *
 * <p>地址存快照而非 {@code addressId}：用户删掉地址簿里那条之后，历史订单仍要
 * 显示当时寄到哪里去了。
 *
 * <p>没有 {@code deleted}：订单是交易凭证，不提供删除。
 */
@Data
@TableName("trade_order")
public class TradeOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号，yyyyMMddHHmmss + 6 位随机。可读、可排序、不需要序列表 */
    private String orderNo;

    private Long userId;

    /** 见 {@code OrderStatus}：0待付款 1待发货 2运输中 3已完成 4已取消 5售后中 */
    private Integer status;

    /**
     * 申请售后时的原状态，驳回后回到它。
     *
     * <p>按时间戳倒推"申请售后之前是什么状态"并不可靠——待发货和运输中都可以
     * 申请售后，驳回时得知道该退回哪一个。直接记下来最省事。
     */
    private Integer prevStatus;

    /** 订单总额，下单时算定。不随商品调价变动 */
    private BigDecimal totalAmount;

    /** 商品总件数 */
    private Integer itemCount;

    // ===== 地址快照 =====

    private String receiver;

    private String phone;

    /** 完整地址快照，省市区 + 详细地址已拼好 */
    private String addressSnapshot;

    /** 支付幂等标识。唯一索引拦重复付款 */
    private String payIdemKey;

    private LocalDateTime paidAt;

    private LocalDateTime shippedAt;

    /** 确认收货时间。第⑤步据此建养护档案 */
    private LocalDateTime receivedAt;

    /** 取消或售后结束的时间 */
    private LocalDateTime closedAt;

    private String cancelReason;

    /** 用户申请售后填的理由 */
    private String refundReason;

    /** 管理员驳回时填的理由 */
    private String refundRejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
