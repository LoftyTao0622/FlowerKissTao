/**
 * 与后端 user/web/vo/SceneProfileVO.java 及 dto/SceneProfileSaveDTO.java 对应。
 *
 * 档位用数字而非字面量联合类型，与后端的 TINYINT 一一对齐；
 * 各档位的中文说法由后端的 *Label 字段给出，前端不再自己维护一套映射，
 * 免得同一个等级在问卷页和首页卡片上出现两种措辞。
 */

/** 1 低光 / 2 柔和散射 / 3 明亮散射 / 4 充足直射，与 species.lightMin~lightMax 同刻度 */
export type LightLevelValue = 1 | 2 | 3 | 4
/** 1 偏冷 / 2 常温 / 3 偏热 */
export type TempLevelValue = 1 | 2 | 3
/** 1 干燥 / 2 适中 / 3 潮湿 */
export type HumidityLevelValue = 1 | 2 | 3
/** 1 桌面 / 2 层架 / 3 落地 */
export type SpaceLevelValue = 1 | 2 | 3
/** 1 较差 / 2 一般 / 3 良好 */
export type VentilationValue = 1 | 2 | 3
/** 1 新手 / 2 有一些 / 3 有经验 */
export type ExperienceValue = 1 | 2 | 3
/** 1 很少 / 2 偶尔 / 3 频繁 */
export type TravelValue = 1 | 2 | 3

export type Placement = 'living_room' | 'bedroom' | 'office' | 'balcony' | 'other'
export type PreferOrnamental = 'leaf' | 'flower' | 'any'

/** 读接口返回，比 DTO 多出 id、isDefault、各 *Label 与 careTags */
export interface SceneProfile {
  id: number
  sceneName: string
  isDefault: boolean

  placement: Placement
  placementLabel: string
  lightLevel: LightLevelValue
  lightLabel: string
  tempLevel: TempLevelValue
  tempLabel: string
  humidityLevel: HumidityLevelValue
  humidityLabel: string
  spaceLevel: SpaceLevelValue
  spaceLabel: string
  /** 冠幅上限厘米，由 spaceLevel 派生，前端只读不填 */
  maxFootprintCm: number
  ventilation: VentilationValue
  ventilationLabel: string
  budgetMin: number
  budgetMax: number
  preferOrnamental: PreferOrnamental
  preferOrnamentalLabel: string

  hasChild: boolean
  hasCat: boolean
  hasDog: boolean

  experienceLevel: ExperienceValue
  experienceLabel: string
  waterTimesWeek: number
  travelFrequency: TravelValue
  travelLabel: string
  forgetful: boolean
  acceptRepot: boolean
  acceptFertilize: boolean
  acceptPrune: boolean

  /** 后端派生的养护能力标签，如"新手养护""需要耐旱品种" */
  careTags: string[]
}

/** 新建与编辑的入参。不含 maxFootprintCm——它由后端按 spaceLevel 派生 */
export interface SceneProfileInput {
  sceneName: string
  placement: Placement
  lightLevel: LightLevelValue
  tempLevel: TempLevelValue
  humidityLevel: HumidityLevelValue
  spaceLevel: SpaceLevelValue
  ventilation: VentilationValue
  budgetMin: number
  budgetMax: number
  preferOrnamental: PreferOrnamental

  hasChild: boolean
  hasCat: boolean
  hasDog: boolean

  experienceLevel: ExperienceValue
  waterTimesWeek: number
  travelFrequency: TravelValue
  forgetful: boolean
  acceptRepot: boolean
  acceptFertilize: boolean
  acceptPrune: boolean
}

/**
 * 问卷填写中的草稿。
 *
 * 每一项都可为 null——用户还没答的题不能有默认值，否则"没选"和"选了第一项"
 * 分不清，必填校验也就无从谈起。提交前由 store 收窄成 SceneProfileInput。
 */
export type SceneProfileDraft = {
  [K in keyof SceneProfileInput]: SceneProfileInput[K] | null
}
