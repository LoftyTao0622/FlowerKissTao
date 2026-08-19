<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { useKnowledgeStore } from '../stores/knowledge'

const store = useKnowledgeStore()
const route = useRoute()
const keywordInput = ref('')

function search() {
  void store.applyFilters({ keyword: keywordInput.value.trim() || undefined })
}

function pickCategory(value?: string) {
  void store.applyFilters({ category: store.query.category === value ? undefined : value })
}

function pickDifficulty(value?: number) {
  void store.applyFilters({ difficulty: store.query.difficulty === value ? undefined : value })
}

function pickSeason(value?: string) {
  void store.applyFilters({ season: store.query.season === value ? undefined : value })
}

function pickTag(value: string) {
  void store.applyFilters({ tag: store.query.tag === value ? undefined : value })
}

function resetAll() {
  keywordInput.value = ''
  void store.clearFilters()
}

const totalPages = () => Math.max(1, Math.ceil(store.total / store.page.size))

onMounted(async () => {
  // 从养护任务跳过来时 URL 带 taskType；从商品详情跳时带 speciesCode。
  // 把它们写进 store 查询条件，三处衔接才不只是跳到知识库首页。
  const taskType = typeof route.query.taskType === 'string' ? route.query.taskType : undefined
  const speciesCode = typeof route.query.speciesCode === 'string' ? route.query.speciesCode : undefined
  if (taskType) store.query.taskType = taskType
  if (speciesCode) store.query.speciesCode = speciesCode

  await Promise.all([store.loadFacets(), store.loadArticles(), store.loadRecommended(3)])
})
</script>

<template>
  <section class="knowledge-page">
    <header class="knowledge-page__header">
      <p class="section-kicker">养护知识库</p>
      <h1>把"照做"变成"知道为什么"</h1>
      <p>每篇都写清适用条件、操作频次、具体步骤，以及最容易踩的坑。</p>
    </header>

    <!-- 个性化推荐位 -->
    <section v-if="store.recommended.length" class="recommend-strip">
      <h2>为你推荐</h2>
      <ul>
        <li v-for="item in store.recommended" :key="item.id">
          <RouterLink :to="{ name: 'knowledge-detail', params: { slug: item.slug } }">
            <span class="recommend-strip__reason">{{ item.recommendReason }}</span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.summary }}</small>
          </RouterLink>
        </li>
      </ul>
    </section>

    <!-- 检索与筛选 -->
    <div class="filter-panel">
      <form role="search" class="filter-panel__search" @submit.prevent="search">
        <input
          v-model="keywordInput"
          type="search"
          placeholder="搜索植物名称、症状或主题，例如：黄叶、浇水"
          aria-label="搜索养护知识"
        />
        <button type="submit">搜索</button>
      </form>

      <div v-if="store.facets" class="filter-panel__groups">
        <div class="filter-group">
          <span class="filter-group__label">主题</span>
          <button
            v-for="item in store.facets.categories"
            :key="item.value"
            type="button"
            :class="{ active: store.query.category === item.value }"
            :aria-pressed="store.query.category === item.value"
            @click="pickCategory(item.value)"
          >
            {{ item.label }}
          </button>
        </div>

        <div class="filter-group">
          <span class="filter-group__label">难度</span>
          <button
            v-for="item in store.facets.difficulties"
            :key="item.value"
            type="button"
            :class="{ active: store.query.difficulty === Number(item.value) }"
            :aria-pressed="store.query.difficulty === Number(item.value)"
            @click="pickDifficulty(Number(item.value))"
          >
            {{ item.label }}
          </button>
        </div>

        <div class="filter-group">
          <span class="filter-group__label">季节</span>
          <button
            v-for="item in store.facets.seasons"
            :key="item.value"
            type="button"
            :class="{ active: store.query.season === item.value }"
            :aria-pressed="store.query.season === item.value"
            @click="pickSeason(item.value)"
          >
            {{ item.label }}
          </button>
        </div>

        <div v-if="store.facets.tags.length" class="filter-group filter-group--tags">
          <span class="filter-group__label">标签</span>
          <button
            v-for="tag in store.facets.tags.slice(0, 14)"
            :key="tag"
            type="button"
            :class="{ active: store.query.tag === tag }"
            @click="pickTag(tag)"
          >
            {{ tag }}
          </button>
        </div>
      </div>

      <button v-if="store.hasFilters" class="text-action" type="button" @click="resetAll">
        清除全部筛选
      </button>
    </div>

    <p v-if="store.errorMessage" class="knowledge-page__error" role="alert">
      {{ store.errorMessage }}
    </p>

    <p class="knowledge-page__count" role="status" aria-live="polite">
      共 {{ store.total }} 篇
    </p>

    <p v-if="store.loading" class="knowledge-page__status">加载中…</p>

    <!-- 无结果：告诉用户这个词已被记录，而不是干巴巴一句"没找到" -->
    <div v-else-if="!store.hasResults" class="knowledge-page__empty">
      <h2>没有找到相关内容</h2>
      <p v-if="store.query.keyword">
        「{{ store.query.keyword }}」还没有对应的文章。这个词已经记进内容需求清单，
        我们会优先补上。
      </p>
      <p v-else>换个筛选条件试试。</p>
      <button class="pill-button pill-button--outline" type="button" @click="resetAll">
        看看全部文章
      </button>
    </div>

    <ul v-else class="article-grid">
      <li v-for="article in store.articles" :key="article.id">
        <RouterLink
          class="article-card"
          :to="{ name: 'knowledge-detail', params: { slug: article.slug } }"
        >
          <div class="article-card__head">
            <span class="article-card__category">{{ article.categoryLabel }}</span>
            <span class="article-card__difficulty">{{ article.difficultyLabel }}</span>
          </div>
          <h3>{{ article.title }}</h3>
          <p>{{ article.summary }}</p>
          <ul v-if="article.tags?.length" class="article-card__tags">
            <li v-for="tag in article.tags.slice(0, 3)" :key="tag">{{ tag }}</li>
          </ul>
          <footer class="article-card__foot">
            <span>{{ article.viewCount }} 次阅读</span>
            <span v-if="article.usefulCount">{{ article.usefulCount }} 人觉得有用</span>
          </footer>
        </RouterLink>
      </li>
    </ul>

    <div v-if="store.total > store.page.size" class="knowledge-page__pager">
      <button
        type="button"
        :disabled="store.page.current <= 1"
        @click="store.changePage(store.page.current - 1)"
      >
        上一页
      </button>
      <span>第 {{ store.page.current }} / {{ totalPages() }} 页</span>
      <button
        type="button"
        :disabled="store.page.current >= totalPages()"
        @click="store.changePage(store.page.current + 1)"
      >
        下一页
      </button>
    </div>
  </section>
</template>

<style scoped>
.knowledge-page {
  max-width: 64rem;
  margin: 0 auto;
  padding: var(--space-2xl, 3rem) var(--space-lg, 1.25rem);
}

.knowledge-page__header {
  margin-bottom: var(--space-lg, 1.5rem);
}

.knowledge-page__header h1 {
  margin: 0.35rem 0 0.5rem;
}

.knowledge-page__header p:last-child {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.88rem;
}

.recommend-strip {
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-lg, 1.5rem);
  background: var(--color-brand-soft, #e4eadb);
  border-radius: var(--radius-card, 0.75rem);
}

.recommend-strip h2 {
  margin: 0 0 var(--space-sm, 0.75rem);
  font-size: 0.9rem;
  color: var(--color-brand, #496544);
}

.recommend-strip ul {
  display: grid;
  gap: var(--space-sm, 0.75rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

@media (min-width: 48rem) {
  .recommend-strip ul {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.recommend-strip a {
  display: block;
  padding: var(--space-sm, 0.75rem);
  background: var(--color-surface, #fffdf7);
  border-radius: var(--radius-card, 0.75rem);
  color: inherit;
  text-decoration: none;
}

.recommend-strip a:hover {
  outline: 1px solid var(--color-brand, #496544);
}

.recommend-strip__reason {
  display: block;
  margin-bottom: 0.2rem;
  color: var(--color-brand, #496544);
  font-size: 0.68rem;
  font-weight: 700;
}

.recommend-strip strong {
  display: block;
  font-size: 0.88rem;
}

.recommend-strip small {
  display: block;
  margin-top: 0.15rem;
  overflow: hidden;
  color: var(--color-text-muted, #68716a);
  font-size: 0.72rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filter-panel {
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
}

.filter-panel__search {
  display: flex;
  gap: var(--space-sm, 0.75rem);
  margin-bottom: var(--space-md, 1rem);
}

.filter-panel__search input {
  flex: 1;
  min-height: 2.75rem;
  padding: 0.5rem 0.9rem;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-pill, 999px);
  font-size: 0.88rem;
}

.filter-panel__search button {
  min-height: 2.75rem;
  padding: 0 1.4rem;
  background: var(--color-brand, #496544);
  border: 1px solid var(--color-brand, #496544);
  border-radius: var(--radius-pill, 999px);
  color: var(--color-on-brand, #fffdf7);
  font-weight: 700;
  cursor: pointer;
}

.filter-panel__groups {
  display: grid;
  gap: var(--space-sm, 0.75rem);
}

.filter-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
}

.filter-group__label {
  min-width: 2.5rem;
  color: var(--color-text-muted, #68716a);
  font-size: 0.75rem;
}

.filter-group button {
  padding: 0.25rem 0.7rem;
  background: none;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-pill, 999px);
  cursor: pointer;
  font-size: 0.75rem;
}

.filter-group button.active {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
  color: var(--color-on-brand, #fffdf7);
  font-weight: 700;
}

.knowledge-page__error {
  padding: var(--space-md, 1rem);
  background: var(--color-danger-soft, #f6e3de);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-danger, #a8442f);
}

.knowledge-page__count {
  margin: 0 0 var(--space-md, 1rem);
  color: var(--color-text-muted, #68716a);
  font-size: 0.78rem;
}

.knowledge-page__status,
.knowledge-page__empty {
  padding: var(--space-2xl, 3rem) 0;
  text-align: center;
  color: var(--color-text-muted, #68716a);
}

.knowledge-page__empty h2 {
  margin: 0 0 0.5rem;
  color: var(--color-ink, #24312a);
}

.knowledge-page__empty p {
  max-width: 30rem;
  margin: 0 auto var(--space-md, 1rem);
  line-height: 1.7;
}

.article-grid {
  display: grid;
  gap: var(--space-md, 1rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

@media (min-width: 40rem) {
  .article-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 60rem) {
  .article-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.article-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: var(--space-md, 1rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
  color: inherit;
  text-decoration: none;
}

.article-card:hover {
  border-color: var(--color-brand, #496544);
}

.article-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.4rem;
}

.article-card__category {
  color: var(--color-brand, #496544);
  font-size: 0.7rem;
  font-weight: 700;
}

.article-card__difficulty {
  padding: 0.05rem 0.45rem;
  background: var(--color-brand-soft, #e4eadb);
  border-radius: var(--radius-pill, 999px);
  color: var(--color-brand, #496544);
  font-size: 0.65rem;
}

.article-card h3 {
  margin: 0 0 0.35rem;
  font-size: 1rem;
  line-height: 1.4;
}

.article-card p {
  flex: 1;
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.8rem;
  line-height: 1.65;
}

.article-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
  margin: var(--space-sm, 0.75rem) 0 0;
  padding: 0;
  list-style: none;
}

.article-card__tags li {
  padding: 0.05rem 0.4rem;
  background: var(--color-bg, #f7f4ec);
  border-radius: var(--radius-pill, 999px);
  color: var(--color-text-muted, #68716a);
  font-size: 0.65rem;
}

.article-card__foot {
  display: flex;
  gap: var(--space-sm, 0.75rem);
  margin-top: var(--space-sm, 0.75rem);
  padding-top: var(--space-sm, 0.75rem);
  border-top: 1px dashed var(--color-border, #d9d3c5);
  color: var(--color-text-muted, #68716a);
  font-size: 0.68rem;
}

.knowledge-page__pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md, 1rem);
  margin-top: var(--space-lg, 1.5rem);
}

.knowledge-page__pager button {
  min-height: 2.5rem;
  padding: 0.4rem 1rem;
  background: none;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-pill, 999px);
  cursor: pointer;
}

.knowledge-page__pager button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
