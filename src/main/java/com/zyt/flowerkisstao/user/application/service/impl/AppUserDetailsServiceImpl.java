package com.zyt.flowerkisstao.user.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.shared.security.AppUserDetails;
import com.zyt.flowerkisstao.shared.security.AppUserDetailsService;
import com.zyt.flowerkisstao.shared.redis.RedisCacheService;
import com.zyt.flowerkisstao.shared.redis.RedisKey;
import com.zyt.flowerkisstao.user.domain.entity.SysUser;
import com.zyt.flowerkisstao.user.infrastructure.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.time.Duration;

/**
 * 把五张表的查询结果装配成 Spring Security 的 UserDetails。
 */
@Service
public class AppUserDetailsServiceImpl implements AppUserDetailsService {

    private final SysUserMapper userMapper;
    private final RedisCacheService cacheService;
    private final RedisKey redisKey;

    public AppUserDetailsServiceImpl(SysUserMapper userMapper,
                                     RedisCacheService cacheService,
                                     RedisKey redisKey) {
        this.userMapper = userMapper;
        this.cacheService = cacheService;
        this.redisKey = redisKey;
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
        String key = redisKey.authUser(userId);
        CachedAuthUser cached = cacheService.get(key, CachedAuthUser.class).orElse(null);
        if (cached != null) {
            return cached.toUserDetails();
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        AppUserDetails details = assemble(user);
        cacheService.set(key, CachedAuthUser.from(details), Duration.ofMinutes(3));
        return details;
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
