import { request } from '@/shared/api/request'
import type { PageResult } from '@/shared/api/types'

import type {
  Article,
  ArticleFacets,
  ArticleInput,
  ArticleQuery,
  SearchMiss,
} from '../types/knowledge'

// ===== 公开读取。GET 在后端白名单里，游客也能调 =====

export function fetchArticles(query: ArticleQuery = {}, current = 1, size = 9) {
  return request<PageResult<Article>>('/knowledge/articles', {
    query: { current, size, ...query },
  })
}

/** 详情按 slug 而非 id，与商品详情页同一约定 */
export function fetchArticle(slug: string) {
  return request<Article>(`/knowledge/articles/${slug}`)
}

export function fetchFacets() {
  return request<ArticleFacets>('/knowledge/facets')
}

/** 个性化推荐。未登录时后端退回热门，不会空 */
export function fetchRecommended(limit = 4) {
  return request<Article[]>('/knowledge/recommended', { query: { limit } })
}

// ===== 需登录 =====

/** 切换"有用"，返回操作后是否处于已标记状态 */
export function toggleUseful(id: number) {
  return request<boolean>(`/knowledge/articles/${id}/useful`, { method: 'POST' })
}

export function toggleFavorite(id: number) {
  return request<boolean>(`/knowledge/articles/${id}/favorite`, { method: 'POST' })
}

export function fetchFavorites() {
  return request<Article[]>('/knowledge/favorites')
}

// ===== 管理端 =====

export function fetchAdminArticles(current = 1, size = 10, status?: number, keyword?: string) {
  return request<PageResult<Article>>('/admin/knowledge/articles', {
    query: { current, size, status, keyword },
  })
}

export function fetchAdminArticle(id: number) {
  return request<Article>(`/admin/knowledge/articles/${id}`)
}

export function createArticle(payload: ArticleInput) {
  return request<number>('/admin/knowledge/articles', { method: 'POST', body: payload })
}

export function updateArticle(id: number, payload: ArticleInput) {
  return request<void>(`/admin/knowledge/articles/${id}`, { method: 'PUT', body: payload })
}

export function deleteArticle(id: number) {
  return request<void>(`/admin/knowledge/articles/${id}`, { method: 'DELETE' })
}

/**
 * 执行一次状态流转。
 *
 * action 取后端返回的 actions[].code，前端不自己判断能不能做——
 * 状态机在后端，前端再实现一遍必然会有出入。
 */
export function transitionArticle(id: number, action: string) {
  const path = {
    SUBMIT: 'submit',
    WITHDRAW: 'withdraw',
    PUBLISH: 'publish',
    REJECT: 'reject',
    OFFLINE: 'offline',
  }[action]
  return request<void>(`/admin/knowledge/articles/${id}/${path}`, { method: 'PUT' })
}

/** 无结果关键词清单，方案要求的"后台内容需求清单" */
export function fetchSearchMisses(limit = 20) {
  return request<SearchMiss[]>('/admin/knowledge/search-misses', { query: { limit } })
}
