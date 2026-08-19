<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { fetchDashboard } from '../api/operation'
import type { Dashboard, TrendPoint } from '../types/operation'

const dashboard = ref<Dashboard | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const range = ref<'7' | '30' | '90'>('30')
const tableVisible = ref(false)
const tooltip = ref<{ point: TrendPoint; x: number; y: number } | null>(null)

/**
 * 数据只有一个时间序列时用单线图，不加图例（标题已经说明画的是什么）。
 * 多个量纲不画在同一轴上：访问量、推荐数、订单数分别用小型图，避免双轴误导。
 */
function dateBefore(days: number) {
  const date = new Date()
  date.setDate(date.getDate() - days + 1)
  return date.toISOString().slice(0, 10)
}

function today() {
  return new Date().toISOString().slice(0, 10)
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    dashboard.value = await fetchDashboard(dateBefore(Number(range.value)), today())
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '看板加载失败'
  } finally {
    loading.value = false
  }
}

function changeRange() {
  void load()
}

function percent(value: number) {
  return `${Number(value).toFixed(1)}%`
}

function money(value: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency', currency: 'CNY', maximumFractionDigits: 0,
  }).format(value)
}

function compact(value: number) {
  return new Intl.NumberFormat('zh-CN', { notation: 'compact', maximumFractionDigits: 1 }).format(value)
}

const maxVisits = computed(() => Math.max(1, ...(dashboard.value?.trends.map((p) => p.visits) ?? [1])))
const maxRecommendations = computed(() => Math.max(1, ...(dashboard.value?.trends.map((p) => p.recommendations) ?? [1])))
const maxOrders = computed(() => Math.max(1, ...(dashboard.value?.trends.map((p) => p.paidOrders) ?? [1])))
const maxOrderCount = computed(() => Math.max(1, ...(dashboard.value?.orderBreakdown.map((p) => p.count) ?? [1])))
const maxPlantQty = computed(() => Math.max(1, ...(dashboard.value?.topPlants.map((p) => p.orderedQuantity) ?? [1])))
const maxArticleViews = computed(() => Math.max(1, ...(dashboard.value?.topArticles.map((p) => p.views) ?? [1])))

function linePoints(metric: 'visits' | 'recommendations' | 'paidOrders', max: number) {
  const points = dashboard.value?.trends ?? []
  if (!points.length) return ''
  const width = 560
  const height = 150
  return points.map((point, index) => {
    const x = points.length === 1 ? width / 2 : (index / (points.length - 1)) * width
    const y = height - (point[metric] / max) * (height - 16) - 8
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')
}

function hoverPoint(event: MouseEvent, point: TrendPoint) {
  const target = event.currentTarget as HTMLElement
  const rect = target.closest('.trend-card')?.getBoundingClientRect()
  if (!rect) return
  tooltip.value = { point, x: event.clientX - rect.left, y: event.clientY - rect.top }
}

function clearTooltip() {
  tooltip.value = null
}

onMounted(() => {
  void load()
})
</script>

<template>
  <section class="dashboard-page viz-root">
    <header class="dashboard-header">
      <div>
        <p class="dashboard-eyebrow">OPERATION OVERVIEW</p>
        <h1>运营看板</h1>
        <p>把问卷、推荐、交易、养护与内容串成同一份可验证的数据。</p>
      </div>
      <!-- 一行全局日期筛选，作用于下方所有指标与图表 -->
      <label class="range-select">
        <span>日期范围</span>
        <select v-model="range" @change="changeRange">
          <option value="7">最近 7 天</option>
          <option value="30">最近 30 天</option>
          <option value="90">最近 90 天</option>
        </select>
      </label>
    </header>

    <p v-if="errorMessage" class="dashboard-error" role="alert">{{ errorMessage }}</p>
    <p v-if="loading && !dashboard" class="dashboard-status" role="status">正在汇总数据…</p>

    <template v-if="dashboard">
      <!-- 五个单值用 KPI stat tiles，不画五根没有比较意义的柱子 -->
      <section class="kpi-grid" :class="{ 'is-refreshing': loading }" aria-label="核心指标">
        <article class="kpi-card">
          <span>问卷完成率</span>
          <strong>{{ percent(dashboard.overview.profileCompletionRate) }}</strong>
          <small>{{ dashboard.overview.profileUserCount }} / {{ dashboard.overview.normalUserCount }} 位正常用户</small>
        </article>
        <article class="kpi-card">
          <span>推荐点击率</span>
          <strong>{{ percent(dashboard.overview.recommendationClickRate) }}</strong>
          <small>{{ dashboard.overview.recommendationClickedCount }} / {{ dashboard.overview.recommendationItemCount }} 条推荐</small>
        </article>
        <article class="kpi-card">
          <span>已支付订单</span>
          <strong>{{ compact(dashboard.overview.paidOrderCount) }}</strong>
          <small>待发货、运输中、已完成与售后中</small>
        </article>
        <article class="kpi-card kpi-card--hero">
          <span>成交金额</span>
          <strong>{{ money(dashboard.overview.paidAmount) }}</strong>
          <small>{{ dashboard.from }} 至 {{ dashboard.to }}</small>
        </article>
        <article class="kpi-card">
          <span>养护任务完成率</span>
          <strong>{{ percent(dashboard.overview.careTaskCompletionRate) }}</strong>
          <small>{{ dashboard.overview.completedCareTaskCount }} / {{ dashboard.overview.handledCareTaskCount }} 项已处理任务</small>
        </article>
      </section>

      <!-- 不同量纲拆成三个小型单线图，避免双轴和虚假相关 -->
      <section class="trend-section" :class="{ 'is-refreshing': loading }">
        <header class="section-title">
          <div>
            <h2>日期趋势</h2>
            <p>每张图只有一个量纲；悬停或键盘聚焦可看具体值。</p>
          </div>
          <button type="button" class="table-toggle" @click="tableVisible = !tableVisible">
            {{ tableVisible ? '隐藏表格' : '查看数据表' }}
          </button>
        </header>

        <div class="trend-grid">
          <article class="trend-card">
            <h3>页面访问</h3>
            <svg viewBox="0 0 560 170" role="img" aria-label="每日页面访问趋势">
              <line x1="0" y1="150" x2="560" y2="150" class="axis" />
              <polyline :points="linePoints('visits', maxVisits)" class="trend-line" />
              <g v-for="(point, i) in dashboard.trends" :key="point.date">
                <circle
                  :cx="dashboard.trends.length === 1 ? 280 : (i / (dashboard.trends.length - 1)) * 560"
                  :cy="150 - (point.visits / maxVisits) * 134"
                  r="10"
                  class="hit-dot"
                  tabindex="0"
                  :aria-label="`${point.date}，访问 ${point.visits} 次`"
                  @mouseenter="hoverPoint($event, point)"
                  @mousemove="hoverPoint($event, point)"
                  @mouseleave="clearTooltip"
                  @focus="hoverPoint($event as MouseEvent, point)"
                  @blur="clearTooltip"
                />
              </g>
            </svg>
            <strong class="end-value">{{ dashboard.trends.at(-1)?.visits ?? 0 }}</strong>
          </article>

          <article class="trend-card">
            <h3>推荐生成</h3>
            <svg viewBox="0 0 560 170" role="img" aria-label="每日推荐生成趋势">
              <line x1="0" y1="150" x2="560" y2="150" class="axis" />
              <polyline :points="linePoints('recommendations', maxRecommendations)" class="trend-line trend-line--second" />
            </svg>
            <strong class="end-value">{{ dashboard.trends.at(-1)?.recommendations ?? 0 }}</strong>
          </article>

          <article class="trend-card">
            <h3>已支付订单</h3>
            <svg viewBox="0 0 560 170" role="img" aria-label="每日已支付订单趋势">
              <line x1="0" y1="150" x2="560" y2="150" class="axis" />
              <polyline :points="linePoints('paidOrders', maxOrders)" class="trend-line trend-line--third" />
            </svg>
            <strong class="end-value">{{ dashboard.trends.at(-1)?.paidOrders ?? 0 }}</strong>
          </article>
        </div>

        <div
          v-if="tooltip"
          class="chart-tooltip"
          :style="{ left: `${tooltip.x}px`, top: `${tooltip.y}px` }"
          role="status"
        >
          <strong>{{ tooltip.point.date }}</strong>
          <span>访问 {{ tooltip.point.visits }}</span>
          <span>推荐 {{ tooltip.point.recommendations }}</span>
          <span>订单 {{ tooltip.point.paidOrders }}</span>
        </div>

        <!-- 每张图的 WCAG 等价表格视图，值不依赖 tooltip 才能读 -->
        <div v-if="tableVisible" class="data-table-wrap">
          <table class="data-table">
            <thead><tr><th>日期</th><th>访问</th><th>推荐</th><th>订单</th><th>成交金额</th></tr></thead>
            <tbody>
              <tr v-for="point in dashboard.trends" :key="point.date">
                <td>{{ point.date }}</td><td>{{ point.visits }}</td><td>{{ point.recommendations }}</td>
                <td>{{ point.paidOrders }}</td><td>{{ money(point.paidAmount) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="insight-grid" :class="{ 'is-refreshing': loading }">
        <!-- 状态是有序工作流，采用单色横条，不用彩虹色 -->
        <article class="insight-card">
          <h2>订单状态</h2>
          <ul class="bar-list">
            <li v-for="item in dashboard.orderBreakdown" :key="item.status">
              <span>{{ item.statusLabel }}</span>
              <span class="bar-track"><i :style="{ width: `${item.count / maxOrderCount * 100}%` }"></i></span>
              <strong>{{ item.count }}</strong>
            </li>
          </ul>
        </article>

        <article class="insight-card">
          <h2>热销植物</h2>
          <p v-if="!dashboard.topPlants.length" class="empty-note">当前范围内暂无成交</p>
          <ul v-else class="bar-list">
            <li v-for="item in dashboard.topPlants" :key="item.speciesId">
              <span>{{ item.name }}</span>
              <span class="bar-track"><i :style="{ width: `${item.orderedQuantity / maxPlantQty * 100}%` }"></i></span>
              <strong>{{ item.orderedQuantity }}</strong>
            </li>
          </ul>
        </article>

        <article class="insight-card">
          <h2>热门知识</h2>
          <p v-if="!dashboard.topArticles.length" class="empty-note">还没有阅读数据</p>
          <ul v-else class="bar-list">
            <li v-for="item in dashboard.topArticles" :key="item.articleId">
              <RouterLink :to="{ name: 'knowledge-detail', params: { slug: item.slug } }">{{ item.title }}</RouterLink>
              <span class="bar-track"><i :style="{ width: `${item.views / maxArticleViews * 100}%` }"></i></span>
              <strong>{{ item.views }}</strong>
            </li>
          </ul>
        </article>

        <article class="insight-card">
          <h2>养护任务状态</h2>
          <ul class="status-list">
            <li v-for="item in dashboard.careBreakdown" :key="item.status">
              <span class="status-dot" :class="`status-dot--${item.status}`" aria-hidden="true"></span>
              <span>{{ item.statusLabel }}</span>
              <strong>{{ item.count }}</strong>
            </li>
          </ul>
        </article>
      </section>
    </template>
  </section>
</template>

<style scoped>
.dashboard-page {
  --viz-surface: #fffdf7;
  --viz-text: #24312a;
  --viz-muted: #718076;
  --viz-grid: #e3ded1;
  --viz-series-1: #2a78d6;
  --viz-series-2: #eb6834;
  --viz-series-3: #1baf7a;
  padding: 2rem 0 3rem;
  color: var(--viz-text);
}

.dashboard-header,
.section-title {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.dashboard-header { margin-bottom: 1.5rem; }
.dashboard-header h1 { margin: 0.2rem 0 0.35rem; font-size: clamp(1.55rem, 3vw, 2.2rem); }
.dashboard-header p:last-child { margin: 0; color: var(--viz-muted); font-size: 0.85rem; }
.dashboard-eyebrow { margin: 0; color: var(--viz-muted); font-size: 0.62rem; letter-spacing: .12em; }

.range-select { display: grid; gap: .3rem; color: var(--viz-muted); font-size: .72rem; }
.range-select select { min-height: 2.5rem; padding: 0 .8rem; border: 1px solid var(--viz-grid); border-radius: .5rem; background: var(--viz-surface); }
.dashboard-error { padding: 1rem; background: #f6e3de; border-radius: .7rem; color: #a8442f; }
.dashboard-status { padding: 3rem; text-align: center; color: var(--viz-muted); }
.is-refreshing { opacity: .55; pointer-events: none; transition: opacity 150ms ease; }

.kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr)); gap: .85rem; margin-bottom: 1.5rem; }
.kpi-card { min-height: 8.5rem; padding: 1rem; background: var(--viz-surface); border: 1px solid var(--viz-grid); border-radius: .75rem; }
.kpi-card > span { display: block; color: var(--viz-muted); font-size: .74rem; }
.kpi-card > strong { display: block; margin: .55rem 0 .35rem; font-family: system-ui, sans-serif; font-size: clamp(1.8rem, 3vw, 2.6rem); font-weight: 650; letter-spacing: -.035em; }
.kpi-card > small { color: var(--viz-muted); font-size: .68rem; line-height: 1.45; }
.kpi-card--hero { background: #203d2c; color: #fffdf7; }
.kpi-card--hero > span, .kpi-card--hero > small { color: rgba(255,253,247,.65); }

.trend-section { position: relative; padding: 1rem; margin-bottom: 1rem; background: var(--viz-surface); border: 1px solid var(--viz-grid); border-radius: .75rem; }
.section-title h2, .insight-card h2 { margin: 0; font-size: .92rem; }
.section-title p { margin: .2rem 0 1rem; color: var(--viz-muted); font-size: .7rem; }
.table-toggle { min-height: 2rem; padding: .25rem .65rem; border: 1px solid var(--viz-grid); border-radius: 999px; background: transparent; color: var(--viz-series-1); cursor: pointer; font-size: .68rem; }
.trend-grid { display: grid; gap: .75rem; }
@media (min-width: 64rem) { .trend-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
.trend-card { position: relative; min-width: 0; padding: .75rem; background: #faf8f2; border-radius: .55rem; }
.trend-card h3 { margin: 0 0 .35rem; color: var(--viz-muted); font-size: .72rem; font-weight: 600; }
.trend-card svg { display: block; width: 100%; height: 9rem; overflow: visible; }
.axis { stroke: var(--viz-grid); stroke-width: 1; }
.trend-line { fill: none; stroke: var(--viz-series-1); stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
.trend-line--second { stroke: var(--viz-series-2); }
.trend-line--third { stroke: var(--viz-series-3); }
.hit-dot { fill: transparent; stroke: transparent; cursor: crosshair; }
.hit-dot:focus { outline: none; stroke: var(--viz-series-1); stroke-width: 2; }
.end-value { position: absolute; top: .65rem; right: .75rem; font-size: .9rem; }
.chart-tooltip { position: absolute; z-index: 3; display: grid; gap: .15rem; min-width: 8.5rem; padding: .55rem .65rem; transform: translate(10px, -105%); pointer-events: none; background: #203d2c; border-radius: .45rem; color: #fffdf7; box-shadow: 0 8px 24px rgba(32,61,44,.2); font-size: .68rem; }
.chart-tooltip span { color: rgba(255,253,247,.72); }

.data-table-wrap { overflow-x: auto; margin-top: 1rem; }
.data-table { width: 100%; border-collapse: collapse; font-size: .72rem; }
.data-table th, .data-table td { padding: .5rem; border-bottom: 1px solid var(--viz-grid); text-align: right; font-variant-numeric: tabular-nums; }
.data-table th:first-child, .data-table td:first-child { text-align: left; }

.insight-grid { display: grid; gap: 1rem; }
@media (min-width: 52rem) { .insight-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
.insight-card { padding: 1rem; background: var(--viz-surface); border: 1px solid var(--viz-grid); border-radius: .75rem; }
.insight-card h2 { margin-bottom: .9rem; }
.bar-list, .status-list { display: grid; gap: .65rem; margin: 0; padding: 0; list-style: none; }
.bar-list li { display: grid; grid-template-columns: minmax(5rem, 9rem) minmax(4rem, 1fr) 2.2rem; align-items: center; gap: .55rem; font-size: .72rem; }
.bar-list li > span:first-child, .bar-list a { overflow: hidden; color: var(--viz-text); text-overflow: ellipsis; white-space: nowrap; text-decoration: none; }
.bar-track { display: block; height: .65rem; background: #dce5d4; }
.bar-track i { display: block; height: 100%; min-width: 2px; background: var(--viz-series-1); border-radius: 0 4px 4px 0; }
.bar-list strong { text-align: right; font-variant-numeric: tabular-nums; }
.status-list li { display: grid; grid-template-columns: .7rem minmax(0, 1fr) auto; align-items: center; gap: .5rem; font-size: .75rem; }
.status-list strong { font-variant-numeric: tabular-nums; }
.status-dot { width: .55rem; height: .55rem; border-radius: 50%; background: #898781; }
.status-dot--1 { background: #0ca30c; }
.status-dot--2 { background: #eda100; }
.status-dot--3 { background: #d03b3b; }
.empty-note { color: var(--viz-muted); font-size: .75rem; }
</style>
