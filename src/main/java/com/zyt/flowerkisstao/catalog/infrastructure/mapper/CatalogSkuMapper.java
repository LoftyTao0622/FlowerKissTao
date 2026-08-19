package com.zyt.flowerkisstao.catalog.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CatalogSkuMapper extends BaseMapper<CatalogSku> {

    /**
     * 扣减库存，防超卖的落点。
     *
     * <p><b>库存条件写在 WHERE 里而不是先查再改。</b>"先 SELECT 判断够不够，再 UPDATE 扣减"
     * 在并发下必然超卖：两个请求可能同时读到 stock=1，都认为够，然后各扣一次，库存变成 -1。
     * 把 {@code stock >= #{quantity}} 放进 WHERE 之后，判断与扣减是同一条语句，
     * 由数据库的行锁保证原子性——第二个请求会看到已经扣过的值，条件不成立，影响行数为 0。
     *
     * <p>调用方必须检查返回值：0 表示库存不足或商品已下架，应抛异常让整个事务回滚。
     *
     * <p>没有用 {@code @Version} 乐观锁：那需要改全局的 MybatisPlusConfig、给实体加列、
     * 影响所有既有的 SKU 写入路径，还得写重试循环。条件 UPDATE 是一条原子语句，
     * 不需要重试，也不碰任何既有代码。
     *
     * @return 影响行数，1 成功 0 失败
     */
    @Update("UPDATE catalog_sku SET stock = stock - #{quantity} "
            + "WHERE id = #{skuId} AND stock >= #{quantity} AND status = 1 AND deleted = 0")
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    /**
     * 归还库存。取消订单与售后通过都走这里。
     *
     * <p>不校验商品状态：下单之后商品可能已被下架，但那笔库存本来就是从它身上扣走的，
     * 取消时必须原样还回去。用上架状态卡住归还，会让下架商品的库存凭空蒸发。
     */
    @Update("UPDATE catalog_sku SET stock = stock + #{quantity} "
            + "WHERE id = #{skuId} AND deleted = 0")
    int restoreStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);
}

