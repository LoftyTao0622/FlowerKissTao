package com.zyt.flowerkisstao.shared.exception;

/**
 * 业务状态码。0 保留给成功，其余按模块分段。
 */
public final class ErrorCode {

    /** 通用业务失败 */
    public static final int BIZ_ERROR = 1000;
    /** 参数校验不通过 */
    public static final int PARAM_INVALID = 1001;
    /** 服务器内部错误 */
    public static final int SERVER_ERROR = 1002;

    /** 未登录或 token 失效 */
    public static final int UNAUTHORIZED = 1401;
    /** 已登录但权限不足 */
    public static final int FORBIDDEN = 1403;

    /** 用户名或密码错误 */
    public static final int LOGIN_FAILED = 2001;
    /** 用户名已被占用 */
    public static final int USERNAME_TAKEN = 2002;
    /** 账号已被封禁 */
    public static final int ACCOUNT_BANNED = 2003;

    /** 植物品种不存在或已下架 */
    public static final int PLANT_NOT_FOUND = 3001;
    /** 品种 code 已被占用 */
    public static final int PLANT_SLUG_TAKEN = 3002;
    /** 品种下仍有未删除的 SKU，不能直接删品种 */
    public static final int SPECIES_HAS_SKU = 3003;
    /** 商品 SKU 不存在 */
    public static final int SKU_NOT_FOUND = 3004;
    /** SKU 编码已被占用 */
    public static final int SKU_CODE_TAKEN = 3005;

    /** 场景画像不存在，或不属于当前用户 */
    public static final int PROFILE_NOT_FOUND = 4001;
    /** 同一用户下场景名重复 */
    public static final int PROFILE_SCENE_NAME_TAKEN = 4002;
    /** 已是最后一个场景，删掉后推荐将没有输入 */
    public static final int PROFILE_LAST_SCENE = 4003;

    /** 推荐记录不存在，或不属于当前用户 */
    public static final int REC_RESULT_NOT_FOUND = 5001;
    /** 七项权重之和不等于 100 */
    public static final int REC_WEIGHT_SUM_INVALID = 5002;
    /** 还没有任何场景画像，推荐没有输入 */
    public static final int REC_NO_PROFILE = 5003;

    /** 订单不存在，或不属于当前用户 */
    public static final int ORDER_NOT_FOUND = 6001;
    /** 当前订单状态不允许这个操作 */
    public static final int ORDER_STATUS_INVALID = 6002;
    /** 库存不足，下单失败 */
    public static final int STOCK_INSUFFICIENT = 6003;
    /** 购物车为空，或勾选的条目一个都不可下单 */
    public static final int CART_EMPTY = 6004;
    /** 商品已下架或已删除 */
    public static final int SKU_UNAVAILABLE = 6005;
    /** 收货地址不存在，或不属于当前用户 */
    public static final int ADDRESS_NOT_FOUND = 6006;
    /** 这笔支付已经处理过，幂等拦截 */
    public static final int PAY_DUPLICATED = 6007;

    /** 养护档案不存在，或不属于当前用户 */
    public static final int CARE_ARCHIVE_NOT_FOUND = 7001;
    /** 养护任务不存在，或不属于当前用户 */
    public static final int CARE_TASK_NOT_FOUND = 7002;
    /** 档案已归档，不能再操作 */
    public static final int CARE_ARCHIVE_CLOSED = 7003;
    /** 上传的图片超出大小上限 */
    public static final int CARE_IMAGE_TOO_LARGE = 7004;
    /** 任务已完成或已跳过，不能重复操作 */
    public static final int CARE_TASK_CLOSED = 7005;

    /** 文章不存在、未发布或已删除 */
    public static final int ARTICLE_NOT_FOUND = 8001;
    /** 文章标识已被占用 */
    public static final int ARTICLE_SLUG_TAKEN = 8002;
    /** 当前状态不允许这个操作 */
    public static final int ARTICLE_STATUS_INVALID = 8003;

    private ErrorCode() {
    }
}
