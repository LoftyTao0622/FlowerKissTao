<script setup lang="ts">
import { Calendar, Pouring, Sunny, Timer } from '@element-plus/icons-vue'

const careItems = [
  { title: '浇水检查', detail: '每周 1 次，先摸土再决定', icon: Pouring },
  { title: '光照建议', detail: '靠近窗边，避开正午直射', icon: Sunny },
  { title: '施肥节奏', detail: '生长期每月补充 1 次', icon: Timer },
  { title: '换盆提醒', detail: '预计 12 个月后评估根系', icon: Calendar },
]
</script>

<template>
  <section class="care" aria-labelledby="care-title">
    <div class="care__inner container section-shell">
      <div class="care__intro">
        <p class="care__eyebrow">购买之后仍然有人陪你</p>
        <h2 id="care-title">一株植物，一份持续更新的养护计划</h2>
        <p>确认收货后，系统会结合季节和你的场景生成任务。完成、延后或反馈状态，下一次建议会更贴合。</p>
        <RouterLink class="care__action" to="/care">查看养护指南</RouterLink>
      </div>

      <ul class="care__list" aria-label="养护计划预览">
        <li v-for="item in careItems" :key="item.title">
          <el-icon aria-hidden="true"><component :is="item.icon" /></el-icon>
          <div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.detail }}</p>
          </div>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.care {
  color: var(--color-surface);
  background: var(--color-brand-deep);
}

.care__inner {
  display: grid;
  gap: clamp(2.5rem, 6vw, 5rem);
  padding-block: clamp(3.5rem, 8vw, 6rem);
}

.care__intro {
  max-width: 35rem;
}

.care__eyebrow {
  margin: 0 0 var(--space-sm);
  color: var(--color-brand-soft);
  font-size: 0.8rem;
  font-weight: 750;
  letter-spacing: 0.12em;
}

.care h2 {
  margin: 0;
  font-size: clamp(1.8rem, 4vw, 3rem);
  line-height: 1.2;
  text-wrap: balance;
}

.care__intro > p:not(.care__eyebrow) {
  max-width: 58ch;
  margin: var(--space-md) 0 var(--space-lg);
  color: color-mix(in oklch, var(--color-surface) 72%, var(--color-brand-soft));
  line-height: 1.75;
}

.care__action {
  min-height: 2.75rem;
  display: inline-flex;
  align-items: center;
  padding: 0.7rem 1.2rem;
  color: var(--color-brand-deep);
  background: var(--color-surface);
  border-radius: var(--radius-pill);
  font-weight: 700;
  text-decoration: none;
  transition: transform 150ms var(--ease-out);
}

.care__action:active {
  transform: scale(0.95);
}

.care__list {
  display: grid;
  margin: 0;
  padding: 0;
  border-block: 1px solid color-mix(in oklch, var(--color-surface) 24%, transparent);
  list-style: none;
}

.care__list li {
  display: grid;
  grid-template-columns: 2rem 1fr;
  gap: var(--space-sm);
  padding: var(--space-lg) 0;
  border-bottom: 1px solid color-mix(in oklch, var(--color-surface) 18%, transparent);
}

.care__list li:last-child {
  border-bottom: 0;
}

.care__list .el-icon {
  color: var(--color-brand-soft);
  font-size: 1.5rem;
}

.care__list h3 {
  margin: 0 0 0.35rem;
  font-size: 1rem;
}

.care__list p {
  margin: 0;
  color: color-mix(in oklch, var(--color-surface) 68%, var(--color-brand-soft));
  font-size: 0.9rem;
  line-height: 1.55;
}

@media (min-width: 64rem) {
  .care__inner {
    grid-template-columns: minmax(0, 1fr) minmax(34rem, 1.25fr);
    align-items: center;
  }

  .care__list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    border-block: 0;
  }

  .care__list li {
    padding: var(--space-lg);
    border-right: 1px solid color-mix(in oklch, var(--color-surface) 18%, transparent);
  }

  .care__list li:nth-child(2n) {
    border-right: 0;
  }

  .care__list li:nth-child(-n + 2) {
    border-bottom: 1px solid color-mix(in oklch, var(--color-surface) 18%, transparent);
  }
}
</style>
