package com.zyt.flowerkisstao.shared.security;

/**
 * 权限点常量，与 sys_permission.code 一一对应。
 *
 * <p>接口上一律写 {@code @PreAuthorize("hasAuthority(Perms.CATALOG_PLANT_UPDATE)")}，
 * 不要写 hasRole。角色是权限的集合，接口耦合角色的话，新增角色就得改一片注解。
 */
public final class Perms {

    // catalog 商品
    public static final String CATALOG_PLANT_READ = "catalog:plant:read";
    public static final String CATALOG_PLANT_CREATE = "catalog:plant:create";
    public static final String CATALOG_PLANT_UPDATE = "catalog:plant:update";
    public static final String CATALOG_PLANT_DELETE = "catalog:plant:delete";
    public static final String CATALOG_PLANT_PUBLISH = "catalog:plant:publish";

    // trade 交易
    public static final String TRADE_ORDER_READ_OWN = "trade:order:read-own";
    public static final String TRADE_ORDER_CREATE = "trade:order:create";
    public static final String TRADE_ORDER_CANCEL_OWN = "trade:order:cancel-own";
    public static final String TRADE_ORDER_READ_ALL = "trade:order:read-all";
    public static final String TRADE_ORDER_SHIP = "trade:order:ship";
    public static final String TRADE_ORDER_REFUND = "trade:order:refund";

    // recommendation 推荐
    public static final String RECOMMENDATION_RESULT_GENERATE = "recommendation:result:generate";
    public static final String RECOMMENDATION_RESULT_READ_OWN = "recommendation:result:read-own";
    public static final String RECOMMENDATION_RULE_MANAGE = "recommendation:rule:manage";

    // care 养护
    public static final String CARE_PLAN_MANAGE_OWN = "care:plan:manage-own";
    public static final String CARE_REMINDER_MANAGE_OWN = "care:reminder:manage-own";
    public static final String CARE_TEMPLATE_MANAGE = "care:template:manage";

    // knowledge 知识
    public static final String KNOWLEDGE_ARTICLE_READ = "knowledge:article:read";
    public static final String KNOWLEDGE_ARTICLE_WRITE = "knowledge:article:write";
    public static final String KNOWLEDGE_ARTICLE_PUBLISH = "knowledge:article:publish";

    // operation 运营
    public static final String OPERATION_BANNER_EDIT = "operation:banner:edit";
    public static final String OPERATION_PAGE_ARRANGE = "operation:page:arrange";
    /** 运营看板读取。不能借用 user:account:read，否则权限语义错误 */
    public static final String OPERATION_DASHBOARD_READ = "operation:dashboard:read";
    /** 管理操作日志读取 */
    public static final String OPERATION_LOG_READ = "operation:log:read";

    // user 账号与权限
    public static final String USER_PROFILE_READ_OWN = "user:profile:read-own";
    public static final String USER_PROFILE_UPDATE_OWN = "user:profile:update-own";
    public static final String USER_ACCOUNT_READ = "user:account:read";
    public static final String USER_ACCOUNT_BAN = "user:account:ban";
    public static final String USER_ROLE_ASSIGN = "user:role:assign";

    private Perms() {
    }
}
