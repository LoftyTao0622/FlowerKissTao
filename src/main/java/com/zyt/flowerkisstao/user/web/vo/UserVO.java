package com.zyt.flowerkisstao.user.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 返回给前端的用户信息。刻意不含 password 字段。
 * 前端依据 permissions 控制菜单与按钮的显隐。
 */
@Data
@Builder
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String avatar;

    private Integer status;

    private Set<String> roles;

    private Set<String> permissions;
}
