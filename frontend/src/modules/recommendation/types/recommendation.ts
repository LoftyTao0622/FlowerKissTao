/** 与后端 recommendation/web/vo/*.java 一一对应 */

/** 七个评分维度的 code，与后端 ScoreDimension 同名 */
export type ScoreCode =
  | 'light'
  | 'temp'
  | 'humidity'
  | 'care'
  | 'space'
  | 'budget'
  | 'preference'

/** 六条硬过滤规则的 code，与后端 FilterRule 同名 */
export type FilterCode = 'pet' | 'child' | 'light' | 'space' | 'budget' | 'water'

/** 与后端 ScoreItemVO 对应。权重列表复用同一结构，那时 score 为 null */
export interface ScoreItem {
  code: ScoreCode
  label: string
  /** 该维得分 0-100。出现在 weights 列表里时为 null */
  score: number | null
  /** 该维在本次推荐中的权重百分比，取自快照而非当前配置 */
  weight: number
}

/** 与后端 FilterDiagnosticVO 对应 */
export interface FilterDiagnostic {
  code: FilterCode
  label: string
  excluded: number
  /** false 表示安全约束，界面上必须标注"不建议放宽" */
  relaxable: boolean
}

/** 与后端 RecItemVO 对应 */
export interface RecItem {
  itemId: number
  rankNo: number

  speciesId: number
  /** 品种 code，详情页路由用的就是它 */
  slug: string | null
  name: string | null
  latinName: string | null
  skuId: number | null
  spec: string | null
  price: number | null
  image: string | null
  imageAlt: string | null
  stock: number | null

  totalScore: number
  scores: ScoreItem[]
  reasons: string[]
  /** 没有明显短板时为 null */
  risk: string | null

  careLevel: number | null
  careLevelLabel: string | null
  lightNote: string | null
  waterNote: string | null
  petFriendly: boolean | null
}

/** 与后端 RecommendationVO 对应 */
export interface Recommendation {
  resultId: number
  profileId: number
  sceneName: string
  createdAt: string
  /** 参与筛选的在售品种总数 */
  totalCount: number
  /** 硬过滤后的候选数。可能大于 items.length，后者受 topN 限制 */
  candidateCount: number
  items: RecItem[]
  diagnostics: FilterDiagnostic[]
  /** 无候选时的放宽建议，只含非安全约束。有候选时为空数组 */
  suggestions: string[]
  /** 本次使用的权重，取自快照 */
  weights: ScoreItem[]
}
