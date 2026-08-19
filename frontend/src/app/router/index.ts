import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import ShopLayout from '@/app/layouts/ShopLayout.vue'
import AdminLayout from '@/app/layouts/AdminLayout.vue'
import { recordVisit } from '@/modules/operation/api/operation'
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
        // 画像要存进账号才能跨设备、跨会话保留，也才能供购后养护计划复用，
        // 所以这一页必须登录。守卫会带上 redirect，登录后自动跳回来
        meta: { title: '智能推荐', requiresAuth: true },
      },
      {
        path: 'care',
        name: 'care-guide',
        component: () => import('@/modules/care/pages/CareGuidePage.vue'),
        meta: { title: '养护指南' },
      },
      {
        path: 'care/my-plants',
        name: 'my-plants',
        component: () => import('@/modules/care/pages/MyPlantsPage.vue'),
        meta: { title: '我的植物', requiresAuth: true },
      },
      {
        path: 'care/plants/:archiveId',
        name: 'plant-care',
        component: () => import('@/modules/care/pages/PlantCarePage.vue'),
        props: true,
        meta: { title: '养护计划', requiresAuth: true },
      },
      {
        path: 'care/notifications',
        name: 'care-notifications',
        component: () => import('@/modules/care/pages/NotificationsPage.vue'),
        meta: { title: '养护提醒', requiresAuth: true },
      },
      {
        path: 'knowledge',
        name: 'knowledge',
        component: () => import('@/modules/knowledge/pages/KnowledgeListPage.vue'),
        // 不加 requiresAuth：知识科普对游客开放，后端 GET 也在白名单里
        meta: { title: '养护知识库' },
      },
      {
        path: 'knowledge/:slug',
        name: 'knowledge-detail',
        component: () => import('@/modules/knowledge/pages/KnowledgeDetailPage.vue'),
        props: true,
        meta: { title: '养护知识' },
      },
      {
        path: 'cart',
        name: 'cart',
        component: () => import('@/modules/trade/pages/CartPage.vue'),
        meta: { title: '购物车', requiresAuth: true },
      },
      {
        path: 'checkout',
        name: 'checkout',
        component: () => import('@/modules/trade/pages/CheckoutPage.vue'),
        meta: { title: '确认订单', requiresAuth: true },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/modules/trade/pages/OrderListPage.vue'),
        meta: { title: '我的订单', requiresAuth: true },
      },
      {
        path: 'orders/:orderId',
        name: 'order-detail',
        component: () => import('@/modules/trade/pages/OrderDetailPage.vue'),
        props: true,
        meta: { title: '订单详情', requiresAuth: true },
      },
      {
        path: 'addresses',
        name: 'addresses',
        component: () => import('@/modules/trade/pages/AddressPage.vue'),
        meta: { title: '收货地址', requiresAuth: true },
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
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/modules/operation/pages/DashboardPage.vue'),
        meta: { title: '运营看板', requiresAuth: true, permission: Perms.OPERATION_DASHBOARD_READ },
      },
      {
        path: 'catalog/species',
        name: 'admin-species',
        component: () => import('@/modules/catalog/pages/SpeciesAdminPage.vue'),
        meta: { title: '品种管理', requiresAuth: true, permission: Perms.CATALOG_PLANT_UPDATE },
      },
      {
        path: 'catalog/skus',
        name: 'admin-skus',
        component: () => import('@/modules/catalog/pages/SkuAdminPage.vue'),
        meta: { title: 'SKU 与库存', requiresAuth: true, permission: Perms.CATALOG_PLANT_UPDATE },
      },
      {
        path: 'orders',
        name: 'admin-orders',
        component: () => import('@/modules/trade/pages/OrderAdminPage.vue'),
        meta: { title: '订单管理', requiresAuth: true, permission: Perms.TRADE_ORDER_READ_ALL },
      },
      {
        path: 'knowledge',
        name: 'admin-knowledge',
        component: () => import('@/modules/knowledge/pages/ArticleAdminPage.vue'),
        meta: { title: '知识文章管理', requiresAuth: true, permission: Perms.KNOWLEDGE_ARTICLE_WRITE },
      },
      {
        path: 'recommendation',
        name: 'admin-recommendation',
        component: () => import('@/modules/operation/pages/WeightConfigPage.vue'),
        meta: { title: '推荐规则', requiresAuth: true, permission: Perms.RECOMMENDATION_RULE_MANAGE },
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/modules/user/pages/UserAdminPage.vue'),
        meta: { title: '用户管理', requiresAuth: true, permission: Perms.USER_ACCOUNT_READ },
      },
      {
        path: 'operation/logs',
        name: 'admin-operation-logs',
        component: () => import('@/modules/operation/pages/OperationLogPage.vue'),
        meta: { title: '操作日志', requiresAuth: true, permission: Perms.OPERATION_LOG_READ },
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

router.afterEach((to, from) => {
  const pageTitle = typeof to.meta.title === 'string' ? to.meta.title : ''
  document.title = pageTitle
    ? `${pageTitle} · 花吻陶`
    : '花吻陶 · 找到真正适合你的植物'

  // 访问埋点是 best-effort，失败不影响导航。pageType 只传固定白名单值，
  // query 不进入后端 operation_visit_log，避免把搜索词等个人输入带进去
  const path = to.path
  const pageType = path.startsWith('/admin') ? 'admin'
    : path.startsWith('/knowledge') ? 'knowledge'
      : path.startsWith('/care') ? 'care'
        : path.startsWith('/plants') ? 'catalog'
          : path.startsWith('/recommendation') ? 'recommendation'
            : path.startsWith('/cart') || path.startsWith('/checkout') || path.startsWith('/orders') ? 'trade'
              : path === '/' ? 'home' : 'home'
  void recordVisit(path, pageType, from.fullPath)
})

export default router
