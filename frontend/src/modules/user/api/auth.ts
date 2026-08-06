import { request } from '@/shared/api/request'
import type { AuthUser, LoginPayload, RegisterPayload, TokenPayload } from '@/shared/api/types'

export function login(payload: LoginPayload) {
  // 登录失败不应触发全局登出逻辑，交由调用方处理错误提示
  return request<TokenPayload>('/auth/login', {
    method: 'POST',
    body: payload,
    handleUnauthorized: false,
  })
}

export function register(payload: RegisterPayload) {
  return request<TokenPayload>('/auth/register', {
    method: 'POST',
    body: payload,
    handleUnauthorized: false,
  })
}

export function fetchCurrentUser() {
  return request<AuthUser>('/auth/me')
}

export function logout() {
  return request<void>('/auth/logout', { method: 'POST' })
}
