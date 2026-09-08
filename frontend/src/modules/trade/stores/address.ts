import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as addressApi from '../api/address'
import type { Address, AddressInput } from '../types/trade'
import { createRequestGuard } from '@/shared/state/requestGuard'
import { registerSessionReset } from '@/shared/state/sessionRegistry'

/** 收货地址簿 */
export const useAddressStore = defineStore('address', () => {
  const addresses = ref<Address[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const errorMessage = ref('')
  const requestGuard = createRequestGuard()

  /** 默认地址。后端已把它排在最前，取第一条即可 */
  const defaultAddress = computed(
    () => addresses.value.find((item) => item.isDefault) ?? addresses.value[0] ?? null,
  )

  const isEmpty = computed(() => addresses.value.length === 0)

  async function load() {
    const token = requestGuard.begin()
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await addressApi.fetchAddresses()
      if (requestGuard.isCurrent(token)) addresses.value = result
    } catch (error) {
      if (requestGuard.isCurrent(token)) {
        errorMessage.value = error instanceof Error ? error.message : '地址加载失败'
      }
    } finally {
      if (requestGuard.isCurrent(token)) loading.value = false
    }
  }

  /**
   * 保存。id 为 null 是新建。
   *
   * 成功后重新拉一次而不是本地拼：后端会派生 fullAddress、还会自动把第一条设为默认，
   * 本地拼的话这些字段会是旧的或空的。
   */
  async function save(id: number | null, payload: AddressInput) {
    const session = requestGuard.captureSession()
    saving.value = true
    errorMessage.value = ''
    try {
      if (id === null) {
        await addressApi.createAddress(payload)
      } else {
        await addressApi.updateAddress(id, payload)
      }
      if (!requestGuard.isSessionCurrent(session)) return false
      await load()
      return true
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '保存失败'
      }
      return false
    } finally {
      if (requestGuard.isSessionCurrent(session)) saving.value = false
    }
  }

  async function remove(id: number) {
    const session = requestGuard.captureSession()
    errorMessage.value = ''
    try {
      await addressApi.deleteAddress(id)
      if (!requestGuard.isSessionCurrent(session)) return false
      await load()
      return true
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '删除失败'
      }
      return false
    }
  }

  async function makeDefault(id: number) {
    const session = requestGuard.captureSession()
    errorMessage.value = ''
    try {
      await addressApi.setDefaultAddress(id)
      if (!requestGuard.isSessionCurrent(session)) return false
      await load()
      return true
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '设置默认地址失败'
      }
      return false
    }
  }

  /** 退出登录时清空，地址是私有数据 */
  function reset() {
    requestGuard.reset()
    addresses.value = []
    errorMessage.value = ''
    loading.value = false
    saving.value = false
  }

  registerSessionReset(reset)

  return {
    addresses,
    loading,
    saving,
    errorMessage,
    defaultAddress,
    isEmpty,
    load,
    save,
    remove,
    makeDefault,
    reset,
  }
})
