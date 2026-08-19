<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { House, Pouring, Sunny, Timer } from '@element-plus/icons-vue'

import { fetchDefaultProfile } from '@/modules/user/api/profile'
import type { SceneProfile } from '@/modules/user/types/profile'
import { useAuthStore } from '@/modules/user/stores/auth'

/**
 * 首页的环境档案卡片。
 *
 * 三种状态各自渲染，不用假数据兜底：显示一份看起来像真的、实际谁也没填过的
 * 档案，会让用户以为系统已经了解自己的环境，这比空着更有误导性。
 */
const authStore = useAuthStore()
const profile = ref<SceneProfile | null>(null)
const loading = ref(false)

const profileItems = computed(() => {
  if (!profile.value) return []
  const p = profile.value
  return [
    { label: '光照条件', value: p.lightLabel, icon: Sunny },
    { label: '摆放空间', value: `${p.placementLabel} · ${p.spaceLabel}`, icon: House },
    { label: '浇水节奏', value: p.waterTimesWeek > 0 ? `每周 ${p.waterTimesWeek} 次` : '几乎不浇', icon: Pouring },
    { label: '养护经验', value: p.experienceLabel, icon: Timer },
  ]
})

onMounted(async () => {
  // 未登录时不发这个请求：/api/profiles 需要认证，发出去只会白拿一个 401
  if (!authStore.isLoggedIn) return

  loading.value = true
  try {
    profile.value = await fetchDefaultProfile()
  } catch {
    // 首页的辅助卡片，拿不到就退回引导态，不打断整页渲染
    profile.value = null
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="profile" aria-labelledby="profile-title">
    <header class="profile__header">
      <div>
        <p class="profile__eyebrow">{{ profile ? profile.sceneName : '尚未建立' }}</p>
        <h2 id="profile-title">你的环境档案</h2>
      </div>
      <RouterLink class="profile__edit" to="/recommendation">
        {{ profile ? '重新填写' : '去填写' }}
      </RouterLink>
    </header>

    <p v-if="loading" class="profile__hint">正在读取你的环境档案…</p>

    <dl v-else-if="profile" class="profile__grid">
      <div v-for="item in profileItems" :key="item.label" class="profile__item">
        <el-icon class="profile__icon" aria-hidden="true"><component :is="item.icon" /></el-icon>
        <div>
          <dt>{{ item.label }}</dt>
          <dd>{{ item.value }}</dd>
        </div>
      </div>
    </dl>

    <p v-else-if="authStore.isLoggedIn" class="profile__hint">
      还没有环境档案。用四步问答记录光照、空间与养护节奏，推荐才有依据。
    </p>

    <p v-else class="profile__hint">
      登录后可以保存客厅、卧室等多个场景的环境档案，推荐结果会随之调整。
    </p>
  </section>
</template>

<style scoped>
.profile {
  width: min(100%, 40rem);
  padding: var(--space-md);
  color: var(--color-ink);
  background: var(--color-brand-soft);
  border: 1px solid color-mix(in oklch, var(--color-brand) 18%, var(--color-border));
  border-radius: var(--radius-card);
}

.profile__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}

.profile__eyebrow {
  margin: 0 0 0.2rem;
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 650;
  letter-spacing: 0.08em;
}

.profile h2 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}

.profile__edit {
  min-height: 2.75rem;
  display: inline-flex;
  align-items: center;
  color: var(--color-brand);
  font-size: 0.875rem;
  font-weight: 650;
  text-decoration: none;
}

.profile__hint {
  margin: 0;
  padding: 0.75rem 0 0.25rem;
  color: var(--color-text-muted);
  font-size: 0.82rem;
  line-height: 1.7;
}

.profile__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1px;
  margin: 0;
  overflow: hidden;
  background: color-mix(in oklch, var(--color-brand) 14%, var(--color-border));
  border-radius: calc(var(--radius-card) - 0.25rem);
}

.profile__item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
  padding: 0.75rem;
  background: var(--color-surface);
}

.profile__icon {
  flex: 0 0 auto;
  color: var(--color-brand);
  font-size: 1.25rem;
}

.profile dt {
  margin-bottom: 0.15rem;
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.profile dd {
  margin: 0;
  font-size: 0.78rem;
  font-weight: 650;
  line-height: 1.35;
}

@media (min-width: 62rem) {
  .profile {
    padding: var(--space-lg);
  }

  .profile__grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .profile__item {
    align-items: flex-start;
    gap: 0.5rem;
    padding-inline: 0.55rem;
  }

}
</style>
