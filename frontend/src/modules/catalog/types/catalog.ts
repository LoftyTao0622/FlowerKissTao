/**
 * 与后端 catalog/web/vo/PlantVO.java 对应。
 *
 * category / light / difficulty 曾是字符串字面量联合类型，改成后端驱动后
 * 这些值在运行时才从数据库来，字面量类型无法满足，因此放宽为 string。
 * 可选值改由 /api/catalog/plant-facets 提供。
 *
 * 后端把品种表与 SKU 表拆开后，这里的字段名保持不变——slug 仍是品种 code、
 * light 仍是展示文案、price 与 image 取自默认 SKU，页面组件因此不需要改动。
 */
export interface CatalogPlant {
  /** 品种主键，管理端用 */
  id: number
  /** 对外标识，路由与购物车用的都是它 */
  slug: string
  name: string
  latinName: string
  price: number
  image: string
  imageAlt: string
  category: string
  light: string
  watering: string
  size: string
  petFriendly: boolean
  petNote: string | null
  difficulty: string
  matchTags: string[]
  recommendationReason: string
  shortDescription: string
  description: string
  careTips: string[]
  featured: boolean
  /** 1 启用 0 停用。公开接口恒为 1 */
  status: number

  /*
   * 以下为品种/商品拆表后新增的字段。
   * 标成可选是因为页面尚未消费它们——推荐算法（第③步）与下单（第④步）会用到，
   * 届时按需去掉问号。
   */

  /** 默认 SKU 主键，加购与下单提交的是它而不是 slug */
  defaultSkuId?: number
  /** 该品种下全部上架 SKU，供详情页切换规格 */
  skus?: CatalogSku[]

  /** leaf 观叶 / flower 观花 */
  ornamentalType?: string
  bloomSeason?: string | null
  bloomColor?: string | null
  fragrance?: string | null

  /** 可接受光照等级区间，1 低光 / 2 柔和散射 / 3 明亮散射 / 4 充足直射 */
  lightMin?: number
  lightMax?: number
  tempMin?: number
  tempMax?: number
  humidityMin?: number
  humidityMax?: number

  /** 浇水间隔天数，越小越费心 */
  waterIntervalDays?: number
  fertilizeIntervalDays?: number
  repotIntervalMonths?: number
  pruneNeeded?: boolean
  /** 1 新手友好 / 2 需要关注 / 3 进阶养护，difficulty 即由它映射而来 */
  careLevel?: number

  /** 毒性分开返回，推荐算法可按用户实际养的宠物类型精确过滤 */
  toxicCat?: boolean
  toxicDog?: boolean
  toxicChild?: boolean
  pollenRisk?: boolean

  matureHeightCm?: number
  /** 成株冠幅厘米，空间硬过滤依据 */
  footprintCm?: number
}

/** 与后端 catalog/web/vo/SkuVO.java 对应 */
export interface CatalogSku {
  id: number
  skuCode: string
  /** 规格文案，如"中大型 · 约 75 厘米" */
  spec: string
  pot: string | null
  price: number
  /** 可售库存。为 0 时应禁用加购按钮 */
  stock: number
  image: string
  imageAlt: string
  featured: boolean
}

/** 逛植物页的筛选项，取自库中上架商品的实际取值 */
export interface PlantFacets {
  categories: string[]
  lights: string[]
}
