import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as careApi from '../api/care'
import type { CareArchive, CareNotification, CareTask, ReEvaluation } from '../types/care'
import { createRequestGuard } from '@/shared/state/requestGuard'
import { registerSessionReset } from '@/shared/state/sessionRegistry'

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
  const requestGuard = createRequestGuard()

  const hasPlants = computed(() => archives.value.length > 0)

  /** 全部植物的待办合计，"我的植物"入口上显示 */
  const totalPending = computed(() =>
    archives.value.reduce((sum, item) => sum + item.pendingCount, 0),
  )

  const totalOverdue = computed(() =>
    archives.value.reduce((sum, item) => sum + item.overdueCount, 0),
  )

  async function loadArchives() {
    const token = requestGuard.begin('archives')
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await careApi.fetchArchives()
      if (requestGuard.isCurrent(token)) archives.value = result
    } catch (error) {
      if (requestGuard.isCurrent(token)) {
        errorMessage.value = error instanceof Error ? error.message : '养护档案加载失败'
      }
    } finally {
      if (requestGuard.isCurrent(token)) loading.value = false
    }
  }

  async function loadArchive(id: number) {
    const token = requestGuard.begin('archive')
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await careApi.fetchArchive(id)
      if (requestGuard.isCurrent(token)) {
        current.value = result
        tasks.value = result.tasks ?? []
      }
    } catch (error) {
      if (requestGuard.isCurrent(token)) {
        errorMessage.value = error instanceof Error ? error.message : '养护档案加载失败'
        current.value = null
        tasks.value = []
      }
    } finally {
      if (requestGuard.isCurrent(token)) loading.value = false
    }
  }

  /** 按日期区间拉任务，日历切换月份时用 */
  async function loadTasks(archiveId: number, from?: string, to?: string) {
    const token = requestGuard.begin('tasks')
    try {
      const result = await careApi.fetchTasks(archiveId, from, to)
      if (requestGuard.isCurrent(token)) tasks.value = result
    } catch (error) {
      if (requestGuard.isCurrent(token)) {
        errorMessage.value = error instanceof Error ? error.message : '任务加载失败'
      }
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
    const session = requestGuard.captureSession()
    submitting.value = true
    errorMessage.value = ''
    try {
      await action()
      if (!requestGuard.isSessionCurrent(session)) return false
      await loadArchive(archiveId)
      return true
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : failMessage
      }
      return false
    } finally {
      if (requestGuard.isSessionCurrent(session)) submitting.value = false
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
    const session = requestGuard.captureSession()
    submitting.value = true
    errorMessage.value = ''
    try {
      await careApi.addNote(archiveId, content, image)
      if (!requestGuard.isSessionCurrent(session)) return false
      await loadArchive(archiveId)
      return true
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '记录保存失败'
      }
      return false
    } finally {
      if (requestGuard.isSessionCurrent(session)) submitting.value = false
    }
  }

  async function reEvaluate(archiveId: number) {
    const session = requestGuard.captureSession()
    submitting.value = true
    errorMessage.value = ''
    try {
      const result = await careApi.reEvaluate(archiveId)
      if (!requestGuard.isSessionCurrent(session)) return null
      lastEvaluation.value = result
      await loadArchive(archiveId)
      return lastEvaluation.value
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '重新评估失败'
      }
      return null
    } finally {
      if (requestGuard.isSessionCurrent(session)) submitting.value = false
    }
  }

  async function reportHealth(archiveId: number, symptom: string, detail?: string) {
    const session = requestGuard.captureSession()
    submitting.value = true
    errorMessage.value = ''
    try {
      const result = await careApi.reportHealth(archiveId, symptom, detail)
      if (!requestGuard.isSessionCurrent(session)) return null
      lastEvaluation.value = result
      await loadArchive(archiveId)
      return lastEvaluation.value
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '反馈提交失败'
      }
      return null
    } finally {
      if (requestGuard.isSessionCurrent(session)) submitting.value = false
    }
  }

  function dismissEvaluation() {
    lastEvaluation.value = null
  }

  // ===== 提醒 =====

  async function loadNotifications() {
    const token = requestGuard.begin('notifications')
    try {
      const result = await careApi.fetchNotifications()
      if (requestGuard.isCurrent(token)) {
        notifications.value = result
        unreadCount.value = result.filter((item) => !item.read).length
      }
    } catch (error) {
      if (requestGuard.isCurrent(token)) {
        errorMessage.value = error instanceof Error ? error.message : '提醒加载失败'
      }
    }
  }

  /** 只拉未读数，Header 铃铛用。失败静默——角标不该拦住任何操作 */
  async function loadUnreadCount() {
    const session = requestGuard.captureSession()
    try {
      const result = await careApi.fetchUnreadCount()
      if (requestGuard.isSessionCurrent(session)) unreadCount.value = result
    } catch {
      // 忽略
    }
  }

  async function markRead(id: number) {
    const session = requestGuard.captureSession()
    try {
      await careApi.markNotificationRead(id)
      if (!requestGuard.isSessionCurrent(session)) return
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
    const session = requestGuard.captureSession()
    try {
      await careApi.markAllNotificationsRead()
      if (requestGuard.isSessionCurrent(session)) {
        notifications.value.forEach((item) => (item.read = true))
        unreadCount.value = 0
      }
    } catch (error) {
      if (requestGuard.isSessionCurrent(session)) {
        errorMessage.value = error instanceof Error ? error.message : '操作失败'
      }
    }
  }

  /** 退出登录时清空，养护数据是私有的 */
  function reset() {
    requestGuard.reset()
    archives.value = []
    current.value = null
    tasks.value = []
    notifications.value = []
    unreadCount.value = 0
    lastEvaluation.value = null
    errorMessage.value = ''
    loading.value = false
    submitting.value = false
  }

  registerSessionReset(reset)

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
