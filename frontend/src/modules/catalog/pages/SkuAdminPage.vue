<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { request } from '@/shared/api/request'
import type { PageResult } from '@/shared/api/types'

interface SkuRow {
  id: number
  skuCode: string
  spec: string
  pot: string | null
  price: number
  stock: number
  image: string
  imageAlt: string
  featured: boolean
  status?: number
}

const rows = ref<SkuRow[]>([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const status = ref<number | undefined>(undefined)
const speciesId = ref<number | undefined>(undefined)
const query = reactive({ current: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const result = await request<PageResult<SkuRow>>('/admin/skus', {
      query: { current: query.current, size: query.size, speciesId: speciesId.value, keyword: keyword.value, status: status.value },
    })
    rows.value = result.records
    total.value = result.total
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '加载失败') }
  finally { loading.value = false }
}
function search() { query.current = 1; void load() }
function changePage(page: number) { query.current = page; void load() }
async function toggle(row: SkuRow) {
  try {
    const next = row.status === 1 ? 0 : 1
    await request<void>(`/admin/skus/${row.id}/status`, { method: 'PUT', query: { status: next } })
    await load()
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '操作失败') }
}
async function remove(row: SkuRow) {
  try {
    await ElMessageBox.confirm(`删除 SKU「${row.skuCode}」？`, '删除 SKU', { type: 'warning' })
    await request<void>(`/admin/skus/${row.id}`, { method: 'DELETE' })
    await load()
  } catch { /* 用户取消 */ }
}
onMounted(() => { void load() })
</script>

<template>
  <section class="sku-admin">
    <header><div><p>INVENTORY</p><h1>SKU 与库存</h1><span>售价与库存来自这一层；下单扣减的也是这里。</span></div><RouterLink class="manage-link" :to="{ name: 'admin-species' }">返回品种管理 →</RouterLink></header>
    <div class="toolbar"><input v-model="keyword" placeholder="SKU 编码 / 规格" @keyup.enter="search" /><input v-model.number="speciesId" type="number" placeholder="品种 ID" /><select v-model="status" @change="search"><option :value="undefined">全部状态</option><option :value="1">上架</option><option :value="0">下架</option></select><button @click="search">搜索</button></div>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="skuCode" label="SKU 编码" min-width="180" />
      <el-table-column prop="spec" label="规格" min-width="160" />
      <el-table-column prop="pot" label="盆器" width="110" />
      <el-table-column label="售价" width="100"><template #default="{ row }">¥{{ row.price }}</template></el-table-column>
      <el-table-column label="库存" width="90"><template #default="{ row }"><strong :class="{ low: row.stock <= 10 }">{{ row.stock }}</strong></template></el-table-column>
      <el-table-column label="精选" width="75"><template #default="{ row }">{{ row.featured ? '是' : '—' }}</template></el-table-column>
      <el-table-column label="状态" width="85"><template #default="{ row }"><el-tag :type="row.status === 0 ? 'info' : 'success'">{{ row.status === 0 ? '下架' : '上架' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="150" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="toggle(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-pagination class="pager" layout="total, prev, pager, next" :total="total" :page-size="query.size" :current-page="query.current" @current-change="changePage" />
  </section>
</template>

<style scoped>
.sku-admin { padding:2rem 0 3rem; }
.sku-admin > header { display:flex;flex-wrap:wrap;justify-content:space-between;gap:1rem;margin-bottom:1.4rem; }
.sku-admin header p { margin:0;color:#718076;font-size:.62rem;letter-spacing:.12em; }
.sku-admin header h1 { margin:.2rem 0 .3rem; }
.sku-admin header span { color:#718076;font-size:.82rem; }
.manage-link { align-self:center;color:#496544;font-size:.78rem;font-weight:700;text-decoration:none; }
.toolbar { display:flex;flex-wrap:wrap;gap:.6rem;margin-bottom:1rem; }
.toolbar input,.toolbar select { min-height:2.45rem;padding:.35rem .7rem;border:1px solid #d9d3c5;border-radius:.45rem;background:#fffdf7; }
.toolbar button { padding:.35rem 1rem;border:1px solid #496544;border-radius:999px;background:#496544;color:#fffdf7;font-weight:700;cursor:pointer; }
.low { color:#a8442f; }
.pager { margin-top:1rem;justify-content:flex-end; }
</style>
