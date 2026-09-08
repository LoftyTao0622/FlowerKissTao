package com.zyt.flowerkisstao.trade.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.flowerkisstao.trade.domain.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {

    /**
     * 串行化同一用户的结算事务。仅锁用户行，不锁整个订单表，既能覆盖购物车
     * 条目不相交的并发结算，也能让幂等键查询与建单形成一个原子序列。
     */
    @Select("SELECT id FROM sys_user WHERE id = #{userId} FOR UPDATE")
    Long lockUserForCheckout(@Param("userId") Long userId);

    @Update("UPDATE trade_order SET status = #{targetStatus} "
            + "WHERE id = #{orderId} AND status = #{expectedStatus}")
    int markStatus(@Param("orderId") Long orderId,
                   @Param("expectedStatus") int expectedStatus,
                   @Param("targetStatus") int targetStatus);

    /**
     * 标记支付成功。防重复支付的落点。
     *
     * <p><b>状态条件写在 WHERE 里而不是先查再改。</b>用户连点两次按钮时，两个请求可能
     * 同时读到"待付款"，然后都认为可以支付。把 {@code status = #{expectedStatus}}
     * 放进 WHERE 之后，判断与更新是同一条语句，由行锁保证只有一个能成功——
     * 第二条语句看到的已经是 status=1，条件不成立，影响行数为 0。
     *
     * <p>与库存扣减是同一个套路。凡是"先判断再修改"的地方，并发下都得这么写。
     *
     * @return 影响行数，1 支付成功 0 已经被别的请求付过了
     */
    @Update("UPDATE trade_order SET status = #{targetStatus}, pay_idem_key = #{idemKey}, "
            + "paid_at = #{paidAt} "
            + "WHERE id = #{orderId} AND status = #{expectedStatus}")
    int markPaid(@Param("orderId") Long orderId,
                 @Param("expectedStatus") int expectedStatus,
                 @Param("targetStatus") int targetStatus,
                 @Param("idemKey") String idemKey,
                 @Param("paidAt") LocalDateTime paidAt);
}
