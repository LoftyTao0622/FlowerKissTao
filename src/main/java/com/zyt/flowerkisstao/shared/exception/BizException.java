package com.zyt.flowerkisstao.shared.exception;

import lombok.Getter;

/**
 * 业务异常。由 GlobalExceptionHandler 统一转成 {@code R.fail}，不打印堆栈。
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String message) {
        this(ErrorCode.BIZ_ERROR, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
