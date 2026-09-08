import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as addressApi from '../api/address'
import * as orderApi from '../api/order'
import type { Address, Order } from '../types/trade'
import { createRequestGuard } from '@/shared/state/requestGuard'
import { registerSessionReset } from '@/shared/state/sessionRegistry'

/**
 * 订单。
 *
 * <p>状态判断一律用后端返回的 {@code actions}，前端不自己推导"这个状态能做什么"——
 * 状态机在后端，前端再实现一遍必然会有出入，界面上就会出现点下去必然报错的按钮。
 */
export const useOrderStore = defineStore('order', () => {
  const orders = ref<Order[]>([])
  const total = ref(0)
  const current = ref<Order | null>(null)
  const addresses = ref<Address[]>([])

  const loading = ref(false)
  const submitting = ref(false)
  const errorMessage = ref('')

  /**
   * 支付幂等标识，按订单 id 缓存。
   *
   * <p>方案要求"结合幂等标识避免重复付款"。关键是**重试时必须用同一个 key**——
   * 每次点击都生成新 key 的话，幂等就完全失效了。所以第一次进入支付流程时生成，
   * 之后同一笔订单一直复用。
   */
  const idemKeys = ref<Record<number, string>>({})
  const checkoutIdemKey = ref<string | null>(null)
  const requestGuard = createRequestGuard()

  const hasOrders = computed(() => orders.value.length > 0)

  function idemKeyFor(orderId: number) {
    const existing = idemKeys.value[orderId]
    if (existing) return existing
    // crypto.randomUUID 在所有现代浏览器与 https/localhost 下都可用
    const key = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `${orderId}-${Date.now()}-${Math.random().toString(36).slice(2)}`
    idemKeys.value = { ...idemKeys.value, [orderId]: key }
    return key
  }

  async function loadMine(page = 1, size = 10, status?: number) {
    const token = requestGuard.begin('list')
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await orderApi.fetchMyOrders(page, size, status)
      if (requestGuard.isCurrent(token)) {
        orders.value = result.records
        total.value = result.total
      }
    } catch (error) {
      if (requestGuard.isCurrent(token)) {
        errorMessage.value = error instanceof Error ? error.message : '订单加载失败'
      }
    } finally {
      if (requestGuard.isCurrent(token)) loading.value = false
    }
  }

  async function loadOne(id: number) {
    const token = requestGuard.begin('detail')
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await orderApi.fetchOrder(id)
      if (requestGuard.isCurrent(token)) current.value = result
    } catch (error) {
      if (requestGuard.isCurrent(token)) {
        errorMessage.value = error instanceof Error ? error.message : '订单加载失败'
        current.value = null
      }
    } finally {
      if (requestGuard.isCurrent(token)) loading.value = false
    }
  }

  /** 下单。成功返回新订单，失败返回 null 并把原因写进 errorMessage */
  async function submit(addressId: number, skuIds: number[]) {
    const session = requestGuard.captureSession()
    const idemKey = checkoutIdemKey.value ?? createIdemKey()
    checkoutIdemKey.value = idemKey
    submitting.value = true
    errorMessage.value = ''
    try {
      const order = await orderApi.createOrder(addressId, skuIds, idemKey)
      if (!requestGuard.isSessionCurrent(session)) return null
      current.value = order
      checkoutIdemKey.value = null
      return order
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '下单失败'
      }
      return null
    } finally {
      if (requestGuard.isSessionCurrent(session)) submitting.value = false
    }
  }

  /** 模拟支付。同一笔订单重试时复用同一个 idemKey */
  async function pay(id: number) {
    const session = requestGuard.captureSession()
    submitting.value = true
    errorMessage.value = ''
    try {
      const result = await orderApi.payOrder(id, idemKeyFor(id))
      if (!requestGuard.isSessionCurrent(session)) return false
      current.value = result
      return true
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '支付失败'
      }
      return false
    } finally {
      if (requestGuard.isSessionCurrent(session)) submitting.value = false
    }
  }

  /** 取消 / 确认收货 / 申请售后共用一条收尾路径，成功后都要刷新详情 */
  async function runAction(id: number, action: () => Promise<void>, failMessage: string) {
    const session = requestGuard.captureSession()
    submitting.value = true
    errorMessage.value = ''
    try {
      await action()
      if (!requestGuard.isSessionCurrent(session)) return false
      await loadOne(id)
      return true
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : failMessage
      }
      return false
    } finally {
      if (requestGuard.isSessionCurrent(session)) submitting.value = false
    }
  }

  function cancel(id: number, reason?: string) {
    return runAction(id, () => orderApi.cancelOrder(id, reason), '取消失败')
  }

  function receive(id: number) {
    return runAction(id, () => orderApi.receiveOrder(id), '确认收货失败')
  }

  function applyAfterSale(id: number, reason: string) {
    return runAction(id, () => orderApi.applyAfterSale(id, reason), '申请售后失败')
  }

  async function loadAddresses() {
    const token = requestGuard.begin('addresses')
    try {
      const result = await addressApi.fetchAddresses()
      if (requestGuard.isCurrent(token)) addresses.value = result
    } catch (error) {
      if (requestGuard.isCurrent(token)) {
        errorMessage.value = error instanceof Error ? error.message : '地址加载失败'
      }
    }
  }

  /** 退出登录时清空，避免下一个账号看到上一个人的订单 */
  function reset() {
    requestGuard.reset()
    orders.value = []
    total.value = 0
    current.value = null
    addresses.value = []
    errorMessage.value = ''
    idemKeys.value = {}
    checkoutIdemKey.value = null
    loading.value = false
    submitting.value = false
  }

  function createIdemKey() {
    return typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `checkout-${Date.now()}-${Math.random().toString(36).slice(2)}`
  }

  registerSessionReset(reset)

  return {
    orders,
    total,
    current,
    addresses,
    loading,
    submitting,
    errorMessage,
    hasOrders,
    idemKeyFor,
    loadMine,
    loadOne,
    submit,
    pay,
    cancel,
    receive,
    applyAfterSale,
    loadAddresses,
    reset,
  }
})
