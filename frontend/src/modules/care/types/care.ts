/** 与后端 care/web/vo/*.java 一一对应 */

/** 任务状态。数值与后端 CareTask 常量一致 */
export const CareTaskStatus = {
  PENDING: 0,
  DONE: 1,
  SKIPPED: 2,
  OVERDUE: 3,
} as const

/** 六类任务，与后端 CareTaskType 同名 */
export type CareTaskTypeCode =
  | 'water'
  | 'fertilize'
  | 'repot'
  | 'prune'
  | 'rotate'
  | 'pest'

/** 与后端 CareTaskVO 对应 */
export interface CareTask {
  id: number
  archiveId: number
  taskType: CareTaskTypeCode
  typeLabel: string
  icon: string
  title: string
  /** 操作方法、用量或注意事项 */
  instruction: string
  dueDate: string
  status: number
  statusLabel: string
  completedAt: string | null
  note: string | null
  /** 距今天几天。负数表示已过期 */
  daysFromToday: number
}

/** 与后端 CareNoteVO 对应 */
export interface CareNote {
  id: number
  /** text 文字 / photo 带图 / health 健康反馈 */
  noteType: string
  content: string | null
  /** base64 图片。列表接口不返回，只在新增时回显 */
  image: string | null
  hasImage: boolean
  createdAt: string
}

/** 与后端 CareArchiveVO 对应 */
export interface CareArchive {
  id: number
  speciesId: number
  slug: string | null
  plantName: string
  plantImage: string | null
  orderNo: string | null
  adoptedAt: string
  /** 已陪伴天数 */
  adoptedDays: number
  status: number

  pendingCount: number
  overdueCount: number
  nextTask: CareTask | null

  /** 当前浇水间隔（已含季节与环境调整） */
  waterIntervalDays: number | null
  /** 频率调整系数，100 表示未调整 */
  waterFactor: number
  /** 调整说明，未调整时为 null */
  adjustmentNote: string | null
  missedCount: number
  lightLevelSnapshot: number | null
  currentLightLevel: number | null

  /** 详情页才有 */
  tasks?: CareTask[]
  notes?: CareNote[]
}

/** 与后端 ReEvaluationVO 对应 */
export interface ReEvaluation {
  /** missed / light / health / manual */
  trigger: string
  triggerLabel: string
  /** 频率有没有真的变 */
  adjusted: boolean
  oldInterval: number | null
  newInterval: number | null
  summary: string
  /** 逐项排查建议 */
  checklist: string[]
  rescheduledCount: number
}

/** 与后端 CareNotificationVO 对应 */
export interface CareNotification {
  id: number
  /** task_due / overdue / health / re_eval */
  type: string
  title: string
  content: string
  archiveId: number | null
  taskId: number | null
  read: boolean
  createdAt: string
}

/** 健康反馈的症状选项，与后端 ReEvaluationRules.checklistFor 的分支对应 */
export const SYMPTOMS = [
  { value: 'yellowing', label: '叶片发黄' },
  { value: 'wilting', label: '植株萎蔫' },
  { value: 'spots', label: '叶片有斑点' },
  { value: 'dropping', label: '掉叶' },
  { value: 'pest', label: '疑似虫害' },
] as const
