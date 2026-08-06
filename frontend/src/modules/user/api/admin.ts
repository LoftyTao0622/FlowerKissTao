import { request } from '@/shared/api/request'
import type { AuthUser, PageResult } from '@/shared/api/types'

// PageResult 原先定义在这里，catalog 模块也要用，已上移到 shared/api/types。
// 保留这行 re-export，让既有的引用方不必改 import 路径。
export type { PageResult } from '@/shared/api/types'

export function fetchUserPage(params: { current: number; size: number; keyword?: string }) {
  return request<PageResult<AuthUser>>('/admin/users', { query: params })
}

export function changeUserStatus(userId: number, status: 0 | 1) {
  return request<void>(`/admin/users/${userId}/status`, {
    method: 'PUT',
    query: { status },
  })
}

export function assignRoles(userId: number, roleIds: number[]) {
  return request<void>(`/admin/users/${userId}/roles`, {
    method: 'PUT',
    body: roleIds,
  })
}

export interface RoleItem {
  id: number
  code: string
  name: string
  description: string | null
  sort: number
}

export function fetchRoles() {
  return request<RoleItem[]>('/admin/roles')
}
