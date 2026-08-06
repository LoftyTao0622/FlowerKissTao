<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { ApiError } from '@/shared/api/request'

import PlantCard from '../components/PlantCard.vue'
import { fetchPlantFacets, fetchPlantPage } from '../api/catalog'
import type { CatalogPlant, PlantFacets } from '../types/catalog'

const route = useRoute()
const router = useRouter()

const keywordFromRoute = computed(() => {
  const value = route.query.q ?? route.query.keyword
  return Array.isArray(value) ? (value[0] ?? '') : (value ?? '')
})

const searchInput = ref(keywordFromRoute.value)
const selectedCategory = ref<string>('全部')
const selectedLight = ref<string>('全部')

const plants = ref<CatalogPlant[]>([])
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const facets = ref<PlantFacets>({ categories: [], lights: [] })

const categories = computed(() => facets.value.categories)
const lightOptions = computed(() => facets.value.lights)

const hasActiveFilters = computed(
  () => keywordFromRoute.value.trim() !== '' || selectedCategory.value !== '全部' || selectedLight.value !== '全部',
)

function describeError(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
}

async function loadPlants() {
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await fetchPlantPage({
      current: 1,
      size: 60,
      keyword: keywordFromRoute.value.trim() || undefined,
      category: selectedCategory.value === '全部' ? undefined : selectedCategory.value,
      light: selectedLight.value === '全部' ? undefined : selectedLight.value,
    })
    plants.value = result.records
    total.value = result.total
  } catch (error) {
    plants.value = []
    total.value = 0
    errorMessage.value = describeError(error, '植物列表加载失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

async function loadFacets() {
  try {
    facets.value = await fetchPlantFacets()
  } catch {
    // 筛选项拿不到不影响浏览，降级成只按关键词搜
    facets.value = { categories: [], lights: [] }
  }
}

watch(keywordFromRoute, (value) => {
  searchInput.value = value
})

watch([keywordFromRoute, selectedCategory, selectedLight], () => {
  void loadPlants()
})

onMounted(() => {
  void loadFacets()
  void loadPlants()
})

function submitSearch() {
  const nextQuery = { ...route.query }
  delete nextQuery.keyword

  const value = searchInput.value.trim()
  if (value) {
    nextQuery.q = value
  } else {
    delete nextQuery.q
  }

  void router.replace({ query: nextQuery })
}

function resetFilters() {
  searchInput.value = ''
  selectedCategory.value = '全部'
  selectedLight.value = '全部'

  const nextQuery = { ...route.query }
  delete nextQuery.q
  delete nextQuery.keyword
  void router.replace({ query: nextQuery })
}
</script>

<template>
  <div class="catalog-page">
    <header class="catalog-hero">
      <div class="container catalog-hero__inner">
        <p class="catalog-hero__eyebrow">为你的空间挑一株恰到好处的绿意</p>
        <h1>找到真正适合你的植物</h1>
        <p>
          从光照、养护节奏和空间尺度出发筛选。每株植物都附有清晰的适配标签与推荐理由，让选择更从容。
        </p>
      </div>
    </header>

    <section class="container catalog-content" aria-labelledby="catalog-filter-title">
      <form class="catalog-filter" role="search" @submit.prevent="submitSearch">
        <h2 id="catalog-filter-title">筛选植物</h2>

        <div class="catalog-filter__fields">
          <label class="catalog-filter__search">
            <span>关键词</span>
            <span class="catalog-filter__search-row">
              <input
                v-model="searchInput"
                name="plant-search"
                type="search"
                placeholder="搜索名称、场景或特点"
                autocomplete="off"
              />
              <button type="submit">搜索</button>
            </span>
          </label>

          <label>
            <span>植物分类</span>
            <select v-model="selectedCategory" name="category">
              <option value="全部">全部分类</option>
              <option v-for="category in categories" :key="category" :value="category">
                {{ category }}
              </option>
            </select>
          </label>

          <label>
            <span>光照条件</span>
            <select v-model="selectedLight" name="light">
              <option value="全部">全部光照</option>
              <option v-for="light in lightOptions" :key="light" :value="light">
                {{ light }}
              </option>
            </select>
          </label>
        </div>

        <button v-if="hasActiveFilters" class="catalog-filter__reset" type="button" @click="resetFilters">
          清除全部条件
        </button>
      </form>

      <div class="catalog-results__heading">
        <div>
          <p class="catalog-results__count" aria-live="polite">
            {{ loading ? '正在加载植物…' : `找到 ${total} 株植物` }}
          </p>
          <p v-if="keywordFromRoute" class="catalog-results__keyword">关键词“{{ keywordFromRoute }}”</p>
        </div>
        <p>价格为含基础花盆的参考价</p>
      </div>

      <div v-if="loading" class="catalog-grid" aria-busy="true">
        <div v-for="placeholder in 6" :key="placeholder" class="catalog-skeleton" aria-hidden="true">
          <div class="catalog-skeleton__image"></div>
          <div class="catalog-skeleton__line catalog-skeleton__line--short"></div>
          <div class="catalog-skeleton__line"></div>
          <div class="catalog-skeleton__line catalog-skeleton__line--short"></div>
        </div>
      </div>

      <div v-else-if="errorMessage" class="catalog-empty" role="alert">
        <span class="catalog-empty__mark" aria-hidden="true">!</span>
        <h2>没能加载到植物列表</h2>
        <p>{{ errorMessage }}</p>
        <button class="pill-button" type="button" @click="loadPlants">重试</button>
      </div>

      <div v-else-if="plants.length" class="catalog-grid">
        <PlantCard v-for="plant in plants" :key="plant.id" :plant="plant" />
      </div>

      <div v-else class="catalog-empty" role="status">
        <span class="catalog-empty__mark" aria-hidden="true">叶</span>
        <h2>暂时没有匹配的植物</h2>
        <p>试试减少筛选条件，或搜索“新手”“书桌”“低频浇水”等场景词。</p>
        <button class="pill-button" type="button" @click="resetFilters">查看全部植物</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.catalog-page {
  min-height: 100%;
  background: var(--color-canvas);
}

.catalog-hero {
  padding: clamp(3.5rem, 8vw, 6.5rem) 0;
  color: var(--color-surface);
  background: var(--color-brand-deep);
}

.catalog-hero__inner {
  max-width: 52rem;
}

.catalog-hero__eyebrow {
  margin: 0 0 var(--space-sm);
  color: color-mix(in oklch, var(--color-on-brand) 76%, transparent);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.catalog-hero h1 {
  max-width: 12ch;
  margin: 0;
  font-size: clamp(2.25rem, 6vw, 4.5rem);
  line-height: 1.08;
  letter-spacing: -0.035em;
}

.catalog-hero p:last-child {
  max-width: 43rem;
  margin: var(--space-lg) 0 0;
  color: color-mix(in oklch, var(--color-on-brand) 78%, transparent);
  font-size: clamp(1rem, 2vw, 1.2rem);
  line-height: 1.75;
}

.catalog-content {
  padding-top: var(--space-xl);
  padding-bottom: var(--space-2xl);
}

.catalog-filter {
  padding: clamp(1.25rem, 4vw, 2rem);
  background: var(--color-surface);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.catalog-filter h2 {
  margin: 0 0 var(--space-md);
  color: var(--color-brand);
  font-size: 1.3rem;
}

.catalog-filter__fields {
  display: grid;
  gap: var(--space-md);
}

.catalog-filter label {
  display: grid;
  gap: var(--space-xs);
  color: var(--color-text);
  font-size: 0.82rem;
  font-weight: 700;
}

.catalog-filter input,
.catalog-filter select {
  width: 100%;
  min-height: 3rem;
  padding: 0 0.9rem;
  color: var(--color-ink);
  font: inherit;
  font-weight: 500;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 0.55rem;
}

.catalog-filter input:focus-visible,
.catalog-filter select:focus-visible,
.catalog-filter button:focus-visible,
.catalog-empty button:focus-visible {
  outline: 3px solid var(--color-brand-accent);
  outline-offset: 2px;
}

.catalog-filter__search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
}

.catalog-filter__search-row input {
  border-radius: 0.55rem 0 0 0.55rem;
}

.catalog-filter__search-row button {
  min-width: 4.5rem;
  min-height: 3rem;
  padding: 0 var(--space-md);
  color: var(--color-surface);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  background: var(--color-brand-accent);
  border: 1px solid var(--color-brand-accent);
  border-radius: 0 var(--radius-pill) var(--radius-pill) 0;
}

.catalog-filter__reset {
  min-height: 2.75rem;
  padding: 0;
  margin-top: var(--space-md);
  color: var(--color-brand-accent);
  font: inherit;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.catalog-results__heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-xl) 0 var(--space-md);
}

.catalog-results__heading p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.catalog-results__count {
  color: var(--color-ink) !important;
  font-size: 1.15rem !important;
  font-weight: 750;
}

.catalog-results__keyword {
  margin-top: 0.25rem !important;
}

.catalog-grid {
  display: grid;
  gap: var(--space-lg);
}

.catalog-skeleton {
  display: grid;
  gap: var(--space-sm);
  padding: var(--space-md);
  background: var(--color-surface);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.catalog-skeleton__image {
  aspect-ratio: 8 / 9;
  background: var(--color-brand-soft);
  border-radius: 0.55rem;
}

.catalog-skeleton__line {
  height: 0.85rem;
  background: var(--color-brand-soft);
  border-radius: var(--radius-pill);
}

.catalog-skeleton__line--short {
  width: 45%;
}

.catalog-skeleton__image,
.catalog-skeleton__line {
  animation: catalog-skeleton-pulse 1.4s var(--ease-out) infinite;
}

@keyframes catalog-skeleton-pulse {
  50% {
    opacity: 0.45;
  }
}

.catalog-empty {
  display: grid;
  justify-items: center;
  max-width: 38rem;
  padding: clamp(3rem, 10vw, 6rem) var(--space-md);
  margin: 0 auto;
  text-align: center;
}

.catalog-empty__mark {
  display: grid;
  width: 3.5rem;
  height: 3.5rem;
  place-items: center;
  color: var(--color-brand-deep);
  font-size: 0.9rem;
  font-weight: 800;
  background: var(--color-brand-soft);
  border-radius: 50%;
}

.catalog-empty h2 {
  margin: var(--space-md) 0 var(--space-sm);
  color: var(--color-ink);
}

.catalog-empty p {
  margin: 0 0 var(--space-lg);
  color: var(--color-text-muted);
  line-height: 1.7;
}

@media (min-width: 44rem) {
  .catalog-filter__fields {
    grid-template-columns: minmax(16rem, 1.7fr) minmax(10rem, 1fr) minmax(10rem, 1fr);
    align-items: end;
  }

  .catalog-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 72rem) {
  .catalog-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 36rem) {
  .catalog-results__heading {
    align-items: start;
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .catalog-filter__search-row button:active,
  .catalog-empty button:active {
    transform: none;
  }

  .catalog-skeleton__image,
  .catalog-skeleton__line {
    animation: none;
  }
}
</style>
