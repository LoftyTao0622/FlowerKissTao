/** 运营后台 API 类型 */

export interface DashboardOverview {
  profileCompletionRate: number
  recommendationClickRate: number
  paidOrderCount: number
  paidAmount: number
  careTaskCompletionRate: number
  normalUserCount: number
  profileUserCount: number
  recommendationItemCount: number
  recommendationClickedCount: number
  handledCareTaskCount: number
  completedCareTaskCount: number
}

export interface TrendPoint {
  date: string
  visits: number
  recommendations: number
  paidOrders: number
  paidAmount: number
}

export interface OrderBreakdown {
  status: number
  statusLabel: string
  count: number
  amount: number
}

export interface TopPlant {
  speciesId: number
  name: string
  orderedQuantity: number
  revenue: number
}

export interface TopArticle {
  articleId: number
  slug: string
  title: string
  views: number
  usefulCount: number
}

export interface CareBreakdown {
  status: number
  statusLabel: string
  count: number
}

export interface Dashboard {
  from: string
  to: string
  overview: DashboardOverview
  trends: TrendPoint[]
  orderBreakdown: OrderBreakdown[]
  topPlants: TopPlant[]
  topArticles: TopArticle[]
  careBreakdown: CareBreakdown[]
}

export interface OperationLog {
  id: number
  userId: number | null
  username: string | null
  module: string
  action: string
  targetType: string | null
  targetId: string | null
  before: Record<string, unknown> | null
  after: Record<string, unknown> | null
  ip: string | null
  userAgent: string | null
  createdAt: string
}

export interface WeightConfig {
  light: number
  temp: number
  humidity: number
  care: number
  space: number
  budget: number
  preference: number
  topN: number
}
