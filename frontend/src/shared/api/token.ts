const STORAGE_KEY = 'flower-kiss-tao-token'

/**
 * 令牌读写集中在这里，避免 store、请求层、路由守卫各写一份 key。
 * 用 localStorage 而非 sessionStorage，关掉标签页后仍保持登录。
 */
export function getToken(): string {
  if (typeof window === 'undefined') return ''
  try {
    return localStorage.getItem(STORAGE_KEY) || ''
  } catch {
    return ''
  }
}

export function setToken(token: string) {
  if (typeof window === 'undefined') return
  try {
    localStorage.setItem(STORAGE_KEY, token)
  } catch {
    /* 隐私模式下写入会抛异常，忽略即可，内存中的登录态仍然可用 */
  }
}

export function clearToken() {
  if (typeof window === 'undefined') return
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    /* 同上 */
  }
}
