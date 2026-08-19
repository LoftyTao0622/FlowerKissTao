import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as profileApi from '@/modules/user/api/profile'
import type {
  SceneProfile,
  SceneProfileDraft,
  SceneProfileInput,
} from '@/modules/user/types/profile'

/** 问卷分步：与 RecommendationPage 的四个 fieldset 分组一一对应 */
export const STEP_KEYS = [
  ['placement', 'lightLevel', 'spaceLevel', 'ventilation'],
  ['tempLevel', 'humidityLevel'],
  ['hasChild', 'hasCat', 'hasDog', 'budgetMax', 'preferOrnamental'],
  ['experienceLevel', 'waterTimesWeek', 'travelFrequency', 'forgetful', 'acceptRepot'],
] as const satisfies readonly (readonly (keyof SceneProfileInput)[])[]

export const TOTAL_STEPS = STEP_KEYS.length

/** 每题未答时的提示，键与 SceneProfileInput 对齐 */
const REQUIRED_MESSAGES: Partial<Record<keyof SceneProfileInput, string>> = {
  placement: '请选择打算把植物放在哪里。',
  lightLevel: '请选择那个位置的光照条件。',
  spaceLevel: '请选择可用的摆放空间。',
  ventilation: '请选择通风状况。',
  tempLevel: '请选择常见的温度档位。',
  humidityLevel: '请选择常见的湿度档位。',
  hasChild: '请确认家中是否有幼童。',
  hasCat: '请确认家中是否养猫。',
  hasDog: '请确认家中是否养狗。',
  budgetMax: '请填写预算上限。',
  preferOrnamental: '请选择观叶还是观花。',
  experienceLevel: '请选择你的养护经验。',
  waterTimesWeek: '请选择每周能浇几次水。',
  travelFrequency: '请选择出差频率。',
  forgetful: '请确认是否容易忘记养护。',
  acceptRepot: '请选择你愿意做哪些养护操作。',
}

function emptyDraft(): SceneProfileDraft {
  return {
    sceneName: '',
    placement: null,
    lightLevel: null,
    tempLevel: null,
    humidityLevel: null,
    spaceLevel: null,
    ventilation: null,
    budgetMin: 0,
    budgetMax: null,
    preferOrnamental: null,
    hasChild: null,
    hasCat: null,
    hasDog: null,
    experienceLevel: null,
    waterTimesWeek: null,
    travelFrequency: null,
    forgetful: null,
    acceptRepot: null,
    acceptFertilize: null,
    acceptPrune: null,
  }
}

/** 已存场景转回草稿，供"编辑现有场景"用 */
function toDraft(profile: SceneProfile): SceneProfileDraft {
  return {
    sceneName: profile.sceneName,
    placement: profile.placement,
    lightLevel: profile.lightLevel,
    tempLevel: profile.tempLevel,
    humidityLevel: profile.humidityLevel,
    spaceLevel: profile.spaceLevel,
    ventilation: profile.ventilation,
    budgetMin: profile.budgetMin,
    budgetMax: profile.budgetMax,
    preferOrnamental: profile.preferOrnamental,
    hasChild: profile.hasChild,
    hasCat: profile.hasCat,
    hasDog: profile.hasDog,
    experienceLevel: profile.experienceLevel,
    waterTimesWeek: profile.waterTimesWeek,
    travelFrequency: profile.travelFrequency,
    forgetful: profile.forgetful,
    acceptRepot: profile.acceptRepot,
    acceptFertilize: profile.acceptFertilize,
    acceptPrune: profile.acceptPrune,
  }
}

export const useProfileStore = defineStore('sceneProfile', () => {
  const profiles = ref<SceneProfile[]>([])
  /** 正在编辑的场景 id；null 表示新建 */
  const editingId = ref<number | null>(null)
  const draft = ref<SceneProfileDraft>(emptyDraft())
  const step = ref(0)
  const errors = ref<Partial<Record<keyof SceneProfileInput, string>>>({})
  const loading = ref(false)
  const saving = ref(false)
  /** 保存失败的整体提示，如后端的跨字段矛盾校验 */
  const saveError = ref('')

  const isFirstStep = computed(() => step.value === 0)
  const isLastStep = computed(() => step.value === TOTAL_STEPS - 1)
  const hasErrors = computed(() => Object.keys(errors.value).length > 0)
  const editingProfile = computed(() =>
    profiles.value.find((item) => item.id === editingId.value) ?? null,
  )

  async function loadProfiles() {
    loading.value = true
    try {
      profiles.value = await profileApi.fetchMyProfiles()

      // 已有场景时默认进入默认场景的编辑态，用户一进页面就能看到自己填过的答案
      const first = profiles.value[0]
      if (first && editingId.value === null) {
        selectProfile(first.id)
      }
    } finally {
      loading.value = false
    }
  }

  /** 切到某个已存场景 */
  function selectProfile(id: number) {
    const target = profiles.value.find((item) => item.id === id)
    if (!target) return
    editingId.value = id
    draft.value = toDraft(target)
    step.value = 0
    errors.value = {}
    saveError.value = ''
  }

  /** 切到新建态 */
  function startNew() {
    editingId.value = null
    draft.value = emptyDraft()
    step.value = 0
    errors.value = {}
    saveError.value = ''
  }

  /** 用户改动某题后清掉它的错误，不必等到再次提交才消失 */
  function clearError(key: keyof SceneProfileInput) {
    if (!errors.value[key]) return
    const next = { ...errors.value }
    delete next[key]
    errors.value = next
  }

  /** 校验指定步骤，返回是否通过。错误写进 errors 供页面渲染 */
  function validateStep(index: number): boolean {
    const next: Partial<Record<keyof SceneProfileInput, string>> = {}
    // 开了 noUncheckedIndexedAccess，越界索引在类型上是 undefined，需显式兜底
    const keys = STEP_KEYS[index] ?? []

    keys.forEach((key) => {
      // 布尔题 false 是合法答案，只有 null 才算未答，不能用 falsy 判断
      if (draft.value[key] === null) {
        const message = REQUIRED_MESSAGES[key]
        if (message) next[key] = message
      }
    })

    // 预算上限单独看：0 是合法数字但不是有效预算
    if (index === 2 && draft.value.budgetMax !== null && draft.value.budgetMax <= 0) {
      next.budgetMax = '预算上限需要大于 0。'
    }

    errors.value = { ...errors.value, ...next }
    keys.forEach((key) => {
      if (!next[key]) clearError(key)
    })
    return Object.keys(next).length === 0
  }

  function nextStep(): boolean {
    if (!validateStep(step.value)) return false
    if (!isLastStep.value) step.value += 1
    return true
  }

  function prevStep() {
    if (!isFirstStep.value) step.value -= 1
  }

  function goToStep(index: number) {
    if (index >= 0 && index < TOTAL_STEPS) step.value = index
  }

  /** 校验全部步骤，跳到第一个出错的步骤。提交前调用 */
  function validateAll(): boolean {
    let firstBadStep = -1
    for (let i = 0; i < TOTAL_STEPS; i += 1) {
      if (!validateStep(i) && firstBadStep === -1) firstBadStep = i
    }
    if (firstBadStep !== -1) step.value = firstBadStep
    return firstBadStep === -1
  }

  /**
   * 草稿收窄成提交入参。
   *
   * 调用前必须先过 validateAll，此处的非空断言才成立。
   * 场景名留空时按摆放位置兜一个默认名，省得用户为了填问卷先想名字。
   */
  function toInput(): SceneProfileInput {
    const d = draft.value
    const fallbackName: Record<string, string> = {
      living_room: '客厅',
      bedroom: '卧室',
      office: '办公室',
      balcony: '阳台',
      other: '我的场景',
    }
    const name = d.sceneName?.trim() || fallbackName[d.placement as string] || '我的场景'

    return {
      sceneName: name,
      placement: d.placement!,
      lightLevel: d.lightLevel!,
      tempLevel: d.tempLevel!,
      humidityLevel: d.humidityLevel!,
      spaceLevel: d.spaceLevel!,
      ventilation: d.ventilation!,
      budgetMin: d.budgetMin ?? 0,
      budgetMax: d.budgetMax!,
      preferOrnamental: d.preferOrnamental!,
      hasChild: d.hasChild!,
      hasCat: d.hasCat!,
      hasDog: d.hasDog!,
      experienceLevel: d.experienceLevel!,
      waterTimesWeek: d.waterTimesWeek!,
      travelFrequency: d.travelFrequency!,
      forgetful: d.forgetful!,
      // 三项养护动作在问卷里是一组多选，acceptRepot 作为该组的必答代表；
      // 另两项未答时按"接受"处理，与后端 DTO 的默认语义一致
      acceptRepot: d.acceptRepot!,
      acceptFertilize: d.acceptFertilize ?? true,
      acceptPrune: d.acceptPrune ?? true,
    }
  }

  /**
   * 保存并返回保存后的场景。
   *
   * 校验不过或后端拒绝时返回 null，错误信息在 errors / saveError 里。
   */
  async function save(): Promise<SceneProfile | null> {
    saveError.value = ''
    if (!validateAll()) return null

    saving.value = true
    try {
      const payload = toInput()
      let id = editingId.value
      if (id === null) {
        id = await profileApi.createProfile(payload)
      } else {
        await profileApi.updateProfile(id, payload)
      }

      // 重新拉一次而不是本地拼：后端会派生 maxFootprintCm、careTags 与各 *Label，
      // 本地拼的话这些字段会是旧的或空的
      profiles.value = await profileApi.fetchMyProfiles()
      editingId.value = id
      return profiles.value.find((item) => item.id === id) ?? null
    } catch (error) {
      saveError.value = error instanceof Error ? error.message : '保存失败，请稍后重试'
      return null
    } finally {
      saving.value = false
    }
  }

  async function remove(id: number) {
    saveError.value = ''
    try {
      await profileApi.deleteProfile(id)
      profiles.value = await profileApi.fetchMyProfiles()
      if (editingId.value === id) {
        const first = profiles.value[0]
        if (first) selectProfile(first.id)
        else startNew()
      }
      return true
    } catch (error) {
      saveError.value = error instanceof Error ? error.message : '删除失败，请稍后重试'
      return false
    }
  }

  async function makeDefault(id: number) {
    await profileApi.setDefaultProfile(id)
    profiles.value = await profileApi.fetchMyProfiles()
  }

  /** 退出登录时清空，避免下一个账号看到上一个人的答案 */
  function reset() {
    profiles.value = []
    editingId.value = null
    draft.value = emptyDraft()
    step.value = 0
    errors.value = {}
    saveError.value = ''
  }

  return {
    profiles,
    editingId,
    editingProfile,
    draft,
    step,
    errors,
    loading,
    saving,
    saveError,
    isFirstStep,
    isLastStep,
    hasErrors,
    loadProfiles,
    selectProfile,
    startNew,
    clearError,
    validateStep,
    validateAll,
    nextStep,
    prevStep,
    goToStep,
    toInput,
    save,
    remove,
    makeDefault,
    reset,
  }
})
