/**
 * 权限点常量，与后端 shared/security/Perms.java 及 sys_permission.code 一一对应。
 * 新增权限点时两端都要加。
 */
export const Perms = {
  CATALOG_PLANT_READ: 'catalog:plant:read',
  CATALOG_PLANT_CREATE: 'catalog:plant:create',
  CATALOG_PLANT_UPDATE: 'catalog:plant:update',
  CATALOG_PLANT_DELETE: 'catalog:plant:delete',
  CATALOG_PLANT_PUBLISH: 'catalog:plant:publish',

  TRADE_ORDER_READ_OWN: 'trade:order:read-own',
  TRADE_ORDER_CREATE: 'trade:order:create',
  TRADE_ORDER_CANCEL_OWN: 'trade:order:cancel-own',
  TRADE_ORDER_READ_ALL: 'trade:order:read-all',
  TRADE_ORDER_SHIP: 'trade:order:ship',
  TRADE_ORDER_REFUND: 'trade:order:refund',

  RECOMMENDATION_RESULT_GENERATE: 'recommendation:result:generate',
  RECOMMENDATION_RESULT_READ_OWN: 'recommendation:result:read-own',
  RECOMMENDATION_RULE_MANAGE: 'recommendation:rule:manage',

  CARE_PLAN_MANAGE_OWN: 'care:plan:manage-own',
  CARE_REMINDER_MANAGE_OWN: 'care:reminder:manage-own',
  CARE_TEMPLATE_MANAGE: 'care:template:manage',

  KNOWLEDGE_ARTICLE_READ: 'knowledge:article:read',
  KNOWLEDGE_ARTICLE_WRITE: 'knowledge:article:write',
  KNOWLEDGE_ARTICLE_PUBLISH: 'knowledge:article:publish',

  OPERATION_BANNER_EDIT: 'operation:banner:edit',
  OPERATION_PAGE_ARRANGE: 'operation:page:arrange',
  OPERATION_DASHBOARD_READ: 'operation:dashboard:read',
  OPERATION_LOG_READ: 'operation:log:read',

  USER_PROFILE_READ_OWN: 'user:profile:read-own',
  USER_PROFILE_UPDATE_OWN: 'user:profile:update-own',
  USER_ACCOUNT_READ: 'user:account:read',
  USER_ACCOUNT_BAN: 'user:account:ban',
  USER_ROLE_ASSIGN: 'user:role:assign',
} as const

export const Roles = {
  USER: 'ROLE_USER',
  OPERATOR: 'ROLE_OPERATOR',
  ADMIN: 'ROLE_ADMIN',
} as const
