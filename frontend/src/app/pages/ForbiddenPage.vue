<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="forbidden container">
    <p class="forbidden__code">403</p>
    <h1>没有访问权限</h1>
    <p class="forbidden__hint">
      当前账号的角色不包含此页面所需的权限。如需访问，请联系管理员调整角色。
    </p>
    <RouterLink class="forbidden__action" :to="{ name: 'home' }">返回首页</RouterLink>
  </section>
</template>

<style scoped>
.forbidden {
  display: grid;
  min-height: 60vh;
  padding-block: var(--space-xl);
  justify-items: center;
  align-content: center;
  text-align: center;
}

.forbidden__code {
  margin: 0;
  color: var(--color-brand-soft);
  font-size: clamp(3.5rem, 12vw, 5rem);
  font-weight: 750;
  line-height: 1;
}

.forbidden h1 {
  margin: var(--space-sm) 0;
  color: var(--color-brand);
  font-size: 1.5rem;
}

.forbidden__hint {
  max-width: 32rem;
  margin: 0 0 var(--space-md);
  color: var(--color-text-muted);
  line-height: 1.65;
}

.forbidden__action {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  padding-inline: 1.5rem;
  border-radius: var(--radius-pill);
  background: var(--color-brand-accent);
  color: var(--color-on-brand);
  font-weight: 750;
  text-decoration: none;
}

.forbidden__action:active {
  transform: scale(0.95);
}
</style>
