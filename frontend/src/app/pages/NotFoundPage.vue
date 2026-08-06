<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="not-found section-shell" aria-labelledby="not-found-title">
    <div class="container not-found__inner">
      <p class="not-found__eyebrow">404 · 这片叶子暂时找不到</p>
      <h1 id="not-found-title">这里还没有种下内容</h1>
      <p>
        你访问的页面可能已经移动。回到首页，或继续从植物目录里寻找适合你的那一盆。
      </p>
      <div class="not-found__actions">
        <RouterLink class="pill-button" :to="{ name: 'home' }">返回首页</RouterLink>
        <RouterLink class="pill-button pill-button--outline" :to="{ name: 'plant-catalog' }">
          逛植物
        </RouterLink>
      </div>
    </div>
  </section>
</template>

<style scoped>
.not-found {
  display: grid;
  min-height: 62vh;
  place-items: center;
}

.not-found__inner {
  max-width: 44rem;
  text-align: center;
}

.not-found__eyebrow {
  margin: 0 0 var(--space-sm);
  color: var(--color-brand);
  font-size: 0.875rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

h1 {
  margin: 0;
  color: var(--color-ink);
  font-size: clamp(2rem, 5vw, 3.5rem);
  line-height: 1.15;
}

p:not(.not-found__eyebrow) {
  max-width: 36rem;
  margin: var(--space-md) auto 0;
  color: var(--color-text-muted);
  line-height: 1.8;
}

.not-found__actions {
  display: flex;
  justify-content: center;
  gap: var(--space-sm);
  margin-top: var(--space-lg);
}

@media (max-width: 30rem) {
  .not-found__actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
