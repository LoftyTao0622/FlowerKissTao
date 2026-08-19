<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessageBox } from 'element-plus'

import { useCareStore } from '../stores/care'
import { SYMPTOMS } from '../types/care'
import type { CareTask } from '../types/care'

const props = defineProps<{ archiveId: string }>()

const careStore = useCareStore()
const plant = computed(() => careStore.current)

/** 成长记录表单 */
const noteContent = ref('')
const noteImage = ref<string | null>(null)
const noteError = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

/** 图片上限 300KB，与后端校验一致——前端先拦一道，省一次失败的往返 */
const MAX_IMAGE_BYTES = 300 * 1024

const archiveIdNum = computed(() => Number(props.archiveId))

/** 未完成的任务，按日期升序。日历主区展示它 */
const openTasks = computed(() =>
  careStore.tasks.filter((task) => task.status === 0 || task.status === 3),
)

/** 已处理的任务，折叠在下方 */
const doneTasks = computed(() =>
  careStore.tasks.filter((task) => task.status === 1 || task.status === 2),
)

/**
 * 按日期分组，日历视图一天一组。
 *
 * 把 daysFromToday 提到组上而不是在模板里取 tasks[0]——同一天的任务这个值本来就相同，
 * 提上来既省一次索引，也不必在模板里应付 noUncheckedIndexedAccess。
 */
const tasksByDate = computed(() => {
  const groups = new Map<string, { date: string; days: number; tasks: CareTask[] }>()
  openTasks.value.forEach((task) => {
    const group = groups.get(task.dueDate)
    if (group) {
      group.tasks.push(task)
    } else {
      groups.set(task.dueDate, {
        date: task.dueDate,
        days: task.daysFromToday,
        tasks: [task],
      })
    }
  })
  return [...groups.values()].sort((a, b) => a.date.localeCompare(b.date))
})

function dueLabel(days: number) {
  if (days < 0) return `逾期 ${Math.abs(days)} 天`
  if (days === 0) return '今天'
  if (days === 1) return '明天'
  return `${days} 天后`
}

function formatDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'short' })
}

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', { hour12: false })
}

// ===== 任务操作 =====

async function complete(task: CareTask) {
  try {
    const result = await ElMessageBox.prompt(
      `${task.instruction}`,
      `完成「${task.title}」`,
      {
        confirmButtonText: '标记完成',
        cancelButtonText: '取消',
        inputPlaceholder: '可选：记一句本次的情况',
        inputValidator: () => true,
      },
    )
    await careStore.completeTask(task.id, archiveIdNum.value, result.value || undefined)
  } catch {
    // 取消
  }
}

async function skip(task: CareTask) {
  try {
    await ElMessageBox.confirm(
      '跳过会计入连续遗漏，连续两次后系统会自动放宽养护频率。',
      `跳过「${task.title}」`,
      { confirmButtonText: '跳过', cancelButtonText: '再想想' },
    )
    await careStore.skipTask(task.id, archiveIdNum.value)
  } catch {
    // 取消
  }
}

async function postpone(task: CareTask) {
  await careStore.postponeTask(task.id, archiveIdNum.value, 3)
}

// ===== 重新评估与健康反馈 =====

async function reEvaluate() {
  await careStore.reEvaluate(archiveIdNum.value)
}

async function reportHealth() {
  try {
    const { value } = await ElMessageBox.prompt(
      '选择最接近的症状编号：\n' +
        SYMPTOMS.map((s, i) => `${i + 1}. ${s.label}`).join('\n'),
      '植物状态反馈',
      {
        confirmButtonText: '获取排查建议',
        cancelButtonText: '取消',
        inputPlaceholder: '输入 1-5',
        inputValidator: (input) =>
          (/^[1-5]$/.test(input ?? '') ? true : '请输入 1 到 5'),
      },
    )
    const symptom = SYMPTOMS[Number(value) - 1]
    if (symptom) {
      await careStore.reportHealth(archiveIdNum.value, symptom.value)
    }
  } catch {
    // 取消
  }
}

// ===== 成长记录 =====

function pickImage() {
  fileInput.value?.click()
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  noteError.value = ''
  if (file.size > MAX_IMAGE_BYTES) {
    noteError.value = `图片不能超过 300KB，当前 ${Math.round(file.size / 1024)}KB，请压缩后再试`
    input.value = ''
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    noteImage.value = typeof reader.result === 'string' ? reader.result : null
  }
  reader.onerror = () => {
    noteError.value = '图片读取失败，请换一张试试'
  }
  reader.readAsDataURL(file)
}

function clearImage() {
  noteImage.value = null
  if (fileInput.value) fileInput.value.value = ''
}

async function submitNote() {
  noteError.value = ''
  if (!noteContent.value.trim() && !noteImage.value) {
    noteError.value = '写点什么或选一张图片吧'
    return
  }
  const ok = await careStore.addNote(
    archiveIdNum.value,
    noteContent.value.trim() || undefined,
    noteImage.value ?? undefined,
  )
  if (ok) {
    noteContent.value = ''
    clearImage()
  } else {
    noteError.value = careStore.errorMessage
  }
}

onMounted(() => {
  void careStore.loadArchive(archiveIdNum.value)
})
</script>

<template>
  <section class="care-page">
    <p v-if="careStore.loading" class="care-page__status" role="status">加载中…</p>

    <div v-else-if="!plant" class="care-page__status">
      <p>这株植物的养护档案不存在。</p>
      <RouterLink class="pill-button" :to="{ name: 'my-plants' }">返回我的植物</RouterLink>
    </div>

    <template v-else>
      <header class="care-page__header">
        <div>
          <p class="section-kicker">养护计划</p>
          <h1>{{ plant.plantName }}</h1>
          <p class="care-page__meta">
            已陪伴 {{ plant.adoptedDays }} 天
            <template v-if="plant.waterIntervalDays">
              · 当前每 {{ plant.waterIntervalDays }} 天浇一次
            </template>
            <template v-if="plant.orderNo"> · 订单 {{ plant.orderNo }}</template>
          </p>
        </div>
        <RouterLink class="text-action" :to="{ name: 'my-plants' }">返回列表</RouterLink>
      </header>

      <p v-if="plant.adjustmentNote" class="care-page__adjust">{{ plant.adjustmentNote }}</p>
      <p v-if="careStore.errorMessage" class="care-page__error" role="alert">
        {{ careStore.errorMessage }}
      </p>

      <!-- 重新评估结果 -->
      <div v-if="careStore.lastEvaluation" class="eval-panel" role="status">
        <header>
          <strong>{{ careStore.lastEvaluation.triggerLabel }}</strong>
          <button class="text-action" type="button" @click="careStore.dismissEvaluation()">
            关闭
          </button>
        </header>
        <p class="eval-panel__summary">{{ careStore.lastEvaluation.summary }}</p>
        <p v-if="careStore.lastEvaluation.adjusted" class="eval-panel__delta">
          浇水间隔 {{ careStore.lastEvaluation.oldInterval }} 天 →
          <strong>{{ careStore.lastEvaluation.newInterval }} 天</strong>
          （已重排 {{ careStore.lastEvaluation.rescheduledCount }} 条未来任务，
          历史记录保留）
        </p>
        <ul class="eval-panel__checklist">
          <li v-for="item in careStore.lastEvaluation.checklist" :key="item">{{ item }}</li>
        </ul>
      </div>

      <div class="care-page__actions">
        <button
          class="pill-button pill-button--outline"
          type="button"
          :disabled="careStore.submitting"
          @click="reportHealth"
        >
          植物状态不好？
        </button>
        <button
          class="pill-button pill-button--outline"
          type="button"
          :disabled="careStore.submitting"
          @click="reEvaluate"
        >
          重新评估养护频率
        </button>
      </div>

      <div class="care-page__layout">
        <!-- 任务日历 -->
        <section class="care-block">
          <h2>养护日历</h2>
          <p class="care-block__hint">按建议日期排列，逾期的排在最前。</p>

          <p v-if="!tasksByDate.length" class="care-block__empty">近期没有待办任务。</p>

          <ol v-else class="calendar">
            <li v-for="group in tasksByDate" :key="group.date" class="calendar__day">
              <div class="calendar__date">
                <strong>{{ formatDate(group.date) }}</strong>
                <small :class="{ 'is-overdue': group.days < 0 }">
                  {{ dueLabel(group.days) }}
                </small>
              </div>

              <ul class="calendar__tasks">
                <li
                  v-for="task in group.tasks"
                  :key="task.id"
                  :class="['task-card', { 'task-card--overdue': task.status === 3 }]"
                >
                  <span class="task-card__icon" aria-hidden="true">{{ task.icon }}</span>
                  <div class="task-card__body">
                    <strong>{{ task.title }}</strong>
                    <span class="task-card__type">{{ task.typeLabel }}</span>
                    <p>{{ task.instruction }}</p>
                    <div class="task-card__actions">
                      <button type="button" class="task-btn task-btn--done" @click="complete(task)">
                        完成
                      </button>
                      <button type="button" class="task-btn" @click="postpone(task)">
                        延后 3 天
                      </button>
                      <button type="button" class="task-btn" @click="skip(task)">跳过</button>
                      <!-- 方案要求可从养护任务直接跳到对应指南 -->
                      <RouterLink
                        class="task-btn task-btn--guide"
                        :to="{ name: 'knowledge', query: { taskType: task.taskType } }"
                      >
                        了解为什么 →
                      </RouterLink>
                    </div>
                  </div>
                </li>
              </ul>
            </li>
          </ol>

          <details v-if="doneTasks.length" class="care-block__done">
            <summary>已处理 {{ doneTasks.length }} 项</summary>
            <ul class="done-list">
              <li v-for="task in doneTasks" :key="task.id">
                <span aria-hidden="true">{{ task.icon }}</span>
                <span class="done-list__title">{{ task.title }}</span>
                <span class="done-list__status">{{ task.statusLabel }}</span>
                <small v-if="task.note">{{ task.note }}</small>
              </li>
            </ul>
          </details>
        </section>

        <!-- 成长时间线 -->
        <aside class="care-block">
          <h2>成长时间线</h2>

          <form class="note-form" @submit.prevent="submitNote">
            <textarea
              v-model="noteContent"
              rows="3"
              maxlength="500"
              placeholder="记一句今天的状态，比如「新叶展开了」"
            ></textarea>

            <div v-if="noteImage" class="note-form__preview">
              <img :src="noteImage" alt="待上传的照片预览" />
              <button class="text-action" type="button" @click="clearImage">移除图片</button>
            </div>

            <input
              ref="fileInput"
              class="sr-only"
              type="file"
              accept="image/*"
              @change="onFileChange"
            />

            <p v-if="noteError" class="note-form__error" role="alert">{{ noteError }}</p>

            <div class="note-form__actions">
              <button class="pill-button pill-button--outline" type="button" @click="pickImage">
                选择照片
              </button>
              <button
                class="pill-button pill-button--primary"
                type="submit"
                :disabled="careStore.submitting"
              >
                {{ careStore.submitting ? '保存中…' : '添加记录' }}
              </button>
            </div>
          </form>

          <p v-if="!plant.notes?.length" class="care-block__empty">
            还没有记录。养护过程中的变化值得留下来。
          </p>

          <ol v-else class="timeline">
            <li v-for="note in plant.notes" :key="note.id">
              <span class="timeline__dot" aria-hidden="true"></span>
              <div class="timeline__body">
                <span
                  v-if="note.noteType === 'health'"
                  class="timeline__tag"
                >状态反馈</span>
                <span v-else-if="note.hasImage" class="timeline__tag">带图</span>
                <p v-if="note.content">{{ note.content }}</p>
                <small>{{ formatTime(note.createdAt) }}</small>
              </div>
            </li>
          </ol>
        </aside>
      </div>
    </template>
  </section>
</template>

<style scoped>
.care-page {
  max-width: 64rem;
  margin: 0 auto;
  padding: var(--space-2xl, 3rem) var(--space-lg, 1.25rem);
}

.care-page__status {
  padding: var(--space-2xl, 3rem) 0;
  text-align: center;
  color: var(--color-text-muted, #68716a);
}

.care-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
}

.care-page__header h1 {
  margin: 0.35rem 0 0.3rem;
}

.care-page__meta {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.8rem;
}

.care-page__adjust {
  padding: var(--space-sm, 0.75rem) var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
  background: var(--color-brand-soft, #e4eadb);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-brand, #496544);
  font-size: 0.82rem;
}

.care-page__error {
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
  background: var(--color-danger-soft, #f6e3de);
  border-radius: var(--radius-card, 0.75rem);
  color: var(--color-danger, #a8442f);
}

.eval-panel {
  padding: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-brand, #496544);
  border-radius: var(--radius-card, 0.75rem);
}

.eval-panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.4rem;
}

.eval-panel__summary {
  margin: 0 0 0.4rem;
  font-size: 0.88rem;
}

.eval-panel__delta {
  margin: 0 0 0.5rem;
  color: var(--color-brand, #496544);
  font-size: 0.82rem;
}

.eval-panel__checklist {
  display: grid;
  gap: 0.35rem;
  margin: 0;
  padding-left: 1.1rem;
  font-size: 0.8rem;
  line-height: 1.6;
  color: var(--color-text-muted, #68716a);
}

.care-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm, 0.75rem);
  margin-bottom: var(--space-lg, 1.5rem);
}

.care-page__layout {
  display: grid;
  gap: var(--space-lg, 1.5rem);
}

@media (min-width: 56rem) {
  .care-page__layout {
    grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr);
    align-items: start;
  }
}

.care-block {
  padding: var(--space-md, 1rem);
  background: var(--color-surface, #fffdf7);
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-card, 0.75rem);
}

.care-block h2 {
  margin: 0 0 0.3rem;
  font-size: 1rem;
}

.care-block__hint {
  margin: 0 0 var(--space-md, 1rem);
  color: var(--color-text-muted, #68716a);
  font-size: 0.75rem;
}

.care-block__empty {
  padding: var(--space-lg, 1.5rem) 0;
  text-align: center;
  color: var(--color-text-muted, #68716a);
  font-size: 0.85rem;
}

.calendar {
  display: grid;
  gap: var(--space-md, 1rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

.calendar__date {
  display: flex;
  align-items: baseline;
  gap: var(--space-sm, 0.75rem);
  margin-bottom: 0.4rem;
  padding-bottom: 0.25rem;
  border-bottom: 1px dashed var(--color-border, #d9d3c5);
}

.calendar__date strong {
  font-size: 0.85rem;
}

.calendar__date small {
  color: var(--color-text-muted, #68716a);
  font-size: 0.72rem;
}

.calendar__date small.is-overdue {
  color: var(--color-danger, #a8442f);
  font-weight: 700;
}

.calendar__tasks {
  display: grid;
  gap: var(--space-sm, 0.75rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

.task-card {
  display: grid;
  grid-template-columns: 1.8rem minmax(0, 1fr);
  gap: var(--space-sm, 0.75rem);
  padding: var(--space-sm, 0.75rem);
  background: var(--color-bg, #f7f4ec);
  border-radius: var(--radius-card, 0.75rem);
}

.task-card--overdue {
  background: var(--color-danger-soft, #f6e3de);
}

.task-card__icon {
  font-size: 1.1rem;
}

.task-card__body strong {
  font-size: 0.88rem;
}

.task-card__type {
  margin-left: 0.4rem;
  padding: 0.05rem 0.4rem;
  background: var(--color-brand-soft, #e4eadb);
  border-radius: var(--radius-pill, 999px);
  color: var(--color-brand, #496544);
  font-size: 0.65rem;
}

.task-card__body p {
  margin: 0.3rem 0 0.5rem;
  color: var(--color-text-muted, #68716a);
  font-size: 0.78rem;
  line-height: 1.6;
}

.task-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.task-btn {
  min-height: 2rem;
  padding: 0.25rem 0.75rem;
  background: none;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-pill, 999px);
  cursor: pointer;
  font-size: 0.75rem;
}

.task-btn--done {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
  color: var(--color-on-brand, #fffdf7);
  font-weight: 700;
}

.task-btn--guide {
  display: inline-flex;
  align-items: center;
  color: var(--color-brand, #496544);
  text-decoration: none;
}

.care-block__done {
  margin-top: var(--space-md, 1rem);
}

.care-block__done summary {
  cursor: pointer;
  color: var(--color-text-muted, #68716a);
  font-size: 0.8rem;
}

.done-list {
  display: grid;
  gap: 0.35rem;
  margin: var(--space-sm, 0.75rem) 0 0;
  padding: 0;
  list-style: none;
  font-size: 0.78rem;
}

.done-list li {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.4rem;
  color: var(--color-text-muted, #68716a);
}

.done-list__title {
  text-decoration: line-through;
}

.done-list__status {
  color: var(--color-brand, #496544);
  font-size: 0.7rem;
}

.note-form {
  display: grid;
  gap: var(--space-sm, 0.75rem);
  margin-bottom: var(--space-md, 1rem);
}

.note-form textarea {
  padding: 0.5rem 0.7rem;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-sm, 0.5rem);
  font: inherit;
  font-size: 0.85rem;
  resize: vertical;
}

.note-form__preview {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.75rem);
}

.note-form__preview img {
  width: 4rem;
  height: 4rem;
  object-fit: cover;
  border-radius: var(--radius-sm, 0.5rem);
}

.note-form__error {
  margin: 0;
  color: var(--color-danger, #a8442f);
  font-size: 0.78rem;
}

.note-form__actions {
  display: flex;
  gap: var(--space-sm, 0.75rem);
}

.timeline {
  display: grid;
  margin: 0;
  padding: 0;
  list-style: none;
}

.timeline li {
  display: grid;
  grid-template-columns: 1rem minmax(0, 1fr);
  gap: var(--space-sm, 0.75rem);
  position: relative;
  padding-bottom: var(--space-md, 1rem);
}

.timeline li:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 0.28rem;
  top: 0.9rem;
  bottom: 0;
  width: 2px;
  background: var(--color-border, #d9d3c5);
}

.timeline__dot {
  width: 0.6rem;
  height: 0.6rem;
  margin-top: 0.3rem;
  border-radius: 50%;
  background: var(--color-brand, #496544);
}

.timeline__tag {
  display: inline-block;
  margin-bottom: 0.2rem;
  padding: 0.05rem 0.4rem;
  background: var(--color-brand-soft, #e4eadb);
  border-radius: var(--radius-pill, 999px);
  color: var(--color-brand, #496544);
  font-size: 0.65rem;
}

.timeline__body p {
  margin: 0;
  font-size: 0.82rem;
  line-height: 1.6;
}

.timeline__body small {
  color: var(--color-text-muted, #68716a);
  font-size: 0.7rem;
}

.pill-button--primary {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
  color: var(--color-on-brand, #fffdf7);
}
</style>
