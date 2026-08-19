import { request } from '@/shared/api/request'

import type { Address, AddressInput } from '../types/trade'

/** 我的全部地址，默认地址排最前 */
export function fetchAddresses() {
  return request<Address[]>('/addresses')
}

/** 我的默认地址。一条都没有时后端返回 null，不是错误 */
export function fetchDefaultAddress() {
  return request<Address | null>('/addresses/default')
}

/** 返回新地址的 id */
export function createAddress(payload: AddressInput) {
  return request<number>('/addresses', { method: 'POST', body: payload })
}

export function updateAddress(id: number, payload: AddressInput) {
  return request<void>(`/addresses/${id}`, { method: 'PUT', body: payload })
}

export function deleteAddress(id: number) {
  return request<void>(`/addresses/${id}`, { method: 'DELETE' })
}

export function setDefaultAddress(id: number) {
  return request<void>(`/addresses/${id}/default`, { method: 'PUT' })
}
