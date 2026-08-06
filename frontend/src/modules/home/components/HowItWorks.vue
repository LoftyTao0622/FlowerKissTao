<script setup lang="ts">
const steps = [
  { number: '01', title: '描述真实环境', detail: '记录摆放位置、光照、空间、安全条件和可投入的养护时间。' },
  { number: '02', title: '查看适配依据', detail: '先排除硬性不合适项，再逐项解释光照、预算和养护能力得分。' },
  { number: '03', title: '购买后继续养护', detail: '收货后生成浇水、施肥、换盆和病虫害预防计划。' },
]
</script>

<template>
  <section class="process container section-shell" aria-labelledby="process-title">
    <header class="process__header">
      <p class="section-eyebrow">不是一次性的推荐</p>
      <h2 id="process-title">从理解环境，到陪你养好</h2>
    </header>

    <ol class="process__steps">
      <li v-for="step in steps" :key="step.number">
        <span aria-hidden="true">{{ step.number }}</span>
        <div>
          <h3>{{ step.title }}</h3>
          <p>{{ step.detail }}</p>
        </div>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.process {
  display: grid;
  gap: var(--space-xl);
  padding-block: clamp(3.5rem, 8vw, 6.5rem);
}

.process__header h2 {
  max-width: 15ch;
  margin: 0;
  color: var(--color-brand-deep);
  font-size: clamp(1.8rem, 4vw, 3rem);
  line-height: 1.2;
  text-wrap: balance;
}

.process__steps {
  margin: 0;
  padding: 0;
  list-style: none;
}

.process__steps li {
  display: grid;
  grid-template-columns: 3.25rem 1fr;
  gap: var(--space-md);
  padding: var(--space-lg) 0;
  border-top: 1px solid var(--color-border);
}

.process__steps li:last-child {
  border-bottom: 1px solid var(--color-border);
}

.process__steps span {
  color: var(--color-brand);
  font-size: 0.8rem;
  font-weight: 750;
  letter-spacing: 0.08em;
}

.process__steps h3 {
  margin: 0 0 var(--space-xs);
  color: var(--color-ink);
  font-size: 1.15rem;
}

.process__steps p {
  max-width: 52ch;
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

@media (min-width: 56rem) {
  .process {
    grid-template-columns: minmax(18rem, 0.7fr) minmax(0, 1.3fr);
    align-items: start;
  }
}
</style>
