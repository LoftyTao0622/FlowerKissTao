package com.zyt.flowerkisstao.trade.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import com.zyt.flowerkisstao.trade.application.service.OrderAdminService;
import com.zyt.flowerkisstao.trade.web.dto.ReasonDTO;
import com.zyt.flowerkisstao.trade.web.vo.OrderVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单管理，运营侧。方案原文："管理员负责发货和审核。"
 *
 * <p>三个权限点分得很细：读全部订单、发货、退款各自独立。运营改不了别人的账号，
 * 普通用户进不来这里——权限边界在 data.sql 里已经配好。
 */
@RestController
@RequestMapping("/api/admin/orders")
@Validated
public class OrderAdminController {

    private final OrderAdminService orderAdminService;

    public OrderAdminController(OrderAdminService orderAdminService) {
        this.orderAdminService = orderAdminService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_READ_ALL + "')")
    public R<IPage<OrderVO>> page(@RequestParam(defaultValue = "1") long current,
                                  @RequestParam(defaultValue = "10") long size,
                                  @RequestParam(required = false) Integer status,
                                  @RequestParam(required = false) String keyword) {
        return R.ok(orderAdminService.page(new Page<>(current, size), status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_READ_ALL + "')")
    public R<OrderVO> get(@PathVariable Long id) {
        return R.ok(orderAdminService.get(id));
    }

    @PutMapping("/{id}/ship")
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_SHIP + "')")
    public R<Void> ship(@PathVariable Long id) {
        orderAdminService.ship(id);
        return R.ok();
    }

    /** 售后通过：退款并归还库存 */
    @PutMapping("/{id}/after-sale/approve")
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_REFUND + "')")
    public R<Void> approveAfterSale(@PathVariable Long id) {
        orderAdminService.approveAfterSale(id);
        return R.ok();
    }

    /** 售后驳回：订单回到申请之前的状态 */
    @PutMapping("/{id}/after-sale/reject")
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_REFUND + "')")
    public R<Void> rejectAfterSale(@PathVariable Long id, @Valid @RequestBody ReasonDTO dto) {
        orderAdminService.rejectAfterSale(id, dto.getReason());
        return R.ok();
    }
}
