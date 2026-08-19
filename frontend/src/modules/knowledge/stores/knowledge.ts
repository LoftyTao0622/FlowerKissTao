import { computed, reactive, ref } from 'vue'
import { defineStore } from 'pinia'

import * as knowledgeApi from '../api/knowledge'
import type { Article, ArticleFacets, ArticleQuery } from '../types/knowledge'

/**
 * 知识库。
 *
 * <p>检索、排序与推荐规则全在后端，这个 store 只负责调接口与存结果。
 */
export const useKnowledgeStore = defineStore('knowledge', () => {
  const articles = ref<Article[]>([])
  const total = ref(0)
  const current = ref<Article | null>(null)
  const facets = ref<ArticleFacets | null>(null)
  const recommended = ref<Article[]>([])
  const favorites = ref<Article[]>([])

  const loading = ref(false)
  const submitting = ref(false)
  const errorMessage = ref('')

  /** 当前筛选条件，列表页的筛选器直接绑它 */
  const query = reactive<ArticleQuery>({})
  const page = reactive({ current: 1, size: 9 })

  const hasResults = computed(() => articles.value.length > 0)

  /** 有没有正在生效的筛选，用于显示"清除筛选" */
  const hasFilters = computed(() =>
    Boolean(
      query.keyword || query.category || query.difficulty || query.season || query.tag
      || query.speciesCode || query.taskType,
    ),
  )

  async function loadArticles() {
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await knowledgeApi.fetchArticles(
        { ...query }, page.current, page.size,
      )
      articles.value = result.records
      total.value = result.total
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '文章加载失败'
    } finally {
      loading.value = false
    }
  }

  /** 改筛选条件后回到第一页——留在第 3 页上很可能直接是空的 */
  function applyFilters(next: Partial<ArticleQuery>) {
    Object.assign(query, next)
    page.current = 1
    return loadArticles()
  }

  function clearFilters() {
    query.keyword = undefined
    query.category = undefined
    query.difficulty = undefined
    query.season = undefined
    query.tag = undefined
    query.speciesCode = undefined
    query.taskType = undefined
    page.current = 1
    return loadArticles()
  }

  function changePage(next: number) {
    page.current = next
    return loadArticles()
  }

  async function loadArticle(slug: string) {
    loading.value = true
    errorMessage.value = ''
    try {
      current.value = await knowledgeApi.fetchArticle(slug)
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '文章加载失败'
      current.value = null
    } finally {
      loading.value = false
    }
  }

  async function loadFacets() {
    try {
      facets.value = await knowledgeApi.fetchFacets()
    } catch {
      // 筛选项加载失败不该拦住看文章，静默即可
    }
  }

  async function loadRecommended(limit = 4) {
    try {
      recommended.value = await knowledgeApi.fetchRecommended(limit)
    } catch {
      // 推荐位空着不影响主流程
    }
  }

  async function loadFavorites() {
    loading.value = true
    try {
      favorites.value = await knowledgeApi.fetchFavorites()
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '收藏加载失败'
    } finally {
      loading.value = false
    }
  }

  /** 切换"有用"。后端返回操作后的状态，直接用它更新本地 */
  async function toggleUseful() {
    if (!current.value) return
    submitting.value = true
    try {
      const marked = await knowledgeApi.toggleUseful(current.value.id)
      current.value.marked = marked
      current.value.usefulCount += marked ? 1 : -1
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '操作失败'
    } finally {
      submitting.value = false
    }
  }

  async function toggleFavorite() {
    if (!current.value) return
    submitting.value = true
    try {
      current.value.favorited = await knowledgeApi.toggleFavorite(current.value.id)
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '操作失败'
    } finally {
      submitting.value = false
    }
  }

  /** 退出登录时清空：收藏与个性化推荐都是私有的 */
  function reset() {
    favorites.value = []
    recommended.value = []
    if (current.value) {
      current.value.marked = false
      current.value.favorited = false
    }
    errorMessage.value = ''
  }

  return {
    articles,
    total,
    current,
    facets,
    recommended,
    favorites,
    loading,
    submitting,
    errorMessage,
    query,
    page,
    hasResults,
    hasFilters,
    loadArticles,
    applyFilters,
    clearFilters,
    changePage,
    loadArticle,
    loadFacets,
    loadRecommended,
    loadFavorites,
    toggleUseful,
    toggleFavorite,
    reset,
  }
})
