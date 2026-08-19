<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import * as knowledgeApi from '../api/knowledge'
import type { Article, ArticleInput, ArticleStep, SearchMiss } from '../types/knowledge'
import { ApiError } from '@/shared/api/request'

const rows = ref<Article[]>([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const query = reactive({ current: 1, size: 10 })

const misses = ref<SearchMiss[]>([])
const showMisses = ref(false)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)

// 状态动作按钮由后端返回的 row.actions 决定，前端不再按权限自行判断——
// 权限差异已经体现在后端返回的动作列表里了

const CATEGORIES = [
  { value: 'watering', label: '浇水方法' },
  { value: 'light', label: '光照判断' },
  { value: 'feeding', label: '施肥' },
  { value: 'repotting', label: '换盆' },
  { value: 'pruning', label: '修剪' },
  { value: 'pest', label: '病虫害防治' },
  { value: 'medium', label: '基质与花盆' },
  { value: 'season', label: '季节养护' },
  { value: 'pet-safety', label: '宠物与儿童安全' },
  { value: 'bloom', label: '花期管理' },
]

const STATUS_OPTIONS = [
  { value: undefined, label: '全部' },
  { value: 0, label: '草稿' },
  { value: 1, label: '待审核' },
  { value: 2, label: '已发布' },
  { value: 3, label: '已下架' },
]

/** 表单。steps 至少一条，与后端的 @NotEmpty 对应 */
const form = reactive<ArticleInput>({
  slug: '',
  title: '',
  summary: '',
  category: 'watering',
  difficulty: 1,
  seasons: [],
  tags: [],
  applicable: '',
  frequency: '',
  steps: [{ title: '', detail: '' }],
  mistakes: [],
  risks: [],
  relatedTaskTypes: [],
  relatedSpecies: [],
  sort: 0,
})

/** 数组字段在表单里用换行文本编辑，提交时再拆开——比动态增删行简单得多 */
const tagsText = ref('')
const mistakesText = ref('')
const risksText = ref('')
const taskTypesText = ref('')
const speciesText = ref('')

function reportError(error: unknown, fallback: string) {
  ElMessage.error(error instanceof ApiError ? error.message : fallback)
}

function toLines(value: string) {
  return value.split('\n').map((line) => line.trim()).filter(Boolean)
}

async function load() {
  loading.value = true
  try {
    const result = await knowledgeApi.fetchAdminArticles(
      query.current, query.size, statusFilter.value, keyword.value,
    )
    rows.value = result.records
    total.value = result.total
  } catch (error) {
    reportError(error, '文章加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  void load()
}

function changePage(page: number) {
  query.current = page
  void load()
}

function filterStatus(value: number | undefined) {
  statusFilter.value = value
  query.current = 1
  void load()
}

async function loadMisses() {
  try {
    misses.value = await knowledgeApi.fetchSearchMisses(20)
    showMisses.value = true
  } catch (error) {
    reportError(error, '清单加载失败')
  }
}

// ===== 编辑 =====

function resetForm() {
  Object.assign(form, {
    slug: '', title: '', summary: '', category: 'watering', difficulty: 1,
    seasons: [], tags: [], applicable: '', frequency: '',
    steps: [{ title: '', detail: '' }],
    mistakes: [], risks: [], relatedTaskTypes: [], relatedSpecies: [], sort: 0,
  })
  tagsText.value = ''
  mistakesText.value = ''
  risksText.value = ''
  taskTypesText.value = ''
  speciesText.value = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: Article) {
  try {
    const detail = await knowledgeApi.fetchAdminArticle(row.id)
    editingId.value = detail.id
    Object.assign(form, {
      slug: detail.slug,
      title: detail.title,
      summary: detail.summary,
      cover: detail.cover ?? undefined,
      category: detail.category,
      difficulty: detail.difficulty,
      seasons: detail.seasons ?? [],
      applicable: detail.applicable ?? '',
      frequency: detail.frequency ?? '',
      steps: detail.steps?.length ? [...detail.steps] : [{ title: '', detail: '' }],
      sort: 0,
    })
    tagsText.value = (detail.tags ?? []).join('\n')
    mistakesText.value = (detail.mistakes ?? []).join('\n')
    risksText.value = (detail.risks ?? []).join('\n')
    taskTypesText.value = (detail.relatedTaskTypes ?? []).join('\n')
    speciesText.value = (detail.relatedSpecies ?? []).join('\n')
    dialogVisible.value = true
  } catch (error) {
    reportError(error, '文章加载失败')
  }
}

function addStep() {
  form.steps.push({ title: '', detail: '' })
}

function removeStep(index: number) {
  if (form.steps.length > 1) form.steps.splice(index, 1)
}

async function save() {
  saving.value = true
  try {
    const payload: ArticleInput = {
      ...form,
      tags: toLines(tagsText.value),
      mistakes: toLines(mistakesText.value),
      risks: toLines(risksText.value),
      relatedTaskTypes: toLines(taskTypesText.value),
      relatedSpecies: toLines(speciesText.value),
      steps: form.steps.filter((s: ArticleStep) => s.title.trim() || s.detail.trim()),
    }
    if (editingId.value === null) {
      await knowledgeApi.createArticle(payload)
      ElMessage.success('已保存为草稿，提交审核后才会发布')
    } else {
      await knowledgeApi.updateArticle(editingId.value, payload)
      ElMessage.success('已保存')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    reportError(error, '保存失败')
  } finally {
    saving.value = false
  }
}

/** 动作按钮来自后端返回的 actions，前端不自己判断状态能做什么 */
async function runAction(row: Article, code: string, label: string) {
  try {
    await ElMessageBox.confirm(`确认对《${row.title}》执行「${label}」？`, label, {
      confirmButtonText: label,
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await knowledgeApi.transitionArticle(row.id, code)
    ElMessage.success(`${label}成功`)
    await load()
  } catch (error) {
    reportError(error, `${label}失败`)
  }
}

async function remove(row: Article) {
  try {
    await ElMessageBox.confirm(`删除《${row.title}》？删除后可以用同一个标识重建。`, '删除文章', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await knowledgeApi.deleteArticle(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    reportError(error, '删除失败')
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <section class="admin-articles container">
    <header class="admin-articles__header">
      <div>
        <h1>知识文章管理</h1>
        <p>新建的文章一律是草稿，需提交审核并发布后读者才能看到。</p>
      </div>
      <div class="admin-articles__header-actions">
        <el-button @click="loadMisses">内容需求清单</el-button>
        <el-button type="primary" @click="openCreate">新建文章</el-button>
      </div>
    </header>

    <!-- 无结果关键词清单：方案要求的"后台内容需求清单" -->
    <el-alert
      v-if="showMisses"
      class="admin-articles__misses"
      type="info"
      :closable="true"
      @close="showMisses = false"
    >
      <template #title>用户搜过但没有结果的词（按搜索次数排序）</template>
      <p v-if="!misses.length">暂时没有——说明现有内容覆盖得还不错。</p>
      <ul v-else>
        <li v-for="item in misses" :key="item.id">
          <strong>{{ item.keyword }}</strong>
          <span>被搜索 {{ item.hitCount }} 次</span>
        </li>
      </ul>
    </el-alert>

    <div class="admin-articles__toolbar">
      <div class="status-tabs" role="tablist" aria-label="按状态筛选">
        <button
          v-for="item in STATUS_OPTIONS"
          :key="String(item.value)"
          type="button"
          role="tab"
          :aria-selected="statusFilter === item.value"
          :class="{ active: statusFilter === item.value }"
          @click="filterStatus(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
      <form role="search" @submit.prevent="search">
        <el-input
          v-model="keyword"
          placeholder="标题或标识"
          clearable
          @clear="search"
        />
        <el-button type="primary" native-type="submit">搜索</el-button>
      </form>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="slug" label="标识" width="140" />
      <el-table-column prop="categoryLabel" label="分类" width="120" />
      <el-table-column prop="difficultyLabel" label="难度" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 2 ? 'success' : row.status === 1 ? 'warning' : 'info'">
            {{ row.statusLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="数据" width="120">
        <template #default="{ row }">
          <small>{{ row.viewCount }} 阅读 / {{ row.usefulCount }} 有用</small>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-for="action in row.actions ?? []"
            :key="action.code"
            link
            :type="action.code === 'PUBLISH' ? 'success' : 'primary'"
            @click="runAction(row, action.code, action.label)"
          >
            {{ action.label }}
          </el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="admin-articles__pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.current"
      @current-change="changePage"
    />

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId === null ? '新建文章' : '编辑文章'"
      width="min(94vw, 44rem)"
      align-center
    >
      <el-form label-position="top" class="article-form">
        <div class="article-form__row">
          <el-form-item label="标识（URL 用，小写字母数字连字符）" required>
            <el-input v-model="form.slug" placeholder="例如 watering" />
          </el-form-item>
          <el-form-item label="排序（大者靠前）">
            <el-input-number v-model="form.sort" :min="0" :max="999" />
          </el-form-item>
        </div>

        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="80" show-word-limit />
        </el-form-item>

        <el-form-item label="摘要" required>
          <el-input v-model="form.summary" maxlength="200" show-word-limit />
        </el-form-item>

        <div class="article-form__row">
          <el-form-item label="分类" required>
            <el-select v-model="form.category">
              <el-option
                v-for="c in CATEGORIES"
                :key="c.value"
                :label="c.label"
                :value="c.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="难度" required>
            <el-select v-model="form.difficulty">
              <el-option label="入门" :value="1" />
              <el-option label="进阶" :value="2" />
              <el-option label="专业" :value="3" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="适用条件（读者据此判断这篇是否适合自己）" required>
          <el-input v-model="form.applicable" type="textarea" :rows="2" maxlength="300" />
        </el-form-item>

        <el-form-item label="操作频次" required>
          <el-input v-model="form.frequency" type="textarea" :rows="2" maxlength="300" />
        </el-form-item>

        <el-form-item label="操作步骤" required>
          <div v-for="(step, index) in form.steps" :key="index" class="step-editor">
            <el-input v-model="step.title" :placeholder="`第 ${index + 1} 步做什么`" />
            <el-input
              v-model="step.detail"
              type="textarea"
              :rows="2"
              placeholder="具体怎么做、用量与注意事项"
            />
            <el-button link type="danger" @click="removeStep(index)">移除这一步</el-button>
          </div>
          <el-button @click="addStep">添加步骤</el-button>
        </el-form-item>

        <el-form-item label="常见误区（一行一条）">
          <el-input v-model="mistakesText" type="textarea" :rows="3" />
        </el-form-item>

        <el-form-item label="风险提示（一行一条）">
          <el-input v-model="risksText" type="textarea" :rows="2" />
        </el-form-item>

        <div class="article-form__row">
          <el-form-item label="标签（一行一个）">
            <el-input v-model="tagsText" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="关联任务类型（water/fertilize/repot/prune/rotate/pest）">
            <el-input v-model="taskTypesText" type="textarea" :rows="3" />
          </el-form-item>
        </div>

        <el-form-item label="关联品种 code（一行一个，留空表示通用文章）">
          <el-input v-model="speciesText" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.admin-articles {
  padding: var(--space-2xl, 3rem) var(--space-lg, 1.25rem);
}

.admin-articles__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
  margin-bottom: var(--space-lg, 1.5rem);
}

.admin-articles__header h1 {
  margin: 0 0 0.3rem;
}

.admin-articles__header p {
  margin: 0;
  color: var(--color-text-muted, #68716a);
  font-size: 0.85rem;
}

.admin-articles__header-actions {
  display: flex;
  gap: var(--space-sm, 0.75rem);
}

.admin-articles__misses {
  margin-bottom: var(--space-md, 1rem);
}

.admin-articles__misses ul {
  margin: 0.5rem 0 0;
  padding-left: 1.1rem;
}

.admin-articles__misses li {
  display: flex;
  gap: var(--space-sm, 0.75rem);
  font-size: 0.82rem;
}

.admin-articles__misses span {
  color: var(--color-text-muted, #68716a);
}

.admin-articles__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md, 1rem);
  margin-bottom: var(--space-md, 1rem);
}

.admin-articles__toolbar form {
  display: flex;
  gap: var(--space-sm, 0.75rem);
}

.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.status-tabs button {
  padding: 0.35rem 0.9rem;
  background: none;
  border: 1px solid var(--color-border, #d9d3c5);
  border-radius: var(--radius-pill, 999px);
  cursor: pointer;
  font-size: 0.8rem;
}

.status-tabs button.active {
  background: var(--color-brand, #496544);
  border-color: var(--color-brand, #496544);
  color: var(--color-on-brand, #fffdf7);
  font-weight: 700;
}

.admin-articles__pager {
  margin-top: var(--space-md, 1rem);
  justify-content: flex-end;
}

.article-form__row {
  display: grid;
  gap: var(--space-md, 1rem);
}

@media (min-width: 34rem) {
  .article-form__row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.step-editor {
  display: grid;
  gap: 0.4rem;
  padding: var(--space-sm, 0.75rem);
  margin-bottom: var(--space-sm, 0.75rem);
  background: var(--color-bg, #f7f4ec);
  border-radius: var(--radius-sm, 0.5rem);
  width: 100%;
}
</style>
