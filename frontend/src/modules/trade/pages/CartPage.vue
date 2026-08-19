<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { useCartStore } from '../stores/cart'

const cartStore = useCartStore()
const busy = ref(false)

const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 2,
})

function formatPrice(value: number) {
  return currencyFormatter.format(value)
}

/** 数量减到 1 就不再减，要移除得点"移除"——误触减号不会把东西删掉 */
async function decrease(skuId: number, quantity: number) {
  if (quantity <= 1) return
  busy.value = true
  await cartStore.setQuantity(skuId, quantity - 1)
  busy.value = false
}

async function increase(skuId: number, quantity: number, stock: number) {
  if (quantity >= stock) return
  busy.value = true
  await cartStore.setQuantity(skuId, quantity + 1)
  busy.value = false
}

async function removeItem(skuId: number) {
  busy.value = true
  await cartStore.remove(skuId)
  busy.value = false
}

onMounted(() => {
  void cartStore.load()
})
</script>

<template>
  <section class="cart-page">
    <header class="cart-page__header">
      <p class="section-kicker">购物车</p>
      <h1>准备好挑选了吗</h1>
      <p>价格和库存都是最新的，失效的商品不会计入结算。</p>
    </header>

    <p v-if="cartStore.errorMessage" class="cart-page__error" role="alert">
      {{ cartStore.errorMessage }}
    </p>

    <!-- 旧版 localStorage 购物车已无法迁移（按 slug 存且带着客户端价格），提示后清空 -->
    <div v-if="cartStore.legacyCleared" class="cart-page__notice" role="status">
      <p>
        购物车已升级为账号同步版本，旧版本地数据因无法安全迁移已被清空，
        请重新加入想买的植物。
      </p>
      <button class="text-action" type="button" @click="cartStore.dismissLegacyNotice()">
        知道了
      </button>
    </div>

    <div v-if="cartStore.isEmpty && !cartStore.loading" class="cart-page__empty">
      <span class="cart-page__leaf" aria-hidden="true">叶</span>
      <h2>购物车还是空的</h2>
      <p>先去逛一逛，或者让智能推荐帮你挑几盆适合环境的植物。</p>
      <div class="cart-page__empty-actions">
        <RouterLink class="pill-button pill-button--primary" :to="{ name: 'plant-catalog' }">
          逛植物
        </RouterLink>
        <RouterLink class="pill-button" :to="{ name: 'recommendation' }">
          去推荐
        </RouterLink>
      </div>
    </div>

    <div v-else-if="cartStore.items.length" class="cart-page__layout">
      <ul class="cart-list">
        <li
          v-for="item in cartStore.items"
          :key="item.skuId"
          :class="['cart-list__item', { 'cart-list__item--invalid': item.invalidReason }]"
        >
          <div class="cart-list__media">
            <RouterLink
              v-if="item.slug && !item.invalidReason"
              :to="{ name: 'plant-detail', params: { plantId: item.slug } }"
            >
              <img v-if="item.image" :src="item.image" :alt="item.imageAlt ?? item.speciesName" />
              <span v-else aria-hidden="true">植</span>
            </RouterLink>
            <span v-else aria-hidden="true">植</span>
          </div>

          <div class="cart-list__body">
            <div class="cart-list__head">
              <div>
                <RouterLink
                  v-if="item.slug && !item.invalidReason"
                  class="cart-list__name"
                  :to="{ name: 'plant-detail', params: { plantId: item.slug } }"
                >
                  {{ item.speciesName }}
                </RouterLink>
                <span v-else class="cart-list__name">{{ item.speciesName }}</span>
                <p v-if="item.spec" class="cart-list__spec">{{ item.spec }}</p>
              </div>
              <strong class="cart-list__price">{{ formatPrice(item.price) }}</strong>
            </div>

            <p v-if="item.invalidReason" class="cart-list__invalid">{{ item.invalidReason }}</p>
            <p v-else class="cart-list__stock">库存 {{ item.stock }} 件</p>

            <div class="cart-list__controls">
              <div class="quantity-control" :aria-label="`${item.speciesName} 数量`">
                <button
                  type="button"
                  :aria-label="`减少一件 ${item.speciesName}`"
                  :disabled="item.quantity <= 1 || busy"
                  @click="decrease(item.skuId, item.quantity)"
                >
                  −
                </button>
                <span aria-live="polite">{{ item.quantity }}</span>
                <button
                  type="button"
                  :aria-label="`增加一件 ${item.speciesName}`"
                  :disabled="item.quantity >= item.stock || busy"
                  @click="increase(item.skuId, item.quantity, item.stock)"
                >
                  +
                </button>
              </div>
              <button
                class="text-action"
                type="button"
                @click="removeItem(item.skuId)"
              >
                移除
              </button>
            </div>
          </div>
        </li>
      </ul>

      <aside class="cart-page__summary">
        <h2>结算明细</h2>
        <div class="summary-row">
          <span>商品件数</span>
          <span>{{ cartStore.totalCount }} 件</span>
        </div>
        <div class="summary-row">
          <span>商品金额</span>
          <strong>{{ formatPrice(cartStore.totalAmount) }}</strong>
        </div>
        <p v-if="cartStore.hasInvalid" class="cart-page__summary-warn">
          有 {{ cartStore.items.filter((i) => i.invalidReason).length }} 件商品已失效，
          结算时会跳过它们。
        </p>
        <RouterLink
          class="pill-button pill-button--primary cart-page__checkout"
          :to="{ name: 'checkout' }"
        >
          去结算（{{ cartStore.totalCount }} 件）
        </RouterLink>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.cart-page {
  max-width: 64rem;
  margin: 0 auto;
  padding: var(--space-2xl, 3rem) var(--space-lg, 1.25rem) var(--space-2xl, 3rem);
}

.cart-page__header {
  margin-bottom: var(--space-xl, 2rem);
}

.cart-page__header h1 {
  margin: 0.35rem 0 0.5rem;
}

.cart-page__header p:last-child {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.88rem;
}

.cart-page__error {
  padding: var(--space-md, 1rem);
  background: var(--color-danger-soft, #f6e3de);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-danger, #a8442f);
}

.cart-page__notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
  background: var(--color-brand-soft, #e4eadb);
  border-radius: var(--radius-card, 0.75rem);
}

.cart-page__notice p {
  margin: 0;
  color: var(--color-brand, #496544);
  font-size: 0.82rem;
  line-height: 1.5;
}

.cart-page__empty {
  display: grid;
  padding: var(--space-2xl, 3rem) 0;
  text-align: center;
  place-items: center;
}

.cart-page__leaf {
  display: grid;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-brand-soft, #e4eadb);
  color: var(--color-brand, #496544);
  font-size: 1.3rem;
  font-weight: 800;
  place-items: center;
}

.cart-page__empty h2 {
  margin: var(--space-md, 1rem) 0 0.4rem;
}

.cart-page__empty p {
  margin: 0 0 var(--space-lg, 1.5rem);
  color: var(--color-text-muted, #68716a);
}

.cart-page__empty-actions {
  display: flex;
  gap: var(--space-sm, 0.75rem);
}

.cart-page__layout {
  display: grid;
  gap: var(--space-lg, 1.5rem);
}

@media (min-width: 56rem) {
  .cart-page__layout {
    grid-template-columns: minmax(0, 1fr) 18rem;
    align-items: start;
  }
}

.cart-list {
  display: grid;
  gap: var(--space-md, 1rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

.cart-list__item {
  display: grid;
  grid-template-columns: 6rem minmax(0, 1fr);
  gap: var(--space-md, 1rem);
  padding: var(--space-md, 1rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
}

.cart-list__item--invalid {
  opacity: 0.65;
}

.cart-list__media {
  display: grid;
  min-height: 6rem;
  place-items: center;
  overflow: hidden;
  background: var(--color-brand-soft, #e4eadb);
  border-radius: var(--radius-card, 0.75rem);
}

.cart-list__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cart-list__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
}

.cart-list__name {
  font-weight: 700;
  text-decoration: none;
  color: inherit;
}

.cart-list__spec {
  margin: 0.2rem 0 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.8rem;
}

.cart-list__price {
  color: var(--color-brand, #496544);
  white-space: nowrap;
}

.cart-list__invalid {
  margin: 0.5rem 0 0;
  color: var(--color-danger, #a8442f);
  font-size: 0.8rem;
}

.cart-list__stock {
  margin: 0.5rem 0 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.78rem;
}

.cart-list__controls {
  display: flex;
  align-items: center;
  gap: var(--space-md, 1rem);
  margin-top: var(--space-md, 1rem);
}

.quantity-control {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-pill, 999px);
  overflow: hidden;
}

.quantity-control button {
  width: 2.2rem;
  height: 2.2rem;
  background: none;
  border: 0;
  cursor: pointer;
  font-size: 1rem;
}

.quantity-control button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.quantity-control span {
  min-width: 2rem;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.cart-page__summary {
  padding: var(--space-md, 1rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
}

.cart-page__summary h2 {
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

.summary-row strong {
  color: var(--color-brand, #496544);
  font-size: 1.1rem;
}

.cart-page__summary-warn {
  margin: var(--space-md, 1rem) 0;
  color: var(--color-danger, #a8442f);
  font-size: 0.78rem;
  line-height: 1.5;
}

.cart-page__checkout {
  width: 100%;
  margin-top: var(--space-md, 1rem);
}

/* 全局只有 pill-button 与 pill-button--outline，主按钮补一个本地同款 */
.pill-button--primary {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
  color: var(--color-on-brand, #fffdf7);
}
</style>
