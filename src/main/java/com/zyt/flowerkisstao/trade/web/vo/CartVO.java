package com.zyt.flowerkisstao.trade.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/** 购物车整体。合计只统计可结算的条目，失效的不算钱 */
@Data
@Builder
public class CartVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CartItemVO> items;

    /** 可结算条目的件数合计 */
    private Integer totalCount;

    /** 可结算条目的金额合计。失效条目不计入，否则用户会看到一个付不出去的数字 */
    private BigDecimal totalAmount;

    /** 有失效条目，界面上要提示 */
    private Boolean hasInvalid;
}
