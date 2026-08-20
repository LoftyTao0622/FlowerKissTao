package com.zyt.flowerkisstao.trade;

import com.zyt.flowerkisstao.trade.infrastructure.mapper.TradeOrderMapper;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单条件更新的 SQL 契约测试。
 *
 * <p>该测试不连接数据库，只锁定并发正确性的关键 WHERE 条件，避免后续改 SQL 时退化为
 * "先查询状态再普通更新"。
 */
class TradeOrderMapperContractTest {

    @Test
    @DisplayName("订单状态更新必须同时约束订单 ID 和期望旧状态")
    void markStatusKeepsCompareAndSetPredicate() throws NoSuchMethodException {
        Method method = TradeOrderMapper.class.getMethod(
                "markStatus", Long.class, int.class, int.class);
        Update update = method.getAnnotation(Update.class);

        assertTrue(update != null, "markStatus 必须使用 MyBatis 更新注解");
        String sql = String.join(" ", update.value()).toLowerCase();
        assertTrue(sql.contains("update trade_order"));
        assertTrue(sql.contains("status = #{targetstatus}"));
        assertTrue(sql.contains("where id = #{orderid}"));
        assertTrue(sql.contains("and status = #{expectedstatus}"));
    }
}
