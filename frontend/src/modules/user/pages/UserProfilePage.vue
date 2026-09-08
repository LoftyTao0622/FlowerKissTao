<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { updateUserProfile, uploadAvatar } from '@/modules/user/api/profile'
import { useAuthStore } from '@/modules/user/stores/auth'
import { ApiError } from '@/shared/api/request'

const authStore = useAuthStore()
const username = ref('')
const nickname = ref('')
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const avatar = ref<string | null>(null)
const saving = ref(false)
const savingPassword = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
let mounted = true

function captureSession() {
  const token = authStore.token
  return () => mounted && authStore.token === token && Boolean(authStore.user)
}

const displayAvatar = computed(() => avatar.value || authStore.user?.avatar || '')
const avatarInitial = computed(() => (nickname.value || username.value || '花').slice(0, 1).toUpperCase())

function syncFromUser() {
  const user = authStore.user
  if (!user) return
  username.value = user.username
  nickname.value = user.nickname ?? ''
  avatar.value = user.avatar
}

function openFilePicker() {
  fileInput.value?.click()
}

async function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) {
    ElMessage.warning('头像只支持 JPG、PNG 或 GIF 图片')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 2 MB')
    return
  }
  uploading.value = true
  const isCurrent = captureSession()
  try {
    const result = await uploadAvatar(file)
    if (!isCurrent()) return
    avatar.value = result.url
    if (authStore.user) authStore.replaceUser({ ...authStore.user, avatar: result.url })
    ElMessage.success('头像已上传')
  } catch (error) {
    if (isCurrent()) ElMessage.error(error instanceof ApiError ? error.message : '头像上传失败')
  } finally {
    uploading.value = false
  }
}

async function saveProfile() {
  if (saving.value || savingPassword.value || uploading.value) return
  if (!username.value.trim()) {
    ElMessage.warning('用户名不能为空')
    return
  }
  saving.value = true
  const isCurrent = captureSession()
  try {
    const user = await updateUserProfile({
      username: username.value.trim(),
      nickname: nickname.value.trim(),
    })
    if (!isCurrent()) return
    authStore.replaceUser(user)
    ElMessage.success('个人资料已保存')
  } catch (error) {
    if (isCurrent()) ElMessage.error(error instanceof ApiError ? error.message : '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  if (saving.value || savingPassword.value || uploading.value) return
  if (!newPassword.value.trim()) {
    ElMessage.warning('请输入新密码，不能全部为空格')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPassword.value = true
  const isCurrent = captureSession()
  try {
    const user = await updateUserProfile({ currentPassword: currentPassword.value, newPassword: newPassword.value })
    if (!isCurrent()) return
    authStore.replaceUser(user)
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    ElMessage.success('密码已修改，下次登录请使用新密码')
  } catch (error) {
    if (isCurrent()) ElMessage.error(error instanceof ApiError ? error.message : '修改密码失败，请稍后重试')
  } finally {
    savingPassword.value = false
  }
}

onMounted(syncFromUser)
onBeforeUnmount(() => { mounted = false })
</script>

<template>
  <section class="profile-page container">
    <header class="profile-page__head">
      <h1>个人资料</h1>
      <p>更新你的公开资料和登录密码，头像会立即同步到导航栏。</p>
    </header>

    <div class="profile-layout">
      <section class="profile-card profile-card--identity" aria-labelledby="identity-title">
        <div class="avatar-editor">
          <div class="avatar-preview">
            <img v-if="displayAvatar" :src="displayAvatar" alt="个人头像" />
            <span v-else aria-hidden="true">{{ avatarInitial }}</span>
          </div>
          <div>
            <h2 id="identity-title">头像</h2>
            <p>支持 JPG、PNG、GIF，大小不超过 2 MB。</p>
            <input ref="fileInput" class="sr-only" type="file" accept="image/jpeg,image/png,image/gif" @change="handleAvatarChange" />
            <button class="outline-button" type="button" :disabled="uploading || saving || savingPassword" @click="openFilePicker">
              {{ uploading ? '上传中…' : '上传新头像' }}
            </button>
          </div>
        </div>

        <form class="profile-form" @submit.prevent="saveProfile">
          <div class="field">
            <label for="profile-username">用户名</label>
            <input id="profile-username" v-model="username" type="text" autocomplete="username" minlength="4" maxlength="20" pattern="[a-zA-Z0-9_\-]+" required />
            <small>用于登录，4–20 位字母、数字、下划线或短横线。</small>
          </div>
          <div class="field">
            <label for="profile-nickname">昵称</label>
            <input id="profile-nickname" v-model="nickname" type="text" maxlength="50" autocomplete="nickname" placeholder="还没有设置昵称" />
          </div>
          <button class="primary-button" type="submit" :disabled="saving || savingPassword || uploading">
            {{ saving ? '保存中…' : '保存个人资料' }}
          </button>
        </form>
      </section>

      <section class="profile-card" aria-labelledby="password-title">
        <div class="card-heading">
          <h2 id="password-title">修改密码</h2>
          <p>为了保护账号，修改密码需要先验证当前密码。</p>
        </div>
        <form class="profile-form" @submit.prevent="savePassword">
          <input class="sr-only" :value="authStore.user?.username" autocomplete="username" readonly aria-label="当前用户名" />
          <div class="field">
            <label for="current-password">当前密码</label>
            <input id="current-password" v-model="currentPassword" type="password" autocomplete="current-password" placeholder="输入当前密码" required />
          </div>
          <div class="field">
            <label for="new-password">新密码</label>
            <input id="new-password" v-model="newPassword" type="password" autocomplete="new-password" minlength="8" maxlength="32" placeholder="8–32 位" required />
          </div>
          <div class="field">
            <label for="confirm-password">确认新密码</label>
            <input id="confirm-password" v-model="confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="32" placeholder="再次输入新密码" required />
          </div>
          <button class="primary-button" type="submit" :disabled="saving || savingPassword || uploading">{{ savingPassword ? '保存中…' : '保存密码' }}</button>
        </form>
      </section>
    </div>
  </section>
</template>

<style scoped>
.profile-page { padding-block: clamp(2rem, 6vw, 5rem); }
.profile-page__head { max-width: 42rem; margin-bottom: var(--space-lg); }
.eyebrow { margin: 0 0 .45rem; color: var(--color-brand-accent); font-size: .72rem; font-weight: 800; letter-spacing: .14em; }
.profile-page h1 { margin: 0; color: var(--color-brand); font-size: clamp(2rem, 5vw, 3.2rem); }
.profile-page__head > p:last-child, .card-heading p:last-child, .avatar-editor p { color: var(--color-text-muted); line-height: 1.65; }
.profile-layout { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(18rem, .75fr); gap: var(--space-lg); align-items: start; }
.profile-card { padding: clamp(1.25rem, 3vw, 2rem); border: 1px solid var(--color-border); border-radius: var(--radius-card); background: var(--color-surface); box-shadow: var(--shadow-card); }
.profile-card h2 { margin: 0; color: var(--color-ink); font-size: 1.25rem; }
.avatar-editor { display: flex; align-items: center; gap: var(--space-md); padding-bottom: var(--space-lg); border-bottom: 1px solid var(--color-border); }
.avatar-preview { display: grid; width: 6rem; height: 6rem; flex: 0 0 auto; overflow: hidden; border-radius: 50%; background: var(--color-brand-soft); color: var(--color-brand); font-size: 2rem; font-weight: 800; place-items: center; }
.avatar-preview img { width: 100%; height: 100%; object-fit: cover; }
.avatar-editor p { margin: .4rem 0 .8rem; font-size: .85rem; }
.profile-form { display: grid; gap: var(--space-md); margin-top: var(--space-lg); }
.field { display: grid; gap: .4rem; }
.field label { color: var(--color-ink); font-weight: 750; }
.field input { width: 100%; min-height: 46px; padding: .65rem .8rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-canvas); color: var(--color-ink); font: inherit; }
.field input:focus { border-color: var(--color-brand-accent); outline: 3px solid var(--color-brand-soft); }
.field small { color: var(--color-text-muted); font-size: .78rem; }
.outline-button, .primary-button { min-height: 44px; padding-inline: 1rem; border-radius: var(--radius-pill); font: inherit; font-weight: 750; cursor: pointer; }
.outline-button { border: 1px solid var(--color-brand-accent); background: transparent; color: var(--color-brand); }
.primary-button { border: 1px solid var(--color-brand); background: var(--color-brand); color: var(--color-on-brand); }
.outline-button:hover, .primary-button:hover { filter: brightness(1.05); }
.outline-button:disabled, .primary-button:disabled { cursor: wait; opacity: .6; }
.card-heading { padding-bottom: var(--space-md); border-bottom: 1px solid var(--color-border); }
.card-heading p:last-child { margin-bottom: 0; font-size: .9rem; }
@media (max-width: 48rem) { .profile-layout { grid-template-columns: 1fr; } }
@media (max-width: 28rem) { .avatar-editor { align-items: flex-start; flex-direction: column; } }
</style>
