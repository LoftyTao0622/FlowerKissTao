import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as careApi from '../api/care'
import type { CareArchive, CareNotification, CareTask, ReEvaluation } from '../types/care'

/**
 * 养护档案与提醒。
 *
 * <p>排期与调频规则全在后端，这个 store 只负责调接口和存结果——
 * 它不该知道"光照变暗时浇水该隔几天"，那是领域逻辑。
 */
export const useCareStore = defineStore('care', () => {
  const archives = ref<CareArchive[]>([])
  const current = ref<CareArchive | null>(null)
  const tasks = ref<CareTask[]>([])
  const notifications = ref<CareNotification[]>([])
  const unreadCount = ref(0)

  const loading = ref(false)
  const submitting = ref(false)
  const errorMessage = ref('')

  /** 最近一次重评估的结果，页面上弹一个说明 */
  const lastEvaluation = ref<ReEvaluation | null>(null)

  const hasPlants = computed(() => archives.value.length > 0)

  /** 全部植物的待办合计，"我的植物"入口上显示 */
  const totalPending = computed(() =>
    archives.value.reduce((sum, item) => sum + item.pendingCount, 0),
  )

  const totalOverdue = computed(() =>
    archives.value.reduce((sum, item) => sum + item.overdueCount, 0),
  )

  async function loadArchives() {
    loading.value = true
    errorMessage.value = ''
    try {
      archives.value = await careApi.fetchArchives()
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '养护档案加载失败'
    } finally {
      loading.value = false
    }
  }

  async function loadArchive(id: number) {
    loading.value = true
    errorMessage.value = ''
    try {
      current.value = await careApi.fetchArchive(id)
      tasks.value = current.value.tasks ?? []
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '养护档案加载失败'
      current.value = null
      tasks.value = []
    } finally {
      loading.value = false
    }
  }

  /** 按日期区间拉任务，日历切换月份时用 */
  async function loadTasks(archiveId: number, from?: string, to?: string) {
    try {
      tasks.value = await careApi.fetchTasks(archiveId, from, to)
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '任务加载失败'
    }
  }

  /**
   * 任务操作后重新拉详情而不是本地改状态：完成任务会清零连续遗漏、
   * 重排可能改动其他任务，本地拼的话这些都对不上。
   */
  async function runTaskAction(
    action: () => Promise<void>,
    archiveId: number,
    failMessage: string,
  ) {
    submitting.value = true
    errorMessage.value = ''
    try {
      await action()
      await loadArchive(archiveId)
      return true
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : failMessage
      return false
    } finally {
      submitting.value = false
    }
  }

  function completeTask(taskId: number, archiveId: number, note?: string) {
    return runTaskAction(() => careApi.completeTask(taskId, note), archiveId, '操作失败')
  }

  function skipTask(taskId: number, archiveId: number) {
    return runTaskAction(() => careApi.skipTask(taskId), archiveId, '跳过失败')
  }

  function postponeTask(taskId: number, archiveId: number, days = 1) {
    return runTaskAction(() => careApi.postponeTask(taskId, days), archiveId, '延后失败')
  }

  async function addNote(archiveId: number, content?: string, image?: string) {
    submitting.value = true
    errorMessage.value = ''
    try {
      await careApi.addNote(archiveId, content, image)
      await loadArchive(archiveId)
      return true
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '记录保存失败'
      return false
    } finally {
      submitting.value = false
    }
  }

  async function reEvaluate(archiveId: number) {
    submitting.value = true
    errorMessage.value = ''
    try {
      lastEvaluation.value = await careApi.reEvaluate(archiveId)
      await loadArchive(archiveId)
      return lastEvaluation.value
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '重新评估失败'
      return null
    } finally {
      submitting.value = false
    }
  }

  async function reportHealth(archiveId: number, symptom: string, detail?: string) {
    submitting.value = true
    errorMessage.value = ''
    try {
      lastEvaluation.value = await careApi.reportHealth(archiveId, symptom, detail)
      await loadArchive(archiveId)
      return lastEvaluation.value
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '反馈提交失败'
      return null
    } finally {
      submitting.value = false
    }
  }

  function dismissEvaluation() {
    lastEvaluation.value = null
  }

  // ===== 提醒 =====

  async function loadNotifications() {
    try {
      notifications.value = await careApi.fetchNotifications()
      unreadCount.value = notifications.value.filter((item) => !item.read).length
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '提醒加载失败'
    }
  }

  /** 只拉未读数，Header 铃铛用。失败静默——角标不该拦住任何操作 */
  async function loadUnreadCount() {
    try {
      unreadCount.value = await careApi.fetchUnreadCount()
    } catch {
      // 忽略
    }
  }

  async function markRead(id: number) {
    try {
      await careApi.markNotificationRead(id)
      const target = notifications.value.find((item) => item.id === id)
      if (target && !target.read) {
        target.read = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch {
      // 忽略
    }
  }

  async function markAllRead() {
    try {
      await careApi.markAllNotificationsRead()
      notifications.value.forEach((item) => (item.read = true))
      unreadCount.value = 0
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '操作失败'
    }
  }

  /** 退出登录时清空，养护数据是私有的 */
  function reset() {
    archives.value = []
    current.value = null
    tasks.value = []
    notifications.value = []
    unreadCount.value = 0
    lastEvaluation.value = null
    errorMessage.value = ''
  }

  return {
    archives,
    current,
    tasks,
    notifications,
    unreadCount,
    loading,
    submitting,
    errorMessage,
    lastEvaluation,
    hasPlants,
    totalPending,
    totalOverdue,
    loadArchives,
    loadArchive,
    loadTasks,
    completeTask,
    skipTask,
    postponeTask,
    addNote,
    reEvaluate,
    reportHealth,
    dismissEvaluation,
    loadNotifications,
    loadUnreadCount,
    markRead,
    markAllRead,
    reset,
  }
})
