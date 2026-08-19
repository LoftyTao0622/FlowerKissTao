package com.zyt.flowerkisstao.trade.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSkuMapper;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSpeciesMapper;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.care.application.service.CareArchiveService;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import com.zyt.flowerkisstao.trade.application.service.CartService;
import com.zyt.flowerkisstao.trade.application.service.OrderService;
import com.zyt.flowerkisstao.trade.domain.entity.TradeCart;
import com.zyt.flowerkisstao.trade.domain.entity.TradeOrder;
import com.zyt.flowerkisstao.trade.domain.entity.TradeOrderItem;
import com.zyt.flowerkisstao.trade.domain.entity.UserAddress;
import com.zyt.flowerkisstao.trade.domain.model.OrderAction;
import com.zyt.flowerkisstao.trade.domain.model.OrderStatus;
import com.zyt.flowerkisstao.trade.domain.model.OrderTransition;
import com.zyt.flowerkisstao.trade.infrastructure.mapper.TradeCartMapper;
import com.zyt.flowerkisstao.trade.infrastructure.mapper.TradeOrderItemMapper;
import com.zyt.flowerkisstao.trade.infrastructure.mapper.TradeOrderMapper;
import com.zyt.flowerkisstao.trade.infrastructure.mapper.UserAddressMapper;
import com.zyt.flowerkisstao.trade.web.dto.OrderCreateDTO;
import com.zyt.flowerkisstao.trade.web.dto.PayDTO;
import com.zyt.flowerkisstao.trade.web.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单，顾客侧。
 *
 * <p>这个类里有本模块全部两处真正的并发正确性设计，都值得单独看一眼：
 * <ul>
 *   <li>{@link #create} —— 事务内条件 UPDATE 扣库存，防超卖</li>
 *   <li>{@link #pay} —— 唯一索引兜底的幂等，防重复支付</li>
 * </ul>
 * 状态流转规则不在这里，在 {@link OrderTransition}——那是个可脱离容器单测的纯函数。
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    /** 订单号前半段：秒级时间戳，可读且天然有序 */
    private static final DateTimeFormatter ORDER_NO_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 订单号撞唯一索引时的重试次数。6 位随机撞一次已属罕见，连撞三次可以认为是别的问题 */
    private static final int ORDER_NO_RETRY = 3;

    private final TradeOrderMapper orderMapper;
    private final TradeOrderItemMapper orderItemMapper;
    private final TradeCartMapper cartMapper;
    private final UserAddressMapper addressMapper;
    private final CatalogSkuMapper skuMapper;
    private final CatalogSpeciesMapper speciesMapper;
    private final CartService cartService;
    private final OrderStockSupport stockSupport;
    /** 确认收货后建立养护档案。方案要求把收货事件传给养护模块 */
    private final CareArchiveService careArchiveService;

    public OrderServiceImpl(TradeOrderMapper orderMapper,
                            TradeOrderItemMapper orderItemMapper,
                            TradeCartMapper cartMapper,
                            UserAddressMapper addressMapper,
                            CatalogSkuMapper skuMapper,
                            CatalogSpeciesMapper speciesMapper,
                            CartService cartService,
                            OrderStockSupport stockSupport,
                            CareArchiveService careArchiveService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartMapper = cartMapper;
        this.addressMapper = addressMapper;
        this.skuMapper = skuMapper;
        this.speciesMapper = speciesMapper;
        this.cartService = cartService;
        this.stockSupport = stockSupport;
        this.careArchiveService = careArchiveService;
    }

    // ================================================================
    // 下单：防超卖
    // ================================================================

    /**
     * 下单。
     *
     * <p>整个方法在一个事务里，顺序是：校地址 → 校商品 → <b>扣库存</b> → 建订单 → 清购物车。
     * 库存扣减放在建订单之前，是因为它是唯一可能失败的一步；先建订单再扣库存的话，
     * 扣失败时已经产生了一条脏订单（虽然会回滚，但 order_no 的自增值已经消耗掉了）。
     *
     * <p>价格全部从 {@code catalog_sku} 实时读，入参里根本没有价格字段。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(OrderCreateDTO dto) {
        Long userId = CurrentUser.requireUserId();

        UserAddress address = addressMapper.selectById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ADDRESS_NOT_FOUND, "收货地址不存在");
        }

        // 数量取购物车里的，不取入参——入参只说"买哪几个 SKU"
        List<TradeCart> cartRows = cartMapper.selectList(Wrappers.<TradeCart>lambdaQuery()
                .eq(TradeCart::getUserId, userId)
                .in(TradeCart::getSkuId, dto.getSkuIds()));
        if (cartRows.isEmpty()) {
            throw new BizException(ErrorCode.CART_EMPTY, "购物车里没有可结算的商品");
        }

        // 按 skuId 升序扣减。两笔订单同时买 A 和 B 时，若一笔按 A→B、另一笔按 B→A
        // 的顺序加锁，就会死锁。统一升序之后加锁顺序一致，死锁不会发生
        cartRows.sort(Comparator.comparing(TradeCart::getSkuId));

        List<Long> skuIds = cartRows.stream().map(TradeCart::getSkuId).toList();
        Map<Long, CatalogSku> skuById = skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(CatalogSku::getId, Function.identity()));
        Map<Long, CatalogSpecies> speciesById = loadSpecies(skuById.values());

        List<TradeOrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int itemCount = 0;

        for (TradeCart row : cartRows) {
            CatalogSku sku = skuById.get(row.getSkuId());
            if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
                throw new BizException(ErrorCode.SKU_UNAVAILABLE,
                        "有商品已下架，请回购物车移除后再结算");
            }

            // ===== 防超卖的那一行 =====
            // 条件写在 WHERE 里，判断与扣减是同一条语句，由数据库行锁保证原子性。
            // 影响行数为 0 就是没抢到，抛异常让整个事务回滚，前面已扣的也一并还原
            int affected = skuMapper.deductStock(sku.getId(), row.getQuantity());
            if (affected == 0) {
                CatalogSpecies species = speciesById.get(sku.getSpeciesId());
                String name = species == null ? sku.getSkuCode() : species.getName();
                throw new BizException(ErrorCode.STOCK_INSUFFICIENT,
                        name + " 库存不足，仅剩 " + safeStock(sku) + " 件");
            }

            BigDecimal unitPrice = sku.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(row.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
            itemCount += row.getQuantity();

            items.add(buildItem(row, sku, speciesById.get(sku.getSpeciesId()), unitPrice, subtotal));
        }

        TradeOrder order = new TradeOrder();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAY.code());
        order.setTotalAmount(totalAmount);
        order.setItemCount(itemCount);
        // 地址存快照：用户删掉地址簿里那条之后，历史订单仍要显示当时寄到哪
        order.setReceiver(address.getReceiver());
        order.setPhone(address.getPhone());
        order.setAddressSnapshot(address.fullAddress());
        insertOrderWithRetry(order);

        for (TradeOrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 只清结算掉的那几条，没勾的留在车里
        cartService.removeBySkuIds(userId, skuIds);

        return TradeConverter.toVO(order, items, OrderAction.Actor.CUSTOMER);
    }

    // ================================================================
    // 支付：幂等
    // ================================================================

    /**
     * 模拟支付。方案原文："支付采用可演示的模拟流程，结合幂等标识避免重复付款。"
     *
     * <p>防重复付款有两道，缺一不可：
     * <ol>
     *   <li><b>条件 UPDATE</b>（{@code WHERE status = 待付款}）——挡住并发。两个请求同时
     *       读到"待付款"时，只有一条 UPDATE 能成功，另一条影响行数为 0</li>
     *   <li><b>唯一索引</b>（{@code pay_idem_key}）——挡住重放。同一个 key 换个时间再来，
     *       数据库层面直接撞键</li>
     * </ol>
     * 只做"先查再改"是不够的：查和改之间有窗口，并发下两个请求都会觉得自己该扣钱。
     *
     * <p>同一个 idemKey 重发时**返回成功而不是报错**：用户的意图已经达成了，
     * 弹一个"支付失败"只会让他以为钱没扣掉，然后再点一次。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO pay(Long id, PayDTO dto) {
        TradeOrder order = requireOwnedOrder(id);

        // 已经用同一个 key 付过：这是重发，幂等返回当前状态
        if (dto.getIdemKey().equals(order.getPayIdemKey())) {
            return get(id);
        }
        requireTransition(order, OrderAction.PAY);

        int affected;
        try {
            affected = orderMapper.markPaid(id,
                    OrderStatus.PENDING_PAY.code(), OrderStatus.PAID.code(),
                    dto.getIdemKey(), LocalDateTime.now());
        } catch (DuplicateKeyException e) {
            // 这个 key 被别的订单用过，是重放而非重发。明确拒绝
            throw new BizException(ErrorCode.PAY_DUPLICATED, "这笔支付已经处理过了");
        }

        if (affected == 0) {
            // 并发下输给了另一个请求。那一个已经把订单付掉了，
            // 对用户而言结果是一样的——查一下当前状态返回即可
            TradeOrder latest = orderMapper.selectById(id);
            if (latest != null && OrderStatus.of(latest.getStatus()) != OrderStatus.PENDING_PAY) {
                return get(id);
            }
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "订单状态已变化，请刷新后重试");
        }
        return get(id);
    }

    // ================================================================
    // 查询
    // ================================================================

    @Override
    public IPage<OrderVO> pageMine(IPage<?> page, Integer status) {
        Long userId = CurrentUser.requireUserId();
        @SuppressWarnings("unchecked")
        IPage<TradeOrder> orderPage = orderMapper.selectPage((IPage<TradeOrder>) page,
                Wrappers.<TradeOrder>lambdaQuery()
                        .eq(TradeOrder::getUserId, userId)
                        .eq(status != null, TradeOrder::getStatus, status)
                        .orderByDesc(TradeOrder::getId));

        Map<Long, List<TradeOrderItem>> itemsByOrder = loadItems(orderPage.getRecords());
        return orderPage.convert(order -> TradeConverter.toVO(order,
                itemsByOrder.getOrDefault(order.getId(), List.of()),
                OrderAction.Actor.CUSTOMER));
    }

    @Override
    public OrderVO get(Long id) {
        TradeOrder order = requireOwnedOrder(id);
        return TradeConverter.toVO(order, itemsOf(id), OrderAction.Actor.CUSTOMER);
    }

    // ================================================================
    // 状态流转（顾客侧）
    // ================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String reason) {
        TradeOrder order = requireOwnedOrder(id);
        OrderStatus target = requireTransition(order, OrderAction.CANCEL);

        // 未支付的订单库存也是扣着的，取消必须还回去。
        // 归还逻辑只有 OrderStockSupport 一处，售后通过走的也是它
        stockSupport.restore(id);

        TradeOrder update = new TradeOrder();
        update.setId(id);
        update.setStatus(target.code());
        update.setCancelReason(reason == null || reason.isBlank() ? "用户主动取消" : reason);
        update.setClosedAt(LocalDateTime.now());
        orderMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long id) {
        TradeOrder order = requireOwnedOrder(id);
        OrderStatus target = requireTransition(order, OrderAction.RECEIVE);

        TradeOrder update = new TradeOrder();
        update.setId(id);
        update.setStatus(target.code());
        update.setReceivedAt(LocalDateTime.now());
        orderMapper.updateById(update);

        // 确认收货 → 建立"我的植物"。方案原文："确认收货事件会把植物品种、购买时间
        // 和用户场景传递给养护模块，自动建立'我的植物'，打通推荐、交易和持续服务。"
        //
        // 建档失败不能让确认收货失败：用户已经收到货了，这是既成事实。养护档案没建上
        // 是可以补的，把收货操作回滚掉则会让用户困在"运输中"状态里出不来
        try {
            careArchiveService.createFromOrder(order.getUserId(), id);
        } catch (Exception e) {
            log.warn("确认收货后建立养护档案失败，订单 {}：{}", id, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyAfterSale(Long id, String reason) {
        TradeOrder order = requireOwnedOrder(id);
        OrderStatus target = requireTransition(order, OrderAction.APPLY_AFTER_SALE);

        TradeOrder update = new TradeOrder();
        update.setId(id);
        update.setStatus(target.code());
        // 记下从哪个状态来的，驳回时要退回去
        update.setPrevStatus(order.getStatus());
        update.setRefundReason(reason);
        orderMapper.updateById(update);
    }

    // ================================================================
    // 共用
    // ================================================================

    /** 校验流转是否允许，返回目标状态。不允许就抛，带上人话理由 */
    static OrderStatus requireTransition(TradeOrder order, OrderAction action) {
        OrderStatus current = OrderStatus.of(order.getStatus());
        OrderStatus prev = order.getPrevStatus() == null
                ? null : OrderStatus.of(order.getPrevStatus());
        OrderTransition.Result result = OrderTransition.check(current, action, prev);
        if (!result.allowed()) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, result.reason());
        }
        return result.target();
    }

    /**
     * 取订单并确认归属。
     *
     * <p>不属于当前用户时抛 ORDER_NOT_FOUND 而非 FORBIDDEN：返回 403 等于确认
     * "这个订单号确实存在"，把别人的订单 id 空间暴露出去了。与画像、推荐同一口径。
     */
    private TradeOrder requireOwnedOrder(Long id) {
        TradeOrder order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(CurrentUser.requireUserId())) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private List<TradeOrderItem> itemsOf(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<TradeOrderItem>lambdaQuery()
                .eq(TradeOrderItem::getOrderId, orderId)
                .orderByAsc(TradeOrderItem::getId));
    }

    /** 批量取条目，避免列表页 N+1 */
    private Map<Long, List<TradeOrderItem>> loadItems(List<TradeOrder> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<Long> orderIds = orders.stream().map(TradeOrder::getId).toList();
        return orderItemMapper.selectList(Wrappers.<TradeOrderItem>lambdaQuery()
                        .in(TradeOrderItem::getOrderId, orderIds)
                        .orderByAsc(TradeOrderItem::getId)).stream()
                .collect(Collectors.groupingBy(TradeOrderItem::getOrderId));
    }

    private Map<Long, CatalogSpecies> loadSpecies(java.util.Collection<CatalogSku> skus) {
        List<Long> speciesIds = skus.stream()
                .map(CatalogSku::getSpeciesId).distinct().toList();
        if (speciesIds.isEmpty()) {
            return Map.of();
        }
        return speciesMapper.selectBatchIds(speciesIds).stream()
                .collect(Collectors.toMap(CatalogSpecies::getId, Function.identity()));
    }

    private TradeOrderItem buildItem(TradeCart row, CatalogSku sku, CatalogSpecies species,
                                     BigDecimal unitPrice, BigDecimal subtotal) {
        TradeOrderItem item = new TradeOrderItem();
        item.setSkuId(sku.getId());
        item.setSpeciesId(sku.getSpeciesId());
        // 快照：商品会改名、调价、下架，历史订单必须显示当时买的是什么
        item.setSpeciesName(species == null ? sku.getSkuCode() : species.getName());
        item.setSpec(sku.getSpec());
        item.setImage(sku.getImage());
        item.setUnitPrice(unitPrice);
        item.setQuantity(row.getQuantity());
        item.setSubtotal(subtotal);
        return item;
    }

    /**
     * 订单号：秒级时间戳 + 6 位随机。可读、可排序、不需要序列表。
     *
     * <p>同一秒内多笔订单靠随机数区分，撞了由唯一索引兜住并重试。
     */
    private String nextOrderNo() {
        return LocalDateTime.now().format(ORDER_NO_TIME)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    /** 订单号撞唯一索引时换一个再来。三次都撞说明不是随机数的问题，让它抛出去 */
    private void insertOrderWithRetry(TradeOrder order) {
        for (int attempt = 1; ; attempt++) {
            try {
                orderMapper.insert(order);
                return;
            } catch (DuplicateKeyException e) {
                if (attempt >= ORDER_NO_RETRY) {
                    throw e;
                }
                order.setOrderNo(nextOrderNo());
            }
        }
    }

    private int safeStock(CatalogSku sku) {
        return sku.getStock() == null ? 0 : sku.getStock();
    }
}
