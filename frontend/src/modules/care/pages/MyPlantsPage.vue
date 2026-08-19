<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'

import { useCareStore } from '../stores/care'

const careStore = useCareStore()

/** 下一个任务的相对日期文案。日历上"3 天后"比"2026-08-20"直观 */
function dueLabel(days: number) {
  if (days < 0) return `已逾期 ${Math.abs(days)} 天`
  if (days === 0) return '今天'
  if (days === 1) return '明天'
  return `${days} 天后`
}

onMounted(() => {
  void careStore.loadArchives()
})
</script>

<template>
  <section class="plants-page">
    <header class="plants-page__header">
      <p class="section-kicker">我的植物</p>
      <h1>买到只是开始，养好才算数</h1>
      <p>确认收货后系统会依据品种、你的场景与当前季节自动排好养护计划。</p>
    </header>

    <p v-if="careStore.errorMessage" class="plants-page__error" role="alert">
      {{ careStore.errorMessage }}
    </p>

    <p v-if="careStore.loading" class="plants-page__status" role="status">加载中…</p>

    <div v-else-if="!careStore.hasPlants" class="plants-page__empty">
      <span class="plants-page__leaf" aria-hidden="true">苗</span>
      <h2>还没有需要照料的植物</h2>
      <p>下单并确认收货后，这里会自动出现它的养护档案与任务日历。</p>
      <div class="plants-page__empty-actions">
        <RouterLink class="pill-button pill-button--primary" :to="{ name: 'recommendation' }">
          先找到适合的植物
        </RouterLink>
        <RouterLink class="pill-button pill-button--outline" :to="{ name: 'plant-catalog' }">
          逛植物
        </RouterLink>
      </div>
    </div>

    <template v-else>
      <p v-if="careStore.totalOverdue > 0" class="plants-page__alert" role="status">
        有 {{ careStore.totalOverdue }} 项养护任务已逾期，现在补做仍然来得及。
      </p>

      <ul class="plant-grid">
        <li v-for="plant in careStore.archives" :key="plant.id">
          <RouterLink
            class="plant-card"
            :to="{ name: 'plant-care', params: { archiveId: plant.id } }"
          >
            <div class="plant-card__media">
              <img v-if="plant.plantImage" :src="plant.plantImage" :alt="plant.plantName" />
              <span v-else aria-hidden="true">植</span>
              <span v-if="plant.overdueCount > 0" class="plant-card__badge">
                {{ plant.overdueCount }} 项逾期
              </span>
            </div>

            <div class="plant-card__body">
              <h2>{{ plant.plantName }}</h2>
              <p class="plant-card__meta">
                已陪伴 {{ plant.adoptedDays }} 天
                <template v-if="plant.waterIntervalDays">
                  · 每 {{ plant.waterIntervalDays }} 天浇一次
                </template>
              </p>

              <p v-if="plant.adjustmentNote" class="plant-card__adjust">
                {{ plant.adjustmentNote }}
              </p>

              <div v-if="plant.nextTask" class="plant-card__next">
                <span class="plant-card__next-icon" aria-hidden="true">
                  {{ plant.nextTask.icon }}
                </span>
                <div>
                  <strong>{{ plant.nextTask.title }}</strong>
                  <small
                    :class="{ 'is-overdue': plant.nextTask.daysFromToday < 0 }"
                  >{{ dueLabel(plant.nextTask.daysFromToday) }}</small>
                </div>
              </div>
              <p v-else class="plant-card__next-empty">近期没有待办任务</p>

              <p class="plant-card__pending">共 {{ plant.pendingCount }} 项待办</p>
            </div>
          </RouterLink>
        </li>
      </ul>
    </template>
  </section>
</template>

<style scoped>
.plants-page {
  max-width: 60rem;
  margin: 0 auto;
  padding: var(--space-2xl, 3rem) var(--space-lg, 1.25rem);
}

.plants-page__header {
  margin-bottom: var(--space-lg, 1.5rem);
}

.plants-page__header h1 {
  margin: 0.35rem 0 0.5rem;
}

.plants-page__header p:last-child {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.88rem;
}

.plants-page__error {
  padding: var(--space-md, 1rem);
  background: var(--color-danger-soft, #f6e3de);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-danger, #a8442f);
}

.plants-page__alert {
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-lg, 1.5rem);
  background: var(--color-danger-soft, #f6e3de);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-danger, #a8442f);
  font-size: 0.85rem;
}

.plants-page__status,
.plants-page__empty {
  padding: var(--space-2xl, 3rem) 0;
  text-align: center;
  color: var(--color-text-muted, #68716a);
}

.plants-page__empty {
  display: grid;
  place-items: center;
}

.plants-page__leaf {
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

.plants-page__empty h2 {
  margin: var(--space-md, 1rem) 0 0.4rem;
  color: var(--color-ink, #24312a);
}

.plants-page__empty-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm, 0.75rem);
  margin-top: var(--space-lg, 1.5rem);
}

.plant-grid {
  display: grid;
  gap: var(--space-md, 1rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

@media (min-width: 44rem) {
  .plant-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.plant-card {
  display: grid;
  grid-template-columns: 7rem minmax(0, 1fr);
  overflow: hidden;
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
  color: inherit;
  text-decoration: none;
}

.plant-card:hover {
  border-color: var(--color-brand, #496544);
}

.plant-card__media {
  position: relative;
  display: grid;
  min-height: 100%;
  place-items: center;
  overflow: hidden;
  background: var(--color-brand-soft, #e4eadb);
  color: var(--color-brand, #496544);
}

.plant-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.plant-card__badge {
  position: absolute;
  top: 0.4rem;
  left: 0.4rem;
  padding: 0.1rem 0.45rem;
  background: var(--color-danger, #a8442f);
  border-radius: var(--radius-pill, 999px);
  color: #fff;
  font-size: 0.65rem;
  font-weight: 700;
}

.plant-card__body {
  padding: var(--space-md, 1rem);
}

.plant-card__body h2 {
  margin: 0;
  font-size: 1rem;
}

.plant-card__meta {
  margin: 0.2rem 0 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.75rem;
}

.plant-card__adjust {
  margin: 0.4rem 0 0;
  padding: 0.25rem 0.5rem;
  background: var(--color-brand-soft, #e4eadb);
  border-radius: var(--radius-sm, 0.5rem);
  color: var(--color-brand, #496544);
  font-size: 0.7rem;
}

.plant-card__next {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.75rem);
  margin-top: var(--space-sm, 0.75rem);
}

.plant-card__next-icon {
  font-size: 1.1rem;
}

.plant-card__next strong {
  display: block;
  font-size: 0.85rem;
}

.plant-card__next small {
  color: var(--color-text-muted, #68716a);
  font-size: 0.72rem;
}

.plant-card__next small.is-overdue {
  color: var(--color-danger, #a8442f);
  font-weight: 700;
}

.plant-card__next-empty,
.plant-card__pending {
  margin: var(--space-sm, 0.75rem) 0 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.72rem;
}

.pill-button--primary {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
  color: var(--color-on-brand, #fffdf7);
}
</style>
