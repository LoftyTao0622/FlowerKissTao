<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { fetchWeights, previewWeights, updateWeights } from '../api/operation'
import type { WeightConfig } from '../types/operation'
import type { Recommendation } from '@/modules/recommendation/types/recommendation'
import { fetchMyProfiles } from '@/modules/user/api/profile'
import type { SceneProfile } from '@/modules/user/types/profile'

const labels: Record<keyof Omit<WeightConfig, 'topN'>, string> = {
  light: '光照', temp: '温度', humidity: '湿度', care: '养护能力',
  space: '空间', budget: '预算贴合', preference: '个人偏好',
}

const form = reactive<WeightConfig>({
  light: 30, temp: 15, humidity: 10, care: 20,
  space: 10, budget: 10, preference: 5, topN: 3,
})
const profiles = ref<SceneProfile[]>([])
const profileId = ref<number | undefined>(undefined)
const preview = ref<Recommendation | null>(null)
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')

const weightKeys = Object.keys(labels) as (keyof typeof labels)[]
const sum = computed(() => weightKeys.reduce((total, key) => total + Number(form[key]), 0))
const valid = computed(() => sum.value === 100 && form.topN >= 1 && form.topN <= 20)

async function load() {
  loading.value = true
  try {
    const [weights, scenes] = await Promise.all([fetchWeights(), fetchMyProfiles()])
    Object.assign(form, weights)
    profiles.value = scenes
    profileId.value = scenes.find((item) => item.isDefault)?.id ?? scenes[0]?.id
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '配置加载失败'
  } finally {
    loading.value = false
  }
}

async function runPreview() {
  if (!valid.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    preview.value = await previewWeights({ profileId: profileId.value, weights: { ...form } })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '预览失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!valid.value) return
  if (!preview.value) {
    await runPreview()
    if (!preview.value) return
  }
  try {
    await ElMessageBox.confirm(
      `确认保存这组权重？当前预览第一名是「${preview.value.items[0]?.name ?? '无候选'}」。`,
      '保存推荐规则',
      { confirmButtonText: '确认保存', cancelButtonText: '再看看' },
    )
  } catch {
    return
  }
  saving.value = true
  try {
    await updateWeights({ ...form })
    ElMessage.success('推荐权重已保存；历史推荐仍按各自快照解释')
    preview.value = null
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saving.value = false
  }
}

function resetDefault() {
  Object.assign(form, { light: 30, temp: 15, humidity: 10, care: 20, space: 10, budget: 10, preference: 5, topN: 3 })
  preview.value = null
}

onMounted(() => { void load() })
</script>

<template>
  <section class="weight-page">
    <header class="weight-header">
      <div>
        <p class="weight-eyebrow">RECOMMENDATION RULES</p>
        <h1>推荐权重配置</h1>
        <p>先用测试画像预览排序与理由，确认无误后再保存。预览不会写配置或推荐快照。</p>
      </div>
      <button class="outline-button" type="button" @click="resetDefault">恢复方案默认值</button>
    </header>

    <p v-if="errorMessage" class="weight-error" role="alert">{{ errorMessage }}</p>

    <div class="weight-layout">
      <section class="weight-form-card" :aria-busy="loading">
        <header>
          <h2>七维权重</h2>
          <span :class="['sum-badge', { valid }]">合计 {{ sum }} / 100</span>
        </header>
        <p v-if="!valid" class="weight-warning" role="alert">七项权重之和必须等于 100。</p>

        <div class="weight-rows">
          <label v-for="key in weightKeys" :key="key" class="weight-row">
            <span>{{ labels[key] }}</span>
            <input v-model.number="form[key]" type="range" min="0" max="100" step="1" @input="preview = null" />
            <input v-model.number="form[key]" class="number-input" type="number" min="0" max="100" @input="preview = null" />
            <small>%</small>
          </label>
        </div>

        <label class="topn-row">
          <span>返回条数 Top-N</span>
          <input v-model.number="form.topN" type="number" min="1" max="20" @input="preview = null" />
        </label>

        <label class="profile-row">
          <span>测试画像</span>
          <select v-model="profileId" @change="preview = null">
            <option v-for="profile in profiles" :key="profile.id" :value="profile.id">
              {{ profile.sceneName }}{{ profile.isDefault ? '（默认）' : '' }}
            </option>
          </select>
        </label>

        <div class="weight-actions">
          <button class="primary-button" type="button" :disabled="!valid || loading" @click="runPreview">
            {{ loading ? '计算中…' : '预览排序' }}
          </button>
          <button class="outline-button" type="button" :disabled="!valid || saving" @click="save">
            {{ saving ? '保存中…' : '保存配置' }}
          </button>
        </div>
      </section>

      <section class="preview-card">
        <header>
          <h2>预览结果</h2>
          <span>不落库</span>
        </header>
        <div v-if="!preview" class="preview-empty">
          调整权重后点击「预览排序」，这里会显示测试画像的 Top-N 与推荐理由。
        </div>
        <template v-else>
          <p class="preview-summary">
            {{ preview.sceneName }} · {{ preview.candidateCount }} 株候选 · 展示 {{ preview.items.length }} 株
          </p>
          <ol class="preview-list">
            <li v-for="item in preview.items" :key="`${item.speciesId}-${item.rankNo}`">
              <div class="preview-rank">{{ String(item.rankNo).padStart(2, '0') }}</div>
              <div class="preview-body">
                <header><strong>{{ item.name }}</strong><span>{{ item.totalScore }} 分</span></header>
                <ul><li v-for="reason in item.reasons" :key="reason">{{ reason }}</li></ul>
                <p v-if="item.risk">注意：{{ item.risk }}</p>
              </div>
            </li>
          </ol>
          <div v-if="preview.items.length === 0" class="preview-empty">
            当前硬条件下没有候选。安全约束不会因预览而放宽。
          </div>
        </template>
      </section>
    </div>
  </section>
</template>

<style scoped>
.weight-page { padding: 2rem 0 3rem; }
.weight-header { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 1rem; margin-bottom: 1.5rem; }
.weight-header h1 { margin: .2rem 0 .35rem; }
.weight-header p:last-child { margin: 0; color: #718076; font-size: .84rem; }
.weight-eyebrow { margin: 0; color: #718076; font-size: .62rem; letter-spacing: .12em; }
.weight-error, .weight-warning { padding: .7rem; border-radius: .55rem; background: #f6e3de; color: #a8442f; font-size: .78rem; }
.weight-layout { display: grid; gap: 1rem; }
@media (min-width: 62rem) { .weight-layout { grid-template-columns: minmax(25rem, .9fr) minmax(25rem, 1.1fr); align-items: start; } }
.weight-form-card, .preview-card { padding: 1.15rem; background: #fffdf7; border: 1px solid #d9d3c5; border-radius: .75rem; }
.weight-form-card > header, .preview-card > header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.weight-form-card h2, .preview-card h2 { margin: 0; font-size: .95rem; }
.preview-card > header span { color: #718076; font-size: .68rem; }
.sum-badge { padding: .15rem .55rem; border-radius: 999px; background: #f6e3de; color: #a8442f; font-size: .7rem; font-weight: 700; }
.sum-badge.valid { background: #e4eadb; color: #496544; }
.weight-rows { display: grid; gap: .8rem; }
.weight-row { display: grid; grid-template-columns: 5rem minmax(7rem, 1fr) 4rem 1rem; align-items: center; gap: .6rem; font-size: .78rem; }
.weight-row input[type='range'] { accent-color: #496544; }
.number-input, .topn-row input, .profile-row select { min-height: 2.35rem; padding: .3rem .5rem; border: 1px solid #d9d3c5; border-radius: .45rem; background: #fffdf7; }
.weight-row small { color: #718076; }
.topn-row, .profile-row { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding-top: 1rem; margin-top: 1rem; border-top: 1px dashed #d9d3c5; font-size: .8rem; }
.topn-row input { width: 5rem; }
.profile-row select { min-width: 12rem; }
.weight-actions { display: flex; gap: .7rem; margin-top: 1.2rem; }
.primary-button, .outline-button { min-height: 2.55rem; padding: .4rem 1rem; border-radius: 999px; border: 1px solid #496544; font-weight: 700; cursor: pointer; }
.primary-button { background: #496544; color: #fffdf7; }
.outline-button { background: transparent; color: #496544; }
.primary-button:disabled, .outline-button:disabled { opacity: .45; cursor: not-allowed; }
.preview-empty { display: grid; min-height: 12rem; place-items: center; color: #718076; font-size: .8rem; text-align: center; }
.preview-summary { margin: 0 0 .8rem; color: #718076; font-size: .75rem; }
.preview-list { display: grid; gap: .75rem; margin: 0; padding: 0; list-style: none; }
.preview-list > li { display: grid; grid-template-columns: 2rem minmax(0, 1fr); gap: .7rem; padding: .75rem; background: #f7f4ec; border-radius: .6rem; }
.preview-rank { color: #92a879; font-size: 1rem; font-weight: 700; }
.preview-body header { display: flex; justify-content: space-between; gap: 1rem; }
.preview-body header span { color: #496544; font-size: .8rem; font-weight: 700; }
.preview-body ul { display: grid; gap: .25rem; margin: .45rem 0 0; padding-left: 1rem; color: #718076; font-size: .72rem; line-height: 1.5; }
.preview-body p { margin: .4rem 0 0; color: #a8442f; font-size: .7rem; }
</style>
