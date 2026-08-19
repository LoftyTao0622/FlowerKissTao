import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  fetchLatestRecommendation,
  generateRecommendation,
  markRecommendationClicked,
} from '../api/recommendation'
import type { Recommendation, RecItem } from '../types/recommendation'

export type RecommendationStatus = 'idle' | 'error' | 'generating' | 'success'

/**
 * 推荐结果的状态容器。
 *
 * <p>第③步之前这里写死了 6 株植物和一套 4 个布尔判断的打分规则。现在算法全部
 * 在后端 {@code RecommendationEngine} 里，这个 store 只负责调接口和存结果——
 * 它不该知道"什么样的植物适合什么环境"，那是领域逻辑，不是视图状态。
 */
export const useRecommendationStore = defineStore('recommendation', () => {
  const status = ref<RecommendationStatus>('idle')
  const result = ref<Recommendation | null>(null)
  const errorMessage = ref('')

  /**
   * 防竞态用的序号：用户连点两次"生成"时，先发的那次若后返回，
   * 会把新结果覆盖掉。带上序号，过期的响应直接丢弃。
   */
  let generationId = 0

  const isGenerating = computed(() => status.value === 'generating')

  /** Top-N 条目。没有结果时是空数组，模板不必到处判 null */
  const items = computed<RecItem[]>(() => result.value?.items ?? [])

  /** 硬过滤后一株都不剩。这与"还没生成"是两回事，界面要分开处理 */
  const isEmptyResult = computed(
    () => status.value === 'success' && items.value.length === 0,
  )

  /** 有排除记录的诊断项，按排除数从多到少。无候选时的漏斗展示用 */
  const activeDiagnostics = computed(() =>
    (result.value?.diagnostics ?? [])
      .filter((item) => item.excluded > 0)
      .sort((a, b) => b.excluded - a.excluded),
  )

  async function generate(profileId?: number) {
    const current = ++generationId
    status.value = 'generating'
    errorMessage.value = ''

    try {
      const data = await generateRecommendation(profileId)
      if (current !== generationId) return false
      result.value = data
      status.value = 'success'
      return true
    } catch (error) {
      if (current !== generationId) return false
      errorMessage.value =
        error instanceof Error ? error.message : '推荐生成失败，请稍后重试'
      status.value = 'error'
      return false
    }
  }

  /** 载入上一次推荐。没有推荐过时保持 idle，由页面引导去填问卷 */
  async function loadLatest() {
    const current = ++generationId
    try {
      const data = await fetchLatestRecommendation()
      if (current !== generationId) return
      if (data) {
        result.value = data
        status.value = 'success'
      }
    } catch {
      // 读历史失败不该拦住用户重新生成，静默即可
    }
  }

  /** 记录点击。埋点失败不能拦住跳转，所以吞掉异常 */
  async function trackClick(itemId: number) {
    const resultId = result.value?.resultId
    if (!resultId) return
    try {
      await markRecommendationClicked(resultId, itemId)
    } catch {
      // 忽略：看板少一条点击记录，远好过用户点不进详情页
    }
  }

  function reset() {
    generationId += 1
    status.value = 'idle'
    result.value = null
    errorMessage.value = ''
  }

  return {
    status,
    result,
    errorMessage,
    isGenerating,
    items,
    isEmptyResult,
    activeDiagnostics,
    generate,
    loadLatest,
    trackClick,
    reset,
  }
})
