import { request } from '@/shared/api/request'
import type { PageResult } from '@/shared/api/types'

import type { Order } from '../types/trade'

/**
 * 下单。
 *
 * 只传地址与要结算哪几个 SKU——数量取购物车里的，价格由后端实时算。
 * 库存不足时抛 ApiError(6003)，商品下架时 6005。
 */
export function createOrder(addressId: number, skuIds: number[], idemKey: string) {
  return request<Order>('/orders', {
    method: 'POST',
    body: { addressId, skuIds, idemKey },
  })
}

/** 我的订单。status 传 undefined 表示全部 */
export function fetchMyOrders(current = 1, size = 10, status?: number) {
  return request<PageResult<Order>>('/orders', { query: { current, size, status } })
}

export function fetchOrder(id: number) {
  return request<Order>(`/orders/${id}`)
}

/**
 * 模拟支付。
 *
 * idemKey 由调用方生成并在重试时保持不变——这是方案要求的"幂等标识"。
 * 同一个 key 重复提交只会真正扣一次款，重发返回同样的结果而不是报错。
 */
export function payOrder(id: number, idemKey: string) {
  return request<Order>(`/orders/${id}/pay`, { method: 'POST', body: { idemKey } })
}

export function cancelOrder(id: number, reason?: string) {
  return request<void>(`/orders/${id}/cancel`, { method: 'PUT', body: { reason } })
}

export function receiveOrder(id: number) {
  return request<void>(`/orders/${id}/receive`, { method: 'PUT' })
}

export function applyAfterSale(id: number, reason: string) {
  return request<void>(`/orders/${id}/after-sale`, { method: 'PUT', body: { reason } })
}

// ===== 管理端 =====

export function fetchAllOrders(current = 1, size = 10, status?: number, keyword?: string) {
  return request<PageResult<Order>>('/admin/orders', {
    query: { current, size, status, keyword },
  })
}

export function shipOrder(id: number) {
  return request<void>(`/admin/orders/${id}/ship`, { method: 'PUT' })
}

export function approveAfterSale(id: number) {
  return request<void>(`/admin/orders/${id}/after-sale/approve`, { method: 'PUT' })
}

export function rejectAfterSale(id: number, reason: string) {
  return request<void>(`/admin/orders/${id}/after-sale/reject`, {
    method: 'PUT',
    body: { reason },
  })
}
