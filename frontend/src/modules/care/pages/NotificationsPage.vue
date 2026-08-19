<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { useCareStore } from '../stores/care'
import type { CareNotification } from '../types/care'

const careStore = useCareStore()
const router = useRouter()

const TYPE_LABELS: Record<string, string> = {
  task_due: '任务提醒',
  overdue: '逾期提醒',
  health: '状态反馈',
  re_eval: '频率调整',
}

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', { hour12: false })
}

/** 点提醒既标已读又跳到对应植物，省得用户再找一遍 */
async function openNotification(item: CareNotification) {
  if (!item.read) {
    await careStore.markRead(item.id)
  }
  if (item.archiveId) {
    await router.push({ name: 'plant-care', params: { archiveId: item.archiveId } })
  }
}

onMounted(() => {
  void careStore.loadNotifications()
})
</script>

<template>
  <section class="notify-page">
    <header class="notify-page__header">
      <div>
        <p class="section-kicker">养护提醒</p>
        <h1>该做的事，系统替你记着</h1>
      </div>
      <button
        v-if="careStore.unreadCount > 0"
        class="text-action"
        type="button"
        @click="careStore.markAllRead()"
      >
        全部标记已读
      </button>
    </header>

    <p v-if="careStore.errorMessage" class="notify-page__error" role="alert">
      {{ careStore.errorMessage }}
    </p>

    <p v-if="!careStore.notifications.length" class="notify-page__empty">
      还没有提醒。养护任务到期时会出现在这里。
      <RouterLink class="text-action" :to="{ name: 'my-plants' }">查看我的植物</RouterLink>
    </p>

    <ul v-else class="notify-list">
      <li v-for="item in careStore.notifications" :key="item.id">
        <button
          type="button"
          :class="['notify-card', { 'notify-card--unread': !item.read }]"
          @click="openNotification(item)"
        >
          <span class="notify-card__type">{{ TYPE_LABELS[item.type] ?? '提醒' }}</span>
          <strong class="notify-card__title">{{ item.title }}</strong>
          <p class="notify-card__content">{{ item.content }}</p>
          <small>{{ formatTime(item.createdAt) }}</small>
          <span v-if="!item.read" class="notify-card__dot" aria-label="未读"></span>
        </button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.notify-page {
  max-width: 42rem;
  margin: 0 auto;
  padding: var(--space-2xl, 3rem) var(--space-lg, 1.25rem);
}

.notify-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
  margin-bottom: var(--space-lg, 1.5rem);
}

.notify-page__header h1 {
  margin: 0.35rem 0 0;
}

.notify-page__error {
  padding: var(--space-md, 1rem);
  background: var(--color-danger-soft, #f6e3de);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-danger, #a8442f);
}

.notify-page__empty {
  padding: var(--space-2xl, 3rem) 0;
  text-align: center;
  color: var(--color-text-muted, #68716a);
}

.notify-list {
  display: grid;
  gap: var(--space-sm, 0.75rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

.notify-card {
  position: relative;
  display: block;
  width: 100%;
  padding: var(--space-md, 1rem);
  text-align: left;
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
  cursor: pointer;
  font: inherit;
}

.notify-card:hover {
  border-color: var(--color-brand, #496544);
}

.notify-card--unread {
  background: var(--color-brand-soft, #e4eadb);
}

.notify-card__type {
  display: inline-block;
  margin-bottom: 0.3rem;
  padding: 0.05rem 0.45rem;
  background: var(--color-surface, #fffdf7);
  border-radius: var(--radius-pill, 999px);
  color: var(--color-brand, #496544);
  font-size: 0.65rem;
  font-weight: 700;
}

.notify-card__title {
  display: block;
  font-size: 0.9rem;
}

.notify-card__content {
  margin: 0.25rem 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.8rem;
  line-height: 1.6;
}

.notify-card small {
  color: var(--color-text-muted, #68716a);
  font-size: 0.7rem;
}

.notify-card__dot {
  position: absolute;
  top: var(--space-md, 1rem);
  right: var(--space-md, 1rem);
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 50%;
  background: var(--color-danger, #a8442f);
}
</style>
