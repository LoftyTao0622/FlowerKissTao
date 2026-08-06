<script setup lang="ts">
import { ArrowRight } from '@element-plus/icons-vue'
import heroDesktop from '@/assets/images/hero-greenhouse.webp'
import heroMobile from '@/assets/images/hero-greenhouse-mobile.webp'
import EnvironmentProfile from './EnvironmentProfile.vue'
</script>

<template>
  <section class="hero" aria-labelledby="hero-title">
    <div class="hero__content">
      <p class="hero__eyebrow">先适配，再选购</p>
      <h1 id="hero-title">
        找到真正适合
        <span>你环境的植物</span>
      </h1>
      <p class="hero__lead">
        不追逐销量榜。根据光照、空间、宠物安全和养护时间，为你筛掉不合适的选择。
      </p>

      <div class="hero__actions" aria-label="开始选购">
        <RouterLink class="button button--primary" to="/recommendation">
          开始智能匹配
          <el-icon aria-hidden="true"><ArrowRight /></el-icon>
        </RouterLink>
        <RouterLink class="button button--outline" to="/plants">浏览全部植物</RouterLink>
      </div>

      <EnvironmentProfile />
      <p class="hero__note">档案只用于计算适配度，你可以随时修改。</p>
    </div>

    <figure class="hero__visual">
      <picture>
        <source media="(max-width: 47.99rem)" :srcset="heroMobile" />
        <img
          :src="heroDesktop"
          alt="自然光照进摆满龟背竹、竹芋和蕨类植物的客厅一角"
          width="1718"
          height="916"
          fetchpriority="high"
        />
      </picture>
      <figcaption>明亮散射光场景示例</figcaption>
    </figure>
  </section>
</template>

<style scoped>
.hero {
  display: grid;
  min-height: calc(100svh - 4.5rem);
  overflow: clip;
  background: var(--color-canvas);
}

.hero__content {
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: var(--space-md);
  padding: clamp(2.5rem, 8vw, 6rem) max(var(--space-md), env(safe-area-inset-left));
}

.hero__eyebrow {
  margin: 0;
  color: var(--color-brand);
  font-size: 0.8rem;
  font-weight: 750;
  letter-spacing: 0.14em;
}

.hero h1 {
  max-width: 12ch;
  margin: 0;
  color: var(--color-brand-deep);
  font-size: clamp(2.55rem, 8vw, 4.75rem);
  font-weight: 650;
  letter-spacing: -0.035em;
  line-height: 1.08;
  text-wrap: balance;
}

.hero h1 span {
  display: block;
}

.hero__lead {
  max-width: 34rem;
  margin: 0;
  color: var(--color-text);
  font-size: 1.05rem;
  line-height: 1.7;
  text-wrap: pretty;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.button {
  min-height: 2.75rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  padding: 0.72rem 1.3rem;
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
  font-size: 0.95rem;
  font-weight: 700;
  text-decoration: none;
  transition: transform 150ms var(--ease-out), background-color 200ms var(--ease-out);
}

.button:active {
  transform: scale(0.95);
}

.button--primary {
  color: var(--color-surface);
  background: var(--color-brand-accent);
}

.button--outline {
  color: var(--color-brand-deep);
  border-color: var(--color-brand-deep);
}

.hero__note {
  margin: -0.25rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

.hero__visual {
  position: relative;
  min-height: 24rem;
  margin: 0;
  background: var(--color-surface-muted);
}

.hero__visual picture,
.hero__visual img {
  width: 100%;
  height: 100%;
  display: block;
}

.hero__visual img {
  object-fit: cover;
  object-position: 68% center;
}

.hero__visual figcaption {
  position: absolute;
  right: var(--space-md);
  bottom: var(--space-md);
  padding: 0.45rem 0.75rem;
  color: var(--color-brand-deep);
  background: var(--color-canvas);
  border-radius: var(--radius-pill);
  font-size: 0.75rem;
  font-weight: 650;
}

@media (min-width: 48rem) {
  .hero {
    grid-template-columns: minmax(0, 46%) minmax(0, 54%);
    min-height: min(49rem, calc(100svh - 5.25rem));
  }

  .hero__content {
    padding: clamp(3rem, 5vw, 5.5rem) clamp(1.5rem, 4vw, 4.5rem);
  }

  .hero h1 {
    font-size: clamp(3rem, 5vw, 4.75rem);
  }

  .hero__visual {
    min-height: 100%;
  }
}

@media (hover: hover) {
  .button--primary:hover {
    background: var(--color-brand);
  }

  .button--outline:hover {
    color: var(--color-surface);
    background: var(--color-brand-deep);
  }
}

@media (prefers-reduced-motion: reduce) {
  .button {
    transition-duration: 0.01ms;
  }
}
</style>
