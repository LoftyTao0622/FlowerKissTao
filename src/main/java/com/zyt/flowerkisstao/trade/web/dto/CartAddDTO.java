package com.zyt.flowerkisstao.trade.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加购入参。
 *
 * <p><b>没有 price 字段，这是有意的。</b>价格由后端从 catalog_sku 读，客户端传什么都不认。
 * 改造前的前端购物车把价格存在 localStorage 里，用户改一下浏览器存储就能按自己
 * 写的价格下单——接口层面不接受价格，这条路就彻底堵死了。
 */
@Data
public class CartAddDTO {

    @NotNull(message = "请选择商品规格")
    private Long skuId;

    @NotNull(message = "请填写数量")
    @Min(value = 1, message = "数量至少为 1")
    // 上限 99：单笔买上千盆的多半是误操作或脚本，真有这种量应当走对公渠道
    @Max(value = 99, message = "单次最多购买 99 件")
    private Integer quantity;
}
