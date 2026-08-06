import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export type LightLevel = 'low' | 'indirect' | 'bright'
export type SpaceType = 'desktop' | 'shelf' | 'floor'
export type PetSituation = 'none' | 'cat' | 'dog'
export type WateringHabit = 'attentive' | 'weekly' | 'forgetful'
export type PreferenceKey = 'light' | 'space' | 'pet' | 'watering'
export type RecommendationStatus = 'idle' | 'error' | 'generating' | 'success'

export interface RecommendationProfile {
  light: LightLevel | null
  space: SpaceType | null
  pet: PetSituation | null
  watering: WateringHabit | null
}

export interface RecommendationResult {
  id: string
  name: string
  latinName: string
  score: number
  difficulty: string
  tone: 'sage' | 'moss' | 'forest'
  reasons: string[]
  risk: string
}

interface CompleteProfile {
  light: LightLevel
  space: SpaceType
  pet: PetSituation
  watering: WateringHabit
}

interface PlantRule {
  id: string
  name: string
  latinName: string
  difficulty: string
  tone: RecommendationResult['tone']
  lights: LightLevel[]
  spaces: SpaceType[]
  watering: WateringHabit[]
  petFriendly: boolean
  risk: string
}

const plantRules: PlantRule[] = [
  {
    id: 'calathea',
    name: '青苹果竹芋',
    latinName: 'Goeppertia orbifolia',
    difficulty: '需要一点耐心',
    tone: 'sage',
    lights: ['low', 'indirect'],
    spaces: ['desktop', 'shelf', 'floor'],
    watering: ['attentive', 'weekly'],
    petFriendly: true,
    risk: '空气持续干燥时容易焦边，空调房建议远离出风口，并定期观察叶缘。',
  },
  {
    id: 'spider-plant',
    name: '金边吊兰',
    latinName: 'Chlorophytum comosum',
    difficulty: '新手友好',
    tone: 'moss',
    lights: ['indirect', 'bright'],
    spaces: ['desktop', 'shelf'],
    watering: ['weekly', 'forgetful'],
    petFriendly: true,
    risk: '叶尖发褐通常与积盐或空气太干有关，先检查水质和通风，不要立刻加大浇水量。',
  },
  {
    id: 'areca-palm',
    name: '散尾葵',
    latinName: 'Dypsis lutescens',
    difficulty: '规律养护',
    tone: 'forest',
    lights: ['indirect', 'bright'],
    spaces: ['floor'],
    watering: ['attentive', 'weekly'],
    petFriendly: true,
    risk: '株型展开后占地会增加，购买前请为叶片预留约 60 厘米的舒展空间。',
  },
  {
    id: 'snake-plant',
    name: '虎尾兰',
    latinName: 'Dracaena trifasciata',
    difficulty: '很省心',
    tone: 'forest',
    lights: ['low', 'indirect', 'bright'],
    spaces: ['shelf', 'floor'],
    watering: ['weekly', 'forgetful'],
    petFriendly: false,
    risk: '最常见的问题是盆土长期潮湿；有宠物或幼儿时，还应放在无法误食的位置。',
  },
  {
    id: 'pothos',
    name: '绿萝',
    latinName: 'Epipremnum aureum',
    difficulty: '新手友好',
    tone: 'moss',
    lights: ['low', 'indirect'],
    spaces: ['desktop', 'shelf'],
    watering: ['weekly', 'forgetful'],
    petFriendly: false,
    risk: '枝叶垂落后更容易被儿童或宠物触碰；若存在误食风险，应选择其他候选。',
  },
  {
    id: 'fiddle-leaf-fig',
    name: '琴叶榕',
    latinName: 'Ficus lyrata',
    difficulty: '进阶养护',
    tone: 'sage',
    lights: ['bright'],
    spaces: ['floor'],
    watering: ['attentive', 'weekly'],
    petFriendly: false,
    risk: '对频繁挪动和冷风较敏感；其汁液也可能带来刺激，请避免儿童或宠物接触。',
  },
]

const lightLabels: Record<LightLevel, string> = {
  low: '柔和弱光',
  indirect: '明亮散射光',
  bright: '充足光线',
}

const spaceLabels: Record<SpaceType, string> = {
  desktop: '桌面小空间',
  shelf: '层架或窗边',
  floor: '落地空间',
}

const wateringLabels: Record<WateringHabit, string> = {
  attentive: '愿意经常观察盆土',
  weekly: '每周固定照看',
  forgetful: '偶尔会忘记浇水',
}

export const useRecommendationStore = defineStore('recommendation', () => {
  const profile = ref<RecommendationProfile>({
    light: null,
    space: null,
    pet: null,
    watering: null,
  })
  const status = ref<RecommendationStatus>('idle')
  const errors = ref<Partial<Record<PreferenceKey, string>>>({})
  const results = ref<RecommendationResult[]>([])
  let generationId = 0

  const isGenerating = computed(() => status.value === 'generating')

  function clearError(key: PreferenceKey) {
    if (errors.value[key]) {
      const nextErrors = { ...errors.value }
      delete nextErrors[key]
      errors.value = nextErrors
      if (Object.keys(nextErrors).length === 0 && status.value === 'error') {
        status.value = 'idle'
      }
    }

    if (status.value === 'success') {
      status.value = 'idle'
      results.value = []
    }
  }

  function validate() {
    const nextErrors: Partial<Record<PreferenceKey, string>> = {}

    if (!profile.value.light) nextErrors.light = '请选择家中主要的光照条件。'
    if (!profile.value.space) nextErrors.space = '请选择计划摆放植物的空间。'
    if (!profile.value.pet) nextErrors.pet = '请选择家中是否有猫或狗。'
    if (!profile.value.watering) nextErrors.watering = '请选择最接近你的浇水习惯。'

    errors.value = nextErrors
    status.value = Object.keys(nextErrors).length > 0 ? 'error' : status.value
    return Object.keys(nextErrors).length === 0
  }

  function buildResult(rule: PlantRule, current: CompleteProfile): RecommendationResult {
    const lightMatches = rule.lights.includes(current.light)
    const spaceMatches = rule.spaces.includes(current.space)
    const wateringMatches = rule.watering.includes(current.watering)
    const petMatches = current.pet === 'none' || rule.petFriendly

    let score = 64
    score += lightMatches ? 12 : -7
    score += spaceMatches ? 9 : -6
    score += wateringMatches ? 10 : -5
    score += petMatches ? 5 : -18
    score = Math.max(58, Math.min(98, score))

    const reasons = [
      lightMatches
        ? `能适应你家的${lightLabels[current.light]}，摆放后不必频繁追着阳光移动。`
        : `更偏好其他光照，若选择它，需要先调整到更合适的位置。`,
      spaceMatches
        ? `株型与${spaceLabels[current.space]}较匹配，日常观察和清洁都更从容。`
        : `成熟株型可能超出${spaceLabels[current.space]}，需要额外预留生长空间。`,
      wateringMatches
        ? `养护节奏贴近“${wateringLabels[current.watering]}”，更容易长期坚持。`
        : `需水节奏与你的习惯有偏差，建议设置提醒并在浇水前检查盆土。`,
    ]

    if (current.pet !== 'none' && rule.petFriendly) {
      reasons.push('本轮优先保留了宠物家庭更安心的候选，但仍建议避免宠物啃咬叶片。')
    }

    return {
      id: rule.id,
      name: rule.name,
      latinName: rule.latinName,
      score,
      difficulty: rule.difficulty,
      tone: rule.tone,
      reasons,
      risk: rule.risk,
    }
  }

  async function generateRecommendations() {
    if (!validate()) return false

    const current: CompleteProfile = {
      light: profile.value.light as LightLevel,
      space: profile.value.space as SpaceType,
      pet: profile.value.pet as PetSituation,
      watering: profile.value.watering as WateringHabit,
    }
    const currentGenerationId = ++generationId
    status.value = 'generating'
    results.value = []

    await new Promise<void>((resolve) => window.setTimeout(resolve, 420))
    if (currentGenerationId !== generationId) return false

    const candidates = current.pet === 'none'
      ? plantRules
      : plantRules.filter((rule) => rule.petFriendly)

    results.value = candidates
      .map((rule) => buildResult(rule, current))
      .sort((first, second) => second.score - first.score)
      .slice(0, 3)
    status.value = 'success'
    return true
  }

  function reset() {
    generationId += 1
    profile.value = {
      light: null,
      space: null,
      pet: null,
      watering: null,
    }
    status.value = 'idle'
    errors.value = {}
    results.value = []
  }

  return {
    profile,
    status,
    errors,
    results,
    isGenerating,
    clearError,
    generateRecommendations,
    reset,
  }
})
