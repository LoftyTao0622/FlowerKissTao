<script setup lang="ts">
import { ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterLink } from 'vue-router'

import { useCartStore } from '@/modules/trade/stores/cart'
import { useAuthStore } from '@/modules/user/stores/auth'

const cartStore = useCartStore()
const authStore = useAuthStore()
const { items, totalAmount, totalCount, hasInvalid } = storeToRefs(cartStore)
const isOpen = ref(false)

// 购物车现在在后端，未登录时没有可读的车。登录状态一变就重新拉一次，
// 免得用户登录后还要手动刷新才看得到自己的购物车
watch(
  () => authStore.isLoggedIn,
  (authenticated) => {
    if (authenticated) {
      void cartStore.load()
    } else {
      cartStore.reset()
    }
  },
  { immediate: true },
)

const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 2,
})

function formatPrice(value: number) {
  return currencyFormatter.format(value)
}
</script>

<template>
  <el-popover
    v-model:visible="isOpen"
    placement="bottom-end"
    trigger="click"
    :width="360"
    popper-class="cart-popper"
  >
    <template #reference>
      <button
        class="cart-trigger"
        type="button"
        :aria-label="`购物车，共 ${totalCount} 件商品`"
        :aria-expanded="isOpen"
      >
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path
            d="M4 5h2l1.7 9.1a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 1.9-1.4L21 8H7"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <circle cx="10" cy="19" r="1.25" fill="currentColor" />
          <circle cx="18" cy="19" r="1.25" fill="currentColor" />
        </svg>
        <span v-if="totalCount" class="cart-trigger__count" aria-hidden="true">
          {{ totalCount > 99 ? '99+' : totalCount }}
        </span>
      </button>
    </template>

    <section class="cart-panel" aria-labelledby="cart-title">
      <div class="cart-panel__heading">
        <h2 id="cart-title">我的购物车</h2>
        <span aria-live="polite">{{ totalCount }} 件</span>
      </div>

      <div v-if="items.length" class="cart-panel__content">
        <p v-if="hasInvalid" class="cart-panel__warn" role="status">
          有商品已下架或库存不足，结算时会跳过它们。
        </p>

        <ul class="cart-list">
          <li
            v-for="item in items"
            :key="item.skuId"
            :class="['cart-item', { 'cart-item--invalid': item.invalidReason }]"
          >
            <RouterLink
              v-if="item.slug"
              class="cart-item__media"
              :to="{ name: 'plant-detail', params: { plantId: item.slug } }"
              @click="isOpen = false"
            >
              <img v-if="item.image" :src="item.image" :alt="item.imageAlt ?? item.speciesName" />
              <span v-else aria-hidden="true">植</span>
            </RouterLink>
            <span v-else class="cart-item__media" aria-hidden="true">植</span>

            <div class="cart-item__body">
              <RouterLink
                v-if="item.slug"
                class="cart-item__name"
                :to="{ name: 'plant-detail', params: { plantId: item.slug } }"
                @click="isOpen = false"
              >
                {{ item.speciesName }}
              </RouterLink>
              <span v-else class="cart-item__name">{{ item.speciesName }}</span>

              <p v-if="item.spec">{{ item.spec }}</p>
              <strong>{{ formatPrice(item.price) }}</strong>
              <p v-if="item.invalidReason" class="cart-item__invalid">{{ item.invalidReason }}</p>

              <div class="quantity-control" :aria-label="`${item.speciesName} 数量`">
                <button
                  type="button"
                  :aria-label="`减少一件 ${item.speciesName}`"
                  @click="cartStore.setQuantity(item.skuId, item.quantity - 1)"
                >
                  −
                </button>
                <span aria-live="polite">{{ item.quantity }}</span>
                <button
                  type="button"
                  :disabled="item.quantity >= item.stock"
                  :aria-label="`增加一件 ${item.speciesName}`"
                  @click="cartStore.setQuantity(item.skuId, item.quantity + 1)"
                >
                  +
                </button>
                <button
                  class="quantity-control__remove"
                  type="button"
                  :aria-label="`从购物车移除 ${item.speciesName}`"
                  @click="cartStore.remove(item.skuId)"
                >
                  移除
                </button>
              </div>
            </div>
          </li>
        </ul>

        <div class="cart-panel__total">
          <span>商品小计</span>
          <strong>{{ formatPrice(totalAmount) }}</strong>
        </div>
        <div class="cart-panel__actions">
          <RouterLink
            class="pill-button pill-button--primary"
            :to="{ name: 'cart' }"
            @click="isOpen = false"
          >
            去结算
          </RouterLink>
          <RouterLink
            class="pill-button cart-panel__continue"
            :to="{ name: 'plant-catalog' }"
            @click="isOpen = false"
          >
            继续选植物
          </RouterLink>
        </div>
      </div>

      <div v-else class="cart-empty">
        <span class="cart-empty__leaf" aria-hidden="true">叶</span>
        <p>购物车还是空的，先去挑一盆适合你环境的植物吧。</p>
        <RouterLink
          class="pill-button"
          :to="{ name: 'plant-catalog' }"
          @click="isOpen = false"
        >
          去逛植物
        </RouterLink>
      </div>
    </section>
  </el-popover>
</template>

<style scoped>
.cart-trigger {
  position: relative;
  display: grid;
  width: 44px;
  height: 44px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 50%;
  background: transparent;
  color: var(--color-ink);
  cursor: pointer;
  place-items: center;
  transition: background-color 200ms var(--ease-out), transform 200ms var(--ease-out);
}

.cart-trigger:hover {
  background: var(--color-brand-soft);
}

.cart-trigger:active {
  transform: scale(0.95);
}

.cart-trigger svg {
  width: 24px;
  height: 24px;
}

.cart-trigger__count {
  position: absolute;
  top: -2px;
  right: -4px;
  display: grid;
  min-width: 22px;
  height: 22px;
  padding-inline: 5px;
  border: 2px solid var(--color-surface);
  border-radius: var(--radius-pill);
  background: var(--color-brand-accent);
  color: var(--color-on-brand);
  font-size: 0.7rem;
  font-weight: 800;
  place-items: center;
}

:global(.cart-popper) {
  width: min(360px, calc(100vw - 1.5rem)) !important;
  padding: 0 !important;
  overflow: hidden;
  border-color: var(--color-border) !important;
  box-shadow: var(--shadow-nav) !important;
}

.cart-panel__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-brand-deep);
  color: var(--color-on-brand);
}

.cart-panel__heading h2 {
  margin: 0;
  font-size: 1.05rem;
}

.cart-panel__heading span {
  font-size: 0.875rem;
  opacity: 0.78;
}

.cart-panel__content {
  padding: var(--space-sm);
}

.cart-list {
  max-height: min(50vh, 28rem);
  padding: 0;
  margin: 0;
  overflow-y: auto;
  list-style: none;
}

.cart-item {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: var(--space-sm);
  padding-block: var(--space-sm);
}

.cart-item + .cart-item {
  border-top: 1px solid var(--color-border);
}

.cart-item__media {
  display: grid;
  width: 72px;
  height: 86px;
  overflow: hidden;
  border-radius: var(--radius-card);
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-size: 1.25rem;
  text-decoration: none;
  place-items: center;
}

.cart-item__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cart-item__body {
  min-width: 0;
}

.cart-item__name {
  display: flex;
  min-height: 44px;
  align-items: center;
  overflow: hidden;
  color: var(--color-ink);
  font-weight: 750;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-item__name:hover {
  color: var(--color-brand-accent);
  text-decoration: underline;
}

.cart-item__name:focus-visible {
  border-radius: 0.2rem;
  outline: 3px solid var(--color-brand-accent);
  outline-offset: 2px;
}

.cart-item__body p {
  margin: 0.2rem 0;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: 0.8rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-item__body strong {
  color: var(--color-brand);
  font-size: 0.9rem;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-top: var(--space-xs);
}

.quantity-control button {
  min-width: 44px;
  min-height: 44px;
  padding: 0;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  color: var(--color-ink);
  cursor: pointer;
}

.quantity-control button:hover {
  border-color: var(--color-brand-accent);
  background: var(--color-brand-soft);
}

.quantity-control span {
  min-width: 1.5rem;
  text-align: center;
}

.quantity-control .quantity-control__remove {
  min-width: auto;
  margin-left: auto;
  padding-inline: 0.65rem;
  border-color: transparent;
  color: var(--color-text-muted);
  font-size: 0.78rem;
}

.cart-panel__total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: var(--space-sm);
  border-top: 1px solid var(--color-border);
  color: var(--color-ink);
}

.cart-panel__total strong {
  color: var(--color-brand);
  font-size: 1.1rem;
}

.cart-panel__actions {
  display: grid;
  gap: var(--space-sm);
}

.cart-panel__actions .pill-button--primary {
  background: var(--color-brand);
  border-color: var(--color-brand);
  color: var(--color-on-dark, #fffdf7);
}

.cart-panel__continue {
  width: 100%;
}

.cart-panel__warn {
  margin: 0 0 var(--space-sm);
  padding: var(--space-sm);
  background: var(--color-brand-soft);
  border-radius: var(--radius-sm);
  color: var(--color-brand-deep);
  font-size: 0.78rem;
  line-height: 1.5;
}

.cart-item--invalid {
  opacity: 0.65;
}

.cart-item__invalid {
  margin: 0.15rem 0 0;
  color: var(--color-danger, #a8442f);
  font-size: 0.72rem;
}

.cart-empty {
  display: grid;
  padding: var(--space-lg);
  text-align: center;
  place-items: center;
}

.cart-empty__leaf {
  display: grid;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-size: 1.15rem;
  font-weight: 800;
  place-items: center;
}

.cart-empty p {
  margin: var(--space-sm) 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}
</style>
