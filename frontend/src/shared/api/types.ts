/** 与后端 shared/web/R.java 对应的统一响应体 */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** MyBatis-Plus IPage 的返回结构，各模块分页接口共用 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

/** 与后端 user/web/vo/UserVO.java 对应 */
export interface AuthUser {
  id: number
  username: string
  nickname: string | null
  phone: string | null
  avatar: string | null
  status: number
  roles: string[]
  permissions: string[]
}

/** 与后端 user/web/vo/TokenVO.java 对应 */
export interface TokenPayload {
  token: string
  tokenType: string
  /** 有效期，单位秒 */
  expiresIn: number
  user: AuthUser
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
  phone?: string
}

/** 与后端 shared/exception/ErrorCode.java 保持一致 */
export const ErrorCode = {
  SUCCESS: 0,
  PARAM_INVALID: 1001,
  UNAUTHORIZED: 1401,
  FORBIDDEN: 1403,
  LOGIN_FAILED: 2001,
  USERNAME_TAKEN: 2002,
  ACCOUNT_BANNED: 2003,
  PLANT_NOT_FOUND: 3001,
  PLANT_SLUG_TAKEN: 3002,
} as const
