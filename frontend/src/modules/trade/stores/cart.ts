import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as cartApi from '../api/cart'
import type { Cart, CartItem } from '../types/trade'
import { createRequestGuard } from '@/shared/state/requestGuard'
import { registerSessionReset } from '@/shared/state/sessionRegistry'

/**
 * 改造前购物车存在这个 localStorage 键下。
 *
 * 旧数据按 slug 存、还带着客户端写的价格，两样都没法安全迁移：slug 换不出 skuId
 * （一个品种有多个规格），价格更是不可信。所以只能提示用户后清掉——
 * 悄悄丢弃会让人以为东西不见了。
 */
const LEGACY_KEY = 'flower-kiss-tao-cart'

/**
 * 购物车。
 *
 * <p>改造前这里是纯 localStorage 实现，且**把价格存在客户端**——用户改一下浏览器
 * 存储就能按自己写的价格下单。现在购物车落在后端表里，前端只存后端返回的快照，
 * 价格与库存每次都由后端从 catalog_sku 实时读。
 */
export const useCartStore = defineStore('cart', () => {
  const cart = ref<Cart | null>(null)
  const loading = ref(false)
  const errorMessage = ref('')

  /** 旧版购物车被清理时置为 true，界面上提示一次 */
  const legacyCleared = ref(false)
  const requestGuard = createRequestGuard()

  const items = computed<CartItem[]>(() => cart.value?.items ?? [])

  /** 徽标数字。只数可结算的，失效条目不该让徽标虚高 */
  const totalCount = computed(() => cart.value?.totalCount ?? 0)

  const totalAmount = computed(() => cart.value?.totalAmount ?? 0)

  const hasInvalid = computed(() => cart.value?.hasInvalid ?? false)

  /** 可结算的条目，结算页提交的就是这些 */
  const purchasableItems = computed(() =>
    items.value.filter((item) => !item.unavailable && !item.outOfStock),
  )

  const isEmpty = computed(() => items.value.length === 0)

  async function load() {
    const token = requestGuard.begin()
    // 首次加载时清掉改造前的 localStorage 购物车。旧数据按 slug 存、还带着客户端
    // 价格，两样都没法安全迁移。检测到旧数据会置 legacyCleared，由界面提示用户。
    purgeLegacyCart()
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await cartApi.fetchCart()
      if (requestGuard.isCurrent(token)) cart.value = result
    } catch (error) {
      if (requestGuard.isCurrent(token)) {
        errorMessage.value = error instanceof Error ? error.message : '购物车加载失败'
      }
    } finally {
      if (requestGuard.isCurrent(token)) loading.value = false
    }
  }

  /**
   * 加购。成功后重新拉一次而不是本地累加——库存上限、失效标记、实时价都由后端算，
   * 本地拼的话这些字段会是旧的。
   */
  async function add(skuId: number, quantity = 1) {
    const session = requestGuard.captureSession()
    errorMessage.value = ''
    try {
      await cartApi.addToCart(skuId, quantity)
      if (!requestGuard.isSessionCurrent(session)) return false
      await load()
      return true
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '加入购物车失败'
      }
      return false
    }
  }

  async function setQuantity(skuId: number, quantity: number) {
    const session = requestGuard.captureSession()
    errorMessage.value = ''
    try {
      await cartApi.updateCartQuantity(skuId, quantity)
      if (!requestGuard.isSessionCurrent(session)) return false
      await load()
      return true
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '修改数量失败'
      }
      return false
    }
  }

  async function remove(skuId: number) {
    const session = requestGuard.captureSession()
    errorMessage.value = ''
    try {
      await cartApi.removeFromCart(skuId)
      if (!requestGuard.isSessionCurrent(session)) return false
      await load()
      return true
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '移除失败'
      }
      return false
    }
  }

  async function clear() {
    const session = requestGuard.captureSession()
    errorMessage.value = ''
    try {
      await cartApi.clearCart()
      if (!requestGuard.isSessionCurrent(session)) return false
      await load()
      return true
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '清空失败'
      }
      return false
    }
  }

  /**
   * 清掉改造前遗留的 localStorage 购物车。
   *
   * 应用启动时调一次。检测到旧数据就置 {@link legacyCleared}，由界面提示用户
   * "购物车已升级，请重新加购"——静默清掉的话，用户会以为自己加的东西丢了。
   */
  function purgeLegacyCart() {
    if (typeof window === 'undefined') return
    try {
      const raw = localStorage.getItem(LEGACY_KEY)
      if (raw && raw !== '[]') {
        legacyCleared.value = true
      }
      if (raw !== null) {
        localStorage.removeItem(LEGACY_KEY)
      }
    } catch {
      // 隐私模式下 localStorage 可能不可用，忽略即可
    }
  }

  function dismissLegacyNotice() {
    legacyCleared.value = false
  }

  /** 退出登录时清空，避免下一个账号看到上一个人的购物车 */
  function reset() {
    requestGuard.reset()
    cart.value = null
    errorMessage.value = ''
    loading.value = false
  }

  registerSessionReset(reset)

  return {
    cart,
    items,
    loading,
    errorMessage,
    legacyCleared,
    totalCount,
    totalAmount,
    hasInvalid,
    purchasableItems,
    isEmpty,
    load,
    add,
    setQuantity,
    remove,
    clear,
    purgeLegacyCart,
    dismissLegacyNotice,
    reset,
  }
})
