import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as authApi from '@/modules/user/api/auth'
import { clearToken, getToken, setToken } from '@/shared/api/token'
import { setUnauthorizedHandler } from '@/shared/api/request'
import type { AuthUser, LoginPayload, RegisterPayload } from '@/shared/api/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const token = ref(getToken())
  /** 刷新页面后需先用 token 换回用户信息，期间路由守卫要等待 */
  const initialized = ref(false)
  const loading = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value && user.value))
  const permissions = computed(() => new Set(user.value?.permissions ?? []))
  const roles = computed(() => new Set(user.value?.roles ?? []))
  const displayName = computed(() => user.value?.nickname || user.value?.username || '')

  /**
   * 是否拥有某个权限点。菜单与按钮的显隐依据这里。
   * 注意：前端隐藏只是体验优化，真正的拦截在后端 @PreAuthorize。
   */
  function can(permission: string): boolean {
    return permissions.value.has(permission)
  }

  function canAny(...list: string[]): boolean {
    return list.some((item) => permissions.value.has(item))
  }

  function hasRole(role: string): boolean {
    return roles.value.has(role)
  }

  function applyToken(nextToken: string, nextUser: AuthUser) {
    token.value = nextToken
    user.value = nextUser
    setToken(nextToken)
  }

  /** 清理本地登录态，不发请求 */
  function reset() {
    token.value = ''
    user.value = null
    clearToken()
  }

  async function login(payload: LoginPayload) {
    loading.value = true
    try {
      const result = await authApi.login(payload)
      applyToken(result.token, result.user)
      return result.user
    } finally {
      loading.value = false
    }
  }

  async function register(payload: RegisterPayload) {
    loading.value = true
    try {
      const result = await authApi.register(payload)
      applyToken(result.token, result.user)
      return result.user
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      if (token.value) await authApi.logout()
    } catch {
      // 登出接口失败不影响本地清理，JWT 本就无状态
    } finally {
      reset()
    }
  }

  /**
   * 应用启动时调用一次：若本地有 token 则换回最新的用户信息与权限。
   * 权限点不存在 token 里，所以后台改过角色后刷新页面即可生效。
   */
  async function initialize() {
    if (initialized.value) return
    if (!token.value) {
      initialized.value = true
      return
    }

    try {
      user.value = await authApi.fetchCurrentUser()
    } catch {
      // token 过期或账号被封禁，清掉本地登录态
      reset()
    } finally {
      initialized.value = true
    }
  }

  // 请求层遇到 401 时回调，保证任意接口失效都会同步清理登录态
  setUnauthorizedHandler(() => {
    reset()
  })

  return {
    user,
    token,
    initialized,
    loading,
    isLoggedIn,
    permissions,
    roles,
    displayName,
    can,
    canAny,
    hasRole,
    login,
    register,
    logout,
    initialize,
    reset,
  }
})
