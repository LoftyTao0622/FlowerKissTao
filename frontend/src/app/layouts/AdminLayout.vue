<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import BrandMark from '@/shared/components/BrandMark.vue'
import { useAuthStore } from '@/modules/user/stores/auth'
import { Perms } from '@/shared/auth/perms'

const authStore = useAuthStore()
const route = useRoute()
const collapsed = ref(false)

const navGroups = computed(() => [
  {
    label: '概览',
    items: [
      { label: '运营看板', name: 'admin-dashboard', permission: Perms.OPERATION_DASHBOARD_READ },
    ],
  },
  {
    label: '商品与交易',
    items: [
      { label: '品种管理', name: 'admin-species', permission: Perms.CATALOG_PLANT_UPDATE },
      { label: 'SKU 与库存', name: 'admin-skus', permission: Perms.CATALOG_PLANT_UPDATE },
      { label: '订单管理', name: 'admin-orders', permission: Perms.TRADE_ORDER_READ_ALL },
    ],
  },
  {
    label: '内容与推荐',
    items: [
      { label: '知识文章', name: 'admin-knowledge', permission: Perms.KNOWLEDGE_ARTICLE_WRITE },
      { label: '推荐规则', name: 'admin-recommendation', permission: Perms.RECOMMENDATION_RULE_MANAGE },
    ],
  },
  {
    label: '系统',
    items: [
      { label: '用户账号', name: 'admin-users', permission: Perms.USER_ACCOUNT_READ },
      { label: '操作日志', name: 'admin-operation-logs', permission: Perms.OPERATION_LOG_READ },
    ],
  },
])

function can(permission: string) {
  return authStore.can(permission)
}

function isActive(name: string) {
  return route.name === name || (typeof route.name === 'string' && route.name.startsWith(name + '-'))
}
</script>

<template>
  <div class="admin-shell" :class="{ 'admin-shell--collapsed': collapsed }">
    <aside class="admin-sidebar" :aria-label="collapsed ? '后台导航已收起' : '后台导航'">
      <div class="admin-sidebar__brand">
        <RouterLink :to="{ name: 'admin-dashboard' }" aria-label="花吻陶后台首页">
          <BrandMark />
        </RouterLink>
        <button
          class="admin-sidebar__toggle"
          type="button"
          :aria-label="collapsed ? '展开后台导航' : '收起后台导航'"
          :aria-expanded="!collapsed"
          @click="collapsed = !collapsed"
        >
          {{ collapsed ? '→' : '←' }}
        </button>
      </div>

      <nav class="admin-nav">
        <section v-for="group in navGroups" :key="group.label" class="admin-nav__group">
          <h2 v-if="!collapsed">{{ group.label }}</h2>
          <template v-for="item in group.items" :key="item.name">
            <RouterLink
              v-if="can(item.permission)"
              :to="{ name: item.name }"
              :class="['admin-nav__item', { active: isActive(item.name) }]"
              :title="collapsed ? item.label : undefined"
            >
              <span class="admin-nav__mark" aria-hidden="true"></span>
              <span v-if="!collapsed">{{ item.label }}</span>
            </RouterLink>
          </template>
        </section>
      </nav>

      <div class="admin-sidebar__footer">
        <RouterLink :to="{ name: 'home' }" :title="collapsed ? '回到商城' : undefined">
          <span aria-hidden="true">↗</span>
          <span v-if="!collapsed">回到商城</span>
        </RouterLink>
        <span v-if="!collapsed" class="admin-sidebar__user">{{ authStore.displayName }}</span>
      </div>
    </aside>

    <main class="admin-main">
      <header class="admin-topbar">
        <div>
          <p class="admin-topbar__eyebrow">FLOWER KISS TAO / OPERATIONS</p>
          <p class="admin-topbar__title">运营后台</p>
        </div>
        <div class="admin-topbar__actions">
          <span>{{ authStore.displayName }}</span>
          <button type="button" @click="authStore.logout()">退出</button>
        </div>
      </header>
      <div class="admin-content">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<script lang="ts">
export default { name: 'AdminLayout' }
</script>

<style scoped>
.admin-shell {
  --admin-ink: #24312a;
  --admin-muted: #718076;
  --admin-border: #d9d3c5;
  --admin-soft: #e4eadb;
  --admin-brand: #496544;
  display: flex;
  min-height: 100vh;
  background: #f7f4ec;
  color: var(--admin-ink);
}

.admin-sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  width: 16rem;
  height: 100vh;
  flex-shrink: 0;
  padding: 1.25rem 0.9rem;
  background: #203d2c;
  color: #fffdf7;
  transition: width 180ms ease;
}

.admin-shell--collapsed .admin-sidebar {
  width: 4.5rem;
}

.admin-sidebar__brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 2.8rem;
  padding: 0 0.45rem;
}

.admin-sidebar__brand :deep(.brand-mark) {
  color: #fffdf7;
}

.admin-sidebar__toggle {
  width: 2rem;
  height: 2rem;
  background: transparent;
  border: 1px solid rgba(255, 253, 247, 0.25);
  border-radius: 50%;
  color: #fffdf7;
  cursor: pointer;
}

.admin-nav {
  flex: 1;
  overflow-y: auto;
  margin-top: 2rem;
}

.admin-nav__group {
  margin-bottom: 1.3rem;
}

.admin-nav__group h2 {
  padding: 0 0.7rem;
  margin: 0 0 0.45rem;
  color: rgba(255, 253, 247, 0.5);
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-nav__item {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  min-height: 2.65rem;
  padding: 0 0.7rem;
  margin: 0.15rem 0;
  border-radius: 0.55rem;
  color: rgba(255, 253, 247, 0.72);
  font-size: 0.84rem;
  text-decoration: none;
  transition: background-color 160ms ease, color 160ms ease;
}

.admin-shell--collapsed .admin-nav__item {
  justify-content: center;
  padding: 0;
}

.admin-nav__item:hover,
.admin-nav__item.active {
  background: rgba(228, 234, 219, 0.16);
  color: #fffdf7;
}

.admin-nav__item.active .admin-nav__mark {
  background: #c4d5a8;
}

.admin-nav__mark {
  width: 0.45rem;
  height: 0.45rem;
  flex-shrink: 0;
  border-radius: 50%;
  background: rgba(255, 253, 247, 0.4);
}

.admin-sidebar__footer {
  display: grid;
  gap: 0.75rem;
  padding: 1rem 0.45rem 0;
  border-top: 1px solid rgba(255, 253, 247, 0.16);
}

.admin-sidebar__footer a {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  color: rgba(255, 253, 247, 0.75);
  font-size: 0.78rem;
  text-decoration: none;
}

.admin-sidebar__user {
  overflow: hidden;
  color: rgba(255, 253, 247, 0.45);
  font-size: 0.72rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-main {
  min-width: 0;
  flex: 1;
}

.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 4.5rem;
  padding: 0 2rem;
  background: #fffdf7;
  border-bottom: 1px solid var(--admin-border);
}

.admin-topbar__eyebrow {
  margin: 0;
  color: var(--admin-muted);
  font-size: 0.62rem;
  letter-spacing: 0.12em;
}

.admin-topbar__title {
  margin: 0.2rem 0 0;
  font-size: 1rem;
  font-weight: 700;
}

.admin-topbar__actions {
  display: flex;
  align-items: center;
  gap: 1rem;
  color: var(--admin-muted);
  font-size: 0.8rem;
}

.admin-topbar__actions button {
  padding: 0.3rem 0.7rem;
  background: transparent;
  border: 1px solid var(--admin-border);
  border-radius: 999px;
  color: var(--admin-brand);
  cursor: pointer;
  font-size: 0.75rem;
}

.admin-content {
  max-width: 92rem;
  margin: 0 auto;
  padding: 0 1.5rem;
}

@media (max-width: 48rem) {
  .admin-sidebar {
    width: 4.5rem;
  }

  .admin-sidebar__brand {
    justify-content: center;
  }

  .admin-sidebar__brand :deep(.brand-mark) {
    display: none;
  }

  .admin-sidebar__toggle {
    display: block;
  }

  .admin-nav__group h2,
  .admin-nav__item span:not(.admin-nav__mark),
  .admin-sidebar__footer span:not([aria-hidden]) {
    display: none;
  }

  .admin-nav__item {
    justify-content: center;
    padding: 0;
  }

  .admin-topbar {
    padding: 0 1rem;
  }

  .admin-topbar__eyebrow,
  .admin-topbar__actions span {
    display: none;
  }

  .admin-content {
    padding: 0 0.75rem;
  }
}
</style>
