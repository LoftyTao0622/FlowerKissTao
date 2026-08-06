package com.zyt.flowerkisstao.user.web.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功后返回的令牌信息。
 */
@Data
@AllArgsConstructor
public class TokenVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;

    private String tokenType;

    /** 有效期，单位秒 */
    private long expiresIn;

    private UserVO user;
}
