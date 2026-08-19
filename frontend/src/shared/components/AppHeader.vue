<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import BrandMark from '@/shared/components/BrandMark.vue'
import CartPopover from '@/shared/components/CartPopover.vue'
import { useAuthStore } from '@/modules/user/stores/auth'
import { useCareStore } from '@/modules/care/stores/care'
import { Perms } from '@/shared/auth/perms'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const careStore = useCareStore()
const isMenuOpen = ref(false)
const menuTrigger = ref<HTMLButtonElement | null>(null)
const searchTerm = ref(typeof route.query.q === 'string' ? route.query.q : '')

const navItems = [
  { label: '首页', to: { name: 'home' } },
  { label: '逛植物', to: { name: 'plant-catalog' } },
  { label: '智能推荐', to: { name: 'recommendation' } },
  { label: '知识库', to: { name: 'knowledge' } },
  { label: '养护指南', to: { name: 'care-guide' } },
]

/** 我的订单，登录才显示 */
const isLoggedIn = computed(() => authStore.isLoggedIn)

/** 有用户管理权限时才显示后台入口 */
const canManageUsers = computed(() => authStore.can(Perms.USER_ACCOUNT_READ))

/** 有订单管理权限（运营/管理员）时才显示订单管理入口 */
const canManageOrders = computed(() => authStore.can(Perms.TRADE_ORDER_READ_ALL))

/** 有文章编辑权限时才显示知识文章管理入口 */
const canManageArticles = computed(() => authStore.can(Perms.KNOWLEDGE_ARTICLE_WRITE))

/** 养护提醒未读数。登录状态一变就重新拉，免得换账号后显示上一个人的角标 */
watch(
  () => authStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      void careStore.loadUnreadCount()
    } else {
      careStore.reset()
    }
  },
  { immediate: true },
)

const displayName = computed(() => {
  const value = authStore.displayName.trim()
  if (!value) return ''
  return value.length > 8 ? `${value.slice(0, 8)}…` : value
})

function submitSearch() {
  const keyword = searchTerm.value.trim()
  isMenuOpen.value = false
  void router.push({
    name: 'plant-catalog',
    query: keyword ? { q: keyword } : {},
  })
}

function goLogin() {
  isMenuOpen.value = false
  // 带上当前路径，登录成功后回跳
  void router.push({ name: 'login', query: { redirect: route.fullPath } })
}

async function logout() {
  isMenuOpen.value = false
  await authStore.logout()
  ElMessage.success('已安全退出登录。')
  // 当前页面可能需要权限，退出后统一回首页
  void router.push({ name: 'home' })
}

function closeMenu(restoreFocus = false) {
  if (!isMenuOpen.value) return

  isMenuOpen.value = false
  if (restoreFocus) {
    void nextTick(() => menuTrigger.value?.focus())
  }
}

function toggleMenu() {
  if (isMenuOpen.value) {
    closeMenu(true)
    return
  }

  isMenuOpen.value = true
}

function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape' && isMenuOpen.value) {
    closeMenu(true)
  }
}

watch(
  () => route.fullPath,
  () => {
    isMenuOpen.value = false
    searchTerm.value = typeof route.query.q === 'string' ? route.query.q : ''
  },
)

onMounted(() => {
  window.addEventListener('keydown', handleEscape)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleEscape)
})
</script>

<template>
  <header class="site-header">
    <a class="skip-link" href="#main-content">跳到主要内容</a>
    <div class="container site-header__inner">
      <RouterLink class="brand" :to="{ name: 'home' }" aria-label="花吻陶首页">
        <BrandMark />
        <span class="brand__copy">
          <strong>花吻陶</strong>
          <small>FLOWER KISS TAO</small>
        </span>
      </RouterLink>

      <nav class="desktop-nav" aria-label="主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.label"
          :to="item.to"
          active-class="desktop-nav__link--active"
          class="desktop-nav__link"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <form class="header-search" role="search" @submit.prevent="submitSearch">
        <label class="sr-only" for="desktop-plant-search">搜索植物</label>
        <input
          id="desktop-plant-search"
          v-model="searchTerm"
          type="search"
          name="q"
          placeholder="搜索植物或养护需求"
          autocomplete="off"
        />
        <button type="submit" aria-label="提交搜索">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle cx="11" cy="11" r="6.5" stroke="currentColor" stroke-width="1.8" />
            <path
              d="m16 16 4 4"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
            />
          </svg>
        </button>
      </form>

      <div class="site-header__actions">
        <template v-if="authStore.isLoggedIn">
          <RouterLink
            v-if="canManageUsers || canManageOrders || canManageArticles || authStore.can(Perms.OPERATION_DASHBOARD_READ)"
            class="text-action desktop-account"
            :to="{ name: 'admin-dashboard' }"
          >
            后台
          </RouterLink>
          <RouterLink class="text-action desktop-account" :to="{ name: 'orders' }">
            我的订单
          </RouterLink>
          <RouterLink class="text-action desktop-account" :to="{ name: 'my-plants' }">
            我的植物
          </RouterLink>
          <RouterLink
            class="notify-bell"
            :to="{ name: 'care-notifications' }"
            :aria-label="careStore.unreadCount > 0
              ? `养护提醒，${careStore.unreadCount} 条未读`
              : '养护提醒'"
          >
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M12 3a5 5 0 0 0-5 5v3.5L5.5 15h13L17 11.5V8a5 5 0 0 0-5-5Z"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linejoin="round"
              />
              <path
                d="M10 18a2 2 0 0 0 4 0"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
              />
            </svg>
            <span v-if="careStore.unreadCount" class="notify-bell__count" aria-hidden="true">
              {{ careStore.unreadCount > 99 ? '99+' : careStore.unreadCount }}
            </span>
          </RouterLink>
          <span class="account-name" :title="authStore.displayName">
            你好，{{ displayName }}
          </span>
          <button class="text-action desktop-account" type="button" @click="logout">退出</button>
        </template>
        <button v-else class="account-button desktop-account" type="button" @click="goLogin">
          登录
        </button>
        <CartPopover />
        <button
          ref="menuTrigger"
          class="menu-trigger"
          type="button"
          aria-controls="mobile-navigation"
          :aria-expanded="isMenuOpen"
          :aria-label="isMenuOpen ? '关闭导航菜单' : '打开导航菜单'"
          @click="toggleMenu"
        >
          <span></span>
          <span></span>
          <span></span>
        </button>
      </div>
    </div>

    <Transition name="menu-reveal">
      <div v-if="isMenuOpen" id="mobile-navigation" class="mobile-panel">
        <div class="container mobile-panel__inner">
          <form class="mobile-search" role="search" @submit.prevent="submitSearch">
            <label for="mobile-plant-search">搜索植物</label>
            <div>
              <input
                id="mobile-plant-search"
                v-model="searchTerm"
                type="search"
                name="q"
                placeholder="例如：耐阴、宠物友好"
                autocomplete="off"
              />
              <button type="submit">搜索</button>
            </div>
          </form>
          <nav class="mobile-nav" aria-label="移动端主导航">
            <RouterLink v-for="item in navItems" :key="item.label" :to="item.to">
              {{ item.label }}
              <span aria-hidden="true">→</span>
            </RouterLink>
            <RouterLink v-if="isLoggedIn" :to="{ name: 'orders' }">
              我的订单
              <span aria-hidden="true">→</span>
            </RouterLink>
            <RouterLink v-if="isLoggedIn" :to="{ name: 'my-plants' }">
              我的植物
              <span aria-hidden="true">→</span>
            </RouterLink>
            <RouterLink v-if="isLoggedIn" :to="{ name: 'care-notifications' }">
              养护提醒
              <span v-if="careStore.unreadCount" class="mobile-nav__badge">
                {{ careStore.unreadCount }}
              </span>
              <span aria-hidden="true">→</span>
            </RouterLink>
            <RouterLink v-if="authStore.can(Perms.OPERATION_DASHBOARD_READ)" :to="{ name: 'admin-dashboard' }">
              运营看板
              <span aria-hidden="true">→</span>
            </RouterLink>
            <RouterLink v-if="canManageOrders" :to="{ name: 'admin-orders' }">
              订单管理
              <span aria-hidden="true">→</span>
            </RouterLink>
            <RouterLink v-if="canManageArticles" :to="{ name: 'admin-knowledge' }">
              文章管理
              <span aria-hidden="true">→</span>
            </RouterLink>
            <RouterLink v-if="canManageUsers" :to="{ name: 'admin-users' }">
              用户管理
              <span aria-hidden="true">→</span>
            </RouterLink>
          </nav>
          <button v-if="authStore.isLoggedIn" class="mobile-account" type="button" @click="logout">
            退出 {{ displayName }}
          </button>
          <button v-else class="mobile-account" type="button" @click="goLogin">
            登录并同步推荐档案
          </button>
        </div>
      </div>
    </Transition>
  </header>
</template>

<style scoped>
.site-header {
  position: sticky;
  z-index: 100;
  top: 0;
  background: var(--color-surface);
  box-shadow: var(--shadow-nav);
}

.skip-link {
  position: fixed;
  z-index: 1000;
  top: var(--space-sm);
  left: var(--space-sm);
  padding: 0.7rem 1rem;
  border-radius: var(--radius-pill);
  background: var(--color-brand-deep);
  color: var(--color-on-brand);
  font-weight: 700;
  transform: translateY(-180%);
  transition: transform 180ms var(--ease-out);
}

.skip-link:focus {
  transform: translateY(0);
}

.site-header__inner {
  display: flex;
  min-height: 84px;
  align-items: center;
  gap: clamp(var(--space-sm), 2vw, var(--space-lg));
}

.brand {
  display: inline-flex;
  min-height: 44px;
  flex: 0 0 auto;
  align-items: center;
  gap: 0.7rem;
  color: var(--color-ink);
  text-decoration: none;
}

.brand__copy {
  display: grid;
  line-height: 1;
}

.brand__copy strong {
  font-size: 1.2rem;
  letter-spacing: 0.08em;
}

.brand__copy small {
  margin-top: 0.35rem;
  color: var(--color-text-muted);
  font-size: 0.58rem;
  font-weight: 700;
  letter-spacing: 0.13em;
}

.desktop-nav {
  display: flex;
  align-items: stretch;
  align-self: stretch;
  gap: 0.2rem;
}

.desktop-nav__link {
  position: relative;
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  padding-inline: 0.8rem;
  color: var(--color-text);
  font-size: 0.94rem;
  font-weight: 700;
  text-decoration: none;
}

.desktop-nav__link::after {
  position: absolute;
  right: 0.8rem;
  bottom: 0;
  left: 0.8rem;
  height: 3px;
  border-radius: var(--radius-pill);
  background: var(--color-brand-accent);
  content: '';
  opacity: 0;
  transform: scaleX(0.35);
  transition: opacity 180ms var(--ease-out), transform 180ms var(--ease-out);
}

.desktop-nav__link:hover,
.desktop-nav__link--active {
  color: var(--color-brand);
}

.desktop-nav__link--active::after {
  opacity: 1;
  transform: scaleX(1);
}

.header-search {
  position: relative;
  min-width: 12rem;
  max-width: 20rem;
  flex: 1 1 18rem;
}

.header-search input {
  width: 100%;
  min-height: 44px;
  padding: 0.65rem 2.8rem 0.65rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-canvas);
  color: var(--color-ink);
  outline: none;
  transition: border-color 180ms var(--ease-out), box-shadow 180ms var(--ease-out);
}

.header-search input:focus {
  border-color: var(--color-brand-accent);
  box-shadow: 0 0 0 3px var(--color-brand-soft);
}

.header-search button {
  position: absolute;
  top: 0;
  right: 0;
  display: grid;
  width: 44px;
  height: 44px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--color-brand);
  cursor: pointer;
  place-items: center;
}

.header-search button:hover {
  background: var(--color-brand-soft);
}

.header-search svg {
  width: 22px;
  height: 22px;
}

.site-header__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 0.15rem;
}

.account-button,
.mobile-account {
  min-height: 44px;
  padding-inline: 1rem;
  border: 1px solid var(--color-brand-accent);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--color-brand);
  font-weight: 750;
  cursor: pointer;
  transition: background-color 180ms var(--ease-out), transform 180ms var(--ease-out);
}

.account-button:hover,
.mobile-account:hover {
  background: var(--color-brand-soft);
}

.account-button:active,
.mobile-account:active,
.text-action:active,
.menu-trigger:active {
  transform: scale(0.95);
}

.account-name {
  max-width: 7.5rem;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: 0.82rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 养护提醒铃铛，与购物车图标同一尺度 */
.notify-bell {
  position: relative;
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  color: var(--color-brand);
  border-radius: 50%;
  text-decoration: none;
  transition: background-color 180ms var(--ease-out);
}

.notify-bell:hover {
  background: var(--color-brand-soft);
}

.notify-bell svg {
  width: 22px;
  height: 22px;
}

.notify-bell__count {
  position: absolute;
  top: 4px;
  right: 2px;
  min-width: 1.05rem;
  padding: 0 0.2rem;
  background: var(--color-danger, #a8442f);
  border-radius: var(--radius-pill);
  color: #fff;
  font-size: 0.6rem;
  font-weight: 700;
  line-height: 1.05rem;
  text-align: center;
}

.mobile-nav__badge {
  margin-left: 0.4rem;
  padding: 0.05rem 0.4rem;
  background: var(--color-danger, #a8442f);
  border-radius: var(--radius-pill);
  color: #fff;
  font-size: 0.65rem;
  font-weight: 700;
}

.text-action {
  min-height: 44px;
  padding-inline: 0.6rem;
  border: 0;
  background: transparent;
  color: var(--color-brand);
  font-weight: 700;
  cursor: pointer;
}

.text-action:hover {
  text-decoration: underline;
}

.menu-trigger {
  display: none;
  width: 44px;
  height: 44px;
  padding: 11px 9px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
}

.menu-trigger:hover {
  background: var(--color-brand-soft);
}

.menu-trigger span {
  display: block;
  width: 24px;
  height: 2px;
  margin-block: 4px;
  border-radius: var(--radius-pill);
  background: var(--color-ink);
}

.mobile-panel {
  display: none;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}

.mobile-panel__inner {
  padding-block: var(--space-md) var(--space-lg);
}

.mobile-search label {
  display: block;
  margin-bottom: 0.45rem;
  color: var(--color-text-muted);
  font-size: 0.8rem;
  font-weight: 700;
}

.mobile-search > div {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: var(--space-xs);
}

.mobile-search input {
  min-width: 0;
  min-height: 48px;
  padding-inline: 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-canvas);
  color: var(--color-ink);
}

.mobile-search button {
  min-height: 48px;
  padding-inline: 1.15rem;
  border: 1px solid var(--color-brand-accent);
  border-radius: var(--radius-pill);
  background: var(--color-brand-accent);
  color: var(--color-on-brand);
  font-weight: 750;
}

.mobile-nav {
  display: grid;
  margin-block: var(--space-md);
}

.mobile-nav a {
  display: flex;
  min-height: 52px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-ink);
  font-weight: 750;
  text-decoration: none;
}

.mobile-nav a.router-link-active {
  color: var(--color-brand-accent);
}

.mobile-account {
  width: 100%;
}

.menu-reveal-enter-active,
.menu-reveal-leave-active {
  transition: opacity 180ms var(--ease-out), transform 180ms var(--ease-out);
  transform-origin: top;
}

.menu-reveal-enter-from,
.menu-reveal-leave-to {
  opacity: 0;
  transform: translateY(-0.5rem);
}

@media (max-width: 76rem) {
  .desktop-nav {
    display: none;
  }

  .menu-trigger,
  .mobile-panel {
    display: block;
  }
}

@media (max-width: 48rem) {
  .site-header__inner {
    min-height: 72px;
  }

  .brand__copy small,
  .header-search,
  .desktop-account,
  .account-name {
    display: none;
  }

  .site-header__actions {
    margin-left: auto;
  }
}

@media (max-width: 24rem) {
  .brand__copy {
    display: none;
  }
}
</style>
