<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'

import { useOrderStore } from '../stores/order'
import { OrderStatus } from '../types/trade'

const props = defineProps<{ orderId: string }>()

const orderStore = useOrderStore()
const order = computed(() => orderStore.current)

const paying = ref(false)
const paySucceeded = ref(false)
/** 确认收货成功后提示养护档案已建立，并给一个直达入口 */
const archiveCreated = ref(false)

const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 2,
})

function formatPrice(value: number) {
  return currencyFormatter.format(value)
}

function formatTime(value: string | null) {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

/** 状态时间线：从下单到当前，按时间顺序 */
const timeline = computed(() => {
  const current = order.value
  if (!current) return []
  const steps: { label: string; time: string | null; done: boolean }[] = [
    { label: '提交订单', time: current.createdAt, done: true },
    { label: '支付成功', time: current.paidAt, done: current.status >= OrderStatus.PAID },
    {
      label: '商家发货',
      time: current.shippedAt,
      done: current.status >= OrderStatus.SHIPPED,
    },
    {
      label: '确认收货',
      time: current.receivedAt,
      done: current.status >= OrderStatus.COMPLETED,
    },
  ]
  if (current.status === OrderStatus.CANCELLED) {
    steps.push({ label: '订单关闭', time: current.closedAt, done: true })
  }
  if (current.status === OrderStatus.AFTER_SALE) {
    steps.push({ label: '售后处理中', time: current.createdAt, done: true })
  }
  return steps
})

const canPay = computed(() => order.value?.actions.some((a) => a.code === 'PAY') ?? false)
const canCancel = computed(() => order.value?.actions.some((a) => a.code === 'CANCEL') ?? false)
const canReceive = computed(() => order.value?.actions.some((a) => a.code === 'RECEIVE') ?? false)
const canApplyAfterSale = computed(
  () => order.value?.actions.some((a) => a.code === 'APPLY_AFTER_SALE') ?? false,
)

async function doPay() {
  if (!order.value || paying.value) return
  paying.value = true
  const ok = await orderStore.pay(order.value.id)
  paying.value = false
  if (ok) {
    paySucceeded.value = true
    window.setTimeout(() => (paySucceeded.value = false), 2500)
  }
}

async function doCancel() {
  if (!order.value) return
  try {
    const reason = await ElMessageBox.prompt('取消后库存会退回，确定取消这笔订单吗？', '取消订单', {
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想',
      inputPlaceholder: '可选：填一下取消原因',
      inputValidator: () => true,
    })
    await orderStore.cancel(order.value.id, reason.value || undefined)
  } catch {
    // 用户点了"再想想"或直接关掉弹窗，什么都不做
  }
}

async function doReceive() {
  if (!order.value) return
  try {
    await ElMessageBox.confirm(
      '确认收货后，系统会依据购买的植物与你的场景自动建立养护档案，并排好未来 60 天的养护任务。',
      '确认收货',
      { confirmButtonText: '确认收货', cancelButtonText: '再看看' },
    )
    const ok = await orderStore.receive(order.value.id)
    if (ok) {
      archiveCreated.value = true
    }
  } catch {
    // 取消
  }
}

async function doApplyAfterSale() {
  if (!order.value) return
  try {
    const reason = await ElMessageBox.prompt('请填写申请售后的原因，管理员会据此审核。', '申请售后', {
      confirmButtonText: '提交申请',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：收到的植物叶片有损伤',
      inputValidator: (value) => (value && value.trim().length > 0) || '请填写原因',
    })
    await orderStore.applyAfterSale(order.value.id, reason.value)
  } catch {
    // 取消
  }
}

onMounted(() => {
  void orderStore.loadOne(Number(props.orderId))
})
</script>

<template>
  <section class="order-detail-page">
    <p class="section-kicker">订单详情</p>

    <div v-if="orderStore.loading" class="order-detail-page__status" role="status">加载中…</div>

    <div v-else-if="!order" class="order-detail-page__status">
      <p>订单不存在或已被删除。</p>
      <RouterLink class="pill-button" :to="{ name: 'orders' }">返回订单列表</RouterLink>
    </div>

    <template v-else>
      <header class="order-detail-page__header">
        <div>
          <h1>{{ order.statusLabel }}</h1>
          <p class="order-detail-page__no">订单号 {{ order.orderNo }}</p>
        </div>
        <RouterLink class="text-action" :to="{ name: 'orders' }">返回订单列表</RouterLink>
      </header>

      <p v-if="orderStore.errorMessage" class="order-detail-page__error" role="alert">
        {{ orderStore.errorMessage }}
      </p>
      <p v-if="paySucceeded" class="order-detail-page__success" role="status">
        支付成功！订单已进入待发货。
      </p>

      <!-- 确认收货 → 养护闭环的衔接点，给一个直达入口 -->
      <div v-if="archiveCreated" class="order-detail-page__archive" role="status">
        <div>
          <strong>养护档案已建立</strong>
          <p>系统已依据品种、你的场景与当前季节排好未来 60 天的养护任务。</p>
        </div>
        <RouterLink class="pill-button pill-button--primary" :to="{ name: 'my-plants' }">
          查看我的植物
        </RouterLink>
      </div>

      <!-- 待付款主操作区 -->
      <div v-if="canPay" class="pay-panel">
        <div>
          <p class="section-kicker">待付款</p>
          <h2>应付 {{ formatPrice(order.totalAmount) }}</h2>
          <p>这是模拟支付，不会产生真实扣款。重复点击不会重复扣款。</p>
        </div>
        <button
          class="pill-button pill-button--primary pay-panel__button"
          type="button"
          :disabled="paying"
          @click="doPay"
        >
          {{ paying ? '正在支付…' : `立即支付 ¥${order.totalAmount}` }}
        </button>
      </div>

      <!-- 状态时间线 -->
      <ol class="order-timeline" aria-label="订单状态时间线">
        <li v-for="(step, index) in timeline" :key="index" :class="{ done: step.done }">
          <span class="order-timeline__dot" aria-hidden="true"></span>
          <div>
            <strong>{{ step.label }}</strong>
            <small v-if="step.time">{{ formatTime(step.time) }}</small>
          </div>
        </li>
      </ol>

      <!-- 收货信息 -->
      <section class="order-block">
        <h2>收货信息</h2>
        <p class="order-block__receiver">
          {{ order.receiver }} <span>{{ order.phone }}</span>
        </p>
        <p class="order-block__address">{{ order.addressSnapshot }}</p>
      </section>

      <!-- 商品清单 -->
      <section class="order-block">
        <h2>商品清单</h2>
        <ul class="order-items">
          <li v-for="item in order.items" :key="item.id" class="order-items__row">
            <img v-if="item.image" :src="item.image" :alt="item.speciesName" />
            <span v-else class="order-items__fallback" aria-hidden="true">植</span>
            <div class="order-items__body">
              <strong>{{ item.speciesName }}</strong>
              <p>{{ item.spec }}</p>
              <small>¥{{ item.unitPrice }} × {{ item.quantity }}</small>
            </div>
            <strong class="order-items__subtotal">{{ formatPrice(item.subtotal) }}</strong>
          </li>
        </ul>
        <div class="order-items__total">
          <span>共 {{ order.itemCount }} 件，实付</span>
          <strong>{{ formatPrice(order.totalAmount) }}</strong>
        </div>
      </section>

      <!-- 取消 / 售后原因 -->
      <section v-if="order.cancelReason || order.refundReason || order.refundRejectReason" class="order-block">
        <h2>订单说明</h2>
        <p v-if="order.cancelReason" class="order-block__reason">
          <strong>取消原因：</strong>{{ order.cancelReason }}
        </p>
        <p v-if="order.refundReason" class="order-block__reason">
          <strong>售后申请：</strong>{{ order.refundReason }}
        </p>
        <p v-if="order.refundRejectReason" class="order-block__reason">
          <strong>驳回说明：</strong>{{ order.refundRejectReason }}
        </p>
      </section>

      <!-- 操作区：后端算好当前能做什么 -->
      <div class="order-actions">
        <button
          v-if="canCancel"
          class="pill-button pill-button--outline"
          type="button"
          :disabled="orderStore.submitting"
          @click="doCancel"
        >
          取消订单
        </button>
        <button
          v-if="canReceive"
          class="pill-button pill-button--primary"
          type="button"
          :disabled="orderStore.submitting"
          @click="doReceive"
        >
          确认收货
        </button>
        <button
          v-if="canApplyAfterSale"
          class="pill-button pill-button--outline"
          type="button"
          :disabled="orderStore.submitting"
          @click="doApplyAfterSale"
        >
          申请售后
        </button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.order-detail-page {
  max-width: 40rem;
  margin: 0 auto;
  padding: var(--space-2xl, 3rem) var(--space-lg, 1.25rem);
}

.order-detail-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
  margin-bottom: var(--space-lg, 1.5rem);
}

.order-detail-page__header h1 {
  margin: 0.35rem 0 0.2rem;
}

.order-detail-page__no {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.8rem;
}

.order-detail-page__status {
  padding: var(--space-xl, 2rem) 0;
  text-align: center;
  color: var(--color-text-muted, #68716a);
}

.order-detail-page__error {
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
  background: var(--color-danger-soft, #f6e3de);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-danger, #a8442f);
}

.order-detail-page__success {
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
  background: var(--color-brand-soft, #e4eadb);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-brand, #496544);
  font-weight: 700;
}

.order-detail-page__archive {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
  background: var(--color-brand-soft, #e4eadb);
  border-radius: var(--radius-card, 0.75rem);
}

.order-detail-page__archive strong {
  color: var(--color-brand, #496544);
}

.order-detail-page__archive p {
  margin: 0.2rem 0 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.8rem;
}

.pay-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
  padding: var(--space-lg, 1.5rem);
  margin-bottom: var(--space-lg, 1.5rem);
  background: var(--color-brand-deep, #203d2c);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-on-dark, #fffdf7);
}

.pay-panel h2 {
  margin: 0.2rem 0;
}

.pay-panel p:last-child {
  margin: 0;
  opacity: 0.8;
  font-size: 0.8rem;
}

.pay-panel__button {
  flex-shrink: 0;
  background: var(--color-brand-soft, #e4eadb);
  border-color: var(--color-brand-soft, #e4eadb);
  color: var(--color-brand-deep, #203d2c);
}

.pay-panel__button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.order-timeline {
  display: grid;
  gap: 0;
  margin: 0 0 var(--space-lg, 1.5rem);
  padding: 0;
  list-style: none;
}

.order-timeline li {
  display: grid;
  grid-template-columns: 1.5rem minmax(0, 1fr);
  gap: var(--space-sm, 0.75rem);
  position: relative;
  padding-bottom: var(--space-md, 1rem);
}

.order-timeline li:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 0.45rem;
  top: 1.1rem;
  bottom: 0;
  width: 2px;
  background: var(--color-border, #d9d3c5);
}

.order-timeline__dot {
  width: 0.9rem;
  height: 0.9rem;
  margin-top: 0.15rem;
  border: 2px solid var(--color-border, #d9d3c5);
  border-radius: 50%;
  background: var(--color-surface, #fffdf7);
}

.order-timeline li.done .order-timeline__dot {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
}

.order-timeline li.done strong {
  color: var(--color-brand, #496544);
}

.order-timeline li:not(.done) strong {
  color: var(--color-text-muted, #68716a);
}

.order-timeline small {
  display: block;
  margin-top: 0.1rem;
  color: var(--color-text-muted, #68716a);
  font-size: 0.72rem;
}

.order-block {
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
}

.order-block h2 {
  margin: 0 0 var(--space-sm, 0.75rem);
  font-size: 0.95rem;
}

.order-block__receiver {
  margin: 0;
  font-weight: 700;
}

.order-block__receiver span {
  margin-left: var(--space-sm, 0.75rem);
  color: var(--color-text-muted, #68716a);
  font-weight: 400;
}

.order-block__address {
  margin: 0.2rem 0 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.85rem;
}

.order-block__reason {
  margin: 0 0 0.3rem;
  color: var(--color-text-muted, #68716a);
  font-size: 0.85rem;
}

.order-items {
  display: grid;
  gap: var(--space-sm, 0.75rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

.order-items__row {
  display: grid;
  grid-template-columns: 3.5rem minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-md, 1rem);
}

.order-items__row img,
.order-items__fallback {
  width: 3.5rem;
  height: 3.5rem;
  object-fit: cover;
  border-radius: var(--radius-card, 0.75rem);
}

.order-items__fallback {
  display: grid;
  place-items: center;
  background: var(--color-brand-soft, #e4eadb);
  color: var(--color-brand, #496544);
}

.order-items__body p {
  margin: 0.1rem 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.78rem;
}

.order-items__body small {
  color: var(--color-text-muted, #68716a);
  font-size: 0.75rem;
}

.order-items__subtotal {
  color: var(--color-brand, #496544);
  white-space: nowrap;
}

.order-items__total {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: var(--space-sm, 0.75rem);
  padding-top: var(--space-md, 1rem);
  margin-top: var(--space-md, 1rem);
  border-top: 1px dashed var(--color-border, #d9d3c5);
}

.order-items__total strong {
  color: var(--color-brand, #496544);
  font-size: 1.15rem;
}

.order-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm, 0.75rem);
}

.pill-button--primary {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
  color: var(--color-on-brand, #fffdf7);
}
</style>
