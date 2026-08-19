import { request } from '@/shared/api/request'

import type { Cart } from '../types/trade'

/** 我的购物车。价格与库存都是后端实时读出来的 */
export function fetchCart() {
  return request<Cart>('/cart')
}

/**
 * 加购。
 *
 * 注意只传 skuId 和数量——接口不接受价格，价格由后端从 catalog_sku 读。
 * 改造前的购物车把价格存在 localStorage 里，用户改一下就能改价。
 */
export function addToCart(skuId: number, quantity = 1) {
  return request<void>('/cart', { method: 'POST', body: { skuId, quantity } })
}

/** 改数量。传 0 等同于移除 */
export function updateCartQuantity(skuId: number, quantity: number) {
  return request<void>(`/cart/${skuId}`, { method: 'PUT', query: { quantity } })
}

export function removeFromCart(skuId: number) {
  return request<void>(`/cart/${skuId}`, { method: 'DELETE' })
}

export function clearCart() {
  return request<void>('/cart', { method: 'DELETE' })
}
