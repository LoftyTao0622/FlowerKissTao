package com.zyt.flowerkisstao.shared.security;

import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * 在标准 UserDetailsService 之外补一个按 ID 加载的入口。
 * 登录走用户名，JWT 校验走 ID，两者语义不同，不该挤在同一个方法里。
 */
public interface AppUserDetailsService extends UserDetailsService {

    /**
     * @return 用户不存在时返回 null，由调用方决定如何处理
     */
    AppUserDetails loadByUserId(Long userId);
}
