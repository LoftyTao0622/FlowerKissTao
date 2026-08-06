<script setup lang="ts">
import { House, Pouring, Sunny, Timer } from '@element-plus/icons-vue'

const profileItems = [
  { label: '光照条件', value: '明亮散射光', icon: Sunny },
  { label: '空间类型', value: '客厅 · 20–30㎡', icon: House },
  { label: '养护习惯', value: '每周 1–2 次', icon: Pouring },
  { label: '可投入时间', value: '每次约 10 分钟', icon: Timer },
]
</script>

<template>
  <section class="profile" aria-labelledby="profile-title">
    <header class="profile__header">
      <div>
        <p class="profile__eyebrow">当前示例</p>
        <h2 id="profile-title">你的环境档案</h2>
      </div>
      <RouterLink class="profile__edit" to="/recommendation">重新填写</RouterLink>
    </header>

    <dl class="profile__grid">
      <div v-for="item in profileItems" :key="item.label" class="profile__item">
        <el-icon class="profile__icon" aria-hidden="true"><component :is="item.icon" /></el-icon>
        <div>
          <dt>{{ item.label }}</dt>
          <dd>{{ item.value }}</dd>
        </div>
      </div>
    </dl>
  </section>
</template>

<style scoped>
.profile {
  width: min(100%, 40rem);
  padding: var(--space-md);
  color: var(--color-ink);
  background: var(--color-brand-soft);
  border: 1px solid color-mix(in oklch, var(--color-brand) 18%, var(--color-border));
  border-radius: var(--radius-card);
}

.profile__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}

.profile__eyebrow {
  margin: 0 0 0.2rem;
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 650;
  letter-spacing: 0.08em;
}

.profile h2 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}

.profile__edit {
  min-height: 2.75rem;
  display: inline-flex;
  align-items: center;
  color: var(--color-brand);
  font-size: 0.875rem;
  font-weight: 650;
  text-decoration: none;
}

.profile__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1px;
  margin: 0;
  overflow: hidden;
  background: color-mix(in oklch, var(--color-brand) 14%, var(--color-border));
  border-radius: calc(var(--radius-card) - 0.25rem);
}

.profile__item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
  padding: 0.75rem;
  background: var(--color-surface);
}

.profile__icon {
  flex: 0 0 auto;
  color: var(--color-brand);
  font-size: 1.25rem;
}

.profile dt {
  margin-bottom: 0.15rem;
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.profile dd {
  margin: 0;
  font-size: 0.78rem;
  font-weight: 650;
  line-height: 1.35;
}

@media (min-width: 62rem) {
  .profile {
    padding: var(--space-lg);
  }

  .profile__grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .profile__item {
    align-items: flex-start;
    gap: 0.5rem;
    padding-inline: 0.55rem;
  }

}
</style>
