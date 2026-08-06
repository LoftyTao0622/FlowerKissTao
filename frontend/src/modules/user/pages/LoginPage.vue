<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/modules/user/stores/auth'
import { ApiError } from '@/shared/api/request'
import { ErrorCode } from '@/shared/api/types'

type Mode = 'login' | 'register'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const mode = ref<Mode>('login')
const formRef = ref<FormInstance>()
const errorText = ref('')

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '', phone: '' })

// 校验规则与后端 RegisterDTO 的注解保持一致，避免前端放行、后端才报错
const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度需在 4 到 20 之间', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_-]+$/,
      message: '只能包含字母、数字、下划线和短横线',
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度需在 8 到 32 之间', trigger: 'blur' },
  ],
  nickname: [{ max: 50, message: '昵称最长 50 个字符', trigger: 'blur' }],
  phone: [
    {
      pattern: /^$|^1[3-9]\d{9}$/,
      message: '手机号格式不正确',
      trigger: 'blur',
    },
  ],
}

const isLogin = computed(() => mode.value === 'login')
const currentRules = computed(() => (isLogin.value ? loginRules : registerRules))
const currentModel = computed(() => (isLogin.value ? loginForm : registerForm))

/** 登录后回跳到此前被守卫拦下的页面 */
const redirectTarget = computed(() =>
  typeof route.query.redirect === 'string' ? route.query.redirect : '/',
)

function switchMode(next: Mode) {
  mode.value = next
  errorText.value = ''
  formRef.value?.clearValidate()
}

async function submit() {
  const form = formRef.value
  if (!form) return

  const valid = await form.validate().catch(() => false)
  if (!valid) return

  errorText.value = ''
  try {
    if (isLogin.value) {
      await authStore.login({ ...loginForm })
      ElMessage.success('登录成功，欢迎回来。')
    } else {
      await authStore.register({
        username: registerForm.username,
        password: registerForm.password,
        nickname: registerForm.nickname || undefined,
        phone: registerForm.phone || undefined,
      })
      ElMessage.success('注册成功，已自动登录。')
    }
    await router.replace(redirectTarget.value)
  } catch (error) {
    if (error instanceof ApiError) {
      errorText.value = error.message
      // 用户名被占用时清空该输入框，提示用户换一个
      if (error.code === ErrorCode.USERNAME_TAKEN) {
        registerForm.username = ''
      }
    } else {
      errorText.value = '操作失败，请稍后重试。'
    }
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <header class="auth-card__head">
        <h1>{{ isLogin ? '登录花吻陶' : '创建账号' }}</h1>
        <p>登录后可保存环境画像、推荐结果与养护计划。</p>
      </header>

      <div class="auth-tabs" role="tablist">
        <button
          type="button"
          role="tab"
          class="auth-tabs__item"
          :aria-selected="isLogin"
          :class="{ 'auth-tabs__item--active': isLogin }"
          @click="switchMode('login')"
        >
          登录
        </button>
        <button
          type="button"
          role="tab"
          class="auth-tabs__item"
          :aria-selected="!isLogin"
          :class="{ 'auth-tabs__item--active': !isLogin }"
          @click="switchMode('register')"
        >
          注册
        </button>
      </div>

      <el-form
        ref="formRef"
        :model="currentModel"
        :rules="currentRules"
        label-position="top"
        @submit.prevent="submit"
      >
        <template v-if="isLogin">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="loginForm.username"
              autocomplete="username"
              placeholder="请输入用户名"
              @input="errorText = ''"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              show-password
              @input="errorText = ''"
            />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="registerForm.username"
              autocomplete="username"
              placeholder="4-20 位字母、数字、下划线"
              @input="errorText = ''"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              autocomplete="new-password"
              placeholder="至少 8 位"
              show-password
              @input="errorText = ''"
            />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="registerForm.nickname" placeholder="选填，默认与用户名相同" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="registerForm.phone" placeholder="选填" />
          </el-form-item>
        </template>

        <p v-if="errorText" class="auth-error" role="alert">{{ errorText }}</p>

        <el-button
          class="auth-submit"
          type="primary"
          native-type="submit"
          :loading="authStore.loading"
        >
          {{ isLogin ? '登录' : '注册并登录' }}
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 70vh;
  padding-block: var(--space-xl);
  place-items: center;
}

.auth-card {
  width: min(92vw, 26rem);
  padding: clamp(1.5rem, 4vw, 2.25rem);
  border-radius: var(--radius-card);
  background: var(--color-surface);
  box-shadow: var(--shadow-nav);
}

.auth-card__head h1 {
  margin: 0 0 0.5rem;
  color: var(--color-brand);
  font-size: 1.5rem;
}

.auth-card__head p {
  margin: 0 0 var(--space-md);
  color: var(--color-text-muted);
  font-size: 0.9rem;
  line-height: 1.65;
}

.auth-tabs {
  display: grid;
  margin-bottom: var(--space-md);
  padding: 0.25rem;
  border-radius: var(--radius-pill);
  background: var(--color-canvas);
  gap: 0.25rem;
  grid-template-columns: 1fr 1fr;
}

.auth-tabs__item {
  min-height: 44px;
  border: 0;
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--color-text-muted);
  font-weight: 700;
  cursor: pointer;
  transition: background-color 180ms var(--ease-out), color 180ms var(--ease-out);
}

.auth-tabs__item:active {
  transform: scale(0.95);
}

.auth-tabs__item--active {
  background: var(--color-surface);
  color: var(--color-brand);
  box-shadow: var(--shadow-nav);
}

.auth-error {
  margin: 0 0 var(--space-sm);
  color: var(--color-danger);
  font-size: 0.875rem;
}

.auth-submit {
  width: 100%;
  min-height: 44px;
  border-radius: var(--radius-pill);
}
</style>
