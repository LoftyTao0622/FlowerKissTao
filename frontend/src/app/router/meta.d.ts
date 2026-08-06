import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** 需要登录才能访问 */
    requiresAuth?: boolean
    /** 已登录用户不应看到的页面，如登录页 */
    guestOnly?: boolean
    /** 需要的权限点，取值见 @/shared/auth/perms */
    permission?: string
  }
}
