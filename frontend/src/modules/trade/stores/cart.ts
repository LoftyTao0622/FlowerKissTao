import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

export interface CartPlantInput {
  id: string | number
  name: string
  price: number
  image?: string
  subtitle?: string
}

export interface CartItem {
  id: string
  name: string
  price: number
  image?: string
  subtitle?: string
  quantity: number
}

const STORAGE_KEY = 'flower-kiss-tao-cart'

function isCartItem(value: unknown): value is CartItem {
  if (!value || typeof value !== 'object') return false
  const item = value as Partial<CartItem>
  return (
    typeof item.id === 'string' &&
    typeof item.name === 'string' &&
    typeof item.price === 'number' &&
    Number.isFinite(item.price) &&
    item.price >= 0 &&
    (item.image === undefined || typeof item.image === 'string') &&
    (item.subtitle === undefined || typeof item.subtitle === 'string') &&
    typeof item.quantity === 'number' &&
    Number.isInteger(item.quantity) &&
    item.quantity > 0
  )
}

function readStoredCart(): CartItem[] {
  if (typeof window === 'undefined') return []

  try {
    const value: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    return Array.isArray(value) ? value.filter(isCartItem) : []
  } catch {
    return []
  }
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>(readStoredCart())
  const totalCount = computed(() =>
    items.value.reduce((total, item) => total + item.quantity, 0),
  )
  const subtotal = computed(() =>
    items.value.reduce((total, item) => total + item.price * item.quantity, 0),
  )

  function addPlant(plant: CartPlantInput, quantity = 1) {
    const normalizedId = String(plant.id)
    const requestedQuantity = Math.trunc(quantity)
    const normalizedQuantity = Number.isFinite(requestedQuantity)
      ? Math.max(1, requestedQuantity)
      : 1
    const requestedPrice = Number(plant.price)
    const normalizedPrice = Number.isFinite(requestedPrice) ? Math.max(0, requestedPrice) : 0
    const existingItem = items.value.find((item) => item.id === normalizedId)

    if (existingItem) {
      existingItem.quantity += normalizedQuantity
      return
    }

    items.value.push({
      id: normalizedId,
      name: plant.name,
      price: normalizedPrice,
      image: plant.image,
      subtitle: plant.subtitle,
      quantity: normalizedQuantity,
    })
  }

  function setQuantity(id: string | number, quantity: number) {
    const item = items.value.find((entry) => entry.id === String(id))
    if (!item) return

    const normalizedQuantity = Math.trunc(quantity)
    if (!Number.isFinite(normalizedQuantity) || normalizedQuantity <= 0) {
      remove(id)
      return
    }

    item.quantity = normalizedQuantity
  }

  function increment(id: string | number) {
    const item = items.value.find((entry) => entry.id === String(id))
    if (item) item.quantity += 1
  }

  function decrement(id: string | number) {
    const item = items.value.find((entry) => entry.id === String(id))
    if (!item) return
    setQuantity(id, item.quantity - 1)
  }

  function remove(id: string | number) {
    items.value = items.value.filter((item) => item.id !== String(id))
  }

  function clear() {
    items.value = []
  }

  watch(
    items,
    (value) => {
      if (typeof window !== 'undefined') {
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
        } catch {
          return
        }
      }
    },
    { deep: true },
  )

  return {
    items,
    totalCount,
    subtotal,
    addPlant,
    setQuantity,
    increment,
    decrement,
    remove,
    clear,
  }
})
