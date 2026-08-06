<script setup lang="ts">
import { computed, nextTick, ref, type ComponentPublicInstance } from 'vue'
import {
  Bell,
  Calendar,
  Check,
  CircleCheck,
  MagicStick,
  Pouring,
  Refresh,
  Sunny,
  WarningFilled,
} from '@element-plus/icons-vue'

interface CareTask {
  id: string
  timing: string
  plant: string
  title: string
  detail: string
  category: '浇水' | '光照' | '施肥' | '换盆'
}

const careTasks: CareTask[] = [
  {
    id: 'soil-check',
    timing: '今天 · 晚饭后',
    plant: '琴叶榕',
    title: '先摸盆土，再决定要不要浇水',
    detail: '手指探入表土约 2 厘米；仍有凉湿感就跳过，不为“打卡”勉强浇水。',
    category: '浇水',
  },
  {
    id: 'turn-pot',
    timing: '本周六 · 上午',
    plant: '虎尾兰',
    title: '将花盆转动四分之一圈',
    detail: '沿同一方向轻转，让新叶受光更均匀，同时查看叶基部是否松软。',
    category: '光照',
  },
  {
    id: 'leaf-review',
    timing: '7 天后',
    plant: '青苹果竹芋',
    title: '复查新叶与叶缘状态',
    detail: '记录卷叶、焦边是否继续发展；先排查空调直吹和空气干燥，再调整浇水。',
    category: '光照',
  },
  {
    id: 'root-review',
    timing: '14 天后',
    plant: '金边吊兰',
    title: '检查根系是否挤满花盆',
    detail: '只观察排水孔和盆土吸水速度，不要为了确认而频繁把整株拔出。',
    category: '换盆',
  },
]

const guideItems = [
  {
    id: 'watering',
    number: '01',
    label: '浇水',
    title: '让盆土告诉你时间',
    summary: '日期只能提醒你去观察，不能直接决定要不要浇。',
    signal: '表土干到适合该植物的深度，花盆明显变轻，叶片还没有严重萎蔫。',
    rhythm: '每次提醒时检查；实际浇水间隔会随温度、光照和盆器改变。',
    steps: [
      '用手指或竹签检查表土下方，不只看表面颜色。',
      '确认需要后，沿盆土缓慢浇透，直到盆底刚有水流出。',
      '约 10 分钟后倒掉托盘积水，避免根系长期泡水。',
    ],
    warning: '叶子发黄不等于缺水。盆土湿、叶片软时继续浇水，往往会让问题更严重。',
    icon: Pouring,
    tone: 'pale',
  },
  {
    id: 'light',
    number: '02',
    label: '光照',
    title: '观察一整天，而非一瞬间',
    summary: '同一扇窗在上午和下午可能是两种环境，季节也会改变光线角度。',
    signal: '新叶变小、叶柄拉长可能在找光；叶面出现发白干斑则可能是突然暴晒。',
    rhythm: '每周观察一次株型；季节转换或移动位置后的两周内提高观察频率。',
    steps: [
      '在上午、中午、下午各看一次叶面是否出现明显光斑。',
      '需要增加光照时分阶段靠近窗户，不要一次跨越太大距离。',
      '每周沿同一方向旋盆约四分之一圈，避免植株长期单侧生长。',
    ],
    warning: '刚到家的植物需要适应。即使品种喜光，也不要立刻从室内弱光移到正午直晒处。',
    icon: Sunny,
    tone: 'sage',
  },
  {
    id: 'feeding',
    number: '03',
    label: '施肥',
    title: '生长稳定，再补充营养',
    summary: '肥料不是急救药。根系、光照或浇水有问题时，先恢复环境。',
    signal: '处在生长期、持续长出健康新叶，且近期没有换盆或明显病害。',
    rhythm: '按肥料标签的适用植物和最低建议浓度开始，宁淡勿浓。',
    steps: [
      '先确认盆土微湿，避免在完全干燥的根系上直接施肥。',
      '严格按标签稀释，不凭感觉增加浓度或混用多种肥料。',
      '施肥后记录日期和浓度，接下来两周观察叶尖与新叶。',
    ],
    warning: '刚换盆、根系受损、极端高温或生长停滞时先暂停施肥，给植物恢复时间。',
    icon: MagicStick,
    tone: 'leaf',
  },
  {
    id: 'repotting',
    number: '04',
    label: '换盆',
    title: '为根系多留一点余地',
    summary: '换盆看根系状态，不以“买回家就换”为默认动作。',
    signal: '根系持续钻出排水孔、盆土很快干透，或水直接从边缘流走且植株明显头重脚轻。',
    rhythm: '每个生长季检查一到两次即可；没有拥挤信号时不必频繁打扰。',
    steps: [
      '选择只比原盆直径大约 2–4 厘米、底部有排水孔的新盆。',
      '尽量保留健康根团，剪除腐烂根后再按原来的栽植深度上盆。',
      '放回稳定的散射光处缓苗，短期内避免施肥和频繁搬动。',
    ],
    warning: '盆器一次增大太多会让多余土壤长期保水。对小根系来说，“更大的家”不一定更安全。',
    icon: Refresh,
    tone: 'forest',
  },
]

const completedIds = ref<Set<string>>(new Set())
const liveMessage = ref('')
const taskButtonRefs = new Map<string, HTMLButtonElement>()
const emptyStateTitleRef = ref<HTMLElement | null>(null)
const pendingTasks = computed(() => careTasks.filter((task) => !completedIds.value.has(task.id)))
const completedCount = computed(() => completedIds.value.size)

function setTaskButtonRef(taskId: string, element: Element | ComponentPublicInstance | null) {
  if (element instanceof HTMLButtonElement) {
    taskButtonRefs.set(taskId, element)
    return
  }

  taskButtonRefs.delete(taskId)
}

async function completeTask(task: CareTask) {
  const completedIndex = pendingTasks.value.findIndex((pendingTask) => pendingTask.id === task.id)
  const nextCompletedIds = new Set(completedIds.value)
  nextCompletedIds.add(task.id)
  completedIds.value = nextCompletedIds
  liveMessage.value = `已完成：${task.plant}，${task.title}`

  await nextTick()

  const nextTask = pendingTasks.value[completedIndex] ?? pendingTasks.value[0]
  if (nextTask) {
    taskButtonRefs.get(nextTask.id)?.focus()
    return
  }

  emptyStateTitleRef.value?.focus()
}

async function completeAllTasks() {
  completedIds.value = new Set(careTasks.map((task) => task.id))
  liveMessage.value = '未来任务已全部完成。'

  await nextTick()
  emptyStateTitleRef.value?.focus()
}

async function restoreTasks() {
  completedIds.value = new Set()
  liveMessage.value = '已恢复全部示例任务。'

  await nextTick()
  const firstTask = pendingTasks.value[0]
  if (firstTask) {
    taskButtonRefs.get(firstTask.id)?.focus()
  }
}
</script>

<template>
  <div class="care-page">
    <p class="sr-only" aria-live="polite">{{ liveMessage }}</p>

    <section class="care-hero section-shell" aria-labelledby="care-title">
      <div class="container care-hero__layout">
        <div class="care-hero__copy">
          <p class="section-kicker">我的植物 · 示例养护计划</p>
          <h1 id="care-title">照顾植物，<br>从读懂它开始。</h1>
          <p>
            提醒你停下来观察，而不是机械执行。叶片、盆土和新芽留下的线索，比固定日历更接近植物真正的需要。
          </p>
        </div>

        <aside class="care-hero__focus" aria-label="今日养护摘要">
          <div class="care-hero__focus-topline">
            <span>今日重点</span>
            <el-icon aria-hidden="true"><Bell /></el-icon>
          </div>
          <strong>先检查琴叶榕的盆土</strong>
          <p>如果指尖仍能感到凉湿，今天最好的照顾就是暂时不浇水。</p>
          <div class="care-hero__meta">
            <span><b>{{ pendingTasks.length }}</b> 项待观察</span>
            <span><b>{{ completedCount }}</b> 项已完成</span>
          </div>
        </aside>
      </div>
    </section>

    <section class="upcoming section-shell" aria-labelledby="upcoming-title">
      <div class="container">
        <header class="section-heading">
          <div>
            <p class="section-kicker">接下来 14 天</p>
            <h2 id="upcoming-title">未来任务</h2>
            <p>这是可交互的示例计划。接入账户数据后，可由每株植物的购买日期与养护记录生成。</p>
          </div>
          <button
            v-if="pendingTasks.length > 1"
            class="quiet-button"
            type="button"
            @click="completeAllTasks"
          >
            <el-icon aria-hidden="true"><CircleCheck /></el-icon>
            全部完成
          </button>
        </header>

        <ul v-if="pendingTasks.length" class="task-list">
          <li v-for="task in pendingTasks" :key="task.id" class="task-item">
            <div class="task-item__time">
              <el-icon aria-hidden="true"><Calendar /></el-icon>
              <span>{{ task.timing }}</span>
            </div>
            <div class="task-item__content">
              <p>{{ task.plant }} · {{ task.category }}</p>
              <h3>{{ task.title }}</h3>
              <span>{{ task.detail }}</span>
            </div>
            <button
              :ref="(element) => setTaskButtonRef(task.id, element)"
              class="task-item__complete"
              type="button"
              :aria-label="`完成任务：${task.plant}，${task.title}`"
              @click="completeTask(task)"
            >
              <el-icon aria-hidden="true"><Check /></el-icon>
              <span>完成</span>
            </button>
          </li>
        </ul>

        <div v-else class="task-empty" role="status">
          <el-icon class="task-empty__icon" aria-hidden="true"><CircleCheck /></el-icon>
          <div>
            <p class="section-kicker">今天已经收尾</p>
            <h3 ref="emptyStateTitleRef" tabindex="-1">没有待处理的养护任务</h3>
            <p>做得刚刚好。接下来让植物安静生长，等新的观察信号出现再行动。</p>
          </div>
          <button class="quiet-button" type="button" @click="restoreTasks">
            <el-icon aria-hidden="true"><Refresh /></el-icon>
            恢复示例任务
          </button>
        </div>

        <footer v-if="completedCount > 0 && pendingTasks.length" class="task-progress">
          <span><el-icon aria-hidden="true"><CircleCheck /></el-icon>{{ completedCount }} 项已完成</span>
          <button type="button" @click="restoreTasks">恢复全部</button>
        </footer>
      </div>
    </section>

    <section class="guides section-shell" aria-labelledby="guides-title">
      <div class="container">
        <header class="section-heading section-heading--guides">
          <div>
            <p class="section-kicker">四类基础养护</p>
            <h2 id="guides-title">先观察，再行动</h2>
          </div>
          <p>每张指南都从“什么时候需要做”开始。具体频率仍应根据植物品种、季节与家中环境微调。</p>
        </header>

        <div class="guide-grid">
          <article
            v-for="guide in guideItems"
            :id="guide.id"
            :key="guide.id"
            :class="['guide-card', `guide-card--${guide.tone}`]"
          >
            <header class="guide-card__header">
              <div class="guide-card__number">{{ guide.number }}</div>
              <el-icon class="guide-card__icon" aria-hidden="true"><component :is="guide.icon" /></el-icon>
            </header>

            <p class="guide-card__label">{{ guide.label }}</p>
            <h3>{{ guide.title }}</h3>
            <p class="guide-card__summary">{{ guide.summary }}</p>

            <dl class="guide-card__signals">
              <div>
                <dt>观察信号</dt>
                <dd>{{ guide.signal }}</dd>
              </div>
              <div>
                <dt>建议节奏</dt>
                <dd>{{ guide.rhythm }}</dd>
              </div>
            </dl>

            <div class="guide-card__steps">
              <h4>这次可以怎么做</h4>
              <ol>
                <li v-for="step in guide.steps" :key="step"><span>{{ step }}</span></li>
              </ol>
            </div>

            <div class="guide-card__warning">
              <el-icon aria-hidden="true"><WarningFilled /></el-icon>
              <p>{{ guide.warning }}</p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section class="care-note section-shell" aria-labelledby="care-note-title">
      <div class="container care-note__inner">
        <span aria-hidden="true">“</span>
        <div>
          <p class="section-kicker">一条简单原则</p>
          <h2 id="care-note-title">不确定时，先记录变化，少做一次。</h2>
          <p>连续观察比频繁调整更容易找到原因。每次只改变一个条件，植物会用接下来的新叶告诉你答案。</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.care-page {
  --green-pale: var(--color-brand-soft, #e4eadb);
  --green-sage: var(--color-brand-accent, #92a879);
  --green-leaf: var(--color-brand, #496544);
  --green-forest: var(--color-brand-deep, #203d2c);
  color: var(--color-ink, #1d261f);
  background: var(--color-canvas, #f7f2e8);
}

.section-kicker {
  margin: 0 0 var(--space-sm, 0.75rem);
  color: var(--green-leaf);
  font-size: 0.75rem;
  font-weight: 750;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.care-hero {
  padding-block: clamp(3.5rem, 8vw, 7rem);
  background: var(--color-surface, #fffdf7);
  border-bottom: 1px solid var(--color-border, #d9d3c5);
}

.care-hero__layout {
  display: grid;
  gap: clamp(2rem, 6vw, 5rem);
  align-items: end;
}

.care-hero__copy {
  max-width: 47rem;
}

.care-hero h1 {
  max-width: 11ch;
  margin: 0;
  font-family: var(--font-sans);
  font-size: clamp(3rem, 7vw, 6.5rem);
  font-weight: 500;
  letter-spacing: -0.06em;
  line-height: 0.98;
}

.care-hero__copy > p:last-child {
  max-width: 39rem;
  margin: var(--space-lg, 2rem) 0 0;
  color: var(--color-text-muted, #68716a);
  font-size: clamp(1rem, 2vw, 1.15rem);
  line-height: 1.85;
}

.care-hero__focus {
  padding: clamp(1.4rem, 4vw, 2.25rem);
  color: #f7f2e8;
  background: var(--green-forest);
  border-radius: var(--radius-card, 0.75rem);
}

.care-hero__focus-topline {
  display: flex;
  justify-content: space-between;
  color: var(--color-on-dark-muted);
  font-size: 0.74rem;
  font-weight: 750;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.care-hero__focus > strong {
  display: block;
  max-width: 15ch;
  margin-top: 2.5rem;
  font-family: var(--font-sans);
  font-size: clamp(1.65rem, 4vw, 2.5rem);
  font-weight: 550;
  line-height: 1.2;
}

.care-hero__focus > p {
  margin: 0.9rem 0 0;
  color: #bdc9ba;
  font-size: 0.85rem;
  line-height: 1.7;
}

.care-hero__meta {
  display: flex;
  gap: 1.5rem;
  margin-top: 2rem;
  padding-top: 1.2rem;
  border-top: 1px solid rgba(247, 242, 232, 0.2);
  color: #dce4d7;
  font-size: 0.75rem;
}

.care-hero__meta b {
  color: #fffdf7;
  font-family: var(--font-sans);
  font-size: 1.15rem;
  font-weight: 500;
}

.upcoming,
.guides,
.care-note {
  padding-block: clamp(3.5rem, 8vw, 7rem);
}

.section-heading {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: var(--space-md, 1.25rem);
  align-items: end;
  margin-bottom: var(--space-lg, 2rem);
}

.section-heading h2,
.care-note h2 {
  margin: 0;
  font-family: var(--font-sans);
  font-size: clamp(2rem, 4vw, 3.5rem);
  font-weight: 550;
  letter-spacing: -0.04em;
  line-height: 1.1;
}

.section-heading > div > p:last-child {
  max-width: 45rem;
  margin: 0.75rem 0 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.84rem;
  line-height: 1.65;
}

.quiet-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  min-height: 2.75rem;
  padding: 0.6rem 1rem;
  color: var(--green-leaf);
  cursor: pointer;
  background: transparent;
  border: 1px solid var(--green-leaf);
  border-radius: var(--radius-pill, 999px);
  font: inherit;
  font-size: 0.82rem;
  font-weight: 700;
  transition: color 180ms var(--ease-out, ease), background-color 180ms var(--ease-out, ease), transform 180ms var(--ease-out, ease);
}

.quiet-button:hover {
  color: #fffdf7;
  background: var(--green-leaf);
}

.quiet-button:focus-visible,
.task-item__complete:focus-visible,
.task-progress button:focus-visible,
.task-empty h3:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--green-leaf) 35%, transparent);
  outline-offset: 2px;
}

.task-list {
  margin: 0;
  padding: 0;
  border-top: 1px solid var(--color-border, #d9d3c5);
  list-style: none;
}

.task-item {
  display: grid;
  gap: var(--space-sm, 0.75rem);
  padding-block: var(--space-md, 1.25rem);
  border-bottom: 1px solid var(--color-border, #d9d3c5);
}

.task-item__time {
  display: flex;
  gap: 0.45rem;
  align-items: center;
  color: var(--green-leaf);
  font-size: 0.76rem;
  font-weight: 700;
}

.task-item__content p {
  margin: 0 0 0.3rem;
  color: var(--green-leaf);
  font-size: 0.68rem;
  font-weight: 750;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.task-item__content h3 {
  margin: 0;
  font-size: 1rem;
}

.task-item__content > span {
  display: block;
  max-width: 49rem;
  margin-top: 0.45rem;
  color: var(--color-text-muted, #68716a);
  font-size: 0.8rem;
  line-height: 1.65;
}

.task-item__complete {
  display: inline-flex;
  justify-self: start;
  align-items: center;
  gap: 0.35rem;
  min-width: 2.75rem;
  min-height: 2.75rem;
  padding: 0.55rem 0.85rem;
  color: var(--green-forest);
  cursor: pointer;
  background: var(--green-pale);
  border: 1px solid transparent;
  border-radius: var(--radius-pill, 999px);
  font: inherit;
  font-size: 0.78rem;
  font-weight: 700;
  transition: border-color 180ms var(--ease-out, ease), transform 180ms var(--ease-out, ease);
}

.task-item__complete:hover {
  border-color: var(--green-leaf);
}

.task-empty {
  display: grid;
  gap: var(--space-md, 1.25rem);
  align-items: center;
  padding: clamp(1.5rem, 5vw, 3.5rem);
  background: var(--green-pale);
  border: 1px solid color-mix(in srgb, var(--green-leaf) 24%, var(--color-border, #d9d3c5));
  border-radius: var(--radius-card, 0.75rem);
}

.task-empty__icon {
  color: var(--green-leaf);
  font-size: 2.5rem;
}

.task-empty h3 {
  margin: 0;
  font-family: var(--font-sans);
  font-size: 1.5rem;
}

.task-empty div > p:last-child {
  margin: 0.5rem 0 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.84rem;
  line-height: 1.65;
}

.task-progress {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-top: 1rem;
  color: var(--color-text-muted, #68716a);
  font-size: 0.78rem;
}

.task-progress span {
  display: inline-flex;
  gap: 0.4rem;
  align-items: center;
  color: var(--green-leaf);
}

.task-progress button {
  min-height: 2.75rem;
  padding-inline: 0.7rem;
  color: var(--green-leaf);
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: var(--radius-pill, 999px);
  font: inherit;
  font-weight: 700;
}

.task-progress button:hover {
  background: var(--green-pale);
}

.guides {
  background: var(--color-surface, #fffdf7);
  border-block: 1px solid var(--color-border, #d9d3c5);
}

.section-heading--guides > p {
  max-width: 32rem;
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.84rem;
  line-height: 1.7;
}

.guide-grid {
  display: grid;
  gap: var(--space-md, 1.25rem);
}

.guide-card {
  padding: clamp(1.25rem, 4vw, 2rem);
  background: var(--green-pale);
  border-radius: var(--radius-card, 0.75rem);
}

.guide-card--sage { background: #d7dfca; }
.guide-card--leaf { color: var(--color-on-brand); background: var(--green-leaf); }
.guide-card--forest { color: var(--color-on-brand); background: var(--green-forest); }

.guide-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: clamp(2.5rem, 8vw, 5rem);
}

.guide-card__number {
  color: var(--green-leaf);
  font-family: var(--font-sans);
  font-size: 1.1rem;
}

.guide-card--leaf .guide-card__number,
.guide-card--forest .guide-card__number {
  color: var(--color-on-brand);
}

.guide-card__icon {
  font-size: 1.75rem;
}

.guide-card__label {
  margin: 0 0 0.4rem;
  color: var(--green-leaf);
  font-size: 0.7rem;
  font-weight: 750;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.guide-card--leaf .guide-card__label,
.guide-card--forest .guide-card__label {
  color: var(--color-on-brand);
}

.guide-card h3 {
  margin: 0;
  font-family: var(--font-sans);
  font-size: clamp(1.5rem, 3vw, 2rem);
  font-weight: 550;
  line-height: 1.2;
}

.guide-card__summary {
  margin: 0.75rem 0 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.82rem;
  line-height: 1.65;
}

.guide-card--leaf .guide-card__summary,
.guide-card--forest .guide-card__summary {
  color: var(--color-on-dark-muted);
}

.guide-card__signals {
  display: grid;
  gap: 1rem;
  margin: var(--space-lg, 2rem) 0 0;
  padding: var(--space-md, 1.25rem) 0;
  border-block: 1px solid rgba(32, 61, 44, 0.2);
}

.guide-card--leaf .guide-card__signals,
.guide-card--forest .guide-card__signals {
  border-color: rgba(247, 242, 232, 0.2);
}

.guide-card__signals dt,
.guide-card__steps h4 {
  margin: 0 0 0.35rem;
  font-size: 0.72rem;
  font-weight: 750;
}

.guide-card__signals dd {
  margin: 0;
  font-size: 0.77rem;
  line-height: 1.6;
}

.guide-card__steps {
  margin-top: var(--space-md, 1.25rem);
}

.guide-card__steps ol {
  display: grid;
  gap: 0.7rem;
  margin: 0;
  padding: 0;
  counter-reset: care-step;
  list-style: none;
}

.guide-card__steps li {
  display: grid;
  grid-template-columns: 1.45rem 1fr;
  gap: 0.55rem;
  font-size: 0.77rem;
  line-height: 1.6;
  counter-increment: care-step;
}

.guide-card__steps li::before {
  content: counter(care-step);
  display: grid;
  width: 1.45rem;
  height: 1.45rem;
  place-items: center;
  color: var(--green-forest);
  background: color-mix(in oklch, var(--color-on-brand) 62%, transparent);
  border-radius: 50%;
  font-family: var(--font-sans);
  font-size: 0.7rem;
}

.guide-card__warning {
  display: flex;
  gap: 0.5rem;
  margin-top: var(--space-md, 1.25rem);
  padding-top: var(--space-md, 1.25rem);
  border-top: 1px solid rgba(32, 61, 44, 0.2);
}

.guide-card--leaf .guide-card__warning,
.guide-card--forest .guide-card__warning {
  border-color: rgba(247, 242, 232, 0.2);
}

.guide-card__warning .el-icon {
  flex: 0 0 auto;
  margin-top: 0.16rem;
}

.guide-card__warning p {
  margin: 0;
  font-size: 0.73rem;
  line-height: 1.58;
}

.care-note__inner {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: clamp(1rem, 4vw, 3rem);
  max-width: 57rem;
}

.care-note__inner > span {
  color: var(--green-sage);
  font-family: var(--font-sans);
  font-size: clamp(4rem, 10vw, 8rem);
  line-height: 0.75;
}

.care-note h2 {
  max-width: 17ch;
}

.care-note__inner div > p:last-child {
  max-width: 42rem;
  margin: var(--space-md, 1.25rem) 0 0;
  color: var(--color-text-muted, #68716a);
  line-height: 1.8;
}

@media (min-width: 42rem) {
  .task-item {
    grid-template-columns: 8.5rem minmax(0, 1fr) auto;
    gap: var(--space-md, 1.25rem);
    align-items: center;
  }

  .task-item__complete {
    justify-self: end;
  }

  .task-empty {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .guide-card__signals {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 64rem) {
  .care-hero__layout {
    grid-template-columns: minmax(0, 1.35fr) minmax(20rem, 0.65fr);
  }

  .guide-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (prefers-reduced-motion: reduce) {
  .quiet-button,
  .task-item__complete {
    transition: none;
  }

  .quiet-button:hover,
  .task-item__complete:hover {
    transform: none;
  }
}
</style>
