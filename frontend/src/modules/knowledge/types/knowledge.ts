/** 与后端 knowledge/web/vo/*.java 一一对应 */

/** 文章状态。数值与后端 ArticleStatus 一致 */
export const ArticleStatus = {
  DRAFT: 0,
  PENDING: 1,
  PUBLISHED: 2,
  OFFLINE: 3,
} as const

/** 与后端 ArticleStep 对应 */
export interface ArticleStep {
  title: string
  detail: string
}

/** 与后端 ArticleVO 对应。列表用摘要形态，详情才带步骤化四件套 */
export interface Article {
  id: number
  slug: string
  title: string
  summary: string
  cover: string | null
  category: string
  categoryLabel: string
  difficulty: number
  difficultyLabel: string | null
  seasons: string[] | null
  tags: string[] | null

  // ===== 方案要求的步骤化四件套，详情才有 =====
  applicable?: string
  frequency?: string
  steps?: ArticleStep[]
  mistakes?: string[] | null
  risks?: string[] | null

  relatedTaskTypes?: string[] | null
  relatedSpecies?: string[] | null

  viewCount: number
  usefulCount: number
  /** 当前登录人是否点过"有用" */
  marked: boolean
  favorited: boolean
  publishedAt: string | null

  /** 只有个性化推荐接口会填 */
  recommendReason?: string | null

  // ===== 管理端才有 =====
  status?: number
  statusLabel?: string | null
  actions?: ArticleAction[]
  updatedAt?: string
}

export interface ArticleAction {
  /** SUBMIT / WITHDRAW / PUBLISH / REJECT / OFFLINE */
  code: string
  label: string
}

/** 与后端 ArticleFacetsVO 对应 */
export interface ArticleFacets {
  categories: FacetOption[]
  difficulties: FacetOption[]
  seasons: FacetOption[]
  tags: string[]
}

export interface FacetOption {
  value: string
  label: string
}

/** 检索条件。与后端 ArticleQueryDTO 对应 */
export interface ArticleQuery {
  keyword?: string
  category?: string
  difficulty?: number
  season?: string
  tag?: string
  /** 品种 code，商品详情页跳转用 */
  speciesCode?: string
  /** 养护任务类型，养护任务页跳转用 */
  taskType?: string
}

/** 保存文章的入参。与后端 ArticleSaveDTO 对应 */
export interface ArticleInput {
  slug: string
  title: string
  summary: string
  cover?: string
  category: string
  difficulty: number
  seasons?: string[]
  tags?: string[]
  applicable: string
  frequency: string
  steps: ArticleStep[]
  mistakes?: string[]
  risks?: string[]
  relatedTaskTypes?: string[]
  relatedSpecies?: string[]
  sort?: number
}

/** 与后端 KnowledgeSearchMiss 对应。方案要求的"后台内容需求清单" */
export interface SearchMiss {
  id: number
  keyword: string
  hitCount: number
  lastSearchedAt: string
  createdAt: string
}
