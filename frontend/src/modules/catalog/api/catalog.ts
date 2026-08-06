import { request } from '@/shared/api/request'
import type { PageResult } from '@/shared/api/types'

import type { CatalogPlant, PlantFacets } from '../types/catalog'

// 写成 type 而非 interface：request 的 query 参数要求 Record<string, ...>，
// 而 TypeScript 只给类型别名隐式索引签名，interface 会报缺少索引签名
export type PlantPageParams = {
  current?: number
  size?: number
  keyword?: string
  category?: string
  light?: string
  featured?: boolean
}

export function fetchPlantPage(params: PlantPageParams = {}) {
  return request<PageResult<CatalogPlant>>('/catalog/plants', { query: params })
}

/** 详情按 slug 查。查不到时后端返回 code 3001（PLANT_NOT_FOUND） */
export function fetchPlantBySlug(slug: string) {
  return request<CatalogPlant>(`/catalog/plants/${encodeURIComponent(slug)}`)
}

export function fetchPlantFacets() {
  return request<PlantFacets>('/catalog/plant-facets')
}
