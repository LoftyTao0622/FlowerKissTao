<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import { Check, Delete, Loading, Plus, Refresh, Star, WarningFilled } from '@element-plus/icons-vue'

import { STEP_KEYS, TOTAL_STEPS, useProfileStore } from '@/modules/user/stores/profile'
import type {
  ExperienceValue,
  HumidityLevelValue,
  LightLevelValue,
  Placement,
  PreferOrnamental,
  SceneProfileInput,
  SpaceLevelValue,
  TempLevelValue,
  TravelValue,
  VentilationValue,
} from '@/modules/user/types/profile'
import { useRecommendationStore } from '../stores/recommendation'
import type { RecItem, ScoreItem } from '../types/recommendation'

interface Choice<T> {
  value: T
  label: string
  description?: string
}

const stepTitles = ['这盆植物放在哪', '那里的气候如何', '谁会靠近它', '你能付出多少'] as const

const stepLeads = [
  '位置、光线和可用空间决定了哪些植物根本活不下来。',
  '温湿度不必精确，按平时的体感选一档就好。',
  '安全条件是硬约束，预算决定候选的价格区间。',
  '诚实的节奏比理想中的勤快更有参考价值。',
] as const

const placementOptions: Choice<Placement>[] = [
  { value: 'living_room', label: '客厅', description: '活动频繁，通常有较大的落地空间' },
  { value: 'bedroom', label: '卧室', description: '光线偏柔和，更在意体积与夜间气味' },
  { value: 'office', label: '办公室', description: '工位或会议区，多为桌面尺度' },
  { value: 'balcony', label: '阳台', description: '光照最充足，但温差和风也最大' },
  { value: 'other', label: '其他位置', description: '玄关、走廊、卫生间等' },
]

const lightOptions: Choice<LightLevelValue>[] = [
  { value: 1, label: '柔和弱光', description: '离窗较远，白天无需开灯但没有明显光斑' },
  { value: 2, label: '柔和散射光', description: '有自然光但照不到，如北向房间' },
  { value: 3, label: '明亮散射光', description: '靠近窗边，光线充足但阳光不直晒' },
  { value: 4, label: '充足直射光', description: '每天能接触一段时间的直射阳光' },
]

const spaceOptions: Choice<SpaceLevelValue>[] = [
  { value: 1, label: '桌面小空间', description: '书桌、床头或面积有限的台面' },
  { value: 2, label: '层架或窗边', description: '适合中小型盆栽或自然垂落的枝叶' },
  { value: 3, label: '落地空间', description: '客厅角落、玄关等可供植株展开的位置' },
]

const ventilationOptions: Choice<VentilationValue>[] = [
  { value: 1, label: '通风较差', description: '很少开窗，空气流动不明显' },
  { value: 2, label: '通风一般', description: '每天会开窗透气一段时间' },
  { value: 3, label: '通风良好', description: '长期对流或靠近开放阳台' },
]

const tempOptions: Choice<TempLevelValue>[] = [
  { value: 1, label: '偏冷', description: '常低于 15℃，冬天没有持续供暖' },
  { value: 2, label: '常温', description: '大致维持在 15–28℃，多数室内环境' },
  { value: 3, label: '偏热', description: '常高于 28℃，或长期靠近热源' },
]

const humidityOptions: Choice<HumidityLevelValue>[] = [
  { value: 1, label: '干燥', description: '常开空调或暖气，皮肤容易发干' },
  { value: 2, label: '适中', description: '没有明显干燥或潮湿的感觉' },
  { value: 3, label: '潮湿', description: '南方回南天、浴室或靠水区域' },
]

const yesNoOptions: Choice<boolean>[] = [
  { value: true, label: '是' },
  { value: false, label: '否' },
]

const ornamentalOptions: Choice<PreferOrnamental>[] = [
  { value: 'leaf', label: '观叶为主', description: '看重叶形与叶色，四季稳定' },
  { value: 'flower', label: '希望能开花', description: '接受花期管理换取开花' },
  { value: 'any', label: '都可以', description: '按环境适配程度来推荐' },
]

const experienceOptions: Choice<ExperienceValue>[] = [
  { value: 1, label: '第一次认真养', description: '需要容错率高、不容易养死的品种' },
  { value: 2, label: '养过一些', description: '能看懂基本的缺水与徒长信号' },
  { value: 3, label: '有养护经验', description: '愿意尝试对环境要求更高的品种' },
]

const waterOptions: Choice<number>[] = [
  { value: 0, label: '几乎不浇', description: '只能依赖极耐旱的品种' },
  { value: 1, label: '每周 1 次', description: '固定在某一天集中打理' },
  { value: 3, label: '每周 2–3 次', description: '愿意隔几天检查一次盆土' },
  { value: 5, label: '每周 4 次以上', description: '几乎每天都会留意植物状态' },
]

const travelOptions: Choice<TravelValue>[] = [
  { value: 1, label: '很少出差', description: '基本每天都在家' },
  { value: 2, label: '偶尔出差', description: '一个月有几天不在' },
  { value: 3, label: '经常出差', description: '经常连续多日无人照看' },
]

const profileStore = useProfileStore()
const recommendationStore = useRecommendationStore()

const errorSummary = ref<HTMLElement | null>(null)
const resultHeading = ref<HTMLElement | null>(null)
const stepHeading = ref<HTMLElement | null>(null)
const firstChoice = ref<HTMLInputElement | null>(null)
const showSceneNameInput = ref(false)

/** 当前步骤里出错的题，用于错误摘要列表 */
const currentStepErrors = computed(() =>
  (STEP_KEYS[profileStore.step] ?? [])
    .map((key) => profileStore.errors[key])
    .filter((message): message is string => Boolean(message)),
)

const hasStepError = computed(() => currentStepErrors.value.length > 0)

/** 新建场景时让用户自己起名，编辑已有场景则沿用原名 */
const isCreating = computed(() => profileStore.editingId === null)

function describedBy(key: keyof SceneProfileInput) {
  return `${key}-hint${profileStore.errors[key] ? ` ${key}-error` : ''}`
}

function onAnswer(key: keyof SceneProfileInput) {
  profileStore.clearError(key)
  // 改了条件说明上一轮结果已过时，清掉，免得用户以为它对应新答案
  if (recommendationStore.status === 'success') recommendationStore.reset()
}

/**
 * 结果卡片的配色。品种 id 取模，同一株每次进来颜色一致，
 * 又不必给 18 个品种各配一个色值。
 */
const CARD_TONES = ['sage', 'moss', 'forest'] as const

function cardTone(item: RecItem) {
  return CARD_TONES[item.speciesId % CARD_TONES.length]
}

/**
 * 后端返回的商品链接。品种在生成推荐后被下架时 slug 为 null，
 * 这时退回商品列表页而不是给一个死链。
 */
function itemDestination(item: RecItem) {
  return item.slug
    ? { name: 'plant-detail', params: { plantId: item.slug } }
    : { name: 'plant-catalog' }
}

/** 分项条的宽度。score 为 null 只出现在权重列表里，结果卡片里不会遇到 */
function scoreWidth(score: ScoreItem) {
  return `${score.score ?? 0}%`
}

/**
 * 最低分维度映射到知识库的任务类型。
 *
 * 方案要求可从推荐理由直接跳到对应指南：不是随便给一个知识库入口，
 * 而是针对这株的短板告诉用户"怎么改善"。
 */
function weakestGuideTaskType(item: RecItem) {
  const weakest = [...item.scores]
    .filter((score) => score.score !== null)
    .sort((a, b) => (a.score ?? 0) - (b.score ?? 0))[0]
  const mapping: Record<string, string> = {
    light: 'rotate',
    humidity: 'water',
    care: 'water',
    space: 'repot',
    budget: 'repot',
    preference: 'prune',
    temp: 'water',
  }
  return mapping[weakest?.code ?? 'care'] ?? 'water'
}

/** 点进详情页前先埋点。失败也照常跳转，store 里已吞掉异常 */
function onItemClick(item: RecItem) {
  void recommendationStore.trackClick(item.itemId)
}

function setFirstChoice(element: Element | ComponentPublicInstance | null) {
  if (element instanceof HTMLInputElement) firstChoice.value = element
}

/** 换步后把焦点交给新的步骤标题，读屏用户才知道内容整块换了 */
async function focusStepHeading() {
  await nextTick()
  stepHeading.value?.focus()
}

async function goNext() {
  if (!profileStore.nextStep()) {
    await nextTick()
    errorSummary.value?.focus()
    return
  }
  await focusStepHeading()
}

async function goPrev() {
  profileStore.prevStep()
  await focusStepHeading()
}

async function submit() {
  const saved = await profileStore.save()
  if (!saved) {
    await nextTick()
    errorSummary.value?.focus()
    return
  }

  // 画像已落库，再让后端按它算推荐。算法全部在服务端，前端不做任何打分
  await recommendationStore.generate(saved.id)
  await nextTick()
  resultHeading.value?.focus()
}

async function switchScene(id: number) {
  profileStore.selectProfile(id)
  recommendationStore.reset()
  await nextTick()
  firstChoice.value?.focus()
}

async function newScene() {
  profileStore.startNew()
  recommendationStore.reset()
  await nextTick()
  firstChoice.value?.focus()
}

async function removeScene(id: number) {
  await profileStore.remove(id)
  recommendationStore.reset()
}

async function makeDefault(id: number) {
  await profileStore.makeDefault(id)
}

// 新建时展开命名输入框，切到已有场景时收起
watch(isCreating, (creating) => {
  showSceneNameInput.value = creating
}, { immediate: true })

onMounted(async () => {
  await profileStore.loadProfiles()
  if (profileStore.profiles.length === 0) profileStore.startNew()
})
</script>

<template>
  <div class="recommendation-page">
    <section class="recommendation-hero section-shell" aria-labelledby="recommendation-title">
      <div class="container recommendation-hero__layout">
        <div class="recommendation-hero__copy">
          <p class="recommendation-hero__eyebrow">环境画像 · 分步问卷</p>
          <h1 id="recommendation-title">找到与你一起长大的那一盆</h1>
          <p class="recommendation-hero__lead">
            不从“好不好看”开始，而是先理解你的光线、空间和生活节奏。四步问答，换来一份说得清理由的植物清单。
          </p>
        </div>

        <aside class="recommendation-hero__note" aria-label="推荐原则">
          <span class="recommendation-hero__note-index" aria-hidden="true">4</span>
          <div>
            <strong>答案会保存在你的账号里</strong>
            <p>可以为客厅、卧室分别建立场景，随时回来修改。结果是养护起点，不替代实际环境观察。</p>
          </div>
        </aside>
      </div>
    </section>

    <section class="section-shell recommendation-workspace" aria-labelledby="profile-form-title">
      <div class="container recommendation-workspace__layout">
        <form class="profile-form surface-card" novalidate @submit.prevent="submit">
          <header class="profile-form__header">
            <div>
              <p class="section-kicker">环境档案</p>
              <h2 id="profile-form-title">你和植物会怎样相处？</h2>
            </div>
            <p class="profile-form__step">第 {{ profileStore.step + 1 }} / {{ TOTAL_STEPS }} 步</p>
          </header>

          <!-- 场景切换：有已存场景才显示，新用户不该被一个空列表干扰 -->
          <div v-if="profileStore.profiles.length > 0" class="scene-bar">
            <p id="scene-bar-label" class="scene-bar__label">我的场景</p>
            <ul class="scene-bar__list" aria-labelledby="scene-bar-label">
              <li v-for="scene in profileStore.profiles" :key="scene.id">
                <button
                  type="button"
                  :class="['scene-chip', { 'scene-chip--active': scene.id === profileStore.editingId }]"
                  :aria-current="scene.id === profileStore.editingId ? 'true' : undefined"
                  @click="switchScene(scene.id)"
                >
                  <el-icon v-if="scene.isDefault" class="scene-chip__star" aria-hidden="true"><Star /></el-icon>
                  <span>{{ scene.sceneName }}</span>
                  <span v-if="scene.isDefault" class="sr-only">（默认场景）</span>
                </button>
              </li>
              <li>
                <button
                  type="button"
                  :class="['scene-chip', 'scene-chip--new', { 'scene-chip--active': isCreating }]"
                  @click="newScene"
                >
                  <el-icon aria-hidden="true"><Plus /></el-icon>
                  <span>新建场景</span>
                </button>
              </li>
            </ul>

            <div v-if="profileStore.editingProfile" class="scene-bar__actions">
              <button
                v-if="!profileStore.editingProfile.isDefault"
                type="button"
                class="scene-action"
                @click="makeDefault(profileStore.editingProfile.id)"
              >
                <el-icon aria-hidden="true"><Star /></el-icon>
                设为默认
              </button>
              <button
                type="button"
                class="scene-action scene-action--danger"
                @click="removeScene(profileStore.editingProfile.id)"
              >
                <el-icon aria-hidden="true"><Delete /></el-icon>
                删除这个场景
              </button>
            </div>
          </div>

          <!-- aria-current="step" 让读屏用户知道自己在第几步 -->
          <ol class="stepper" aria-label="问卷进度">
            <li
              v-for="(title, index) in stepTitles"
              :key="title"
              :class="['stepper__item', {
                'stepper__item--active': index === profileStore.step,
                'stepper__item--done': index < profileStore.step,
              }]"
              :aria-current="index === profileStore.step ? 'step' : undefined"
            >
              <span class="stepper__index" aria-hidden="true">{{ index + 1 }}</span>
              <span class="stepper__title">{{ title }}</span>
            </li>
          </ol>

          <div
            v-if="hasStepError || profileStore.saveError"
            ref="errorSummary"
            class="form-alert"
            role="alert"
            tabindex="-1"
          >
            <el-icon aria-hidden="true"><WarningFilled /></el-icon>
            <div>
              <strong>{{ profileStore.saveError ? '这些答案凑不到一起' : '还差一点信息' }}</strong>
              <p v-if="profileStore.saveError">{{ profileStore.saveError }}</p>
              <ul v-else class="form-alert__list">
                <li v-for="message in currentStepErrors" :key="message">{{ message }}</li>
              </ul>
            </div>
          </div>

          <div class="profile-form__step-head">
            <h3 ref="stepHeading" tabindex="-1">{{ stepTitles[profileStore.step] }}</h3>
            <p>{{ stepLeads[profileStore.step] }}</p>
          </div>

          <div class="profile-form__groups">
            <!-- ================= STEP 1 ================= -->
            <template v-if="profileStore.step === 0">
              <fieldset v-if="showSceneNameInput" class="choice-group" :disabled="profileStore.saving">
                <legend><span>·</span> 给这个场景起个名字</legend>
                <p id="sceneName-hint" class="choice-group__hint">留空的话按摆放位置自动命名，之后也能改。</p>
                <input
                  v-model="profileStore.draft.sceneName"
                  class="scene-name-input"
                  type="text"
                  maxlength="20"
                  placeholder="例如：朝南的客厅"
                  aria-describedby="sceneName-hint"
                >
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.placement }]" :disabled="profileStore.saving">
                <legend><span>01</span> 打算把它放在哪个房间？</legend>
                <p id="placement-hint" class="choice-group__hint">同一个家里，客厅和卧室的条件可能完全不同。</p>
                <div class="choice-grid">
                  <label v-for="(option, index) in placementOptions" :key="option.value" class="choice-card">
                    <input
                      :ref="index === 0 ? setFirstChoice : undefined"
                      v-model="profileStore.draft.placement"
                      type="radio"
                      name="placement"
                      :value="option.value"
                      :aria-describedby="describedBy('placement')"
                      :aria-invalid="Boolean(profileStore.errors.placement)"
                      @change="onAnswer('placement')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>{{ option.label }}</strong>
                      <small>{{ option.description }}</small>
                    </span>
                  </label>
                </div>
                <p v-if="profileStore.errors.placement" id="placement-error" class="choice-group__error">
                  {{ profileStore.errors.placement }}
                </p>
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.lightLevel }]" :disabled="profileStore.saving">
                <legend><span>02</span> 那个位置的光线如何？</legend>
                <p id="lightLevel-hint" class="choice-group__hint">回想植物计划摆放处在白天最常见的状态。</p>
                <div class="choice-grid">
                  <label v-for="option in lightOptions" :key="option.value" class="choice-card">
                    <input
                      v-model="profileStore.draft.lightLevel"
                      type="radio"
                      name="lightLevel"
                      :value="option.value"
                      :aria-describedby="describedBy('lightLevel')"
                      :aria-invalid="Boolean(profileStore.errors.lightLevel)"
                      @change="onAnswer('lightLevel')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>{{ option.label }}</strong>
                      <small>{{ option.description }}</small>
                    </span>
                  </label>
                </div>
                <p v-if="profileStore.errors.lightLevel" id="lightLevel-error" class="choice-group__error">
                  {{ profileStore.errors.lightLevel }}
                </p>
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.spaceLevel }]" :disabled="profileStore.saving">
                <legend><span>03</span> 有多大的空间留给它？</legend>
                <p id="spaceLevel-hint" class="choice-group__hint">按成熟后的植物体量来选，而不只是刚买回家时的大小。</p>
                <div class="choice-grid">
                  <label v-for="option in spaceOptions" :key="option.value" class="choice-card">
                    <input
                      v-model="profileStore.draft.spaceLevel"
                      type="radio"
                      name="spaceLevel"
                      :value="option.value"
                      :aria-describedby="describedBy('spaceLevel')"
                      :aria-invalid="Boolean(profileStore.errors.spaceLevel)"
                      @change="onAnswer('spaceLevel')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>{{ option.label }}</strong>
                      <small>{{ option.description }}</small>
                    </span>
                  </label>
                </div>
                <p v-if="profileStore.errors.spaceLevel" id="spaceLevel-error" class="choice-group__error">
                  {{ profileStore.errors.spaceLevel }}
                </p>
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.ventilation }]" :disabled="profileStore.saving">
                <legend><span>04</span> 那里的通风状况怎样？</legend>
                <p id="ventilation-hint" class="choice-group__hint">通风差的位置更容易积水烂根和滋生病虫害。</p>
                <div class="choice-grid">
                  <label v-for="option in ventilationOptions" :key="option.value" class="choice-card">
                    <input
                      v-model="profileStore.draft.ventilation"
                      type="radio"
                      name="ventilation"
                      :value="option.value"
                      :aria-describedby="describedBy('ventilation')"
                      :aria-invalid="Boolean(profileStore.errors.ventilation)"
                      @change="onAnswer('ventilation')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>{{ option.label }}</strong>
                      <small>{{ option.description }}</small>
                    </span>
                  </label>
                </div>
                <p v-if="profileStore.errors.ventilation" id="ventilation-error" class="choice-group__error">
                  {{ profileStore.errors.ventilation }}
                </p>
              </fieldset>
            </template>

            <!-- ================= STEP 2 ================= -->
            <template v-else-if="profileStore.step === 1">
              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.tempLevel }]" :disabled="profileStore.saving">
                <legend><span>05</span> 那里全年大致的温度？</legend>
                <p id="tempLevel-hint" class="choice-group__hint">按体感选一档即可，不需要真的去量。</p>
                <div class="choice-grid">
                  <label v-for="(option, index) in tempOptions" :key="option.value" class="choice-card">
                    <input
                      :ref="index === 0 ? setFirstChoice : undefined"
                      v-model="profileStore.draft.tempLevel"
                      type="radio"
                      name="tempLevel"
                      :value="option.value"
                      :aria-describedby="describedBy('tempLevel')"
                      :aria-invalid="Boolean(profileStore.errors.tempLevel)"
                      @change="onAnswer('tempLevel')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>{{ option.label }}</strong>
                      <small>{{ option.description }}</small>
                    </span>
                  </label>
                </div>
                <p v-if="profileStore.errors.tempLevel" id="tempLevel-error" class="choice-group__error">
                  {{ profileStore.errors.tempLevel }}
                </p>
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.humidityLevel }]" :disabled="profileStore.saving">
                <legend><span>06</span> 空气偏干还是偏潮？</legend>
                <p id="humidityLevel-hint" class="choice-group__hint">长期开空调或暖气的房间通常偏干。</p>
                <div class="choice-grid">
                  <label v-for="option in humidityOptions" :key="option.value" class="choice-card">
                    <input
                      v-model="profileStore.draft.humidityLevel"
                      type="radio"
                      name="humidityLevel"
                      :value="option.value"
                      :aria-describedby="describedBy('humidityLevel')"
                      :aria-invalid="Boolean(profileStore.errors.humidityLevel)"
                      @change="onAnswer('humidityLevel')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>{{ option.label }}</strong>
                      <small>{{ option.description }}</small>
                    </span>
                  </label>
                </div>
                <p v-if="profileStore.errors.humidityLevel" id="humidityLevel-error" class="choice-group__error">
                  {{ profileStore.errors.humidityLevel }}
                </p>
              </fieldset>
            </template>

            <!-- ================= STEP 3 ================= -->
            <template v-else-if="profileStore.step === 2">
              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.hasChild }]" :disabled="profileStore.saving">
                <legend><span>07</span> 家里有会误食的幼童吗？</legend>
                <p id="hasChild-hint" class="choice-group__hint">有的话，我们会直接排除误食有风险的品种。</p>
                <div class="choice-grid choice-grid--compact">
                  <label v-for="(option, index) in yesNoOptions" :key="String(option.value)" class="choice-card">
                    <input
                      :ref="index === 0 ? setFirstChoice : undefined"
                      v-model="profileStore.draft.hasChild"
                      type="radio"
                      name="hasChild"
                      :value="option.value"
                      :aria-describedby="describedBy('hasChild')"
                      :aria-invalid="Boolean(profileStore.errors.hasChild)"
                      @change="onAnswer('hasChild')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body"><strong>{{ option.label }}</strong></span>
                  </label>
                </div>
                <p v-if="profileStore.errors.hasChild" id="hasChild-error" class="choice-group__error">
                  {{ profileStore.errors.hasChild }}
                </p>
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.hasCat }]" :disabled="profileStore.saving">
                <legend><span>08</span> 家里养猫吗？</legend>
                <p id="hasCat-hint" class="choice-group__hint">对猫有毒和对狗有毒的植物并非同一批，所以分开问。</p>
                <div class="choice-grid choice-grid--compact">
                  <label v-for="option in yesNoOptions" :key="String(option.value)" class="choice-card">
                    <input
                      v-model="profileStore.draft.hasCat"
                      type="radio"
                      name="hasCat"
                      :value="option.value"
                      :aria-describedby="describedBy('hasCat')"
                      :aria-invalid="Boolean(profileStore.errors.hasCat)"
                      @change="onAnswer('hasCat')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body"><strong>{{ option.label }}</strong></span>
                  </label>
                </div>
                <p v-if="profileStore.errors.hasCat" id="hasCat-error" class="choice-group__error">
                  {{ profileStore.errors.hasCat }}
                </p>
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.hasDog }]" :disabled="profileStore.saving">
                <legend><span>09</span> 家里养狗吗？</legend>
                <p id="hasDog-hint" class="choice-group__hint">同样用于安全过滤，不影响其他维度的评分。</p>
                <div class="choice-grid choice-grid--compact">
                  <label v-for="option in yesNoOptions" :key="String(option.value)" class="choice-card">
                    <input
                      v-model="profileStore.draft.hasDog"
                      type="radio"
                      name="hasDog"
                      :value="option.value"
                      :aria-describedby="describedBy('hasDog')"
                      :aria-invalid="Boolean(profileStore.errors.hasDog)"
                      @change="onAnswer('hasDog')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body"><strong>{{ option.label }}</strong></span>
                  </label>
                </div>
                <p v-if="profileStore.errors.hasDog" id="hasDog-error" class="choice-group__error">
                  {{ profileStore.errors.hasDog }}
                </p>
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.budgetMax }]" :disabled="profileStore.saving">
                <legend><span>10</span> 单株预算大概到多少？</legend>
                <p id="budgetMax-hint" class="choice-group__hint">超出预算的商品会被直接排除，不参与排序。</p>
                <div class="budget-row">
                  <label class="budget-field">
                    <span>最低</span>
                    <input
                      v-model.number="profileStore.draft.budgetMin"
                      type="number"
                      min="0"
                      step="10"
                      aria-label="预算下限，单位元"
                    >
                    <span aria-hidden="true">元</span>
                  </label>
                  <span class="budget-sep" aria-hidden="true">—</span>
                  <label class="budget-field">
                    <span>最高</span>
                    <input
                      v-model.number="profileStore.draft.budgetMax"
                      type="number"
                      min="0"
                      step="10"
                      aria-label="预算上限，单位元"
                      :aria-describedby="describedBy('budgetMax')"
                      :aria-invalid="Boolean(profileStore.errors.budgetMax)"
                      @change="onAnswer('budgetMax')"
                    >
                    <span aria-hidden="true">元</span>
                  </label>
                </div>
                <p v-if="profileStore.errors.budgetMax" id="budgetMax-error" class="choice-group__error">
                  {{ profileStore.errors.budgetMax }}
                </p>
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.preferOrnamental }]" :disabled="profileStore.saving">
                <legend><span>11</span> 更想要观叶还是观花？</legend>
                <p id="preferOrnamental-hint" class="choice-group__hint">开花通常意味着更多的光照与养护投入。</p>
                <div class="choice-grid">
                  <label v-for="option in ornamentalOptions" :key="option.value" class="choice-card">
                    <input
                      v-model="profileStore.draft.preferOrnamental"
                      type="radio"
                      name="preferOrnamental"
                      :value="option.value"
                      :aria-describedby="describedBy('preferOrnamental')"
                      :aria-invalid="Boolean(profileStore.errors.preferOrnamental)"
                      @change="onAnswer('preferOrnamental')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>{{ option.label }}</strong>
                      <small>{{ option.description }}</small>
                    </span>
                  </label>
                </div>
                <p v-if="profileStore.errors.preferOrnamental" id="preferOrnamental-error" class="choice-group__error">
                  {{ profileStore.errors.preferOrnamental }}
                </p>
              </fieldset>
            </template>

            <!-- ================= STEP 4 ================= -->
            <template v-else>
              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.experienceLevel }]" :disabled="profileStore.saving">
                <legend><span>12</span> 你养植物的经验如何？</legend>
                <p id="experienceLevel-hint" class="choice-group__hint">没有标准答案，如实选择才能匹配到合适的难度。</p>
                <div class="choice-grid">
                  <label v-for="(option, index) in experienceOptions" :key="option.value" class="choice-card">
                    <input
                      :ref="index === 0 ? setFirstChoice : undefined"
                      v-model="profileStore.draft.experienceLevel"
                      type="radio"
                      name="experienceLevel"
                      :value="option.value"
                      :aria-describedby="describedBy('experienceLevel')"
                      :aria-invalid="Boolean(profileStore.errors.experienceLevel)"
                      @change="onAnswer('experienceLevel')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>{{ option.label }}</strong>
                      <small>{{ option.description }}</small>
                    </span>
                  </label>
                </div>
                <p v-if="profileStore.errors.experienceLevel" id="experienceLevel-error" class="choice-group__error">
                  {{ profileStore.errors.experienceLevel }}
                </p>
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.waterTimesWeek }]" :disabled="profileStore.saving">
                <legend><span>13</span> 每周能照看几次？</legend>
                <p id="waterTimesWeek-hint" class="choice-group__hint">诚实的节奏比理想中的勤快更有参考价值。</p>
                <div class="choice-grid">
                  <label v-for="option in waterOptions" :key="option.value" class="choice-card">
                    <input
                      v-model="profileStore.draft.waterTimesWeek"
                      type="radio"
                      name="waterTimesWeek"
                      :value="option.value"
                      :aria-describedby="describedBy('waterTimesWeek')"
                      :aria-invalid="Boolean(profileStore.errors.waterTimesWeek)"
                      @change="onAnswer('waterTimesWeek')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>{{ option.label }}</strong>
                      <small>{{ option.description }}</small>
                    </span>
                  </label>
                </div>
                <p v-if="profileStore.errors.waterTimesWeek" id="waterTimesWeek-error" class="choice-group__error">
                  {{ profileStore.errors.waterTimesWeek }}
                </p>
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.travelFrequency }]" :disabled="profileStore.saving">
                <legend><span>14</span> 会经常出差或长期离家吗？</legend>
                <p id="travelFrequency-hint" class="choice-group__hint">连续多日无人照看时，耐旱程度会成为关键。</p>
                <div class="choice-grid">
                  <label v-for="option in travelOptions" :key="option.value" class="choice-card">
                    <input
                      v-model="profileStore.draft.travelFrequency"
                      type="radio"
                      name="travelFrequency"
                      :value="option.value"
                      :aria-describedby="describedBy('travelFrequency')"
                      :aria-invalid="Boolean(profileStore.errors.travelFrequency)"
                      @change="onAnswer('travelFrequency')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>{{ option.label }}</strong>
                      <small>{{ option.description }}</small>
                    </span>
                  </label>
                </div>
                <p v-if="profileStore.errors.travelFrequency" id="travelFrequency-error" class="choice-group__error">
                  {{ profileStore.errors.travelFrequency }}
                </p>
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.forgetful }]" :disabled="profileStore.saving">
                <legend><span>15</span> 你容易忘记养护吗？</legend>
                <p id="forgetful-hint" class="choice-group__hint">承认容易忘并不丢人，它会显著改变推荐结果。</p>
                <div class="choice-grid choice-grid--compact">
                  <label v-for="option in yesNoOptions" :key="String(option.value)" class="choice-card">
                    <input
                      v-model="profileStore.draft.forgetful"
                      type="radio"
                      name="forgetful"
                      :value="option.value"
                      :aria-describedby="describedBy('forgetful')"
                      :aria-invalid="Boolean(profileStore.errors.forgetful)"
                      @change="onAnswer('forgetful')"
                    >
                    <span class="choice-card__control" aria-hidden="true"></span>
                    <span class="choice-card__body"><strong>{{ option.label }}</strong></span>
                  </label>
                </div>
                <p v-if="profileStore.errors.forgetful" id="forgetful-error" class="choice-group__error">
                  {{ profileStore.errors.forgetful }}
                </p>
              </fieldset>

              <fieldset :class="['choice-group', { 'choice-group--error': profileStore.errors.acceptRepot }]" :disabled="profileStore.saving">
                <legend><span>16</span> 愿意做哪些养护操作？</legend>
                <p id="acceptRepot-hint" class="choice-group__hint">可多选。都不选的话，我们只推荐几乎免打理的品种。</p>
                <div class="choice-grid choice-grid--compact">
                  <label class="choice-card">
                    <input
                      v-model="profileStore.draft.acceptRepot"
                      type="checkbox"
                      :aria-describedby="describedBy('acceptRepot')"
                      @change="onAnswer('acceptRepot')"
                    >
                    <span class="choice-card__control choice-card__control--box" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>换盆</strong>
                      <small>每 1–2 年一次，需要动土</small>
                    </span>
                  </label>
                  <label class="choice-card">
                    <input v-model="profileStore.draft.acceptFertilize" type="checkbox">
                    <span class="choice-card__control choice-card__control--box" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>施肥</strong>
                      <small>生长期每月一次薄肥</small>
                    </span>
                  </label>
                  <label class="choice-card">
                    <input v-model="profileStore.draft.acceptPrune" type="checkbox">
                    <span class="choice-card__control choice-card__control--box" aria-hidden="true"></span>
                    <span class="choice-card__body">
                      <strong>修剪</strong>
                      <small>定期剪除枯枝与徒长枝</small>
                    </span>
                  </label>
                </div>
                <p v-if="profileStore.errors.acceptRepot" id="acceptRepot-error" class="choice-group__error">
                  {{ profileStore.errors.acceptRepot }}
                </p>
              </fieldset>
            </template>
          </div>

          <footer class="profile-form__actions">
            <button
              v-if="!profileStore.isFirstStep"
              class="profile-form__reset"
              type="button"
              :disabled="profileStore.saving"
              @click="goPrev"
            >
              上一步
            </button>

            <button
              v-if="!profileStore.isLastStep"
              class="pill-button profile-form__submit"
              type="button"
              @click="goNext"
            >
              下一步
            </button>

            <button
              v-else
              class="pill-button profile-form__submit"
              type="submit"
              :disabled="profileStore.saving || recommendationStore.isGenerating"
            >
              <el-icon v-if="profileStore.saving || recommendationStore.isGenerating" class="is-loading" aria-hidden="true"><Loading /></el-icon>
              <Check v-else class="profile-form__submit-icon" aria-hidden="true" />
              {{ profileStore.saving ? '正在保存…' : recommendationStore.isGenerating ? '正在匹配…' : '保存并生成植物清单' }}
            </button>
          </footer>
        </form>

        <p class="recommendation-status" role="status" aria-live="polite" aria-atomic="true">
          <template v-if="profileStore.saving">正在保存环境档案。</template>
          <template v-else-if="recommendationStore.isGenerating">正在生成植物推荐。</template>
          <template v-else-if="recommendationStore.status === 'error'">
            推荐生成失败：{{ recommendationStore.errorMessage }}
          </template>
          <template v-else-if="recommendationStore.isEmptyResult">
            当前条件下没有找到合适的植物，下方列出了各条筛选各排除了多少株。
          </template>
          <template v-else-if="recommendationStore.status === 'success'">
            推荐完成，从 {{ recommendationStore.result?.totalCount }} 个品种里筛出
            {{ recommendationStore.result?.candidateCount }} 株，展示前
            {{ recommendationStore.items.length }} 株。
          </template>
          <template v-else>当前是第 {{ profileStore.step + 1 }} 步，共 {{ TOTAL_STEPS }} 步。</template>
        </p>

        <aside class="result-panel" :aria-busy="recommendationStore.isGenerating">
          <div v-if="recommendationStore.status === 'idle'" class="result-empty">
            <div class="result-empty__leaves" aria-hidden="true"><i></i><i></i><i></i></div>
            <p class="section-kicker">等待你的答案</p>
            <h2>好的推荐，应该先懂你的生活</h2>
            <p>完成四步问答后，这里会出现匹配候选、每一项的得分依据和不可忽略的养护风险。</p>
            <ul>
              <li><span>01</span> 不因热门程度加分</li>
              <li><span>02</span> 宠物与儿童安全优先筛选</li>
              <li><span>03</span> 明确展示不完美之处</li>
            </ul>
          </div>

          <div v-else-if="recommendationStore.status === 'generating'" class="result-generating">
            <div class="result-generating__mark" aria-hidden="true">
              <span></span><span></span><span></span>
            </div>
            <p class="section-kicker">正在计算</p>
            <h2>把环境条件放在一起比较…</h2>
            <p>正在核对光照、成熟株型、浇水容错和安全条件。</p>
          </div>

          <div v-else-if="recommendationStore.status === 'error'" class="result-failed" role="alert">
            <el-icon class="result-failed__icon" aria-hidden="true"><WarningFilled /></el-icon>
            <p class="section-kicker">没能算出来</p>
            <h2 ref="resultHeading" tabindex="-1">推荐生成失败</h2>
            <p>{{ recommendationStore.errorMessage }}</p>
            <button class="result-success__restart" type="button" @click="submit">
              <el-icon aria-hidden="true"><Refresh /></el-icon>
              重试
            </button>
          </div>

          <!--
            无候选。方案原文要求"无候选时仅建议放宽非安全约束"，所以这里把漏斗
            摊开给用户看，并且安全约束那两条明确标注"不建议放宽"——它们不是
            可以商量的条件。
          -->
          <div v-else-if="recommendationStore.isEmptyResult" class="result-none">
            <header class="result-none__header">
              <p class="section-kicker">没有合适的候选</p>
              <h2 ref="resultHeading" tabindex="-1">当前条件下，18 株里一株都没留下</h2>
              <p>
                这不是没有结果，而是条件互相冲突。下面是
                {{ recommendationStore.result?.totalCount }} 个品种各被哪一条筛掉的。
              </p>
            </header>

            <ul class="funnel-list">
              <li
                v-for="diagnostic in recommendationStore.activeDiagnostics"
                :key="diagnostic.code"
                :class="{ 'funnel-list__item--locked': !diagnostic.relaxable }"
              >
                <div class="funnel-list__head">
                  <span class="funnel-list__label">{{ diagnostic.label }}</span>
                  <span class="funnel-list__count">排除 {{ diagnostic.excluded }} 株</span>
                </div>
                <div
                  class="funnel-list__bar"
                  role="progressbar"
                  :aria-label="`${diagnostic.label}排除的品种数`"
                  aria-valuemin="0"
                  :aria-valuemax="recommendationStore.result?.totalCount ?? 0"
                  :aria-valuenow="diagnostic.excluded"
                >
                  <span
                    :style="{
                      width: `${(diagnostic.excluded / (recommendationStore.result?.totalCount || 1)) * 100}%`,
                    }"
                  ></span>
                </div>
                <p v-if="!diagnostic.relaxable" class="funnel-list__locked">
                  <el-icon aria-hidden="true"><WarningFilled /></el-icon>
                  安全条件，不建议放宽
                </p>
              </li>
            </ul>

            <div v-if="recommendationStore.result?.suggestions.length" class="result-none__advice">
              <h3>可以试试这些调整</h3>
              <ul>
                <li v-for="advice in recommendationStore.result.suggestions" :key="advice">
                  <el-icon aria-hidden="true"><Check /></el-icon>
                  <span>{{ advice }}</span>
                </li>
              </ul>
            </div>

            <button class="result-success__restart" type="button" @click="profileStore.goToStep(0)">
              <el-icon aria-hidden="true"><Refresh /></el-icon>
              回到第一步修改条件
            </button>
          </div>

          <div v-else class="result-success">
            <header class="result-success__header">
              <div>
                <p class="section-kicker">匹配完成</p>
                <h2 ref="resultHeading" tabindex="-1">
                  更适合你的 {{ recommendationStore.items.length }} 种植物
                </h2>
              </div>
              <span class="result-success__count">
                {{ recommendationStore.result?.candidateCount }} 株候选
              </span>
            </header>
            <p class="result-success__intro">
              匹配度用于比较当前候选，不代表植物一定存活；收到植物后仍需根据盆土和叶片状态调整。
              每一项得分后的百分比是它在总分里的权重。
            </p>

            <ol class="result-list">
              <li v-for="(item, index) in recommendationStore.items" :key="item.itemId">
                <article class="result-card">
                  <img
                    v-if="item.image"
                    class="result-card__image"
                    :src="item.image"
                    :alt="item.imageAlt ?? item.name ?? ''"
                    loading="lazy"
                  />
                  <div
                    v-else
                    :class="['result-card__portrait', `result-card__portrait--${cardTone(item)}`]"
                    aria-hidden="true"
                  >
                    <span>{{ (item.name ?? '?').slice(0, 1) }}</span>
                    <i></i>
                  </div>

                  <div class="result-card__content">
                    <header class="result-card__header">
                      <div>
                        <p>
                          推荐 {{ String(index + 1).padStart(2, '0') }}
                          <template v-if="item.careLevelLabel"> · {{ item.careLevelLabel }}</template>
                          <template v-if="item.petFriendly"> · 猫狗无毒</template>
                        </p>
                        <h3>{{ item.name ?? '该品种已下架' }}</h3>
                        <small>{{ item.latinName }}</small>
                      </div>
                      <div class="result-card__score" :aria-label="`匹配度 ${item.totalScore} 分`">
                        <strong>{{ item.totalScore }}</strong><span>/100</span>
                      </div>
                    </header>

                    <div
                      class="result-card__progress"
                      role="progressbar"
                      aria-label="综合匹配度"
                      aria-valuemin="0"
                      aria-valuemax="100"
                      :aria-valuenow="item.totalScore"
                    >
                      <span :style="{ width: `${item.totalScore}%` }"></span>
                    </div>

                    <!--
                      分项得分。只给一个总分的话，用户没法判断"95 分"是光照贴合
                      还是单纯便宜——方案要求展示"分项匹配度"，指的就是这一块。
                    -->
                    <div class="result-card__scores">
                      <h4>各项匹配度</h4>
                      <ul>
                        <li v-for="score in item.scores" :key="score.code">
                          <span class="score-row__label">{{ score.label }}</span>
                          <span
                            class="score-row__bar"
                            role="progressbar"
                            :aria-label="`${score.label}匹配度，权重 ${score.weight}%`"
                            aria-valuemin="0"
                            aria-valuemax="100"
                            :aria-valuenow="score.score ?? 0"
                          >
                            <i :style="{ width: scoreWidth(score) }"></i>
                          </span>
                          <span class="score-row__value">
                            {{ score.score }}
                            <small>权重 {{ score.weight }}%</small>
                          </span>
                        </li>
                      </ul>
                    </div>

                    <div class="result-card__reasons">
                      <h4>为什么适合</h4>
                      <ul>
                        <li v-for="reason in item.reasons" :key="reason">
                          <el-icon aria-hidden="true"><Check /></el-icon>
                          <span>{{ reason }}</span>
                        </li>
                      </ul>
                      <!-- 方案要求可从推荐理由跳到对应指南。针对最低分短板，不是泛泛跳首页 -->
                      <RouterLink
                        class="result-card__guide"
                        :to="{
                          name: 'knowledge',
                          query: { taskType: weakestGuideTaskType(item) },
                        }"
                      >
                        了解如何改善这项短板 →
                      </RouterLink>
                    </div>

                    <div v-if="item.risk" class="result-card__risk">
                      <el-icon aria-hidden="true"><WarningFilled /></el-icon>
                      <p><strong>选择前请留意：</strong>{{ item.risk }}</p>
                    </div>

                    <footer class="result-card__footer">
                      <p v-if="item.price !== null" class="result-card__price">
                        <strong>¥{{ item.price }}</strong>
                        <span v-if="item.spec">{{ item.spec }}</span>
                      </p>
                      <RouterLink
                        class="result-card__action"
                        :to="itemDestination(item)"
                        @click="onItemClick(item)"
                      >
                        {{ item.slug ? '查看商品详情' : '浏览可购植物' }}
                      </RouterLink>
                    </footer>
                  </div>
                </article>
              </li>
            </ol>

            <button class="result-success__restart" type="button" @click="profileStore.goToStep(0)">
              <el-icon aria-hidden="true"><Refresh /></el-icon>
              回到第一步修改条件
            </button>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<style scoped>
.recommendation-page {
  --green-pale: var(--color-brand-soft, #e4eadb);
  --green-sage: var(--color-brand-accent, #92a879);
  --green-leaf: var(--color-brand, #496544);
  --green-forest: var(--color-brand-deep, #203d2c);
  --on-dark: var(--color-on-brand, #f7f2e8);
  --on-dark-accent: var(--color-brand-soft, #e4eadb);
  --on-dark-muted: color-mix(in oklch, var(--on-dark) 78%, var(--green-forest));
  color: var(--color-ink, #1d261f);
  background: var(--color-canvas, #f7f2e8);
}

.recommendation-status {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
  border: 0;
}

.recommendation-hero {
  padding-block: clamp(3.5rem, 8vw, 7.5rem) clamp(2.5rem, 6vw, 5rem);
  background: var(--green-forest);
  color: var(--on-dark);
}

.recommendation-hero__layout {
  display: grid;
  gap: clamp(2rem, 6vw, 5rem);
  align-items: end;
}

.recommendation-hero__copy {
  max-width: 51rem;
}

.recommendation-hero__eyebrow,
.section-kicker {
  margin: 0 0 var(--space-sm, 0.75rem);
  color: var(--green-sage);
  font-size: 0.75rem;
  font-weight: 750;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.recommendation-hero__eyebrow,
.result-empty .section-kicker,
.result-generating .section-kicker {
  color: var(--on-dark-accent);
}

.recommendation-hero h1 {
  max-width: 12ch;
  margin: 0;
  font-family: var(--font-sans);
  font-size: clamp(2.75rem, 7vw, 6.4rem);
  font-weight: 500;
  letter-spacing: -0.055em;
  line-height: 0.98;
  text-wrap: balance;
}

.recommendation-hero__lead {
  max-width: 43rem;
  margin: var(--space-lg, 2rem) 0 0;
  color: var(--on-dark-muted);
  font-size: clamp(1rem, 2vw, 1.2rem);
  line-height: 1.85;
}

.recommendation-hero__note {
  display: flex;
  gap: var(--space-md, 1.25rem);
  max-width: 30rem;
  padding-top: var(--space-md, 1.25rem);
  border-top: 1px solid rgba(247, 242, 232, 0.28);
}

.recommendation-hero__note-index {
  flex: 0 0 auto;
  color: var(--on-dark-accent);
  font-family: ui-serif, Georgia, serif;
  font-size: 2.5rem;
  line-height: 1;
}

.recommendation-hero__note strong {
  display: block;
  margin-bottom: 0.45rem;
  font-size: 0.95rem;
}

.recommendation-hero__note p {
  margin: 0;
  color: var(--on-dark-muted);
  font-size: 0.85rem;
  line-height: 1.7;
}

.recommendation-workspace {
  padding-block: clamp(2.5rem, 6vw, 5rem) clamp(4rem, 8vw, 8rem);
}

.recommendation-workspace__layout {
  display: grid;
  gap: var(--space-lg, 2rem);
  align-items: start;
}

.profile-form {
  padding: clamp(1.25rem, 4vw, 2.5rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
  box-shadow: var(--shadow-card, 0 16px 42px rgba(36, 49, 39, 0.07));
}

.profile-form__header,
.result-success__header,
.result-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md, 1.25rem);
  align-items: flex-start;
}

.profile-form__header {
  padding-bottom: var(--space-lg, 2rem);
  border-bottom: 1px solid var(--color-border, #d9d3c5);
}

.profile-form h2,
.result-panel h2 {
  margin: 0;
  font-family: var(--font-sans);
  font-size: clamp(1.65rem, 3vw, 2.2rem);
  font-weight: 550;
  letter-spacing: -0.03em;
  line-height: 1.2;
}

.profile-form__step,
.result-success__count {
  flex: 0 0 auto;
  margin: 0;
  padding: 0.45rem 0.8rem;
  color: var(--green-forest);
  background: var(--green-pale);
  border-radius: var(--radius-pill, 999px);
  font-size: 0.75rem;
  font-weight: 700;
}

.form-alert {
  display: flex;
  gap: var(--space-sm, 0.75rem);
  margin-top: var(--space-md, 1.25rem);
  padding: 0.9rem 1rem;
  color: #6d322a;
  background: #fbede7;
  border: 1px solid #e6bdb1;
  border-radius: var(--radius-card, 0.75rem);
  outline: none;
}

.form-alert:focus-visible {
  box-shadow: 0 0 0 3px rgba(109, 50, 42, 0.2);
}

.form-alert .el-icon {
  flex: 0 0 auto;
  margin-top: 0.15rem;
  font-size: 1.15rem;
}

.form-alert p {
  margin: 0.25rem 0 0;
  font-size: 0.84rem;
  line-height: 1.6;
}

.profile-form__groups {
  display: grid;
}

.choice-group {
  min-width: 0;
  margin: 0;
  padding: var(--space-lg, 2rem) 0;
  border: 0;
  border-bottom: 1px solid var(--color-border, #d9d3c5);
}

.choice-group legend {
  padding: 0;
  font-size: 1rem;
  font-weight: 750;
}

.choice-group legend span {
  margin-right: 0.4rem;
  color: var(--green-sage);
  font-family: ui-serif, Georgia, serif;
}

.choice-group__hint {
  margin: 0.5rem 0 1rem;
  color: var(--color-text-muted, #68716a);
  font-size: 0.82rem;
  line-height: 1.6;
}

.choice-grid {
  display: grid;
  gap: 0.65rem;
}

.choice-card {
  position: relative;
  display: flex;
  gap: 0.75rem;
  min-width: 0;
  min-height: 4.75rem;
  padding: 0.9rem;
  cursor: pointer;
  background: var(--color-surface-muted, #f3efe5);
  border: 1px solid transparent;
  border-radius: var(--radius-card, 0.75rem);
  transition: border-color 180ms var(--ease-out, ease), background-color 180ms var(--ease-out, ease);
}

.choice-card:hover {
  border-color: var(--green-sage);
}

.choice-card input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.choice-card__control {
  position: relative;
  flex: 0 0 auto;
  width: 1.15rem;
  height: 1.15rem;
  margin-top: 0.15rem;
  background: var(--color-surface, #fffdf7);
  border: 1px solid #98a096;
  border-radius: 50%;
}

.choice-card input:checked + .choice-card__control {
  border: 0.32rem solid var(--green-leaf);
}

.choice-card:has(input:checked) {
  background: var(--green-pale);
  border-color: var(--green-leaf);
}

.choice-card:has(input:focus-visible) {
  outline: 3px solid color-mix(in srgb, var(--green-leaf) 35%, transparent);
  outline-offset: 2px;
}

.choice-card__body {
  display: grid;
  gap: 0.25rem;
  min-width: 0;
}

.choice-card__body strong {
  font-size: 0.9rem;
}

.choice-card__body small {
  color: var(--color-text-muted, #68716a);
  font-size: 0.76rem;
  line-height: 1.55;
}

.choice-group__error {
  margin: 0.65rem 0 0;
  color: #a3473d;
  font-size: 0.8rem;
  font-weight: 650;
}

.choice-group--error .choice-card {
  border-color: #e2b3aa;
}

.choice-group:disabled .choice-card {
  cursor: wait;
  opacity: 0.72;
}

.profile-form__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm, 0.75rem);
  align-items: center;
  padding-top: var(--space-lg, 2rem);
}

.profile-form__submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  min-height: 3rem;
  padding: 0.75rem 1.25rem;
  color: #fffdf7;
  background: var(--green-forest);
  border: 1px solid var(--green-forest);
  border-radius: var(--radius-pill, 999px);
  cursor: pointer;
  font: inherit;
  font-size: 0.9rem;
  font-weight: 700;
  transition: background-color 180ms var(--ease-out, ease);
}

.profile-form__submit:hover:not(:disabled) {
  background: var(--green-leaf);
}

.profile-form__submit:disabled,
.profile-form__reset:disabled {
  cursor: wait;
  opacity: 0.68;
}

.profile-form__submit-icon {
  width: 1rem;
  height: 1rem;
}

.profile-form__reset,
.result-success__restart {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  min-height: 2.75rem;
  padding: 0.55rem 0.8rem;
  color: var(--green-leaf);
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: var(--radius-pill, 999px);
  font: inherit;
  font-size: 0.84rem;
  font-weight: 700;
}

.profile-form__reset:hover,
.result-success__restart:hover {
  background: var(--green-pale);
}

.profile-form button:focus-visible,
.result-success button:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--green-leaf) 35%, transparent);
  outline-offset: 2px;
}

.is-loading {
  animation: loading-rotate 1s linear infinite;
}

.result-panel {
  min-width: 0;
}

.result-empty,
.result-generating {
  min-height: 31rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: clamp(1.5rem, 5vw, 3.5rem);
  color: var(--on-dark);
  background: var(--green-forest);
  border-radius: var(--radius-card, 0.75rem);
}

.result-empty > p:not(.section-kicker),
.result-generating > p:not(.section-kicker) {
  max-width: 31rem;
  margin: var(--space-md, 1.25rem) 0 0;
  color: var(--on-dark-muted);
  font-size: 0.9rem;
  line-height: 1.75;
}

.result-empty ul {
  display: grid;
  gap: 0.75rem;
  margin: var(--space-lg, 2rem) 0 0;
  padding: var(--space-md, 1.25rem) 0 0;
  border-top: 1px solid rgba(247, 242, 232, 0.18);
  list-style: none;
}

.result-empty li {
  display: flex;
  gap: 0.65rem;
  align-items: center;
  color: var(--on-dark-muted);
  font-size: 0.82rem;
}

.result-empty li span {
  color: var(--on-dark-accent);
  font-family: ui-serif, Georgia, serif;
}

.result-empty__leaves {
  position: relative;
  width: 7rem;
  height: 6rem;
  margin-bottom: var(--space-lg, 2rem);
}

.result-empty__leaves i {
  position: absolute;
  display: block;
  width: 3.7rem;
  height: 2rem;
  background: var(--green-sage);
  border-radius: 100% 0 100% 0;
  transform-origin: bottom right;
}

.result-empty__leaves i:nth-child(1) { left: 0; bottom: 0.8rem; transform: rotate(28deg); }
.result-empty__leaves i:nth-child(2) { left: 2.3rem; bottom: 2.1rem; transform: rotate(-18deg) scale(0.82); }
.result-empty__leaves i:nth-child(3) { left: 3.6rem; bottom: 0.1rem; transform: rotate(72deg) scale(0.68); }

.result-generating__mark {
  display: flex;
  gap: 0.55rem;
  align-items: end;
  height: 4.5rem;
  margin-bottom: var(--space-lg, 2rem);
}

.result-generating__mark span {
  display: block;
  width: 1.15rem;
  background: var(--green-sage);
  border-radius: 999px 999px 0 0;
  animation: grow-mark 1.1s ease-in-out infinite alternate;
}

.result-generating__mark span:nth-child(1) { height: 45%; }
.result-generating__mark span:nth-child(2) { height: 75%; animation-delay: 150ms; }
.result-generating__mark span:nth-child(3) { height: 100%; animation-delay: 300ms; }

.result-success {
  min-width: 0;
}

.result-success__header h2:focus {
  outline: none;
}

.result-success__header h2:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--green-leaf) 35%, transparent);
  outline-offset: 4px;
}

.result-success__intro {
  margin: 0.8rem 0 var(--space-md, 1.25rem);
  color: var(--color-text-muted, #68716a);
  font-size: 0.82rem;
  line-height: 1.65;
}

.result-list {
  display: grid;
  gap: var(--space-md, 1.25rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

.result-card {
  display: grid;
  overflow: hidden;
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
  box-shadow: var(--shadow-card, 0 16px 42px rgba(36, 49, 39, 0.07));
}

.result-card__portrait {
  position: relative;
  display: grid;
  min-height: 8rem;
  place-items: center;
  overflow: hidden;
  color: #fffdf7;
  background: var(--green-sage);
}

.result-card__portrait--moss { background: var(--green-leaf); }
.result-card__portrait--forest { background: var(--green-forest); }

.result-card__portrait span {
  position: relative;
  z-index: 1;
  font-family: ui-serif, "Noto Serif SC", serif;
  font-size: 3.25rem;
}

.result-card__portrait i {
  position: absolute;
  right: -1.8rem;
  bottom: -1.4rem;
  width: 7rem;
  height: 4rem;
  background: color-mix(in oklch, var(--color-on-brand) 16%, transparent);
  border-radius: 100% 0 100% 0;
  transform: rotate(-18deg);
}

.result-card__content {
  min-width: 0;
  padding: var(--space-md, 1.25rem);
}

.result-card__header p {
  margin: 0 0 0.25rem;
  color: var(--green-leaf);
  font-size: 0.68rem;
  font-weight: 750;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.result-card__header h3 {
  margin: 0;
  font-family: var(--font-sans);
  font-size: 1.35rem;
  font-weight: 600;
}

.result-card__header small {
  display: block;
  margin-top: 0.2rem;
  color: var(--color-text-muted, #68716a);
  font-family: ui-serif, Georgia, serif;
  font-size: 0.73rem;
  font-style: italic;
}

.result-card__score {
  display: flex;
  align-items: baseline;
  color: var(--green-forest);
}

.result-card__score strong {
  font-family: ui-serif, Georgia, serif;
  font-size: 1.75rem;
  font-weight: 500;
}

.result-card__score span {
  color: var(--color-text-muted, #68716a);
  font-size: 0.68rem;
}

.result-card__progress {
  height: 0.25rem;
  margin: 0.9rem 0 var(--space-md, 1.25rem);
  overflow: hidden;
  background: var(--green-pale);
  border-radius: var(--radius-pill, 999px);
}

.result-card__progress span {
  display: block;
  height: 100%;
  background: var(--green-leaf);
  border-radius: inherit;
}

.result-card__reasons h4 {
  margin: 0 0 0.65rem;
  font-size: 0.8rem;
}

.result-card__reasons ul {
  display: grid;
  gap: 0.55rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.result-card__reasons li {
  display: flex;
  gap: 0.5rem;
  align-items: flex-start;
  color: var(--color-text, #37423a);
  font-size: 0.77rem;
  line-height: 1.55;
}

.result-card__reasons .el-icon {
  flex: 0 0 auto;
  margin-top: 0.2rem;
  color: var(--green-leaf);
}

.result-card__guide {
  display: inline-block;
  margin-top: 0.6rem;
  color: var(--green-leaf);
  font-size: 0.75rem;
  font-weight: 700;
  text-decoration: none;
}

.result-card__guide:hover {
  text-decoration: underline;
}

.result-card__risk {
  display: flex;
  gap: 0.55rem;
  margin-top: var(--space-md, 1.25rem);
  padding: 0.75rem;
  color: #675036;
  background: #f3ead7;
  border-radius: calc(var(--radius-card, 0.75rem) - 0.2rem);
}

.result-card__risk .el-icon {
  flex: 0 0 auto;
  margin-top: 0.18rem;
}

.result-card__risk p {
  margin: 0;
  font-size: 0.74rem;
  line-height: 1.55;
}

.result-card__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.75rem;
  margin-top: var(--space-md, 1.25rem);
  padding: 0.6rem 1rem;
  color: var(--on-dark);
  background: var(--green-forest);
  border: 1px solid var(--green-forest);
  border-radius: var(--radius-pill, 999px);
  font-size: 0.82rem;
  font-weight: 700;
  text-decoration: none;
}

.result-card__action:hover {
  background: var(--green-leaf);
  border-color: var(--green-leaf);
}

.result-card__action:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--green-leaf) 35%, transparent);
  outline-offset: 2px;
}

.result-success__restart {
  margin-top: var(--space-md, 1.25rem);
}

/* ===== 商品图与价格：结果直接连到可购买的 SKU ===== */

.result-card__image {
  width: 100%;
  height: 100%;
  min-height: 8rem;
  object-fit: cover;
}

.result-card__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-top: var(--space-md, 1.25rem);
}

.result-card__footer .result-card__action {
  margin-top: 0;
}

.result-card__price {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  margin: 0;
}

.result-card__price strong {
  color: var(--green-forest);
  font-size: 1.15rem;
}

.result-card__price span {
  color: var(--color-text-muted, #68716a);
  font-size: 0.75rem;
}

/* ===== 七维分项条 =====
   方案要求展示"分项匹配度"。只给总分的话，用户判断不出 95 分是光照贴合
   还是单纯便宜。权重也一并显示，否则七个百分比之间看不出轻重。 */

.result-card__scores {
  margin-bottom: var(--space-md, 1.25rem);
}

.result-card__scores h4 {
  margin: 0 0 0.65rem;
  font-size: 0.8rem;
}

.result-card__scores ul {
  display: grid;
  gap: 0.4rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.result-card__scores li {
  display: grid;
  grid-template-columns: 2.6rem minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.6rem;
}

.score-row__label {
  color: var(--color-text-muted, #68716a);
  font-size: 0.72rem;
}

.score-row__bar {
  height: 0.4rem;
  overflow: hidden;
  background: var(--green-pale);
  border-radius: var(--radius-pill, 999px);
}

.score-row__bar i {
  display: block;
  height: 100%;
  background: var(--green-sage);
  border-radius: inherit;
}

.score-row__value {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  min-width: 5.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.score-row__value small {
  color: var(--color-text-muted, #68716a);
  font-size: 0.65rem;
  font-weight: 400;
}

/* ===== 无候选诊断 =====
   方案原文："无候选时仅建议放宽非安全约束"。所以漏斗里安全那两条要
   显式标注不建议放宽，视觉上也压暗，别让用户顺手把它关掉。 */

.result-none__header h2 {
  margin: 0.35rem 0 0.6rem;
}

.result-none__header p {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.85rem;
}

.funnel-list {
  display: grid;
  gap: 0.85rem;
  margin: var(--space-md, 1.25rem) 0;
  padding: 0;
  list-style: none;
}

.funnel-list__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
}

.funnel-list__label {
  font-size: 0.82rem;
  font-weight: 700;
}

.funnel-list__count {
  color: var(--color-text-muted, #68716a);
  font-size: 0.75rem;
  font-variant-numeric: tabular-nums;
}

.funnel-list__bar {
  display: block;
  height: 0.5rem;
  overflow: hidden;
  background: var(--green-pale);
  border-radius: var(--radius-pill, 999px);
}

.funnel-list__bar span {
  display: block;
  height: 100%;
  background: var(--green-sage);
  border-radius: inherit;
}

/* 安全约束用与可放宽项不同的颜色，光靠文案说明不够——
   PRODUCT.md 要求状态不只依赖颜色，所以下面还有一行文字标注 */
.funnel-list__item--locked .funnel-list__bar span {
  background: var(--green-forest);
}

.funnel-list__locked {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin: 0.35rem 0 0;
  color: var(--green-forest);
  font-size: 0.72rem;
  font-weight: 700;
}

.result-none__advice {
  padding: var(--space-md, 1.25rem);
  background: var(--green-pale);
  border-radius: var(--radius-card, 0.75rem);
}

.result-none__advice h3 {
  margin: 0 0 0.65rem;
  font-size: 0.85rem;
}

.result-none__advice ul {
  display: grid;
  gap: 0.55rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.result-none__advice li {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.8rem;
  line-height: 1.6;
}

.result-none__advice .el-icon {
  flex-shrink: 0;
  margin-top: 0.15rem;
  color: var(--green-leaf);
}

.result-failed {
  display: grid;
  justify-items: start;
  gap: 0.5rem;
}

.result-failed__icon {
  font-size: 1.5rem;
  color: var(--color-danger, #a8442f);
}

.result-failed h2 {
  margin: 0;
}

.result-failed p {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.85rem;
}

@keyframes loading-rotate {
  to { transform: rotate(360deg); }
}

@keyframes grow-mark {
  to { transform: scaleY(0.68); opacity: 0.55; }
}

@media (min-width: 40rem) {
  .choice-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .result-card {
    grid-template-columns: 8rem minmax(0, 1fr);
  }

  .result-card__portrait {
    min-height: 100%;
  }
}

@media (min-width: 64rem) {
  .recommendation-hero__layout {
    grid-template-columns: minmax(0, 1.45fr) minmax(17rem, 0.55fr);
  }

  .recommendation-workspace__layout {
    grid-template-columns: minmax(0, 1.08fr) minmax(25rem, 0.92fr);
  }

  .result-panel {
    position: sticky;
    top: 6.5rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .choice-card,
  .profile-form__submit {
    transition: none;
  }

  .choice-card:hover,
  .profile-form__submit:hover:not(:disabled) {
    transform: none;
  }

  .is-loading,
  .result-generating__mark span {
    animation: none;
  }
}

/* ===== 以下为分步问卷新增，其余样式沿用改造前 ===== */

.scene-bar {
  padding: var(--space-sm, 0.75rem);
  margin-top: var(--space-md, 1.25rem);
  background: var(--green-pale);
  border-radius: var(--radius-card, 0.75rem);
}

.scene-bar__label {
  margin: 0 0 0.5rem;
  color: var(--green-forest);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.scene-bar__list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.scene-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 2.75rem;
  padding: 0.4rem 0.9rem;
  color: var(--color-ink, #1d261f);
  font: inherit;
  font-size: 0.85rem;
  font-weight: 650;
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-pill, 999px);
  cursor: pointer;
}

.scene-chip:hover {
  border-color: var(--green-leaf);
}

.scene-chip--active {
  color: var(--on-dark);
  background: var(--green-leaf);
  border-color: var(--green-leaf);
}

.scene-chip--new {
  border-style: dashed;
}

.scene-chip__star {
  font-size: 0.8rem;
}

.scene-chip:focus-visible {
  outline: 2px solid var(--green-forest);
  outline-offset: 2px;
}

.scene-bar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-top: 0.6rem;
}

.scene-action {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  min-height: 2.75rem;
  padding: 0;
  color: var(--color-text-muted, #5c6b5e);
  font: inherit;
  font-size: 0.8rem;
  font-weight: 650;
  background: none;
  border: 0;
  cursor: pointer;
}

.scene-action:hover {
  color: var(--green-leaf);
}

.scene-action--danger:hover {
  color: #a1372b;
}

.scene-action:focus-visible {
  outline: 2px solid var(--green-forest);
  outline-offset: 2px;
  border-radius: 4px;
}

.stepper {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.4rem;
  margin: var(--space-lg, 2rem) 0 0;
  padding: 0;
  list-style: none;
}

.stepper__item {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  padding-top: 0.55rem;
  color: var(--color-text-muted, #5c6b5e);
  font-size: 0.72rem;
  border-top: 3px solid var(--color-border, #d9d3c5);
}

.stepper__item--done {
  color: var(--color-ink, #1d261f);
  border-top-color: var(--green-sage);
}

.stepper__item--active {
  color: var(--green-leaf);
  font-weight: 700;
  border-top-color: var(--green-leaf);
}

.stepper__index {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.stepper__title {
  line-height: 1.3;
}

.profile-form__step-head {
  margin-top: var(--space-lg, 2rem);
}

.profile-form__step-head h3 {
  margin: 0 0 0.3rem;
  font-size: 1.05rem;
  font-weight: 700;
}

.profile-form__step-head h3:focus {
  outline: none;
}

.profile-form__step-head h3:focus-visible {
  outline: 2px solid var(--green-forest);
  outline-offset: 4px;
  border-radius: 4px;
}

.profile-form__step-head p {
  margin: 0;
  color: var(--color-text-muted, #5c6b5e);
  font-size: 0.85rem;
  line-height: 1.6;
}

.form-alert__list {
  margin: 0.3rem 0 0;
  padding-left: 1.1rem;
  font-size: 0.84rem;
  line-height: 1.7;
}

.scene-name-input,
.budget-field input {
  min-height: 2.75rem;
  padding: 0 0.75rem;
  color: var(--color-ink, #1d261f);
  font: inherit;
  font-size: 0.9rem;
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: 0.5rem;
}

.scene-name-input {
  width: min(100%, 22rem);
}

.scene-name-input:focus-visible,
.budget-field input:focus-visible {
  outline: 2px solid var(--green-forest);
  outline-offset: 1px;
  border-color: var(--green-leaf);
}

.budget-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.budget-field {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--color-text-muted, #5c6b5e);
  font-size: 0.8rem;
}

.budget-field input {
  width: 6.5rem;
}

.budget-sep {
  color: var(--color-text-muted, #5c6b5e);
}

.choice-grid--compact {
  grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
}

/* 多选题用方形勾选框，与单选的圆点区分，避免用户以为只能选一个 */
.choice-card__control--box {
  border-radius: 0.25rem;
}

.choice-card input[type='checkbox']:checked + .choice-card__control--box {
  background: var(--green-leaf);
  border-color: var(--green-leaf);
  box-shadow: inset 0 0 0 3px var(--color-surface, #fffdf7);
}

@media (max-width: 48rem) {
  .stepper {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.6rem;
  }
}
</style>
