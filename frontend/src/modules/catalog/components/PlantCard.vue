<script setup lang="ts">
import type { CatalogPlant } from '../types/catalog'

withDefaults(
  defineProps<{
    plant: CatalogPlant
    featured?: boolean
  }>(),
  {
    featured: false,
  },
)
</script>

<template>
  <article class="plant-card" :class="{ 'plant-card--featured': featured }">
    <RouterLink
      class="plant-card__image-link"
      :to="{ name: 'plant-detail', params: { plantId: plant.slug } }"
      :aria-label="`查看${plant.name}详情`"
    >
      <img
        class="plant-card__image"
        :src="plant.image"
        :alt="plant.imageAlt"
        loading="lazy"
        width="640"
        height="720"
      />
      <span v-if="featured" class="plant-card__featured-label">本周精选</span>
    </RouterLink>

    <div class="plant-card__content">
      <div>
        <p class="plant-card__eyebrow">{{ plant.category }} · {{ plant.difficulty }}</p>
        <h3 class="plant-card__title">{{ plant.name }}</h3>
        <p class="plant-card__latin">{{ plant.latinName }}</p>
      </div>

      <ul class="plant-card__tags" aria-label="适配标签">
        <li v-for="tag in plant.matchTags" :key="tag">{{ tag }}</li>
      </ul>

      <dl class="plant-card__facts">
        <div>
          <dt>光照</dt>
          <dd>{{ plant.light }}</dd>
        </div>
        <div>
          <dt>浇水</dt>
          <dd>{{ plant.watering }}</dd>
        </div>
        <div>
          <dt>株型</dt>
          <dd>{{ plant.size }}</dd>
        </div>
        <div>
          <dt>宠物</dt>
          <dd>{{ plant.petFriendly ? '宠物友好' : plant.petNote }}</dd>
        </div>
      </dl>

      <p class="plant-card__reason">
        <span>推荐理由</span>
        {{ plant.recommendationReason }}
      </p>

      <footer class="plant-card__footer">
        <p class="plant-card__price">
          <span aria-hidden="true">
            <span class="plant-card__price-symbol">¥</span>{{ plant.price }}
          </span>
          <span class="sr-only">人民币{{ plant.price }}元</span>
        </p>
        <RouterLink
          class="plant-card__action"
          :to="{ name: 'plant-detail', params: { plantId: plant.slug } }"
        >
          了解这株植物
          <span aria-hidden="true">→</span>
        </RouterLink>
      </footer>
    </div>
  </article>
</template>

<style scoped>
.plant-card {
  display: grid;
  min-width: 0;
  overflow: hidden;
  background: var(--color-surface);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.plant-card__image-link {
  position: relative;
  display: block;
  overflow: hidden;
  background: var(--color-surface-muted);
  aspect-ratio: 5 / 4;
}

.plant-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 300ms var(--ease-out);
}

.plant-card__image-link:hover .plant-card__image {
  transform: scale(1.025);
}

.plant-card__image-link:focus-visible {
  outline: 3px solid var(--color-brand-accent);
  outline-offset: -3px;
}

.plant-card__featured-label {
  position: absolute;
  top: var(--space-sm);
  left: var(--space-sm);
  padding: 0.35rem 0.75rem;
  color: var(--color-surface);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  background: var(--color-brand-deep);
  border-radius: var(--radius-pill);
}

.plant-card__content {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  padding: clamp(1.1rem, 3vw, 1.5rem);
}

.plant-card__eyebrow,
.plant-card__latin,
.plant-card__price,
.plant-card__reason {
  margin: 0;
}

.plant-card__eyebrow {
  color: var(--color-brand);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.plant-card__title {
  margin: 0.3rem 0 0;
  color: var(--color-ink);
  font-size: clamp(1.35rem, 2vw, 1.65rem);
  line-height: 1.2;
}

.plant-card__action:focus-visible {
  outline: 3px solid var(--color-brand-accent);
  outline-offset: 3px;
  border-radius: 0.2rem;
}

.plant-card__latin {
  margin-top: 0.25rem;
  color: var(--color-text-muted);
  font-size: 0.88rem;
  font-style: italic;
}

.plant-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
  padding: 0;
  margin: 0;
  list-style: none;
}

.plant-card__tags li {
  padding: 0.35rem 0.65rem;
  color: var(--color-brand-deep);
  font-size: 0.78rem;
  font-weight: 650;
  background: var(--color-brand-soft);
  border-radius: var(--radius-pill);
}

.plant-card__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-sm) var(--space-md);
  margin: 0;
}

.plant-card__facts div {
  min-width: 0;
}

.plant-card__facts dt {
  margin-bottom: 0.15rem;
  color: var(--color-text-muted);
  font-size: 0.72rem;
}

.plant-card__facts dd {
  margin: 0;
  color: var(--color-text);
  font-size: 0.86rem;
  line-height: 1.45;
}

.plant-card__reason {
  color: var(--color-text);
  font-size: 0.9rem;
  line-height: 1.65;
}

.plant-card__reason span {
  display: block;
  margin-bottom: 0.2rem;
  color: var(--color-brand);
  font-size: 0.75rem;
  font-weight: 700;
}

.plant-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  padding-top: var(--space-sm);
  margin-top: auto;
  border-top: 1px solid var(--color-border);
}

.plant-card__price {
  color: var(--color-ink);
  font-size: 1.3rem;
  font-weight: 750;
}

.plant-card__price-symbol {
  margin-right: 0.1rem;
  font-size: 0.8em;
}

.plant-card__action {
  display: inline-flex;
  gap: 0.4rem;
  align-items: center;
  min-height: 2.75rem;
  color: var(--color-brand-accent);
  font-size: 0.88rem;
  font-weight: 700;
  text-decoration: none;
}

@media (min-width: 52rem) {
  .plant-card--featured {
    grid-template-columns: minmax(15rem, 0.82fr) minmax(18rem, 1fr);
  }

  .plant-card--featured .plant-card__image-link {
    height: 100%;
    min-height: 30rem;
    aspect-ratio: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .plant-card__image {
    transition: none;
  }

  .plant-card__image-link:hover .plant-card__image {
    transform: none;
  }
}
</style>
