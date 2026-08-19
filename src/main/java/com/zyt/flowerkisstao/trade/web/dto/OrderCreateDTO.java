package com.zyt.flowerkisstao.trade.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "请选择收货地址")
    private Long addressId;

    /** 要结算的购物车条目。不传全部是有意的：用户可能只想先买其中两件 */
    @NotEmpty(message = "请至少选择一件商品")
    private List<Long> skuIds;
}
