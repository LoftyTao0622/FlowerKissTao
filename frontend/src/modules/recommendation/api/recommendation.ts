import { request } from '@/shared/api/request'

import type { Recommendation } from '../types/recommendation'

/**
 * 生成一次推荐并存快照。
 *
 * @param profileId 不传则用当前用户的默认场景
 */
export function generateRecommendation(profileId?: number) {
  return request<Recommendation>('/recommendations', {
    method: 'POST',
    body: profileId === undefined ? {} : { profileId },
  })
}

/** 我最近一次推荐。从没推荐过时后端返回 null，不是错误 */
export function fetchLatestRecommendation() {
  return request<Recommendation | null>('/recommendations/latest')
}

export function fetchRecommendation(id: number) {
  return request<Recommendation>(`/recommendations/${id}`)
}

/**
 * 记录一次点击，第⑦步运营看板算推荐点击率。
 *
 * 埋点失败不该拦住用户跳转，调用方一律 catch 掉。
 */
export function markRecommendationClicked(resultId: number, itemId: number) {
  return request<void>(`/recommendations/${resultId}/items/${itemId}/click`, {
    method: 'PUT',
  })
}
