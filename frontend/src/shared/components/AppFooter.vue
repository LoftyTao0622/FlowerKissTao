<script setup lang="ts">
const currentYear = new Date().getFullYear()

const footerLinks = [
  { label: '逛植物', to: { name: 'plant-catalog' } },
  { label: '智能推荐', to: { name: 'recommendation' } },
  { label: '知识库', to: { name: 'knowledge' } },
  { label: '养护指南', to: { name: 'care-guide' } },
]
</script>

<template>
  <footer class="site-footer">
    <div class="container site-footer__main">
      <div class="site-footer__intro">
        <p class="site-footer__brand">花吻陶</p>
        <p>从光照、空间与生活习惯出发，让每一盆植物都有更合适的位置。</p>
      </div>
      <nav aria-label="页脚导航">
        <RouterLink v-for="item in footerLinks" :key="item.label" :to="item.to">
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="site-footer__promise">
        <strong>先判断适合，再安心带回家</strong>
        <span>推荐依据清晰可见，养护建议具体可执行。</span>
      </div>
    </div>
    <div class="container site-footer__bottom">
      <span>© {{ currentYear }} 花吻陶</span>
      <span>为家庭与办公空间认真选植物</span>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  padding-top: var(--space-xl);
  background: var(--color-brand-deep);
  color: var(--color-on-brand);
}

.site-footer__main {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) auto minmax(15rem, 0.8fr);
  gap: clamp(var(--space-lg), 5vw, var(--space-2xl));
  align-items: start;
  padding-bottom: var(--space-xl);
}

.site-footer__brand {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.site-footer__intro p:last-child {
  max-width: 28rem;
  margin: var(--space-sm) 0 0;
  line-height: 1.75;
  opacity: 0.74;
}

nav {
  display: grid;
  gap: 0.25rem;
}

nav a {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  color: var(--color-on-brand);
  font-weight: 700;
  text-decoration: none;
}

nav a:hover {
  text-decoration: underline;
}

.site-footer__promise {
  display: grid;
  gap: var(--space-xs);
  padding: var(--space-md);
  border: 1px solid var(--color-brand-accent);
  border-radius: var(--radius-card);
}

.site-footer__promise span {
  font-size: 0.875rem;
  line-height: 1.6;
  opacity: 0.74;
}

.site-footer__bottom {
  display: flex;
  min-height: 64px;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  border-top: 1px solid var(--color-brand-accent);
  font-size: 0.8rem;
  opacity: 0.72;
}

@media (max-width: 52rem) {
  .site-footer__main {
    grid-template-columns: 1fr 1fr;
  }

  .site-footer__intro {
    grid-column: 1 / -1;
  }
}

@media (max-width: 34rem) {
  .site-footer__main {
    grid-template-columns: 1fr;
  }

  .site-footer__intro {
    grid-column: auto;
  }

  .site-footer__bottom {
    align-items: flex-start;
    flex-direction: column;
    justify-content: center;
    padding-block: var(--space-sm);
  }
}
</style>
