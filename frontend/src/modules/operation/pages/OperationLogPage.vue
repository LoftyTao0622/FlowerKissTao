<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchOperationLogs } from '../api/operation'
import type { OperationLog } from '../types/operation'

const rows = ref<OperationLog[]>([])
const total = ref(0)
const current = ref(1)
const size = 12
const moduleFilter = ref('')
const actionFilter = ref('')
const loading = ref(false)
const errorMessage = ref('')
const expanded = ref<number | null>(null)

const modules = [
  ['', '全部模块'], ['catalog', '商品'], ['trade', '交易'], ['knowledge', '知识'],
  ['recommendation', '推荐'], ['user', '用户'], ['care', '养护'],
]

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

function json(value: Record<string, unknown> | null) {
  return value ? JSON.stringify(value, null, 2) : '—'
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchOperationLogs(current.value, size, moduleFilter.value || undefined, actionFilter.value || undefined)
    rows.value = result.records
    total.value = result.total
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '日志加载失败'
  } finally {
    loading.value = false
  }
}

function filter() {
  current.value = 1
  void load()
}

function changePage(page: number) {
  current.value = page
  void load()
}

onMounted(() => { void load() })
</script>

<template>
  <section class="logs-page">
    <header class="logs-header">
      <div><p class="logs-eyebrow">AUDIT TRAIL</p><h1>操作日志</h1><p>关键配置与运营动作的可追溯记录。前后值只保留安全白名单字段。</p></div>
    </header>

    <div class="logs-filters">
      <select v-model="moduleFilter" aria-label="按模块筛选" @change="filter">
        <option v-for="item in modules" :key="item[0]" :value="item[0]">{{ item[1] }}</option>
      </select>
      <input v-model="actionFilter" placeholder="动作，如 CONFIG / PUBLISH" aria-label="按动作筛选" @keyup.enter="filter" />
      <button type="button" @click="filter">筛选</button>
    </div>

    <p v-if="errorMessage" class="logs-error" role="alert">{{ errorMessage }}</p>
    <p v-if="loading" class="logs-status" role="status">加载中…</p>
    <p v-else-if="!rows.length" class="logs-status">暂无日志。</p>

    <div v-else class="log-list">
      <article v-for="row in rows" :key="row.id" class="log-row">
        <button class="log-row__main" type="button" @click="expanded = expanded === row.id ? null : row.id">
          <time>{{ formatTime(row.createdAt) }}</time>
          <span class="log-module">{{ row.module }}</span>
          <strong>{{ row.action }}</strong>
          <span>{{ row.targetType }} #{{ row.targetId ?? '—' }}</span>
          <span class="log-user">{{ row.username ?? '游客' }}</span>
          <span aria-hidden="true">{{ expanded === row.id ? '⌃' : '⌄' }}</span>
        </button>
        <div v-if="expanded === row.id" class="log-detail">
          <div><h3>修改前</h3><pre>{{ json(row.before) }}</pre></div>
          <div><h3>修改后</h3><pre>{{ json(row.after) }}</pre></div>
          <p>来源 IP：{{ row.ip ?? '—' }} · User-Agent：{{ row.userAgent ?? '—' }}</p>
        </div>
      </article>
    </div>

    <div v-if="total > size" class="logs-pager">
      <button type="button" :disabled="current <= 1" @click="changePage(current - 1)">上一页</button>
      <span>第 {{ current }} / {{ Math.ceil(total / size) }} 页</span>
      <button type="button" :disabled="current >= Math.ceil(total / size)" @click="changePage(current + 1)">下一页</button>
    </div>
  </section>
</template>

<style scoped>
.logs-page { padding: 2rem 0 3rem; }
.logs-header { margin-bottom: 1.5rem; }
.logs-header h1 { margin: .2rem 0 .35rem; }
.logs-header p:last-child { margin: 0; color: #718076; font-size: .84rem; }
.logs-eyebrow { margin: 0; color: #718076; font-size: .62rem; letter-spacing: .12em; }
.logs-filters { display: flex; flex-wrap: wrap; gap: .6rem; margin-bottom: 1rem; }
.logs-filters select, .logs-filters input { min-height: 2.45rem; padding: .35rem .7rem; border: 1px solid #d9d3c5; border-radius: .45rem; background: #fffdf7; font-size: .78rem; }
.logs-filters button { min-height: 2.45rem; padding: .35rem 1rem; border: 1px solid #496544; border-radius: 999px; background: #496544; color: #fffdf7; cursor: pointer; font-weight: 700; }
.logs-error { padding: .7rem; background: #f6e3de; border-radius: .55rem; color: #a8442f; font-size: .78rem; }
.logs-status { padding: 2.5rem; color: #718076; text-align: center; }
.log-list { display: grid; gap: .45rem; }
.log-row { background: #fffdf7; border: 1px solid #d9d3c5; border-radius: .55rem; overflow: hidden; }
.log-row__main { display: grid; grid-template-columns: 10rem 7rem 7rem minmax(8rem, 1fr) 7rem 1.5rem; align-items: center; gap: .6rem; width: 100%; min-height: 3rem; padding: .5rem .75rem; background: transparent; border: 0; color: #24312a; cursor: pointer; text-align: left; font-size: .74rem; }
.log-row__main time, .log-user { color: #718076; font-size: .68rem; }
.log-module { display: inline-block; padding: .1rem .4rem; background: #e4eadb; border-radius: 999px; color: #496544; font-size: .66rem; text-align: center; }
.log-detail { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; padding: .75rem; border-top: 1px solid #d9d3c5; background: #f7f4ec; }
.log-detail h3 { margin: 0 0 .35rem; font-size: .7rem; color: #718076; }
.log-detail pre { max-height: 12rem; overflow: auto; margin: 0; padding: .55rem; background: #fffdf7; border-radius: .35rem; font-size: .68rem; white-space: pre-wrap; }
.log-detail p { grid-column: 1 / -1; margin: 0; color: #718076; font-size: .65rem; }
.logs-pager { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1rem; font-size: .75rem; }
.logs-pager button { min-height: 2.3rem; padding: .3rem .8rem; border: 1px solid #d9d3c5; border-radius: 999px; background: transparent; cursor: pointer; }
.logs-pager button:disabled { opacity: .45; cursor: not-allowed; }
@media (max-width: 52rem) { .log-row__main { grid-template-columns: 1fr 1fr 1fr 1rem; } .log-row__main time, .log-user { grid-column: span 2; } .log-detail { grid-template-columns: 1fr; } }
</style>
