package com.zyt.flowerkisstao.trade.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zyt.flowerkisstao.trade.web.vo.OrderVO;

/**
 * 订单，管理端。方案原文："管理员负责发货和审核。"
 *
 * <p>与顾客侧分成两个 Service 而不是塞进一个：两边的归属校验规则完全相反——
 * 顾客只能碰自己的订单，运营能碰所有人的。混在一起就得在每个方法里传一个
 * "是不是管理员"的布尔值，那是最容易漏判的写法。
 */
public interface OrderAdminService {

    /** 全部订单，可按状态与订单号筛 */
    IPage<OrderVO> page(IPage<?> page, Integer status, String keyword);

    OrderVO get(Long id);

    /** 发货：待发货 → 运输中 */
    void ship(Long id);

    /** 售后通过：退款并归还库存，订单走向已取消 */
    void approveAfterSale(Long id);

    /** 售后驳回：回到申请之前的状态 */
    void rejectAfterSale(Long id, String reason);
}
