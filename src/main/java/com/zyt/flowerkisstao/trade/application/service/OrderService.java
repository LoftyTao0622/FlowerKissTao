package com.zyt.flowerkisstao.trade.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zyt.flowerkisstao.trade.web.dto.OrderCreateDTO;
import com.zyt.flowerkisstao.trade.web.dto.PayDTO;
import com.zyt.flowerkisstao.trade.web.vo.OrderVO;

/**
 * 订单，顾客侧。
 *
 * <p>方案原文："提交订单时再次校验商品状态、最新价格、可售库存和配送范围，
 * 以数据库事务完成库存扣减与订单创建，防止超卖；支付采用可演示的模拟流程，
 * 结合幂等标识避免重复付款。"
 */
public interface OrderService {

    /** 下单。事务内扣库存 + 建订单，任何一步失败全部回滚 */
    OrderVO create(OrderCreateDTO dto);

    /** 我的订单。status 传 null 表示全部 */
    IPage<OrderVO> pageMine(IPage<?> page, Integer status);

    OrderVO get(Long id);

    /** 模拟支付。同一个 idemKey 重复提交只会成功一次 */
    OrderVO pay(Long id, PayDTO dto);

    /** 取消未支付订单，归还库存 */
    void cancel(Long id, String reason);

    /** 确认收货。第⑤步养护模块在这里挂接 */
    void receive(Long id);

    /** 申请售后 */
    void applyAfterSale(Long id, String reason);
}
