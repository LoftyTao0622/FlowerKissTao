import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import ShopLayout from '@/app/layouts/ShopLayout.vue'
import { useAuthStore } from '@/modules/user/stores/auth'
import { Perms } from '@/shared/auth/perms'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/modules/user/pages/LoginPage.vue'),
    meta: { title: '登录', guestOnly: true },
  },
  {
    path: '/',
    component: ShopLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/modules/home/pages/HomePage.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'plants',
        name: 'plant-catalog',
        component: () => import('@/modules/catalog/pages/PlantCatalogPage.vue'),
        meta: { title: '逛植物' },
      },
      {
        path: 'plants/:plantId',
        name: 'plant-detail',
        component: () => import('@/modules/catalog/pages/PlantDetailPage.vue'),
        props: true,
        meta: { title: '植物详情' },
      },
      {
        path: 'recommendation',
        name: 'recommendation',
        component: () => import('@/modules/recommendation/pages/RecommendationPage.vue'),
        meta: { title: '智能推荐' },
      },
      {
        path: 'care',
        name: 'care-guide',
        component: () => import('@/modules/care/pages/CareGuidePage.vue'),
        meta: { title: '养护指南' },
      },
      {
        path: 'admin/users',
        name: 'admin-users',
        component: () => import('@/modules/user/pages/UserAdminPage.vue'),
        meta: {
          title: '用户管理',
          requiresAuth: true,
          // 与后端 UserAdminController 上的 @PreAuthorize 对应
          permission: Perms.USER_ACCOUNT_READ,
        },
      },
      {
        path: 'forbidden',
        name: 'forbidden',
        component: () => import('@/app/pages/ForbiddenPage.vue'),
        meta: { title: '没有访问权限' },
      },
      {
        path: ':pathMatch(.*)*',
        name: 'not-found',
        component: () => import('@/app/pages/NotFoundPage.vue'),
        meta: { title: '页面未找到' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    if (to.hash) {
      return { el: to.hash, top: 96, behavior: 'smooth' }
    }

    if (to.path !== from.path) {
      return { top: 0 }
    }

    return false
  },
})

/**
 * 登录与权限守卫。
 *
 * 这里做的是体验优化——把无权访问的入口挡在前面，避免用户点进去才看到报错。
 * 真正的安全边界在后端 @PreAuthorize：前端守卫可被绕过，不能作为唯一防线。
 */
router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  // 刷新页面后 store 是空的，需先用本地 token 换回用户信息与权限
  if (!authStore.initialized) {
    await authStore.initialize()
  }

  if (to.meta.guestOnly && authStore.isLoggedIn) {
    return { name: 'home' }
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const required = to.meta.permission
  if (typeof required === 'string' && !authStore.can(required)) {
    return { name: 'forbidden' }
  }

  return true
})

router.afterEach((to) => {
  const pageTitle = typeof to.meta.title === 'string' ? to.meta.title : ''
  document.title = pageTitle
    ? `${pageTitle} · 花吻陶`
    : '花吻陶 · 找到真正适合你的植物'
})

export default router
