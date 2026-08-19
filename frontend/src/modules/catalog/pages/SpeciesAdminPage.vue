<script setup lang="ts">
/**
 * 品种管理暂复用后端已就绪的分页、上下架与删除能力。
 * 详细编辑使用 Element Plus 对话框；字段多，先提供常用字段与跳转到 SKU 管理。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { request } from '@/shared/api/request'
import type { PageResult } from '@/shared/api/types'
import type { CatalogPlant } from '@/modules/catalog/types/catalog'

const rows = ref<CatalogPlant[]>([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const status = ref<number | undefined>(undefined)
const query = reactive({ current: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const result = await request<PageResult<CatalogPlant>>('/admin/species', {
      query: { current: query.current, size: query.size, keyword: keyword.value, status: status.value },
    })
    rows.value = result.records
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally { loading.value = false }
}

function search() { query.current = 1; void load() }
function changePage(page: number) { query.current = page; void load() }

async function toggle(row: CatalogPlant) {
  try {
    await request<void>(`/admin/species/${row.id}/status`, {
      method: 'PUT', query: { status: row.status === 1 ? 0 : 1 },
    })
    ElMessage.success(row.status === 1 ? '已停用' : '已启用')
    await load()
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '操作失败') }
}

async function remove(row: CatalogPlant) {
  try {
    await ElMessageBox.confirm(`删除品种「${row.name}」？品种下仍有 SKU 时后端会拒绝。`, '删除品种', { type: 'warning' })
    await request<void>(`/admin/species/${row.id}`, { method: 'DELETE' })
    await load()
  } catch { /* cancel or business error is surfaced by request in future interaction */ }
}

onMounted(() => { void load() })
</script>

<template>
  <section class="catalog-admin">
    <header><div><p>CATALOG</p><h1>品种管理</h1><span>维护推荐算法依赖的结构化植物属性。</span></div><RouterLink class="manage-link" :to="{ name: 'admin-skus' }">管理 SKU 与库存 →</RouterLink></header>
    <div class="toolbar"><input v-model="keyword" placeholder="中文名 / 学名 / code" @keyup.enter="search" /><select v-model="status" @change="search"><option :value="undefined">全部状态</option><option :value="1">启用</option><option :value="0">停用</option></select><button @click="search">搜索</button></div>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="中文名" min-width="120" />
      <el-table-column prop="latinName" label="学名" min-width="180" />
      <el-table-column prop="slug" label="code" min-width="150" />
      <el-table-column prop="category" label="分类" width="110" />
      <el-table-column prop="difficulty" label="养护难度" width="110" />
      <el-table-column label="光照" min-width="130"><template #default="{ row }">{{ row.light }}</template></el-table-column>
      <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="160" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-pagination class="pager" layout="total, prev, pager, next" :total="total" :page-size="query.size" :current-page="query.current" @current-change="changePage" />
  </section>
</template>

<style scoped>
.catalog-admin { padding: 2rem 0 3rem; }
.catalog-admin > header { display:flex;flex-wrap:wrap;justify-content:space-between;gap:1rem;margin-bottom:1.4rem; }
.catalog-admin header p { margin:0;color:#718076;font-size:.62rem;letter-spacing:.12em; }
.catalog-admin header h1 { margin:.2rem 0 .3rem; }
.catalog-admin header span { color:#718076;font-size:.82rem; }
.manage-link { align-self:center;color:#496544;font-size:.78rem;font-weight:700;text-decoration:none; }
.toolbar { display:flex;gap:.6rem;margin-bottom:1rem; }
.toolbar input,.toolbar select { min-height:2.45rem;padding:.35rem .7rem;border:1px solid #d9d3c5;border-radius:.45rem;background:#fffdf7; }
.toolbar input { min-width:16rem; }
.toolbar button { padding:.35rem 1rem;border:1px solid #496544;border-radius:999px;background:#496544;color:#fffdf7;font-weight:700;cursor:pointer; }
.pager { margin-top:1rem;justify-content:flex-end; }
</style>
