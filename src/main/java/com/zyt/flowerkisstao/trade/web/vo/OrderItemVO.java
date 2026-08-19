package com.zyt.flowerkisstao.trade.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 订单条目，全部字段都是下单时的快照 */
@Data
@Builder
public class OrderItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long skuId;

    /** 品种 id。前端跳详情页要先拿它换 slug，第⑤步建养护档案也用它 */
    private Long speciesId;

    private String speciesName;

    private String spec;

    private String image;

    /** 成交单价，非当前售价 */
    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal subtotal;
}
