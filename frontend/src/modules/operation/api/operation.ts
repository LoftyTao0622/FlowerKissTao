import { request } from '@/shared/api/request'
import type { PageResult } from '@/shared/api/types'
import type { Recommendation } from '@/modules/recommendation/types/recommendation'
import type { Dashboard, OperationLog, WeightConfig } from '../types/operation'

export function fetchDashboard(from?: string, to?: string) {
  return request<Dashboard>('/admin/operation/dashboard', { query: { from, to } })
}

export function fetchOperationLogs(
  current = 1,
  size = 10,
  module?: string,
  action?: string,
) {
  return request<PageResult<OperationLog>>('/admin/operation/logs', {
    query: { current, size, module, action },
  })
}

export function recordVisit(path: string, pageType: string, referrer?: string) {
  return request<void>('/operation/visits', {
    method: 'POST',
    body: { path, pageType, referrer },
    handleUnauthorized: false,
  })
}

export function fetchWeights() {
  return request<WeightConfig>('/admin/rec-weights')
}

export function updateWeights(payload: WeightConfig) {
  return request<void>('/admin/rec-weights', { method: 'PUT', body: payload })
}

/** 临时预览，不落配置、不存推荐快照 */
export function previewWeights(payload: { profileId?: number; weights: WeightConfig }) {
  return request<Recommendation>('/admin/rec-weights/preview', {
    method: 'POST',
    body: payload,
  })
}
