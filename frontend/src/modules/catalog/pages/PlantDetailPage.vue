<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { useCartStore } from '@/modules/trade/stores/cart'
import { fetchArticles } from '@/modules/knowledge/api/knowledge'
import type { Article } from '@/modules/knowledge/types/knowledge'
import { ApiError } from '@/shared/api/request'
import { ErrorCode } from '@/shared/api/types'

import { fetchPlantBySlug } from '../api/catalog'
import type { CatalogPlant, CatalogSku } from '../types/catalog'

const props = defineProps<{
  plantId?: string
}>()

const route = useRoute()
const cart = useCartStore()
const added = ref(false)
const adding = ref(false)
let feedbackTimer: number | undefined

const plant = ref<CatalogPlant | null>(null)
const loading = ref(true)
const notFound = ref(false)
const errorMessage = ref('')

/** 与这个品种相关的知识文章，方案要求可从商品详情跳到对应指南 */
const guides = ref<Article[]>([])

/**
 * 选中的 SKU。默认取 defaultSkuId，没有则取第一个。
 *
 * 改造前加购按 slug 提交，同一株植物有多个规格时根本表达不了"买的是哪个"。
 * 现在详情页能切换规格，加购提交的是 skuId，价格与库存也都跟着选中的规格走。
 */
const selectedSkuId = ref<number | null>(null)

const currentPlantId = computed(() => props.plantId ?? String(route.params.plantId ?? ''))

const skus = computed<CatalogSku[]>(() => plant.value?.skus ?? [])

const selectedSku = computed<CatalogSku | null>(() =>
  skus.value.find((sku) => sku.id === selectedSkuId.value) ?? skus.value[0] ?? null,
)

const outOfStock = computed(() => (selectedSku.value?.stock ?? 0) <= 0)

async function loadPlant() {
  const slug = currentPlantId.value
  if (!slug) {
    loading.value = false
    notFound.value = true
    return
  }

  loading.value = true
  notFound.value = false
  errorMessage.value = ''

  try {
    plant.value = await fetchPlantBySlug(slug)
    // 切换植物时重置规格选择，防止残留上一株的选中态
    selectedSkuId.value = plant.value?.defaultSkuId ?? null
  } catch (error) {
    plant.value = null
    if (error instanceof ApiError && error.code === ErrorCode.PLANT_NOT_FOUND) {
      // 确实没有这株植物，走"未找到"文案而不是报错
      notFound.value = true
    } else {
      errorMessage.value = error instanceof ApiError ? error.message : '植物详情加载失败，请稍后重试。'
    }
  } finally {
    loading.value = false
  }

  // 相关指南与主内容并行加载失败也不影响看商品，所以单独 try 住
  guides.value = []
  if (plant.value) {
    try {
      const result = await fetchArticles({ speciesCode: plant.value.slug }, 1, 3)
      guides.value = result.records
    } catch {
      // 指南加载失败静默：商品详情本身不依赖它
    }
  }
}

// 在两株植物的详情页之间跳转时组件不会重新挂载，只有路由参数在变
watch(currentPlantId, loadPlant, { immediate: true })

function selectSku(sku: CatalogSku) {
  selectedSkuId.value = sku.id
}

async function addToCart() {
  if (!plant.value || !selectedSku.value) return

  adding.value = true
  const ok = await cart.add(selectedSku.value.id, 1)
  adding.value = false
  if (!ok) {
    errorMessage.value = cart.errorMessage
    return
  }

  added.value = true
  if (feedbackTimer) window.clearTimeout(feedbackTimer)
  feedbackTimer = window.setTimeout(() => {
    added.value = false
  }, 2200)
}

onBeforeUnmount(() => {
  if (feedbackTimer) window.clearTimeout(feedbackTimer)
})
</script>

<template>
  <div class="detail-page">
    <template v-if="plant">
      <nav class="container detail-breadcrumb" aria-label="面包屑导航">
        <RouterLink :to="{ name: 'plant-catalog' }">植物商店</RouterLink>
        <span aria-hidden="true">/</span>
        <span aria-current="page">{{ plant.name }}</span>
      </nav>

      <section class="detail-hero" aria-labelledby="plant-title">
        <div class="container detail-hero__grid">
          <div class="detail-hero__image-wrap">
            <img
              class="detail-hero__image"
              :src="plant.image"
              :alt="plant.imageAlt"
              width="900"
              height="960"
            />
            <span class="detail-hero__difficulty">{{ plant.difficulty }}</span>
          </div>

          <div class="detail-hero__content">
            <p class="detail-hero__eyebrow">{{ plant.category }}</p>
            <h1 id="plant-title">{{ plant.name }}</h1>
            <p class="detail-hero__latin">{{ plant.latinName }}</p>
            <p class="detail-hero__intro">{{ plant.shortDescription }}</p>

            <ul class="detail-tags" aria-label="适配标签">
              <li v-for="tag in plant.matchTags" :key="tag">{{ tag }}</li>
            </ul>

            <div class="detail-reason">
              <p>为什么推荐它</p>
              <strong>{{ plant.recommendationReason }}</strong>
            </div>

            <!-- 规格选择器：同一株植物可能有多个尺寸与价格 -->
            <fieldset v-if="skus.length > 1" class="detail-skus">
              <legend>选择规格</legend>
              <div class="detail-skus__options">
                <button
                  v-for="sku in skus"
                  :key="sku.id"
                  type="button"
                  :class="{ active: selectedSku?.id === sku.id }"
                  :disabled="sku.stock <= 0"
                  :aria-pressed="selectedSku?.id === sku.id"
                  @click="selectSku(sku)"
                >
                  <span>{{ sku.spec }}</span>
                  <small v-if="sku.stock > 0">¥{{ sku.price }} · 库存 {{ sku.stock }}</small>
                  <small v-else>已售罄</small>
                </button>
              </div>
            </fieldset>

            <div class="detail-purchase">
              <p class="detail-price"><span>¥</span>{{ selectedSku?.price ?? plant.price }}</p>
              <button
                class="detail-purchase__button"
                type="button"
                :disabled="outOfStock || adding"
                @click="addToCart"
              >
                {{ outOfStock ? '暂时缺货' : adding ? '正在加入…' : added ? '已加入购物车' : '加入购物车' }}
              </button>
            </div>
            <p class="detail-feedback" aria-live="polite">
              {{ added ? `${plant.name}已加入购物车` : outOfStock ? '这件规格暂时缺货，可以看看其他规格。' : '' }}
            </p>
            <p v-if="errorMessage" class="detail-error" role="alert">{{ errorMessage }}</p>
          </div>
        </div>
      </section>

      <section class="container detail-profile" aria-labelledby="profile-title">
        <div class="detail-profile__heading">
          <p>植物档案</p>
          <h2 id="profile-title">带回家之前，先了解它的日常</h2>
        </div>

        <dl class="detail-facts">
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
            <dt>宠物家庭</dt>
            <dd>{{ plant.petFriendly ? '可以安心相处' : plant.petNote }}</dd>
          </div>
        </dl>
      </section>

      <section class="detail-story">
        <div class="container detail-story__grid">
          <div>
            <p class="detail-story__eyebrow">关于这株植物</p>
            <h2>让绿意自然融入生活</h2>
            <p>{{ plant.description }}</p>
          </div>

          <div class="detail-care">
            <h2>养护备忘</h2>
            <ol>
              <li v-for="(tip, index) in plant.careTips" :key="tip">
                <span aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span>
                <p>{{ tip }}</p>
              </li>
            </ol>

            <!-- 方案要求可从商品详情跳到对应指南。按品种关联查出来的，不是写死的 -->
            <div v-if="guides.length" class="detail-guides">
              <h3>想了解更多</h3>
              <ul>
                <li v-for="guide in guides" :key="guide.id">
                  <RouterLink :to="{ name: 'knowledge-detail', params: { slug: guide.slug } }">
                    {{ guide.title }}
                    <small>{{ guide.categoryLabel }}</small>
                  </RouterLink>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </section>
    </template>

    <section v-else-if="notFound" class="container detail-missing" role="status">
      <p>植物档案暂未找到</p>
      <h1>这株植物可能已经移栽到别处了</h1>
      <RouterLink class="pill-button" :to="{ name: 'plant-catalog' }">返回植物商店</RouterLink>
    </section>

    <section v-else-if="errorMessage" class="container detail-missing" role="alert">
      <p>加载失败</p>
      <h1>没能取到这株植物的档案</h1>
      <p class="detail-missing__reason">{{ errorMessage }}</p>
      <button class="pill-button" type="button" @click="loadPlant">重试</button>
    </section>

    <section v-else class="container detail-missing" role="status" aria-busy="true">
      <p>正在加载</p>
      <h1>正在取出这株植物的档案…</h1>
    </section>
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 100%;
  color: var(--color-text);
  background: var(--color-canvas);
}

.detail-breadcrumb {
  display: flex;
  gap: var(--space-xs);
  align-items: center;
  padding-top: var(--space-md);
  padding-bottom: var(--space-md);
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.detail-breadcrumb a {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  color: var(--color-brand-accent);
  font-weight: 700;
  text-underline-offset: 0.2em;
}

.detail-breadcrumb a:focus-visible,
.detail-missing a:focus-visible {
  outline: 3px solid var(--color-brand-accent);
  outline-offset: 3px;
}

.detail-hero {
  padding-bottom: var(--space-2xl);
}

.detail-hero__grid {
  display: grid;
  gap: clamp(2rem, 6vw, 5rem);
  align-items: center;
}

.detail-hero__image-wrap {
  position: relative;
  overflow: hidden;
  background: var(--color-surface-muted);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  aspect-ratio: 5 / 6;
}

.detail-hero__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-hero__difficulty {
  position: absolute;
  right: var(--space-md);
  bottom: var(--space-md);
  padding: 0.45rem 0.8rem;
  color: var(--color-brand-deep);
  font-size: 0.8rem;
  font-weight: 750;
  background: var(--color-surface);
  border-radius: var(--radius-pill);
  box-shadow: var(--shadow-card);
}

.detail-hero__eyebrow,
.detail-hero__latin,
.detail-hero__intro,
.detail-price,
.detail-feedback,
.detail-reason p,
.detail-reason strong,
.detail-profile__heading p,
.detail-story__eyebrow,
.detail-story p {
  margin: 0;
}

.detail-hero__eyebrow,
.detail-profile__heading p,
.detail-story__eyebrow {
  color: var(--color-brand);
  font-size: 0.8rem;
  font-weight: 750;
  letter-spacing: 0.1em;
}

.detail-hero h1 {
  margin: var(--space-xs) 0 0;
  color: var(--color-ink);
  font-size: clamp(2.5rem, 7vw, 5rem);
  line-height: 1.05;
  letter-spacing: -0.045em;
}

.detail-hero__latin {
  margin-top: var(--space-xs);
  color: var(--color-text-muted);
  font-size: 1rem;
  font-style: italic;
}

.detail-hero__intro {
  max-width: 35rem;
  margin-top: var(--space-lg);
  color: var(--color-text);
  font-size: clamp(1.05rem, 2vw, 1.25rem);
  line-height: 1.75;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
  padding: 0;
  margin: var(--space-md) 0 0;
  list-style: none;
}

.detail-tags li {
  padding: 0.4rem 0.75rem;
  color: var(--color-brand-deep);
  font-size: 0.82rem;
  font-weight: 700;
  background: var(--color-brand-soft);
  border-radius: var(--radius-pill);
}

.detail-reason {
  padding: var(--space-md);
  margin-top: var(--space-lg);
  background: var(--color-brand-soft);
  border: 1px solid color-mix(in oklch, var(--color-brand) 24%, var(--color-border));
  border-radius: var(--radius-card);
}

.detail-reason p {
  margin-bottom: 0.3rem;
  color: var(--color-brand);
  font-size: 0.78rem;
  font-weight: 750;
}

.detail-reason strong {
  display: block;
  max-width: 38rem;
  color: var(--color-text);
  font-size: 1rem;
  font-weight: 550;
  line-height: 1.7;
}

.detail-purchase {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-md);
  align-items: center;
  margin-top: var(--space-lg);
}

/* 规格选择器：同一株植物可能有多个尺寸与价格 */
.detail-skus {
  margin: var(--space-lg) 0 0;
  padding: 0;
  border: 0;
}

.detail-skus legend {
  margin-bottom: var(--space-sm);
  font-size: 0.85rem;
  font-weight: 700;
}

.detail-skus__options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
  gap: var(--space-sm);
}

.detail-skus__options button {
  display: grid;
  gap: 0.2rem;
  padding: var(--space-sm) var(--space-md);
  text-align: left;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  cursor: pointer;
}

.detail-skus__options button.active {
  border-color: var(--color-brand);
  outline: 2px solid color-mix(in srgb, var(--color-brand) 25%, transparent);
}

.detail-skus__options button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.detail-skus__options span {
  font-size: 0.85rem;
  font-weight: 700;
}

.detail-skus__options small {
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.detail-error {
  margin: var(--space-sm) 0 0;
  color: var(--color-danger);
  font-size: 0.82rem;
}

.detail-price {
  min-width: 6rem;
  color: var(--color-ink);
  font-size: 2rem;
  font-weight: 800;
}

.detail-price span {
  margin-right: 0.15rem;
  font-size: 0.58em;
}

.detail-purchase__button {
  min-width: 10rem;
  min-height: 3.25rem;
  padding: 0 var(--space-lg);
  color: var(--color-surface);
  font: inherit;
  font-weight: 750;
  cursor: pointer;
  background: var(--color-brand-accent);
  border: 1px solid var(--color-brand-accent);
  border-radius: var(--radius-pill);
  transition:
    transform 180ms var(--ease-out),
    background-color 180ms var(--ease-out);
}

.detail-purchase__button:hover {
  background: var(--color-brand);
}

.detail-purchase__button:active {
  transform: scale(0.95);
}

.detail-purchase__button:focus-visible {
  outline: 3px solid var(--color-brand-accent);
  outline-offset: 3px;
}

.detail-feedback {
  min-height: 1.5em;
  margin-top: var(--space-xs);
  color: var(--color-brand);
  font-size: 0.85rem;
  font-weight: 700;
}

.detail-profile {
  padding-top: var(--space-xl);
  padding-bottom: var(--space-xl);
  border-top: 1px solid var(--color-border);
}

.detail-profile__heading h2,
.detail-story h2 {
  max-width: 18ch;
  margin: var(--space-xs) 0 0;
  color: var(--color-ink);
  font-size: clamp(1.65rem, 4vw, 2.5rem);
  line-height: 1.2;
}

.detail-facts {
  display: grid;
  gap: var(--space-sm);
  margin: var(--space-lg) 0 0;
}

.detail-facts div {
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--color-border);
}

.detail-facts dt {
  margin-bottom: var(--space-xs);
  color: var(--color-text-muted);
  font-size: 0.78rem;
  font-weight: 700;
}

.detail-facts dd {
  margin: 0;
  color: var(--color-ink);
  font-size: 1rem;
  font-weight: 650;
}

.detail-story {
  padding: var(--space-2xl) 0;
  color: var(--color-surface);
  background: var(--color-brand-deep);
}

.detail-story__grid {
  display: grid;
  gap: clamp(3rem, 8vw, 7rem);
}

.detail-story__eyebrow {
  color: color-mix(in oklch, var(--color-on-brand) 70%, transparent);
}

.detail-story h2 {
  color: var(--color-surface);
}

.detail-story__grid > div:first-child > p:last-child {
  max-width: 38rem;
  margin-top: var(--space-lg);
  color: color-mix(in oklch, var(--color-on-brand) 76%, transparent);
  line-height: 1.8;
}

.detail-care h2 {
  margin: 0 0 var(--space-lg);
  font-size: 1.4rem;
}

.detail-care ol {
  display: grid;
  gap: var(--space-md);
  padding: 0;
  margin: 0;
  list-style: none;
}

.detail-care li {
  display: grid;
  grid-template-columns: 2rem 1fr;
  gap: var(--space-sm);
  align-items: start;
}

.detail-care li span {
  color: color-mix(in oklch, var(--color-on-brand) 54%, transparent);
  font-size: 0.8rem;
  font-weight: 750;
}

.detail-care li p {
  color: color-mix(in oklch, var(--color-on-brand) 84%, transparent);
  line-height: 1.65;
}

.detail-guides {
  margin-top: var(--space-lg);
  padding-top: var(--space-md);
  border-top: 1px dashed color-mix(in oklch, var(--color-on-brand) 28%, transparent);
}

.detail-guides h3 {
  margin: 0 0 var(--space-sm);
  color: var(--color-on-brand);
  font-size: 0.88rem;
}

.detail-guides ul {
  display: grid;
  gap: 0.45rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.detail-guides a {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-sm);
  color: var(--color-on-brand);
  font-size: 0.82rem;
  font-weight: 700;
  text-decoration: none;
}

.detail-guides a:hover {
  text-decoration: underline;
}

.detail-guides small {
  color: color-mix(in oklch, var(--color-on-brand) 65%, transparent);
  font-size: 0.68rem;
  font-weight: 400;
}

.detail-missing {
  display: grid;
  min-height: 65vh;
  place-content: center;
  justify-items: start;
  padding-top: var(--space-2xl);
  padding-bottom: var(--space-2xl);
}

.detail-missing p {
  margin: 0;
  color: var(--color-brand);
  font-weight: 750;
}

.detail-missing h1 {
  max-width: 16ch;
  margin: var(--space-sm) 0 var(--space-lg);
  color: var(--color-ink);
  font-size: clamp(2rem, 6vw, 4rem);
}

/* 具体错误原因是正文，不该套用上面那条 eyebrow 的品牌色 */
.detail-missing__reason {
  max-width: 42ch;
  margin: calc(var(--space-lg) * -1) 0 var(--space-lg) !important;
  color: var(--color-text-muted) !important;
  font-weight: 500 !important;
  line-height: 1.7;
}

.detail-missing button {
  font: inherit;
  cursor: pointer;
}

.detail-missing a {
  text-decoration: none;
}

@media (min-width: 48rem) {
  .detail-hero__grid {
    grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  }

  .detail-facts {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .detail-facts div {
    padding: var(--space-md);
    border-right: 1px solid var(--color-border);
    border-bottom: 0;
  }

  .detail-facts div:first-child {
    padding-left: 0;
  }

  .detail-facts div:last-child {
    border-right: 0;
  }

  .detail-story__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 32rem) {
  .detail-purchase {
    align-items: stretch;
    flex-direction: column;
  }

  .detail-purchase__button {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .detail-purchase__button {
    transition: none;
  }

  .detail-purchase__button:active {
    transform: none;
  }
}
</style>
