<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'

import { useAuthStore } from '@/modules/user/stores/auth'
import { useKnowledgeStore } from '../stores/knowledge'

const props = defineProps<{ slug: string }>()

const store = useKnowledgeStore()
const authStore = useAuthStore()

const article = computed(() => store.current)
const isLoggedIn = computed(() => authStore.isLoggedIn)

const SEASON_LABELS: Record<string, string> = {
  spring: '春季',
  summer: '夏季',
  autumn: '秋季',
  winter: '冬季',
}

function seasonLabel(code: string) {
  return SEASON_LABELS[code] ?? code
}

// 在两篇文章之间跳转时组件不重新挂载，只有路由参数在变
watch(() => props.slug, (slug) => {
  void store.loadArticle(slug)
})

onMounted(() => {
  void store.loadArticle(props.slug)
})
</script>

<template>
  <article class="article-page">
    <p v-if="store.loading" class="article-page__status" role="status">加载中…</p>

    <div v-else-if="!article" class="article-page__status">
      <p>这篇文章不存在或已下架。</p>
      <RouterLink class="pill-button" :to="{ name: 'knowledge' }">返回知识库</RouterLink>
    </div>

    <template v-else>
      <nav class="article-page__crumb" aria-label="面包屑导航">
        <RouterLink :to="{ name: 'knowledge' }">养护知识库</RouterLink>
        <span aria-hidden="true">/</span>
        <span aria-current="page">{{ article.categoryLabel }}</span>
      </nav>

      <header class="article-page__header">
        <div class="article-page__badges">
          <span class="badge badge--brand">{{ article.categoryLabel }}</span>
          <span class="badge">{{ article.difficultyLabel }}</span>
          <span v-for="s in article.seasons ?? []" :key="s" class="badge">
            {{ seasonLabel(s) }}
          </span>
        </div>
        <h1>{{ article.title }}</h1>
        <p class="article-page__summary">{{ article.summary }}</p>
        <p class="article-page__meta">
          {{ article.viewCount }} 次阅读
          <template v-if="article.usefulCount">
            · {{ article.usefulCount }} 人觉得有用
          </template>
        </p>
      </header>

      <!-- 适用条件与操作频次：方案要求的前两项，放最前面让读者先判断是否适用 -->
      <section class="article-page__brief">
        <div>
          <h2>适用条件</h2>
          <p>{{ article.applicable }}</p>
        </div>
        <div>
          <h2>操作频次</h2>
          <p>{{ article.frequency }}</p>
        </div>
      </section>

      <!-- 步骤化说明 -->
      <section v-if="article.steps?.length" class="article-page__steps">
        <h2>怎么做</h2>
        <ol>
          <li v-for="(step, index) in article.steps" :key="index">
            <span class="step-no" aria-hidden="true">
              {{ String(index + 1).padStart(2, '0') }}
            </span>
            <div>
              <strong>{{ step.title }}</strong>
              <p>{{ step.detail }}</p>
            </div>
          </li>
        </ol>
      </section>

      <!-- 常见误区 -->
      <section v-if="article.mistakes?.length" class="article-page__block article-page__block--warn">
        <h2>常见误区</h2>
        <ul>
          <li v-for="item in article.mistakes" :key="item">{{ item }}</li>
        </ul>
      </section>

      <!-- 风险提示 -->
      <section v-if="article.risks?.length" class="article-page__block article-page__block--risk">
        <h2>风险提示</h2>
        <ul>
          <li v-for="item in article.risks" :key="item">{{ item }}</li>
        </ul>
      </section>

      <ul v-if="article.tags?.length" class="article-page__tags" aria-label="标签">
        <li v-for="tag in article.tags" :key="tag">{{ tag }}</li>
      </ul>

      <!-- 收藏与有用性反馈 -->
      <footer v-if="isLoggedIn" class="article-page__actions">
        <button
          type="button"
          :class="['feedback-btn', { active: article.marked }]"
          :disabled="store.submitting"
          :aria-pressed="article.marked"
          @click="store.toggleUseful()"
        >
          {{ article.marked ? '已标记有用' : '这篇有用' }}
        </button>
        <button
          type="button"
          :class="['feedback-btn', { active: article.favorited }]"
          :disabled="store.submitting"
          :aria-pressed="article.favorited"
          @click="store.toggleFavorite()"
        >
          {{ article.favorited ? '已收藏' : '收藏' }}
        </button>
      </footer>
      <p v-else class="article-page__login-hint">
        <RouterLink :to="{ name: 'login' }">登录</RouterLink>
        后可以收藏这篇文章，并获得依据你养的植物推荐的内容。
      </p>

      <p v-if="store.errorMessage" class="article-page__error" role="alert">
        {{ store.errorMessage }}
      </p>
    </template>
  </article>
</template>

<style scoped>
.article-page {
  max-width: 44rem;
  margin: 0 auto;
  padding: var(--space-2xl, 3rem) var(--space-lg, 1.25rem);
}

.article-page__status {
  padding: var(--space-2xl, 3rem) 0;
  text-align: center;
  color: var(--color-text-muted, #68716a);
}

.article-page__crumb {
  display: flex;
  gap: 0.4rem;
  margin-bottom: var(--space-md, 1rem);
  color: var(--color-text-muted, #68716a);
  font-size: 0.78rem;
}

.article-page__crumb a {
  color: var(--color-brand, #496544);
  text-decoration: none;
}

.article-page__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-bottom: 0.6rem;
}

.badge {
  padding: 0.1rem 0.5rem;
  background: var(--color-bg, #f7f4ec);
  border-radius: var(--radius-pill, 999px);
  color: var(--color-text-muted, #68716a);
  font-size: 0.68rem;
}

.badge--brand {
  background: var(--color-brand-soft, #e4eadb);
  color: var(--color-brand, #496544);
  font-weight: 700;
}

.article-page__header h1 {
  margin: 0 0 0.5rem;
  line-height: 1.35;
}

.article-page__summary {
  margin: 0 0 0.4rem;
  color: var(--color-ink, #24312a);
  font-size: 1rem;
  line-height: 1.7;
}

.article-page__meta {
  margin: 0 0 var(--space-lg, 1.5rem);
  color: var(--color-text-muted, #68716a);
  font-size: 0.72rem;
}

/* 适用条件与操作频次并排：读者先判断适不适用，再看怎么做 */
.article-page__brief {
  display: grid;
  gap: var(--space-md, 1rem);
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-lg, 1.5rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
}

@media (min-width: 40rem) {
  .article-page__brief {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.article-page__brief h2 {
  margin: 0 0 0.3rem;
  color: var(--color-brand, #496544);
  font-size: 0.8rem;
}

.article-page__brief p {
  margin: 0;
  font-size: 0.85rem;
  line-height: 1.7;
}

.article-page__steps {
  margin-bottom: var(--space-lg, 1.5rem);
}

.article-page__steps h2 {
  margin: 0 0 var(--space-md, 1rem);
  font-size: 1.05rem;
}

.article-page__steps ol {
  display: grid;
  gap: var(--space-md, 1rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

.article-page__steps li {
  display: grid;
  grid-template-columns: 2.4rem minmax(0, 1fr);
  gap: var(--space-sm, 0.75rem);
}

.step-no {
  font-family: ui-serif, 'Noto Serif SC', serif;
  font-size: 1.3rem;
  color: var(--color-brand-accent, #92a879);
  line-height: 1.2;
}

.article-page__steps strong {
  display: block;
  margin-bottom: 0.2rem;
  font-size: 0.92rem;
}

.article-page__steps p {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.85rem;
  line-height: 1.75;
}

.article-page__block {
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
  border-radius: var(--radius-card, 0.75rem);
}

.article-page__block h2 {
  margin: 0 0 0.5rem;
  font-size: 0.9rem;
}

.article-page__block ul {
  display: grid;
  gap: 0.5rem;
  margin: 0;
  padding-left: 1.1rem;
}

.article-page__block li {
  font-size: 0.85rem;
  line-height: 1.75;
}

.article-page__block--warn {
  background: var(--color-brand-soft, #e4eadb);
  color: var(--color-brand-deep, #203d2c);
}

.article-page__block--risk {
  background: var(--color-danger-soft, #f6e3de);
  color: var(--color-danger, #a8442f);
}

.article-page__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin: var(--space-lg, 1.5rem) 0;
  padding: 0;
  list-style: none;
}

.article-page__tags li {
  padding: 0.1rem 0.55rem;
  background: var(--color-bg, #f7f4ec);
  border-radius: var(--radius-pill, 999px);
  color: var(--color-text-muted, #68716a);
  font-size: 0.7rem;
}

.article-page__actions {
  display: flex;
  gap: var(--space-sm, 0.75rem);
  padding-top: var(--space-md, 1rem);
  border-top: 1px solid var(--color-border, #d9d3c5);
}

.feedback-btn {
  min-height: 2.6rem;
  padding: 0.4rem 1.2rem;
  background: none;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-pill, 999px);
  cursor: pointer;
  font-size: 0.82rem;
}

.feedback-btn.active {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
  color: var(--color-on-brand, #fffdf7);
  font-weight: 700;
}

.feedback-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.article-page__login-hint {
  padding-top: var(--space-md, 1rem);
  border-top: 1px solid var(--color-border, #d9d3c5);
  color: var(--color-text-muted, #68716a);
  font-size: 0.8rem;
}

.article-page__login-hint a {
  color: var(--color-brand, #496544);
}

.article-page__error {
  margin-top: var(--space-md, 1rem);
  color: var(--color-danger, #a8442f);
  font-size: 0.8rem;
}
</style>
