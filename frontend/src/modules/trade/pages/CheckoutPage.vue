<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useCartStore } from '../stores/cart'
import { useOrderStore } from '../stores/order'
import { useAddressStore } from '../stores/address'

const cartStore = useCartStore()
const orderStore = useOrderStore()
const addressStore = useAddressStore()
const router = useRouter()

/** 结算页必须选一条地址。默认第一条是默认地址，回车或直接点提交即用它 */
const selectedAddressId = ref<number | null>(null)

const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 2,
})

function formatPrice(value: number) {
  return currencyFormatter.format(value)
}

/** 只结算可买的，失效的条目被后端实时标过 */
const checkoutItems = computed(() => cartStore.purchasableItems)

const canSubmit = computed(
  () =>
    checkoutItems.value.length > 0 &&
    selectedAddressId.value !== null &&
    !orderStore.submitting,
)

function selectAddress(id: number) {
  selectedAddressId.value = id
}

async function submitOrder() {
  if (!canSubmit.value) return

  const order = await orderStore.submit(
    selectedAddressId.value as number,
    checkoutItems.value.map((item) => item.skuId),
  )

  if (order) {
    // 下单成功 → 直接进详情页付款，不要让用户再去订单列表里找
    await router.replace({ name: 'order-detail', params: { orderId: order.id } })
    return
  }

  // 下单失败（库存不足 / 商品下架）：购物车的失效标记是实时的，重新拉一次
  if (orderStore.errorMessage.includes('库存') || orderStore.errorMessage.includes('下架')) {
    await cartStore.load()
  }
}

onMounted(async () => {
  await Promise.all([cartStore.load(), addressStore.load()])
  // 默认地址排在最前，直接选中它，用户不点也能提交
  selectedAddressId.value = addressStore.addresses[0]?.id ?? null
})
</script>

<template>
  <section class="checkout-page">
    <header class="checkout-page__header">
      <p class="section-kicker">确认订单</p>
      <h1>再核对一遍，就下单了</h1>
      <p>金额以结算页显示为准，提交时后端会重新校验库存与最新价格。</p>
    </header>

    <p v-if="orderStore.errorMessage" class="checkout-page__error" role="alert">
      {{ orderStore.errorMessage }}
    </p>

    <div class="checkout-page__layout">
      <div class="checkout-page__main">
        <!-- 收货地址 -->
        <section class="checkout-card">
          <header class="checkout-card__head">
            <h2>收货地址</h2>
            <RouterLink class="text-action" :to="{ name: 'addresses' }">管理地址</RouterLink>
          </header>

          <div v-if="addressStore.addresses.length" class="address-picker">
            <button
              v-for="address in addressStore.addresses"
              :key="address.id"
              type="button"
              class="address-picker__item"
              :class="{ 'address-picker__item--active': selectedAddressId === address.id }"
              :aria-pressed="selectedAddressId === address.id"
              @click="selectAddress(address.id)"
            >
              <span class="address-picker__receiver">
                {{ address.receiver }}
                <small>{{ address.phone }}</small>
              </span>
              <span class="address-picker__detail">{{ address.fullAddress }}</span>
              <span v-if="address.isDefault" class="address-picker__tag">默认</span>
            </button>
          </div>

          <div v-else class="checkout-card__empty">
            <p>还没有收货地址。</p>
            <RouterLink class="pill-button" :to="{ name: 'addresses' }">去添加地址</RouterLink>
          </div>
        </section>

        <!-- 商品清单 -->
        <section class="checkout-card">
          <header class="checkout-card__head">
            <h2>商品清单</h2>
            <RouterLink class="text-action" :to="{ name: 'cart' }">返回修改</RouterLink>
          </header>

          <ul v-if="checkoutItems.length" class="checkout-items">
            <li v-for="item in checkoutItems" :key="item.skuId" class="checkout-items__row">
              <img v-if="item.image" :src="item.image" :alt="item.imageAlt ?? item.speciesName" />
              <span v-else class="checkout-items__fallback" aria-hidden="true">植</span>
              <div class="checkout-items__body">
                <strong>{{ item.speciesName }}</strong>
                <p v-if="item.spec">{{ item.spec }}</p>
                <small>¥{{ item.price }} × {{ item.quantity }}</small>
              </div>
              <strong class="checkout-items__subtotal">{{ formatPrice(item.subtotal) }}</strong>
            </li>
          </ul>
          <p v-else class="checkout-card__empty">没有可结算的商品。</p>

          <p v-if="cartStore.hasInvalid" class="checkout-page__warn">
            有 {{ cartStore.items.filter((i) => i.invalidReason).length }} 件商品已失效，
            未计入本次结算。
          </p>
        </section>
      </div>

      <!-- 金额汇总 -->
      <aside class="checkout-page__summary">
        <h2>金额明细</h2>
        <div class="summary-row">
          <span>商品件数</span>
          <span>{{ cartStore.totalCount }} 件</span>
        </div>
        <div class="summary-row">
          <span>运费</span>
          <span>免运费</span>
        </div>
        <div class="summary-row summary-row--total">
          <span>应付金额</span>
          <strong>{{ formatPrice(cartStore.totalAmount) }}</strong>
        </div>
        <button
          class="pill-button pill-button--primary checkout-page__submit"
          type="button"
          :disabled="!canSubmit"
          @click="submitOrder"
        >
          {{ orderStore.submitting ? '正在提交…' : `提交订单 ¥${cartStore.totalAmount}` }}
        </button>
        <p class="checkout-page__hint">
          提交即生成待付款订单并锁定库存，超时未支付可在订单详情取消。
        </p>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.checkout-page {
  max-width: 64rem;
  margin: 0 auto;
  padding: var(--space-2xl, 3rem) var(--space-lg, 1.25rem);
}

.checkout-page__header {
  margin-bottom: var(--space-xl, 2rem);
}

.checkout-page__header h1 {
  margin: 0.35rem 0 0.5rem;
}

.checkout-page__header p:last-child {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.88rem;
}

.checkout-page__error {
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
  background: var(--color-danger-soft, #f6e3de);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-danger, #a8442f);
}

.checkout-page__layout {
  display: grid;
  gap: var(--space-lg, 1.5rem);
}

@media (min-width: 56rem) {
  .checkout-page__layout {
    grid-template-columns: minmax(0, 1fr) 18rem;
    align-items: start;
  }
}

.checkout-page__main {
  display: grid;
  gap: var(--space-lg, 1.5rem);
}

.checkout-card {
  padding: var(--space-md, 1rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
}

.checkout-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-md, 1rem);
}

.checkout-card__head h2 {
  margin: 0;
  font-size: 1rem;
}

.checkout-card__empty {
  padding: var(--space-lg, 1.5rem) 0;
  text-align: center;
  color: var(--color-text-muted, #68716a);
}

.address-picker {
  display: grid;
  gap: var(--space-sm, 0.75rem);
}

.address-picker__item {
  display: grid;
  gap: 0.2rem;
  padding: var(--space-md, 1rem);
  text-align: left;
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
  cursor: pointer;
}

.address-picker__item--active {
  border-color: var(--color-brand, #496544);
  outline: 2px solid color-mix(in srgb, var(--color-brand, #496544) 25%, transparent);
}

.address-picker__receiver {
  display: flex;
  align-items: baseline;
  gap: var(--space-sm, 0.75rem);
  font-weight: 700;
}

.address-picker__receiver small {
  color: var(--color-text-muted, #68716a);
  font-size: 0.8rem;
  font-weight: 400;
}

.address-picker__detail {
  color: var(--color-text-muted, #68716a);
  font-size: 0.82rem;
  line-height: 1.5;
}

.address-picker__tag {
  justify-self: start;
  padding: 0.1rem 0.5rem;
  background: var(--color-brand-soft, #e4eadb);
  border-radius: var(--radius-pill, 999px);
  color: var(--color-brand, #496544);
  font-size: 0.7rem;
  font-weight: 700;
}

.checkout-items {
  display: grid;
  gap: var(--space-sm, 0.75rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

.checkout-items__row {
  display: grid;
  grid-template-columns: 3.5rem minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-md, 1rem);
}

.checkout-items__row img,
.checkout-items__fallback {
  width: 3.5rem;
  height: 3.5rem;
  object-fit: cover;
  border-radius: var(--radius-card, 0.75rem);
}

.checkout-items__fallback {
  display: grid;
  place-items: center;
  background: var(--color-brand-soft, #e4eadb);
  color: var(--color-brand, #496544);
}

.checkout-items__body strong {
  display: block;
}

.checkout-items__body p {
  margin: 0.1rem 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.78rem;
}

.checkout-items__body small {
  color: var(--color-text-muted, #68716a);
  font-size: 0.75rem;
}

.checkout-items__subtotal {
  color: var(--color-brand, #496544);
  white-space: nowrap;
}

.checkout-page__warn {
  margin: var(--space-md, 1rem) 0 0;
  color: var(--color-danger, #a8442f);
  font-size: 0.78rem;
}

.checkout-page__summary {
  padding: var(--space-md, 1rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
}

.checkout-page__summary h2 {
  margin: 0 0 var(--space-md, 1rem);
  font-size: 1rem;
}

.summary-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--space-sm, 0.75rem);
  font-size: 0.88rem;
}

.summary-row--total {
  padding-top: var(--space-sm, 0.75rem);
  border-top: 1px dashed var(--color-border, #d9d3c5);
}

.summary-row--total strong {
  color: var(--color-brand, #496544);
  font-size: 1.25rem;
}

.checkout-page__submit {
  width: 100%;
  margin-top: var(--space-md, 1rem);
}

.checkout-page__submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.checkout-page__hint {
  margin: var(--space-sm, 0.75rem) 0 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.72rem;
  line-height: 1.5;
  text-align: center;
}

.pill-button--primary {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
  color: var(--color-on-brand, #fffdf7);
}
</style>
