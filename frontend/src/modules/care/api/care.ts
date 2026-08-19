import { request } from '@/shared/api/request'

import type {
  CareArchive,
  CareNote,
  CareNotification,
  CareTask,
  ReEvaluation,
} from '../types/care'

// ===== 我的植物 =====

export function fetchArchives() {
  return request<CareArchive[]>('/care/archives')
}

export function fetchArchive(id: number) {
  return request<CareArchive>(`/care/archives/${id}`)
}

/** 任务列表，日历数据源。不传日期时后端取未来 30 天 */
export function fetchTasks(archiveId: number, from?: string, to?: string) {
  return request<CareTask[]>(`/care/archives/${archiveId}/tasks`, {
    query: { from, to },
  })
}

/** 手动重新评估。改完场景问卷立刻能看到养护频率变化 */
export function reEvaluate(archiveId: number) {
  return request<ReEvaluation>(`/care/archives/${archiveId}/re-evaluate`, {
    method: 'POST',
  })
}

/** 报告叶片发黄等症状，返回逐项排查建议 */
export function reportHealth(archiveId: number, symptom: string, detail?: string) {
  return request<ReEvaluation>(`/care/archives/${archiveId}/health-report`, {
    method: 'POST',
    body: { symptom, detail },
  })
}

// ===== 任务操作 =====

export function completeTask(taskId: number, note?: string) {
  return request<void>(`/care/tasks/${taskId}/complete`, {
    method: 'POST',
    body: { note },
  })
}

export function skipTask(taskId: number) {
  return request<void>(`/care/tasks/${taskId}/skip`, { method: 'POST' })
}

export function postponeTask(taskId: number, days = 1) {
  return request<void>(`/care/tasks/${taskId}/postpone`, {
    method: 'PUT',
    body: { days },
  })
}

// ===== 成长记录 =====

/** image 为 base64（可带 data: 前缀），后端限 300KB */
export function addNote(archiveId: number, content?: string, image?: string) {
  return request<CareNote>(`/care/archives/${archiveId}/notes`, {
    method: 'POST',
    body: { content, image },
  })
}

// ===== 站内提醒 =====

export function fetchNotifications() {
  return request<CareNotification[]>('/care/notifications')
}

/** 铃铛角标用。单独一个接口，免得为了一个数字拉整个列表 */
export function fetchUnreadCount() {
  return request<number>('/care/notifications/unread-count')
}

export function markNotificationRead(id: number) {
  return request<void>(`/care/notifications/${id}/read`, { method: 'PUT' })
}

export function markAllNotificationsRead() {
  return request<void>('/care/notifications/read-all', { method: 'PUT' })
}
