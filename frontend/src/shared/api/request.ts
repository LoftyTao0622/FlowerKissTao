import { ErrorCode, type ApiResult } from './types'
import { clearToken, getToken } from './token'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

/** 业务异常。调用方 catch 后可依据 code 做差异化提示 */
export class ApiError extends Error {
  readonly code: number
  readonly httpStatus: number

  constructor(code: number, message: string, httpStatus = 0) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.httpStatus = httpStatus
  }
}

/** 401 时通知外部清理登录态。由 auth store 注册，避免此处直接 import store 造成循环依赖 */
type UnauthorizedHandler = () => void
let onUnauthorized: UnauthorizedHandler | null = null

export function setUnauthorizedHandler(handler: UnauthorizedHandler) {
  onUnauthorized = handler
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  body?: unknown
  query?: Record<string, string | number | boolean | undefined | null>
  /** 传 false 可跳过 401 时的自动登出，用于登录页自身的请求 */
  handleUnauthorized?: boolean
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = `${BASE_URL}${path}`
  if (!query) return url

  const params = new URLSearchParams()
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params.append(key, String(value))
    }
  })
  const queryString = params.toString()
  return queryString ? `${url}?${queryString}` : url
}

/**
 * 统一请求入口。
 *
 * <p>后端的业务失败有两种形态：HTTP 4xx（未登录、越权、参数错误）与 HTTP 200 但 code≠0
 * （密码错误、用户名占用等）。两种都在这里收敛成 ApiError 抛出，调用方只需 try/catch。
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query, handleUnauthorized = true } = options

  const headers: Record<string, string> = {}
  const token = getToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (body !== undefined) {
    // 显式声明 charset，否则中文昵称在某些环境下会以非 UTF-8 发出，后端解析报错
    headers['Content-Type'] = 'application/json; charset=utf-8'
  }

  let response: Response
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    // 断网、后端未启动、CORS 被拒都会走到这里
    throw new ApiError(-1, '网络异常，请检查网络连接或后端服务是否已启动')
  }

  if (response.status === 401 && handleUnauthorized) {
    clearToken()
    onUnauthorized?.()
  }

  let payload: ApiResult<T> | null = null
  try {
    payload = (await response.json()) as ApiResult<T>
  } catch {
    payload = null
  }

  // 后端所有异常都经 GlobalExceptionHandler 转成 R，理论上一定有 body；
  // 拿不到说明是网关或容器层的错误页
  if (!payload) {
    throw new ApiError(response.status, `请求失败（HTTP ${response.status}）`, response.status)
  }

  if (payload.code !== ErrorCode.SUCCESS) {
    throw new ApiError(payload.code, payload.message || '请求失败', response.status)
  }

  return payload.data
}
