<script setup lang="ts">
import { nextTick, ref } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import { Check, Loading, Refresh, WarningFilled } from '@element-plus/icons-vue'
import type {
  LightLevel,
  PetSituation,
  PreferenceKey,
  SpaceType,
  WateringHabit,
} from '../stores/recommendation'
import { useRecommendationStore } from '../stores/recommendation'

interface Choice<T extends string> {
  value: T
  label: string
  description: string
}

const lightOptions: Choice<LightLevel>[] = [
  { value: 'low', label: '柔和弱光', description: '离窗较远，白天无需开灯但没有明显光斑' },
  { value: 'indirect', label: '明亮散射光', description: '靠近窗边，有充足自然光但阳光不直晒' },
  { value: 'bright', label: '充足光线', description: '每天能接触一段时间的柔和直射光' },
]

const spaceOptions: Choice<SpaceType>[] = [
  { value: 'desktop', label: '桌面小空间', description: '书桌、床头或面积有限的台面' },
  { value: 'shelf', label: '层架或窗边', description: '适合中小型盆栽或自然垂落的枝叶' },
  { value: 'floor', label: '落地空间', description: '客厅角落、玄关等可供植株展开的位置' },
]

const petOptions: Choice<PetSituation>[] = [
  { value: 'none', label: '没有猫狗', description: '目前不需要考虑宠物误食的风险' },
  { value: 'cat', label: '家里有猫', description: '优先筛选对猫家庭更安心的候选' },
  { value: 'dog', label: '家里有狗', description: '优先筛选对狗家庭更安心的候选' },
]

const wateringOptions: Choice<WateringHabit>[] = [
  { value: 'attentive', label: '经常观察', description: '愿意每隔几天检查一次盆土和叶片' },
  { value: 'weekly', label: '每周照看', description: '习惯在固定的一天集中打理植物' },
  { value: 'forgetful', label: '偶尔会忘', description: '更需要耐旱、容错率高的植物' },
]

const store = useRecommendationStore()
const errorSummary = ref<HTMLElement | null>(null)
const resultHeading = ref<HTMLElement | null>(null)
const firstChoice = ref<HTMLInputElement | null>(null)

function describedBy(key: PreferenceKey) {
  return `${key}-hint${store.errors[key] ? ` ${key}-error` : ''}`
}

function hasProductDetail(id: string) {
  return id === 'fiddle-leaf-fig' || id === 'snake-plant'
}

function resultDestination(id: string) {
  return hasProductDetail(id)
    ? { name: 'plant-detail', params: { plantId: id } }
    : { name: 'plant-catalog' }
}

function setFirstChoice(element: Element | ComponentPublicInstance | null) {
  if (element instanceof HTMLInputElement) firstChoice.value = element
}

async function submitProfile() {
  const generated = await store.generateRecommendations()
  await nextTick()

  if (!generated && store.status === 'error') {
    errorSummary.value?.focus()
    return
  }

  if (generated) resultHeading.value?.focus()
}

async function resetProfile() {
  store.reset()
  await nextTick()
  firstChoice.value?.focus()
}
</script>

<template>
  <div class="recommendation-page">
    <section class="recommendation-hero section-shell" aria-labelledby="recommendation-title">
      <div class="container recommendation-hero__layout">
        <div class="recommendation-hero__copy">
          <p class="recommendation-hero__eyebrow">植物匹配 · 本地规则计算</p>
          <h1 id="recommendation-title">找到与你一起长大的那一盆</h1>
          <p class="recommendation-hero__lead">
            不从“好不好看”开始，而是先理解你的光线、空间和生活节奏。四个选择，换来一份说得清理由的植物清单。
          </p>
        </div>

        <aside class="recommendation-hero__note" aria-label="推荐原则">
          <span class="recommendation-hero__note-index" aria-hidden="true">4</span>
          <div>
            <strong>只问四件真正重要的事</strong>
            <p>选择仅用于当前页面的本地匹配，不会上传。结果是养护起点，不替代实际环境观察。</p>
          </div>
        </aside>
      </div>
    </section>

    <section class="section-shell recommendation-workspace" aria-labelledby="profile-form-title">
      <div class="container recommendation-workspace__layout">
        <form class="profile-form surface-card" novalidate @submit.prevent="submitProfile">
          <header class="profile-form__header">
            <div>
              <p class="section-kicker">环境档案</p>
              <h2 id="profile-form-title">你和植物会怎样相处？</h2>
            </div>
            <p class="profile-form__step">约 1 分钟</p>
          </header>

          <div
            v-if="store.status === 'error'"
            ref="errorSummary"
            class="form-alert"
            role="alert"
            tabindex="-1"
          >
            <el-icon aria-hidden="true"><WarningFilled /></el-icon>
            <div>
              <strong>还差一点信息</strong>
              <p>请完成标有提示的选项，我们才能给出可靠的匹配结果。</p>
            </div>
          </div>

          <div class="profile-form__groups">
            <fieldset :class="['choice-group', { 'choice-group--error': store.errors.light }]" :disabled="store.isGenerating">
              <legend><span>01</span> 你家主要是什么光线？</legend>
              <p id="light-hint" class="choice-group__hint">回想植物计划摆放处在白天最常见的状态。</p>
              <div class="choice-grid">
                <label v-for="(option, index) in lightOptions" :key="option.value" class="choice-card">
                  <input
                    :ref="index === 0 ? setFirstChoice : undefined"
                    v-model="store.profile.light"
                    type="radio"
                    name="light"
                    :value="option.value"
                    :aria-describedby="describedBy('light')"
                    :aria-invalid="Boolean(store.errors.light)"
                    @change="store.clearError('light')"
                  >
                  <span class="choice-card__control" aria-hidden="true"></span>
                  <span class="choice-card__body">
                    <strong>{{ option.label }}</strong>
                    <small>{{ option.description }}</small>
                  </span>
                </label>
              </div>
              <p v-if="store.errors.light" id="light-error" class="choice-group__error">
                {{ store.errors.light }}
              </p>
            </fieldset>

            <fieldset :class="['choice-group', { 'choice-group--error': store.errors.space }]" :disabled="store.isGenerating">
              <legend><span>02</span> 你想把它放在哪里？</legend>
              <p id="space-hint" class="choice-group__hint">按成熟后的植物体量来选择，而不只是刚买回家时的大小。</p>
              <div class="choice-grid">
                <label v-for="option in spaceOptions" :key="option.value" class="choice-card">
                  <input
                    v-model="store.profile.space"
                    type="radio"
                    name="space"
                    :value="option.value"
                    :aria-describedby="describedBy('space')"
                    :aria-invalid="Boolean(store.errors.space)"
                    @change="store.clearError('space')"
                  >
                  <span class="choice-card__control" aria-hidden="true"></span>
                  <span class="choice-card__body">
                    <strong>{{ option.label }}</strong>
                    <small>{{ option.description }}</small>
                  </span>
                </label>
              </div>
              <p v-if="store.errors.space" id="space-error" class="choice-group__error">
                {{ store.errors.space }}
              </p>
            </fieldset>

            <fieldset :class="['choice-group', { 'choice-group--error': store.errors.pet }]" :disabled="store.isGenerating">
              <legend><span>03</span> 家里有会接触植物的宠物吗？</legend>
              <p id="pet-hint" class="choice-group__hint">有猫狗时，我们会先排除不适合作为首选的植物。</p>
              <div class="choice-grid">
                <label v-for="option in petOptions" :key="option.value" class="choice-card">
                  <input
                    v-model="store.profile.pet"
                    type="radio"
                    name="pet"
                    :value="option.value"
                    :aria-describedby="describedBy('pet')"
                    :aria-invalid="Boolean(store.errors.pet)"
                    @change="store.clearError('pet')"
                  >
                  <span class="choice-card__control" aria-hidden="true"></span>
                  <span class="choice-card__body">
                    <strong>{{ option.label }}</strong>
                    <small>{{ option.description }}</small>
                  </span>
                </label>
              </div>
              <p v-if="store.errors.pet" id="pet-error" class="choice-group__error">
                {{ store.errors.pet }}
              </p>
            </fieldset>

            <fieldset :class="['choice-group', { 'choice-group--error': store.errors.watering }]" :disabled="store.isGenerating">
              <legend><span>04</span> 哪种浇水习惯更像你？</legend>
              <p id="watering-hint" class="choice-group__hint">没有标准答案，诚实的节奏比理想中的勤快更有参考价值。</p>
              <div class="choice-grid">
                <label v-for="option in wateringOptions" :key="option.value" class="choice-card">
                  <input
                    v-model="store.profile.watering"
                    type="radio"
                    name="watering"
                    :value="option.value"
                    :aria-describedby="describedBy('watering')"
                    :aria-invalid="Boolean(store.errors.watering)"
                    @change="store.clearError('watering')"
                  >
                  <span class="choice-card__control" aria-hidden="true"></span>
                  <span class="choice-card__body">
                    <strong>{{ option.label }}</strong>
                    <small>{{ option.description }}</small>
                  </span>
                </label>
              </div>
              <p v-if="store.errors.watering" id="watering-error" class="choice-group__error">
                {{ store.errors.watering }}
              </p>
            </fieldset>
          </div>

          <footer class="profile-form__actions">
            <button class="pill-button profile-form__submit" type="submit" :disabled="store.isGenerating">
              <el-icon v-if="store.isGenerating" class="is-loading" aria-hidden="true"><Loading /></el-icon>
              <Check v-else class="profile-form__submit-icon" aria-hidden="true" />
              {{ store.isGenerating ? '正在本地匹配…' : '生成我的植物清单' }}
            </button>
            <button
              v-if="store.status !== 'idle'"
              class="profile-form__reset"
              type="button"
              :disabled="store.isGenerating"
              @click="resetProfile"
            >
              <el-icon aria-hidden="true"><Refresh /></el-icon>
              重置选择
            </button>
          </footer>
        </form>

        <p class="recommendation-status" role="status" aria-live="polite" aria-atomic="true">
          <template v-if="store.status === 'generating'">正在生成植物推荐。</template>
          <template v-else-if="store.status === 'success'">推荐完成，共有 {{ store.results.length }} 个候选。</template>
        </p>

        <aside class="result-panel" :aria-busy="store.isGenerating">
          <div v-if="store.status === 'idle' || store.status === 'error'" class="result-empty">
            <div class="result-empty__leaves" aria-hidden="true"><i></i><i></i><i></i></div>
            <p class="section-kicker">等待你的答案</p>
            <h2>好的推荐，应该先懂你的生活</h2>
            <p>完成左侧四组选择后，这里会出现 3 个匹配候选、具体原因和不可忽略的养护风险。</p>
            <ul>
              <li><span>01</span> 不因热门程度加分</li>
              <li><span>02</span> 宠物家庭优先安全筛选</li>
              <li><span>03</span> 明确展示不完美之处</li>
            </ul>
          </div>

          <div v-else-if="store.status === 'generating'" class="result-generating">
            <div class="result-generating__mark" aria-hidden="true">
              <span></span><span></span><span></span>
            </div>
            <p class="section-kicker">正在本地计算</p>
            <h2>把环境条件放在一起比较…</h2>
            <p>正在核对光照、成熟株型、浇水容错和宠物情况，没有向服务器发送数据。</p>
          </div>

          <div v-else class="result-success">
            <header class="result-success__header">
              <div>
                <p class="section-kicker">匹配完成</p>
                <h2 ref="resultHeading" tabindex="-1">
                  更适合你的 {{ store.results.length }} 种植物
                </h2>
              </div>
              <span class="result-success__count">{{ store.results.length }} 个候选</span>
            </header>
            <p class="result-success__intro">匹配度用于比较当前候选，不代表植物一定存活；收到植物后仍需根据盆土和叶片状态调整。</p>

            <ol class="result-list">
              <li v-for="(result, index) in store.results" :key="result.id">
                <article class="result-card">
                  <div :class="['result-card__portrait', `result-card__portrait--${result.tone}`]" aria-hidden="true">
                    <span>{{ result.name.slice(0, 1) }}</span>
                    <i></i>
                  </div>
                  <div class="result-card__content">
                    <header class="result-card__header">
                      <div>
                        <p>推荐 {{ String(index + 1).padStart(2, '0') }} · {{ result.difficulty }}</p>
                        <h3>{{ result.name }}</h3>
                        <small>{{ result.latinName }}</small>
                      </div>
                      <div class="result-card__score" :aria-label="`匹配度 ${result.score} 分`">
                        <strong>{{ result.score }}</strong><span>/100</span>
                      </div>
                    </header>

                    <div
                      class="result-card__progress"
                      role="progressbar"
                      aria-label="匹配度"
                      aria-valuemin="0"
                      aria-valuemax="100"
                      :aria-valuenow="result.score"
                    >
                      <span :style="{ width: `${result.score}%` }"></span>
                    </div>

                    <div class="result-card__reasons">
                      <h4>为什么适合</h4>
                      <ul>
                        <li v-for="reason in result.reasons" :key="reason">
                          <el-icon aria-hidden="true"><Check /></el-icon>
                          <span>{{ reason }}</span>
                        </li>
                      </ul>
                    </div>

                    <div class="result-card__risk">
                      <el-icon aria-hidden="true"><WarningFilled /></el-icon>
                      <p><strong>选择前请留意：</strong>{{ result.risk }}</p>
                    </div>

                    <RouterLink class="result-card__action" :to="resultDestination(result.id)">
                      {{ hasProductDetail(result.id) ? '查看商品详情' : '浏览可购植物' }}
                    </RouterLink>
                  </div>
                </article>
              </li>
            </ol>

            <button class="result-success__restart" type="button" @click="resetProfile">
              <el-icon aria-hidden="true"><Refresh /></el-icon>
              换一个生活场景重新匹配
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
</style>
