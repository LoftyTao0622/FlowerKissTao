package com.zyt.flowerkisstao.user.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import com.zyt.flowerkisstao.shared.redis.AfterCommitCacheInvalidator;
import com.zyt.flowerkisstao.shared.redis.RedisKey;
import com.zyt.flowerkisstao.user.application.service.UserAdminService;
import com.zyt.flowerkisstao.user.domain.entity.SysUser;
import com.zyt.flowerkisstao.user.domain.entity.SysUserRole;
import com.zyt.flowerkisstao.user.infrastructure.mapper.SysRoleMapper;
import com.zyt.flowerkisstao.user.infrastructure.mapper.SysUserMapper;
import com.zyt.flowerkisstao.user.infrastructure.mapper.SysUserRoleMapper;
import com.zyt.flowerkisstao.user.web.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final AfterCommitCacheInvalidator cacheInvalidator;
    private final RedisKey redisKey;

    public UserAdminServiceImpl(SysUserMapper userMapper,
                                SysRoleMapper roleMapper,
                                SysUserRoleMapper userRoleMapper,
                                AfterCommitCacheInvalidator cacheInvalidator,
                                RedisKey redisKey) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.cacheInvalidator = cacheInvalidator;
        this.redisKey = redisKey;
    }

    @Override
    public IPage<UserVO> page(IPage<?> page, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
                .and(keyword != null && !keyword.trim().isEmpty(),
                        w -> w.like(SysUser::getUsername, keyword)
                                .or().like(SysUser::getNickname, keyword)
                                .or().like(SysUser::getPhone, keyword))
                .orderByDesc(SysUser::getId);

        @SuppressWarnings("unchecked")
        IPage<SysUser> userPage = userMapper.selectPage((IPage<SysUser>) page, wrapper);
        // 列表页只带角色，不逐行查权限点，避免 N+1 放大成两倍
        return userPage.convert(user -> UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .roles(userMapper.selectRoleCodes(user.getId()))
                .permissions(Collections.emptySet())
                .build());
    }

    @Override
    public void changeStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("status 只能是 0 或 1");
        }
        // 防止管理员误封自己后无人可解封
        if (userId.equals(CurrentUser.requireUserId())) {
            throw new BizException("不能修改自己的账号状态");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
        cacheInvalidator.delete(redisKey.authUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (userMapper.selectById(userId) == null) {
            throw new BizException("用户不存在");
        }
        // 自己给自己改角色可能一步丢掉管理员权限，直接禁止
        if (userId.equals(CurrentUser.requireUserId())) {
            throw new BizException("不能修改自己的角色");
        }
        if (roleIds != null && !roleIds.isEmpty()) {
            long existing = roleMapper.selectCount(
                    Wrappers.lambdaQuery(com.zyt.flowerkisstao.user.domain.entity.SysRole.class)
                            .in(com.zyt.flowerkisstao.user.domain.entity.SysRole::getId, roleIds));
            if (existing != roleIds.size()) {
                throw new BizException("存在无效的角色 ID");
            }
        }

        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId));
        if (roleIds != null) {
            roleIds.stream().distinct()
                    .forEach(roleId -> userRoleMapper.insert(new SysUserRole(userId, roleId)));
        }
        cacheInvalidator.delete(redisKey.authUser(userId));
    }
}
