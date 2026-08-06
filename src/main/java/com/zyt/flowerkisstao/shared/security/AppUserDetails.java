package com.zyt.flowerkisstao.shared.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security 的当前登录人。
 * authorities 里放的是权限点 code（catalog:plant:update）与角色 code（ROLE_ADMIN），
 * 两者都进同一个集合，所以 hasAuthority 与 hasRole 都能用。
 */
@Getter
@AllArgsConstructor
public class AppUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String password;
    /** 1 正常 0 封禁 */
    private final Integer status;
    private final Set<String> roles;
    private final Set<String> permissions;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.stream.Stream.concat(roles.stream(), permissions.stream())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 封禁账号在此拦截，AuthenticationProvider 会抛 DisabledException */
    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
