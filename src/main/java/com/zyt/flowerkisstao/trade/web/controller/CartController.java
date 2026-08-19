package com.zyt.flowerkisstao.trade.web.controller;

import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import com.zyt.flowerkisstao.trade.application.service.CartService;
import com.zyt.flowerkisstao.trade.web.dto.CartAddDTO;
import com.zyt.flowerkisstao.trade.web.vo.CartVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 购物车。
 *
 * <p>路径挂 /api/cart，不落 SecurityConfig 的公开 GET 白名单前缀，自动要求登录。
 *
 * <p>权限点用 trade:order:create——能加购就意味着能下单，两者是同一件事的两个阶段，
 * 不必再拆一个 cart 权限出来。
 *
 * <p>接口一概不接受价格：价格由后端从 catalog_sku 实时读。
 */
@RestController
@RequestMapping("/api/cart")
@Validated
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_CREATE + "')")
    public R<CartVO> listMine() {
        return R.ok(cartService.listMine());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_CREATE + "')")
    public R<Void> add(@Valid @RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return R.ok();
    }

    /** 改数量。传 0 等同于移除 */
    @PutMapping("/{skuId}")
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_CREATE + "')")
    public R<Void> updateQuantity(@PathVariable Long skuId, @RequestParam Integer quantity) {
        cartService.updateQuantity(skuId, quantity);
        return R.ok();
    }

    @DeleteMapping("/{skuId}")
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_CREATE + "')")
    public R<Void> remove(@PathVariable Long skuId) {
        cartService.remove(skuId);
        return R.ok();
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_CREATE + "')")
    public R<Void> clear() {
        cartService.clear();
        return R.ok();
    }
}
