package com.zyt.flowerkisstao.trade.application.service.impl;

import com.zyt.flowerkisstao.trade.domain.entity.TradeOrder;
import com.zyt.flowerkisstao.trade.domain.entity.TradeOrderItem;
import com.zyt.flowerkisstao.trade.domain.entity.UserAddress;
import com.zyt.flowerkisstao.trade.domain.model.OrderAction;
import com.zyt.flowerkisstao.trade.domain.model.OrderStatus;
import com.zyt.flowerkisstao.trade.domain.model.OrderTransition;
import com.zyt.flowerkisstao.trade.web.vo.AddressVO;
import com.zyt.flowerkisstao.trade.web.vo.OrderItemVO;
import com.zyt.flowerkisstao.trade.web.vo.OrderVO;

import java.util.List;

/**
 * 交易模块的实体到展示对象映射。集中放一处，免得同一份文案在服务与控制器里各写一遍。
 */
final class TradeConverter {

    private TradeConverter() {
    }

    static AddressVO toVO(UserAddress address) {
        if (address == null) {
            return null;
        }
        return AddressVO.builder()
                .id(address.getId())
                .receiver(address.getReceiver())
                .phone(address.getPhone())
                .province(address.getProvince())
                .city(address.getCity())
                .district(address.getDistrict())
                .detail(address.getDetail())
                .isDefault(address.getIsDefault() != null && address.getIsDefault() == 1)
                .fullAddress(address.fullAddress())
                .build();
    }

    static OrderVO toVO(TradeOrder order, List<TradeOrderItem> items, OrderAction.Actor actor) {
        OrderStatus status = OrderStatus.of(order.getStatus());
        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .status(status.code())
                .statusLabel(status.label())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItemCount())
                .receiver(order.getReceiver())
                .phone(order.getPhone())
                .addressSnapshot(order.getAddressSnapshot())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .receivedAt(order.getReceivedAt())
                .closedAt(order.getClosedAt())
                .cancelReason(order.getCancelReason())
                .refundReason(order.getRefundReason())
                .refundRejectReason(order.getRefundRejectReason())
                .items(items == null ? List.of() : items.stream().map(TradeConverter::toVO).toList())
                // 由后端说了算当前能做哪些动作，界面上就不会出现点下去必然报错的按钮
                .actions(OrderTransition.availableActions(status, actor).stream()
                        .map(action -> new OrderVO.ActionVO(action.name(), action.label()))
                        .toList())
                .build();
    }

    static OrderItemVO toVO(TradeOrderItem item) {
        return OrderItemVO.builder()
                .id(item.getId())
                .skuId(item.getSkuId())
                .speciesId(item.getSpeciesId())
                .speciesName(item.getSpeciesName())
                .spec(item.getSpec())
                .image(item.getImage())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
