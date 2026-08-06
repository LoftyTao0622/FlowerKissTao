package com.zyt.flowerkisstao.user.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.shared.security.AppUserDetails;
import com.zyt.flowerkisstao.shared.security.AppUserDetailsService;
import com.zyt.flowerkisstao.user.domain.entity.SysUser;
import com.zyt.flowerkisstao.user.infrastructure.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 把五张表的查询结果装配成 Spring Security 的 UserDetails。
 */
@Service
public class AppUserDetailsServiceImpl implements AppUserDetailsService {

    private final SysUserMapper userMapper;

    public AppUserDetailsServiceImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 登录入口：按用户名加载 */
    @Override
    public AppUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return assemble(user);
    }

    /** JWT 校验入口：按 ID 加载，用户不存在时返回 null 而非抛异常 */
    @Override
    public AppUserDetails loadByUserId(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : assemble(user);
    }

    private AppUserDetails assemble(SysUser user) {
        Set<String> roles = userMapper.selectRoleCodes(user.getId());
        Set<String> permissions = userMapper.selectPermissionCodes(user.getId());
        return new AppUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getStatus(),
                roles,
                permissions);
    }
}
