<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'

import PlantCard from '@/modules/catalog/components/PlantCard.vue'
import { fetchPlantPage } from '@/modules/catalog/api/catalog'
import type { CatalogPlant } from '@/modules/catalog/types/catalog'

const featuredPlants = ref<CatalogPlant[]>([])

onMounted(async () => {
  try {
    const result = await fetchPlantPage({ featured: true, size: 3 })
    featuredPlants.value = result.records
  } catch {
    // 首页的次要版块，失败就整块不渲染。
    // 在 hero 下面挂一条错误横幅，比少一个推荐区更打扰人。
    featuredPlants.value = []
  }
})
</script>

<template>
  <section
    v-if="featuredPlants.length"
    id="featured-plants"
    class="featured section-shell"
    aria-labelledby="featured-title"
  >
    <div class="container">
      <header class="featured__header">
        <div>
          <p class="section-eyebrow">为当前档案筛选</p>
          <h2 id="featured-title">为你的光照、空间和养护习惯而选</h2>
        </div>
        <RouterLink class="featured__all" to="/plants">
          查看全部推荐
          <el-icon aria-hidden="true"><ArrowRight /></el-icon>
        </RouterLink>
      </header>

      <div class="featured__grid">
        <PlantCard
          v-for="(plant, index) in featuredPlants"
          :key="plant.id"
          :plant="plant"
          :featured="index === 0"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.featured {
  padding-block: clamp(3.5rem, 8vw, 6.5rem);
}

.featured__header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
}

.featured__header h2 {
  max-width: 24ch;
  margin: 0;
  color: var(--color-brand-deep);
  font-size: clamp(1.7rem, 3vw, 2.5rem);
  line-height: 1.25;
  text-wrap: balance;
}

.featured__all {
  min-height: 2.75rem;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 0.35rem;
  color: var(--color-brand);
  font-weight: 700;
  text-decoration: none;
}

.featured__grid {
  display: grid;
  gap: var(--space-md);
}

@media (min-width: 48rem) {
  .featured__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .featured__grid > :first-child {
    grid-column: 1 / -1;
  }
}

@media (min-width: 72rem) {
  .featured__grid {
    grid-template-columns: minmax(0, 1.35fr) repeat(2, minmax(0, 0.92fr));
  }

  .featured__grid > :first-child {
    grid-column: auto;
  }
}

@media (max-width: 39.99rem) {
  .featured__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
