<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  assignRoles,
  changeUserStatus,
  fetchRoles,
  fetchUserPage,
  type RoleItem,
} from '@/modules/user/api/admin'
import { useAuthStore } from '@/modules/user/stores/auth'
import { ApiError } from '@/shared/api/request'
import { Perms } from '@/shared/auth/perms'
import type { AuthUser } from '@/shared/api/types'

const authStore = useAuthStore()

const rows = ref<AuthUser[]>([])
const roles = ref<RoleItem[]>([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const query = reactive({ current: 1, size: 10 })

const roleDialogVisible = ref(false)
const editingUser = ref<AuthUser | null>(null)
const selectedRoleIds = ref<number[]>([])
const saving = ref(false)

const canBan = computed(() => authStore.can(Perms.USER_ACCOUNT_BAN))
const canAssign = computed(() => authStore.can(Perms.USER_ROLE_ASSIGN))

function roleLabel(code: string) {
  return roles.value.find((role) => role.code === code)?.name ?? code
}

function reportError(error: unknown, fallback: string) {
  ElMessage.error(error instanceof ApiError ? error.message : fallback)
}

async function loadUsers() {
  loading.value = true
  try {
    const page = await fetchUserPage({
      current: query.current,
      size: query.size,
      keyword: keyword.value.trim() || undefined,
    })
    rows.value = page.records
    total.value = page.total
  } catch (error) {
    reportError(error, '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  if (!canAssign.value) return
  try {
    roles.value = await fetchRoles()
  } catch (error) {
    reportError(error, '加载角色列表失败')
  }
}

function search() {
  query.current = 1
  void loadUsers()
}

function changePage(page: number) {
  query.current = page
  void loadUsers()
}

async function toggleStatus(row: AuthUser) {
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 0 ? '封禁' : '解封'

  try {
    await ElMessageBox.confirm(
      `确定要${actionText}用户「${row.nickname || row.username}」吗？`,
      `${actionText}确认`,
      { type: 'warning', confirmButtonText: actionText, cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  try {
    await changeUserStatus(row.id, nextStatus)
    ElMessage.success(`已${actionText}`)
    await loadUsers()
  } catch (error) {
    reportError(error, `${actionText}失败`)
  }
}

function openRoleDialog(row: AuthUser) {
  editingUser.value = row
  // 后端列表接口只返回角色 code，这里映射回 id
  selectedRoleIds.value = roles.value
    .filter((role) => row.roles.includes(role.code))
    .map((role) => role.id)
  roleDialogVisible.value = true
}

async function submitRoles() {
  const target = editingUser.value
  if (!target) return

  saving.value = true
  try {
    await assignRoles(target.id, selectedRoleIds.value)
    ElMessage.success('角色已更新，该用户下次请求即刻生效')
    roleDialogVisible.value = false
    await loadUsers()
  } catch (error) {
    reportError(error, '保存角色失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadRoles()
  await loadUsers()
})
</script>

<template>
  <section class="admin-users container">
    <header class="admin-users__head">
      <div>
        <h1>用户管理</h1>
        <p>分配角色后立即生效，无需对方重新登录。</p>
      </div>
      <form class="admin-users__search" role="search" @submit.prevent="search">
        <el-input
          v-model="keyword"
          placeholder="搜索用户名、昵称或手机号"
          clearable
          @clear="search"
        />
        <el-button type="primary" native-type="submit">搜索</el-button>
      </form>
    </header>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="phone" label="手机号" min-width="130">
        <template #default="{ row }">{{ row.phone || '—' }}</template>
      </el-table-column>
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="code in row.roles" :key="code" class="role-tag" type="success" effect="plain">
            {{ roleLabel(code) }}
          </el-tag>
          <span v-if="!row.roles.length" class="muted">未分配</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="dark">
            {{ row.status === 1 ? '正常' : '已封禁' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <!-- 后端禁止操作自己，这里同步禁用按钮，避免点了才报错 -->
          <el-button
            v-if="canAssign"
            link
            type="primary"
            :disabled="row.id === authStore.user?.id"
            @click="openRoleDialog(row)"
          >
            分配角色
          </el-button>
          <el-button
            v-if="canBan"
            link
            :type="row.status === 1 ? 'danger' : 'success'"
            :disabled="row.id === authStore.user?.id"
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '封禁' : '解封' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="admin-users__pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.current"
      @current-change="changePage"
    />

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="min(92vw, 26rem)" align-center>
      <p class="dialog-intro">
        为「{{ editingUser?.nickname || editingUser?.username }}」选择角色，可多选。
      </p>
      <el-checkbox-group v-model="selectedRoleIds" class="role-options">
        <el-checkbox v-for="role in roles" :key="role.id" :value="role.id" class="role-option">
          <strong>{{ role.name }}</strong>
          <small>{{ role.description }}</small>
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRoles">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.admin-users {
  padding-block: var(--space-lg);
}

.admin-users__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: var(--space-md);
  gap: var(--space-sm);
}

.admin-users__head h1 {
  margin: 0 0 0.35rem;
  color: var(--color-brand);
  font-size: 1.5rem;
}

.admin-users__head p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.admin-users__search {
  display: flex;
  min-width: min(100%, 22rem);
  gap: var(--space-xs);
}

.admin-users__pager {
  justify-content: flex-end;
  margin-top: var(--space-md);
}

.role-tag {
  margin-right: 0.35rem;
}

.muted {
  color: var(--color-text-muted);
}

.dialog-intro {
  margin: 0 0 var(--space-sm);
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.role-options {
  display: grid;
  gap: 0.5rem;
}

.role-option {
  display: flex;
  height: auto;
  align-items: flex-start;
  padding: 0.5rem 0;
}

.role-option small {
  display: block;
  color: var(--color-text-muted);
  font-weight: 400;
  line-height: 1.5;
  white-space: normal;
}
</style>
