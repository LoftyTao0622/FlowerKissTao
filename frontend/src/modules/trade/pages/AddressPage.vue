<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'

import { useAddressStore } from '../stores/address'
import type { Address, AddressInput } from '../types/trade'

const addressStore = useAddressStore()

/** null = 新建，number = 编辑某条 */
const editingId = ref<number | null>(null)
const showForm = ref(false)

const form = reactive<AddressInput>({
  receiver: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
})

const formError = ref('')

function startCreate() {
  editingId.value = null
  Object.assign(form, {
    receiver: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
  })
  formError.value = ''
  showForm.value = true
}

function startEdit(address: Address) {
  editingId.value = address.id
  Object.assign(form, {
    receiver: address.receiver,
    phone: address.phone,
    province: address.province,
    city: address.city,
    district: address.district,
    detail: address.detail,
  })
  formError.value = ''
  showForm.value = true
}

function cancelForm() {
  showForm.value = false
  formError.value = ''
}

async function submitForm() {
  formError.value = ''
  // 简单前端校验，兜底的还是后端 @Valid
  if (!form.receiver.trim() || !form.phone.trim() || !form.detail.trim()) {
    formError.value = '收件人、电话和详细地址不能为空'
    return
  }
  const ok = await addressStore.save(editingId.value, { ...form })
  if (ok) {
    showForm.value = false
  } else {
    formError.value = addressStore.errorMessage
  }
}

async function removeAddress(address: Address) {
  try {
    await ElMessageBox.confirm(
      `删除地址「${address.receiver} · ${address.fullAddress}」？`,
      '删除地址',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
    await addressStore.remove(address.id)
  } catch {
    // 取消
  }
}

async function makeDefault(address: Address) {
  if (address.isDefault) return
  await addressStore.makeDefault(address.id)
}

onMounted(() => {
  void addressStore.load()
})
</script>

<template>
  <section class="address-page">
    <header class="address-page__header">
      <p class="section-kicker">收货地址</p>
      <h1>寄到哪里，由你说了算</h1>
      <p>默认地址在结算时会自动选中，订单会保存下单时的地址快照。</p>
    </header>

    <p v-if="addressStore.errorMessage" class="address-page__error" role="alert">
      {{ addressStore.errorMessage }}
    </p>

    <button
      v-if="!showForm"
      class="pill-button pill-button--primary address-page__add"
      type="button"
      @click="startCreate"
    >
      新增收货地址
    </button>

    <!-- 新增 / 编辑表单 -->
    <form v-if="showForm" class="address-form" @submit.prevent="submitForm">
      <h2>{{ editingId === null ? '新增地址' : '编辑地址' }}</h2>
      <p v-if="formError" class="address-form__error" role="alert">{{ formError }}</p>

      <div class="address-form__grid">
        <label>
          <span>收件人</span>
          <input v-model="form.receiver" type="text" maxlength="30" autocomplete="name" />
        </label>
        <label>
          <span>联系电话</span>
          <input v-model="form.phone" type="tel" maxlength="20" autocomplete="tel" />
        </label>
      </div>

      <div class="address-form__grid address-form__grid--three">
        <label>
          <span>省份</span>
          <input v-model="form.province" type="text" maxlength="30" />
        </label>
        <label>
          <span>城市</span>
          <input v-model="form.city" type="text" maxlength="30" />
        </label>
        <label>
          <span>区 / 县</span>
          <input v-model="form.district" type="text" maxlength="30" />
        </label>
      </div>

      <label>
        <span>详细地址</span>
        <input v-model="form.detail" type="text" maxlength="120" placeholder="街道、门牌号、楼栋房间" />
      </label>

      <div class="address-form__actions">
        <button class="pill-button pill-button--primary" type="submit" :disabled="addressStore.saving">
          {{ addressStore.saving ? '保存中…' : '保存' }}
        </button>
        <button class="pill-button pill-button--outline" type="button" @click="cancelForm">
          取消
        </button>
      </div>
    </form>

    <!-- 地址列表 -->
    <ul v-if="addressStore.addresses.length" class="address-list">
      <li v-for="address in addressStore.addresses" :key="address.id">
        <div class="address-card">
          <div class="address-card__body">
            <p class="address-card__receiver">
              {{ address.receiver }}
              <span>{{ address.phone }}</span>
              <span v-if="address.isDefault" class="address-card__tag">默认</span>
            </p>
            <p class="address-card__detail">{{ address.fullAddress }}</p>
          </div>
          <div class="address-card__actions">
            <button class="text-action" type="button" @click="startEdit(address)">编辑</button>
            <button
              v-if="!address.isDefault"
              class="text-action"
              type="button"
              @click="makeDefault(address)"
            >
              设为默认
            </button>
            <button class="text-action text-action--danger" type="button" @click="removeAddress(address)">
              删除
            </button>
          </div>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.address-page {
  max-width: 40rem;
  margin: 0 auto;
  padding: var(--space-2xl, 3rem) var(--space-lg, 1.25rem);
}

.address-page__header {
  margin-bottom: var(--space-lg, 1.5rem);
}

.address-page__header h1 {
  margin: 0.35rem 0 0.5rem;
}

.address-page__header p:last-child {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.88rem;
}

.address-page__error {
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
  background: var(--color-danger-soft, #f6e3de);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-danger, #a8442f);
}

.address-page__add {
  margin-bottom: var(--space-lg, 1.5rem);
}

.address-form {
  display: grid;
  gap: var(--space-md, 1rem);
  padding: var(--space-lg, 1.5rem);
  margin-bottom: var(--space-lg, 1.5rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
}

.address-form h2 {
  margin: 0;
  font-size: 1rem;
}

.address-form__error {
  margin: 0;
  color: var(--color-danger, #a8442f);
  font-size: 0.82rem;
}

.address-form label {
  display: grid;
  gap: 0.35rem;
}

.address-form label span {
  font-size: 0.8rem;
  font-weight: 600;
}

.address-form input {
  min-height: 2.75rem;
  padding: 0.5rem 0.8rem;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-sm, 0.5rem);
  font-size: 0.9rem;
}

.address-form input:focus-visible {
  outline: 2px solid var(--color-brand, #496544);
  outline-offset: 1px;
}

.address-form__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-md, 1rem);
}

.address-form__grid--three {
  grid-template-columns: 1fr 1fr 1fr;
}

@media (max-width: 32rem) {
  .address-form__grid,
  .address-form__grid--three {
    grid-template-columns: 1fr;
  }
}

.address-form__actions {
  display: flex;
  gap: var(--space-sm, 0.75rem);
}

.address-list {
  display: grid;
  gap: var(--space-md, 1rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

.address-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
  padding: var(--space-md, 1rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
}

.address-card__receiver {
  display: flex;
  align-items: baseline;
  gap: var(--space-sm, 0.75rem);
  margin: 0;
  font-weight: 700;
}

.address-card__receiver span {
  color: var(--color-text-muted, #68716a);
  font-weight: 400;
}

.address-card__tag {
  padding: 0.1rem 0.5rem;
  background: var(--color-brand-soft, #e4eadb);
  border-radius: var(--radius-pill, 999px);
  color: var(--color-brand, #496544);
  font-size: 0.7rem;
  font-weight: 700;
}

.address-card__detail {
  margin: 0.3rem 0 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.85rem;
  line-height: 1.5;
}

.address-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm, 0.75rem);
}

.text-action--danger {
  color: var(--color-danger, #a8442f);
}

.pill-button--primary {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
  color: var(--color-on-brand, #fffdf7);
}
</style>
