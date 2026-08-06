/**
 * 与后端 catalog/web/vo/PlantVO.java 对应。
 *
 * category / light / difficulty 曾是字符串字面量联合类型，改成后端驱动后
 * 这些值在运行时才从数据库来，字面量类型无法满足，因此放宽为 string。
 * 可选值改由 /api/catalog/plant-facets 提供。
 */
export interface CatalogPlant {
  /** 数据库主键，管理端用 */
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
  /** 1 上架 0 下架。公开接口恒为 1 */
  status: number
}

/** 逛植物页的筛选项，取自库中上架商品的实际取值 */
export interface PlantFacets {
  categories: string[]
  lights: string[]
}
