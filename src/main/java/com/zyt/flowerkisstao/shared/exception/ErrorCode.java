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

    /** 植物商品不存在或已下架 */
    public static final int PLANT_NOT_FOUND = 3001;
    /** slug 已被占用 */
    public static final int PLANT_SLUG_TAKEN = 3002;

    private ErrorCode() {
    }
}
