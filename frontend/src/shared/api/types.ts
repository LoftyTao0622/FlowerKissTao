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
  SPECIES_HAS_SKU: 3003,
  SKU_NOT_FOUND: 3004,
  SKU_CODE_TAKEN: 3005,
  PROFILE_NOT_FOUND: 4001,
  PROFILE_SCENE_NAME_TAKEN: 4002,
  PROFILE_LAST_SCENE: 4003,
  REC_RESULT_NOT_FOUND: 5001,
  REC_WEIGHT_SUM_INVALID: 5002,
  /** 还没有任何场景画像，推荐没有输入。前端据此引导去填问卷 */
  REC_NO_PROFILE: 5003,
  ORDER_NOT_FOUND: 6001,
  ORDER_STATUS_INVALID: 6002,
  STOCK_INSUFFICIENT: 6003,
  CART_EMPTY: 6004,
  SKU_UNAVAILABLE: 6005,
  ADDRESS_NOT_FOUND: 6006,
  PAY_DUPLICATED: 6007,
  CARE_ARCHIVE_NOT_FOUND: 7001,
  CARE_TASK_NOT_FOUND: 7002,
  CARE_ARCHIVE_CLOSED: 7003,
  CARE_IMAGE_TOO_LARGE: 7004,
  CARE_TASK_CLOSED: 7005,
  ARTICLE_NOT_FOUND: 8001,
  ARTICLE_SLUG_TAKEN: 8002,
  ARTICLE_STATUS_INVALID: 8003,
} as const
