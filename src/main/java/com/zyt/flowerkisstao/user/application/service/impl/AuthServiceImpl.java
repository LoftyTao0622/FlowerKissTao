package com.zyt.flowerkisstao.user.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.security.AppUserDetails;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import com.zyt.flowerkisstao.shared.security.JwtTokenProvider;
import com.zyt.flowerkisstao.user.application.service.AuthService;
import com.zyt.flowerkisstao.user.domain.entity.SysRole;
import com.zyt.flowerkisstao.user.domain.entity.SysUser;
import com.zyt.flowerkisstao.user.domain.entity.SysUserRole;
import com.zyt.flowerkisstao.user.infrastructure.mapper.SysRoleMapper;
import com.zyt.flowerkisstao.user.infrastructure.mapper.SysUserMapper;
import com.zyt.flowerkisstao.user.infrastructure.mapper.SysUserRoleMapper;
import com.zyt.flowerkisstao.user.web.dto.LoginDTO;
import com.zyt.flowerkisstao.user.web.dto.RegisterDTO;
import com.zyt.flowerkisstao.user.web.vo.TokenVO;
import com.zyt.flowerkisstao.user.web.vo.UserVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class AuthServiceImpl implements AuthService {

    /** 注册时默认授予的角色 */
    private static final String DEFAULT_ROLE_CODE = "ROLE_USER";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(SysUserMapper userMapper,
                           SysRoleMapper roleMapper,
                           SysUserRoleMapper userRoleMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           AuthenticationManager authenticationManager) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenVO register(RegisterDTO dto) {
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isEmpty()
                ? dto.getUsername() : dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        user.setDeleted(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册同名时依赖唯一索引兜底，比先查后插可靠
            throw new BizException(ErrorCode.USERNAME_TAKEN, "该用户名已被占用");
        }

        SysRole defaultRole = roleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, DEFAULT_ROLE_CODE));
        if (defaultRole == null) {
            throw new BizException("默认角色未初始化，请先执行 db/data.sql");
        }
        userRoleMapper.insert(new SysUserRole(user.getId(), defaultRole.getId()));

        String token = tokenProvider.generateToken(user.getId(), user.getUsername());
        return buildToken(token, toVO(user,
                Collections.singleton(DEFAULT_ROLE_CODE),
                userMapper.selectPermissionCodes(user.getId())));
    }

    @Override
    public TokenVO login(LoginDTO dto) {
        AppUserDetails principal;
        try {
            principal = (AppUserDetails) authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()))
                    .getPrincipal();
        } catch (DisabledException e) {
            throw new BizException(ErrorCode.ACCOUNT_BANNED, "账号已被封禁，请联系管理员");
        } catch (AuthenticationException e) {
            // 不区分用户不存在与密码错误，避免用户名枚举
            throw new BizException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }

        String token = tokenProvider.generateToken(principal.getUserId(), principal.getUsername());
        SysUser user = userMapper.selectById(principal.getUserId());
        return buildToken(token, toVO(user, principal.getRoles(), principal.getPermissions()));
    }

    @Override
    public UserVO currentUser() {
        AppUserDetails principal = CurrentUser.require();
        SysUser user = userMapper.selectById(principal.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return toVO(user, principal.getRoles(), principal.getPermissions());
    }

    private TokenVO buildToken(String token, UserVO user) {
        return new TokenVO(token, "Bearer", tokenProvider.getExpireMinutes() * 60, user);
    }

    private UserVO toVO(SysUser user, java.util.Set<String> roles, java.util.Set<String> permissions) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .roles(roles)
                .permissions(permissions)
                .build();
    }
}
