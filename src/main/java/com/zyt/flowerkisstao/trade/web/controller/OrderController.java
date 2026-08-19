package com.zyt.flowerkisstao.trade.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import com.zyt.flowerkisstao.trade.application.service.OrderService;
import com.zyt.flowerkisstao.trade.web.dto.OrderCreateDTO;
import com.zyt.flowerkisstao.trade.web.dto.PayDTO;
import com.zyt.flowerkisstao.trade.web.dto.ReasonDTO;
import com.zyt.flowerkisstao.trade.web.vo.OrderVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单，顾客侧。
 *
 * <p>路径挂 /api/orders，不落公开 GET 白名单前缀，自动要求登录。越权访问一律按
 * "不存在"处理——返回 403 等于确认这个订单号真实存在。
 */
@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 下单。事务内扣库存，库存不足返回 6003 */
    @PostMapping
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_CREATE + "')")
    public R<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return R.ok(orderService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_READ_OWN + "')")
    public R<IPage<OrderVO>> pageMine(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(orderService.pageMine(new Page<>(current, size), status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_READ_OWN + "')")
    public R<OrderVO> get(@PathVariable Long id) {
        return R.ok(orderService.get(id));
    }

    /**
     * 模拟支付。同一个 idemKey 重复提交只会真正成功一次，重发返回同样的结果。
     *
     * <p>用 POST 而非 PUT：它不是幂等的资源覆盖，而是一次有副作用的动作。
     */
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_CREATE + "')")
    public R<OrderVO> pay(@PathVariable Long id, @Valid @RequestBody PayDTO dto) {
        return R.ok(orderService.pay(id, dto));
    }

    /** 取消未支付订单，库存归还 */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_CANCEL_OWN + "')")
    public R<Void> cancel(@PathVariable Long id,
                          @RequestBody(required = false) ReasonDTO dto) {
        orderService.cancel(id, dto == null ? null : dto.getReason());
        return R.ok();
    }

    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_READ_OWN + "')")
    public R<Void> receive(@PathVariable Long id) {
        orderService.receive(id);
        return R.ok();
    }

    @PutMapping("/{id}/after-sale")
    @PreAuthorize("hasAuthority('" + Perms.TRADE_ORDER_READ_OWN + "')")
    public R<Void> applyAfterSale(@PathVariable Long id, @Valid @RequestBody ReasonDTO dto) {
        orderService.applyAfterSale(id, dto.getReason());
        return R.ok();
    }
}
