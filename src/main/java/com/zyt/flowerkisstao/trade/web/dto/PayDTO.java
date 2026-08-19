package com.zyt.flowerkisstao.trade.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 模拟支付入参。
 *
 * <p>{@code idemKey} 是方案要求的"幂等标识"。前端进入支付页时生成一次（uuid），
 * 重复提交时带的是同一个 key，后端靠 {@code trade_order.pay_idem_key} 的唯一索引拦下。
 *
 * <p>用客户端生成而非服务端下发：用户连点两次按钮、网络重发、页面刷新后重试，
 * 这些场景里前端手上的 key 都还是同一个，服务端下发则要多一次往返且同样要防重。
 */
@Data
public class PayDTO {

    @NotBlank(message = "缺少支付幂等标识")
    @Size(max = 64, message = "幂等标识过长")
    private String idemKey;
}
