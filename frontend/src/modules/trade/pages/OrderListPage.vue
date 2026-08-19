<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { useOrderStore } from '../stores/order'
import { OrderStatus } from '../types/trade'

const orderStore = useOrderStore()
const activeStatus = ref<number | undefined>(undefined)

const tabs = [
  { label: '全部', value: undefined },
  { label: '待付款', value: OrderStatus.PENDING_PAY },
  { label: '待发货', value: OrderStatus.PAID },
  { label: '运输中', value: OrderStatus.SHIPPED },
  { label: '已完成', value: OrderStatus.COMPLETED },
  { label: '售后中', value: OrderStatus.AFTER_SALE },
]

const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 2,
})

function formatPrice(value: number) {
  return currencyFormatter.format(value)
}

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', { hour12: false })
}

function switchTab(status: number | undefined) {
  activeStatus.value = status
  void orderStore.loadMine(1, 10, status)
}

onMounted(() => {
  void orderStore.loadMine()
})
</script>

<template>
  <section class="orders-page">
    <header class="orders-page__header">
      <p class="section-kicker">我的订单</p>
      <h1>每一盆都有来处</h1>
      <p>点击订单查看状态时间线与操作入口。</p>
    </header>

    <div class="order-tabs" role="tablist" aria-label="按状态筛选订单">
      <button
        v-for="tab in tabs"
        :key="tab.label"
        type="button"
        role="tab"
        :aria-selected="activeStatus === tab.value"
        :class="{ active: activeStatus === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <p v-if="orderStore.errorMessage" class="orders-page__error" role="alert">
      {{ orderStore.errorMessage }}
    </p>

    <p v-if="orderStore.loading" class="orders-page__status" role="status">加载中…</p>

    <p v-else-if="!orderStore.hasOrders" class="orders-page__empty">
      这个分类下还没有订单。
      <RouterLink class="text-action" :to="{ name: 'plant-catalog' }">去逛植物</RouterLink>
    </p>

    <ul v-else class="order-list">
      <li v-for="order in orderStore.orders" :key="order.id">
        <RouterLink
          class="order-list__card"
          :to="{ name: 'order-detail', params: { orderId: order.id } }"
        >
          <header class="order-list__head">
            <span class="order-list__status">{{ order.statusLabel }}</span>
            <time :datetime="order.createdAt">{{ formatTime(order.createdAt) }}</time>
          </header>

          <ul class="order-list__items">
            <li v-for="item in order.items.slice(0, 3)" :key="item.id">
              <img v-if="item.image" :src="item.image" :alt="item.speciesName" />
              <span v-else class="order-list__fallback" aria-hidden="true">植</span>
              <span class="order-list__item-name">{{ item.speciesName }}</span>
              <span class="order-list__item-qty">×{{ item.quantity }}</span>
            </li>
          </ul>

          <footer class="order-list__foot">
            <span class="order-list__no">订单号 {{ order.orderNo }}</span>
            <span>
              共 {{ order.itemCount }} 件，合计
              <strong>{{ formatPrice(order.totalAmount) }}</strong>
            </span>
          </footer>
        </RouterLink>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.orders-page {
  max-width: 48rem;
  margin: 0 auto;
  padding: var(--space-2xl, 3rem) var(--space-lg, 1.25rem);
}

.orders-page__header {
  margin-bottom: var(--space-lg, 1.5rem);
}

.orders-page__header h1 {
  margin: 0.35rem 0 0.5rem;
}

.orders-page__header p:last-child {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.88rem;
}

.order-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm, 0.75rem);
  margin-bottom: var(--space-lg, 1.5rem);
}

.order-tabs button {
  padding: 0.4rem 1rem;
  background: none;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-pill, 999px);
  cursor: pointer;
  font-size: 0.82rem;
}

.order-tabs button.active {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
  color: var(--color-on-brand, #fffdf7);
  font-weight: 700;
}

.orders-page__error {
  padding: var(--space-md, 1rem);
  background: var(--color-danger-soft, #f6e3de);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-danger, #a8442f);
}

.orders-page__status {
  padding: var(--space-xl, 2rem) 0;
  text-align: center;
  color: var(--color-text-muted, #68716a);
}

.orders-page__empty {
  padding: var(--space-xl, 2rem) 0;
  text-align: center;
  color: var(--color-text-muted, #68716a);
}

.order-list {
  display: grid;
  gap: var(--space-md, 1rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

.order-list__card {
  display: block;
  padding: var(--space-md, 1rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
  color: inherit;
  text-decoration: none;
}

.order-list__card:hover {
  border-color: var(--color-brand, #496544);
}

.order-list__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--space-sm, 0.75rem);
}

.order-list__status {
  color: var(--color-brand, #496544);
  font-weight: 700;
}

.order-list__head time {
  color: var(--color-text-muted, #68716a);
  font-size: 0.75rem;
}

.order-list__items {
  display: grid;
  gap: 0.4rem;
  margin: 0 0 var(--space-sm, 0.75rem);
  padding: 0;
  list-style: none;
}

.order-list__items li {
  display: grid;
  grid-template-columns: 2.5rem minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-sm, 0.75rem);
}

.order-list__items img,
.order-list__fallback {
  width: 2.5rem;
  height: 2.5rem;
  object-fit: cover;
  border-radius: var(--radius-sm, 0.5rem);
}

.order-list__fallback {
  display: grid;
  place-items: center;
  background: var(--color-brand-soft, #e4eadb);
  color: var(--color-brand, #496544);
  font-size: 0.7rem;
}

.order-list__item-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.85rem;
}

.order-list__item-qty {
  color: var(--color-text-muted, #68716a);
  font-size: 0.78rem;
}

.order-list__foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-sm, 0.75rem);
  padding-top: var(--space-sm, 0.75rem);
  border-top: 1px dashed var(--color-border, #d9d3c5);
  font-size: 0.8rem;
}

.order-list__no {
  color: var(--color-text-muted, #68716a);
}

.order-list__foot strong {
  color: var(--color-brand, #496544);
  font-size: 0.95rem;
}
</style>
