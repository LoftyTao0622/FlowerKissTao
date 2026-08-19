<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'

import * as orderApi from '../api/order'
import type { Order } from '../types/trade'
import { OrderStatus } from '../types/trade'

const orders = ref<Order[]>([])
const total = ref(0)
const current = ref(1)
const size = 10
const loading = ref(false)
const errorMessage = ref('')
const activeStatus = ref<number | undefined>(undefined)
const keyword = ref('')

const tabs = [
  { label: '全部', value: undefined },
  { label: '待付款', value: OrderStatus.PENDING_PAY },
  { label: '待发货', value: OrderStatus.PAID },
  { label: '运输中', value: OrderStatus.SHIPPED },
  { label: '已完成', value: OrderStatus.COMPLETED },
  { label: '售后中', value: OrderStatus.AFTER_SALE },
  { label: '已取消', value: OrderStatus.CANCELLED },
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

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await orderApi.fetchAllOrders(current.value, size, activeStatus.value, keyword.value)
    orders.value = result.records
    total.value = result.total
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '订单加载失败'
  } finally {
    loading.value = false
  }
}

function switchTab(status: number | undefined) {
  activeStatus.value = status
  current.value = 1
  void load()
}

function search() {
  current.value = 1
  void load()
}

async function ship(order: Order) {
  try {
    await ElMessageBox.confirm(
      `确认发货订单 ${order.orderNo}？状态将变为运输中。`,
      '确认发货',
      { confirmButtonText: '发货', cancelButtonText: '取消' },
    )
    await orderApi.shipOrder(order.id)
    await load()
  } catch {
    // 取消
  }
}

async function approveAfterSale(order: Order) {
  try {
    await ElMessageBox.confirm(
      `确认通过订单 ${order.orderNo} 的售后申请？将退款并归还库存。`,
      '售后审核',
      { confirmButtonText: '通过并退款', cancelButtonText: '取消', type: 'warning' },
    )
    await orderApi.approveAfterSale(order.id)
    await load()
  } catch {
    // 取消
  }
}

async function rejectAfterSale(order: Order) {
  try {
    const reason = await ElMessageBox.prompt(
      `驳回订单 ${order.orderNo} 的售后申请，请填写驳回理由，用户会看到。`,
      '驳回售后',
      {
        confirmButtonText: '驳回',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：已超过七天无理由退货期',
        inputValidator: (value) => (value && value.trim().length > 0) || '请填写驳回理由',
      },
    )
    await orderApi.rejectAfterSale(order.id, reason.value)
    await load()
  } catch {
    // 取消
  }
}

function hasAction(order: Order, code: string) {
  return order.actions.some((action) => action.code === code)
}

onMounted(() => {
  void load()
})
</script>

<template>
  <section class="order-admin-page">
    <header class="order-admin-page__header">
      <p class="section-kicker">订单管理</p>
      <h1>发货与售后，都在这里</h1>
      <p>发货需待付款订单支付完成，售后审核会原路归还库存。</p>
    </header>

    <div class="order-admin-toolbar">
      <div class="order-admin-tabs" role="tablist" aria-label="按状态筛选订单">
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
      <form class="order-admin-search" @submit.prevent="search">
        <input
          v-model="keyword"
          type="search"
          placeholder="订单号 / 收件人"
          aria-label="按订单号或收件人搜索"
        />
        <button type="submit">搜索</button>
      </form>
    </div>

    <p v-if="errorMessage" class="order-admin-page__error" role="alert">{{ errorMessage }}</p>
    <p v-if="loading" class="order-admin-page__status" role="status">加载中…</p>

    <p v-else-if="orders.length === 0" class="order-admin-page__empty">没有符合条件的订单。</p>

    <div v-else class="order-admin-list">
      <article v-for="order in orders" :key="order.id" class="order-admin-card">
        <header class="order-admin-card__head">
          <div>
            <strong class="order-admin-card__status">{{ order.statusLabel }}</strong>
            <span class="order-admin-card__no">{{ order.orderNo }}</span>
          </div>
          <time :datetime="order.createdAt">{{ formatTime(order.createdAt) }}</time>
        </header>

        <p class="order-admin-card__buyer">
          {{ order.receiver }} · {{ order.phone }} · {{ order.addressSnapshot }}
        </p>

        <ul class="order-admin-card__items">
          <li v-for="item in order.items" :key="item.id">
            <span class="order-admin-card__item-name">{{ item.speciesName }}</span>
            <span class="order-admin-card__item-qty">×{{ item.quantity }}</span>
            <span class="order-admin-card__item-price">{{ formatPrice(item.subtotal) }}</span>
          </li>
        </ul>

        <footer class="order-admin-card__foot">
          <span>共 {{ order.itemCount }} 件，合计 {{ formatPrice(order.totalAmount) }}</span>
          <div class="order-admin-card__actions">
            <button
              v-if="hasAction(order, 'SHIP')"
              class="pill-button"
              type="button"
              @click="ship(order)"
            >
              发货
            </button>
            <button
              v-if="hasAction(order, 'APPROVE_AFTER_SALE')"
              class="pill-button pill-button--danger"
              type="button"
              @click="approveAfterSale(order)"
            >
              售后通过
            </button>
            <button
              v-if="hasAction(order, 'REJECT_AFTER_SALE')"
              class="pill-button pill-button--outline"
              type="button"
              @click="rejectAfterSale(order)"
            >
              售后驳回
            </button>
          </div>
        </footer>
      </article>
    </div>

    <div v-if="total > size" class="order-admin-pager">
      <button
        type="button"
        :disabled="current <= 1"
        @click="current--; load()"
      >
        上一页
      </button>
      <span>第 {{ current }} / {{ Math.ceil(total / size) }} 页</span>
      <button
        type="button"
        :disabled="current >= Math.ceil(total / size)"
        @click="current++; load()"
      >
        下一页
      </button>
    </div>
  </section>
</template>

<style scoped>
.order-admin-page {
  max-width: 56rem;
  margin: 0 auto;
  padding: var(--space-2xl, 3rem) var(--space-lg, 1.25rem);
}

.order-admin-page__header {
  margin-bottom: var(--space-lg, 1.5rem);
}

.order-admin-page__header h1 {
  margin: 0.35rem 0 0.5rem;
}

.order-admin-page__header p:last-child {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.88rem;
}

.order-admin-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
  margin-bottom: var(--space-lg, 1.5rem);
}

.order-admin-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm, 0.75rem);
}

.order-admin-tabs button {
  padding: 0.4rem 1rem;
  background: none;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-pill, 999px);
  cursor: pointer;
  font-size: 0.82rem;
}

.order-admin-tabs button.active {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
  color: var(--color-on-brand, #fffdf7);
  font-weight: 700;
}

.order-admin-search {
  display: flex;
  gap: var(--space-sm, 0.75rem);
}

.order-admin-search input {
  min-height: 2.5rem;
  padding: 0.4rem 0.8rem;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-sm, 0.5rem);
}

.order-admin-search button {
  min-height: 2.5rem;
  padding: 0.4rem 1rem;
  background: var(--color-brand, #496544);
  border: 1px solid var(--color-brand, #496544);
  border-radius: var(--radius-pill, 999px);
  color: var(--color-on-brand, #fffdf7);
  font-weight: 700;
  cursor: pointer;
}

.order-admin-page__error {
  padding: var(--space-md, 1rem);
  background: var(--color-danger-soft, #f6e3de);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-danger, #a8442f);
}

.order-admin-page__status,
.order-admin-page__empty {
  padding: var(--space-xl, 2rem) 0;
  text-align: center;
  color: var(--color-text-muted, #68716a);
}

.order-admin-list {
  display: grid;
  gap: var(--space-md, 1rem);
}

.order-admin-card {
  padding: var(--space-md, 1rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
}

.order-admin-card__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
  margin-bottom: 0.4rem;
}

.order-admin-card__status {
  color: var(--color-brand, #496544);
}

.order-admin-card__no {
  margin-left: var(--space-sm, 0.75rem);
  color: var(--color-text-muted, #68716a);
  font-size: 0.78rem;
}

.order-admin-card__head time {
  color: var(--color-text-muted, #68716a);
  font-size: 0.75rem;
}

.order-admin-card__buyer {
  margin: 0 0 var(--space-sm, 0.75rem);
  color: var(--color-text-muted, #68716a);
  font-size: 0.82rem;
}

.order-admin-card__items {
  display: grid;
  gap: 0.3rem;
  margin: 0 0 var(--space-sm, 0.75rem);
  padding: 0;
  list-style: none;
}

.order-admin-card__items li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: var(--space-sm, 0.75rem);
  font-size: 0.85rem;
}

.order-admin-card__item-qty,
.order-admin-card__item-price {
  color: var(--color-text-muted, #68716a);
}

.order-admin-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
  padding-top: var(--space-sm, 0.75rem);
  border-top: 1px dashed var(--color-border, #d9d3c5);
  font-size: 0.85rem;
}

.order-admin-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm, 0.75rem);
}

.pill-button--danger {
  background: var(--color-danger, #a8442f);
  border-color: var(--color-danger, #a8442f);
  color: var(--color-on-brand, #fffdf7);
}

.order-admin-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md, 1rem);
  margin-top: var(--space-lg, 1.5rem);
}

.order-admin-pager button {
  min-height: 2.5rem;
  padding: 0.4rem 1rem;
  background: none;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-pill, 999px);
  cursor: pointer;
}

.order-admin-pager button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
