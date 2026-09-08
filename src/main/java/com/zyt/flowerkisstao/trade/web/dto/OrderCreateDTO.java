package com.zyt.flowerkisstao.trade.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 下单入参。
 *
 * <p><b>只有地址 id 和要买哪几个 SKU，没有价格也没有数量。</b>数量取购物车里的，
 * 价格由后端从 catalog_sku 实时读——客户端能决定的只是"买什么"，
 * "多少钱"永远由服务端说了算。
 */
@Data
public class OrderCreateDTO {

    /**
     * 结算请求幂等键。客户端重试同一次结算时必须复用它；数据库会把它与用户 id
     * 一起做唯一约束，避免网络重试生成第二张订单。
     */
    @NotBlank(message = "缺少结算幂等标识")
    @Size(max = 64, message = "结算幂等标识不能超过 64 个字符")
    private String idemKey;

    @NotNull(message = "请选择收货地址")
    private Long addressId;

    /** 要结算的购物车条目。不传全部是有意的：用户可能只想先买其中两件 */
    @NotEmpty(message = "请至少选择一件商品")
    private List<Long> skuIds;
}
