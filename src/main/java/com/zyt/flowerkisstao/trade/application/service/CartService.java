package com.zyt.flowerkisstao.trade.application.service;

import com.zyt.flowerkisstao.trade.web.dto.CartAddDTO;
import com.zyt.flowerkisstao.trade.web.vo.CartVO;

import java.util.List;

/**
 * 购物车。
 *
 * <p>价格与库存一律实时从 {@code catalog_sku} 读，购物车表里只存 skuId 与数量。
 */
public interface CartService {

    /** 我的购物车，含失效标记 */
    CartVO listMine();

    /** 加购。同一 SKU 再次加购是累加数量 */
    void add(CartAddDTO dto);

    /** 改数量。传 0 或负数等同于移除 */
    void updateQuantity(Long skuId, Integer quantity);

    void remove(Long skuId);

    void clear();

    /** 下单成功后按 skuId 批量清掉已购条目，未勾选的留在车里 */
    void removeBySkuIds(Long userId, List<Long> skuIds);
}
