package com.zyt.flowerkisstao.trade.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSkuMapper;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSpeciesMapper;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import com.zyt.flowerkisstao.trade.application.service.CartService;
import com.zyt.flowerkisstao.trade.domain.entity.TradeCart;
import com.zyt.flowerkisstao.trade.infrastructure.mapper.TradeCartMapper;
import com.zyt.flowerkisstao.trade.web.dto.CartAddDTO;
import com.zyt.flowerkisstao.trade.web.vo.CartItemVO;
import com.zyt.flowerkisstao.trade.web.vo.CartVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车。
 *
 * <p><b>价格永远实时读，购物车表里一分钱都不存。</b>用户昨天加购、今天商品调价，
 * 结算页必须显示今天的价格；而且客户端根本没有渠道把价格送进来，篡改这条路是堵死的。
 */
@Service
public class CartServiceImpl implements CartService {

    private final TradeCartMapper cartMapper;
    private final CatalogSkuMapper skuMapper;
    private final CatalogSpeciesMapper speciesMapper;

    public CartServiceImpl(TradeCartMapper cartMapper,
                           CatalogSkuMapper skuMapper,
                           CatalogSpeciesMapper speciesMapper) {
        this.cartMapper = cartMapper;
        this.skuMapper = skuMapper;
        this.speciesMapper = speciesMapper;
    }

    @Override
    public CartVO listMine() {
        List<TradeCart> rows = cartMapper.selectList(Wrappers.<TradeCart>lambdaQuery()
                .eq(TradeCart::getUserId, CurrentUser.requireUserId())
                .orderByDesc(TradeCart::getUpdatedAt));
        if (rows.isEmpty()) {
            return CartVO.builder()
                    .items(List.of()).totalCount(0)
                    .totalAmount(BigDecimal.ZERO).hasInvalid(false)
                    .build();
        }

        // 两次批量查而不是逐条查：条目多时逐条查就是典型的 N+1。
        // 这里刻意不加 status/deleted 条件——下架的商品也要查出来，
        // 才能在界面上标成"已下架"而不是让它凭空消失
        List<Long> skuIds = rows.stream().map(TradeCart::getSkuId).toList();
        Map<Long, CatalogSku> skuById = skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(CatalogSku::getId, Function.identity()));

        Map<Long, CatalogSpecies> speciesById = Collections.emptyMap();
        List<Long> speciesIds = skuById.values().stream()
                .map(CatalogSku::getSpeciesId).distinct().toList();
        if (!speciesIds.isEmpty()) {
            speciesById = speciesMapper.selectBatchIds(speciesIds).stream()
                    .collect(Collectors.toMap(CatalogSpecies::getId, Function.identity()));
        }

        List<CartItemVO> items = new ArrayList<>();
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        boolean hasInvalid = false;

        for (TradeCart row : rows) {
            CartItemVO item = toItem(row, skuById.get(row.getSkuId()), speciesById);
            items.add(item);
            if (item.isPurchasable()) {
                totalCount += item.getQuantity();
                totalAmount = totalAmount.add(item.getSubtotal());
            } else {
                hasInvalid = true;
            }
        }

        return CartVO.builder()
                .items(items)
                .totalCount(totalCount)
                .totalAmount(totalAmount)
                .hasInvalid(hasInvalid)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(CartAddDTO dto) {
        Long userId = CurrentUser.requireUserId();
        CatalogSku sku = requireOnSaleSku(dto.getSkuId());

        TradeCart existing = cartMapper.selectOne(Wrappers.<TradeCart>lambdaQuery()
                .eq(TradeCart::getUserId, userId)
                .eq(TradeCart::getSkuId, dto.getSkuId())
                .last("LIMIT 1"));

        int target = dto.getQuantity() + (existing == null ? 0 : existing.getQuantity());
        // 加购阶段就按库存封顶，省得用户一路走到结算页才被告知买不了这么多。
        // 真正防超卖的是下单时的条件 UPDATE，这里只是体验
        if (sku.getStock() != null && target > sku.getStock()) {
            target = Math.max(sku.getStock(), 0);
            if (target == 0) {
                throw new BizException(ErrorCode.STOCK_INSUFFICIENT, "这件商品暂时缺货了");
            }
        }

        if (existing == null) {
            TradeCart row = new TradeCart();
            row.setUserId(userId);
            row.setSkuId(dto.getSkuId());
            row.setQuantity(target);
            cartMapper.insert(row);
        } else {
            TradeCart update = new TradeCart();
            update.setId(existing.getId());
            update.setQuantity(target);
            cartMapper.updateById(update);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(Long skuId, Integer quantity) {
        Long userId = CurrentUser.requireUserId();
        if (quantity == null || quantity <= 0) {
            // 数量减到 0 等同于移除。让前端少写一个分支
            remove(skuId);
            return;
        }

        TradeCart existing = cartMapper.selectOne(Wrappers.<TradeCart>lambdaQuery()
                .eq(TradeCart::getUserId, userId)
                .eq(TradeCart::getSkuId, skuId)
                .last("LIMIT 1"));
        if (existing == null) {
            return;
        }

        CatalogSku sku = skuMapper.selectById(skuId);
        int target = quantity;
        if (sku != null && sku.getStock() != null && target > sku.getStock()) {
            target = Math.max(sku.getStock(), 1);
        }

        TradeCart update = new TradeCart();
        update.setId(existing.getId());
        update.setQuantity(target);
        cartMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long skuId) {
        cartMapper.delete(Wrappers.<TradeCart>lambdaQuery()
                .eq(TradeCart::getUserId, CurrentUser.requireUserId())
                .eq(TradeCart::getSkuId, skuId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clear() {
        cartMapper.delete(Wrappers.<TradeCart>lambdaQuery()
                .eq(TradeCart::getUserId, CurrentUser.requireUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeBySkuIds(Long userId, List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        cartMapper.delete(Wrappers.<TradeCart>lambdaQuery()
                .eq(TradeCart::getUserId, userId)
                .in(TradeCart::getSkuId, skuIds));
    }

    // ================================================================

    /**
     * 拼一条购物车展示行，顺带判定是否失效。
     *
     * <p>失效的条目仍然返回，只是标记出来——直接过滤掉的话，用户会觉得
     * "我明明加过的东西不见了"。
     */
    private CartItemVO toItem(TradeCart row, CatalogSku sku, Map<Long, CatalogSpecies> speciesById) {
        if (sku == null) {
            // SKU 已被删除。名字都查不到了，只能给一个占位
            return CartItemVO.builder()
                    .skuId(row.getSkuId())
                    .speciesName("商品已下架")
                    .quantity(row.getQuantity())
                    .price(BigDecimal.ZERO)
                    .subtotal(BigDecimal.ZERO)
                    .stock(0)
                    .unavailable(true)
                    .outOfStock(false)
                    .invalidReason("该商品已下架")
                    .build();
        }

        CatalogSpecies species = speciesById.get(sku.getSpeciesId());
        boolean unavailable = sku.getStatus() == null || sku.getStatus() != 1;
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        boolean outOfStock = !unavailable && stock < row.getQuantity();

        String reason = null;
        if (unavailable) {
            reason = "该商品已下架";
        } else if (stock == 0) {
            reason = "已售罄";
        } else if (outOfStock) {
            reason = "库存仅剩 " + stock + " 件";
        }

        return CartItemVO.builder()
                .skuId(sku.getId())
                .speciesId(sku.getSpeciesId())
                .slug(species == null ? null : species.getCode())
                .speciesName(species == null ? sku.getSkuCode() : species.getName())
                .spec(sku.getSpec())
                .image(sku.getImage())
                .imageAlt(sku.getImageAlt())
                .price(sku.getPrice())
                .quantity(row.getQuantity())
                .subtotal(sku.getPrice().multiply(BigDecimal.valueOf(row.getQuantity())))
                .stock(stock)
                .unavailable(unavailable)
                .outOfStock(outOfStock)
                .invalidReason(reason)
                .build();
    }

    private CatalogSku requireOnSaleSku(Long skuId) {
        CatalogSku sku = skuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BizException(ErrorCode.SKU_UNAVAILABLE, "这件商品已下架");
        }
        return sku;
    }
}
