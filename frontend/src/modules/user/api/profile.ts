import { request } from '@/shared/api/request'

import type { SceneProfile, SceneProfileInput } from '../types/profile'
import type { AuthUser } from '@/shared/api/types'

export interface ProfileUpdatePayload {
  username?: string
  nickname?: string
  currentPassword?: string
  newPassword?: string
}

export function fetchUserProfile() {
  return request<AuthUser>('/user/profile')
}

export function updateUserProfile(payload: ProfileUpdatePayload) {
  return request<AuthUser>('/user/profile', { method: 'PUT', body: payload })
}

export function uploadAvatar(file: File) {
  const body = new FormData()
  body.append('file', file)
  return request<{ url: string }>('/user/profile/avatar', { method: 'POST', body })
}

/** 我的全部场景，默认场景排在最前 */
export function fetchMyProfiles() {
  return request<SceneProfile[]>('/profiles')
}

/** 我的默认场景。新用户还没建过时后端返回 null，不是错误 */
export function fetchDefaultProfile() {
  return request<SceneProfile | null>('/profiles/default')
}

export function fetchProfile(id: number) {
  return request<SceneProfile>(`/profiles/${id}`)
}

/** 返回新场景的 id */
export function createProfile(payload: SceneProfileInput) {
  return request<number>('/profiles', { method: 'POST', body: payload })
}

export function updateProfile(id: number, payload: SceneProfileInput) {
  return request<void>(`/profiles/${id}`, { method: 'PUT', body: payload })
}

/** 删到只剩一个时后端返回 4003（PROFILE_LAST_SCENE） */
export function deleteProfile(id: number) {
  return request<void>(`/profiles/${id}`, { method: 'DELETE' })
}

export function setDefaultProfile(id: number) {
  return request<void>(`/profiles/${id}/default`, { method: 'PUT' })
}
