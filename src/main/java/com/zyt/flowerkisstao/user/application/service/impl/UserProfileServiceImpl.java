package com.zyt.flowerkisstao.user.application.service.impl;

import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.redis.AfterCommitCacheInvalidator;
import com.zyt.flowerkisstao.shared.redis.RedisKey;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import com.zyt.flowerkisstao.user.application.service.UserProfileService;
import com.zyt.flowerkisstao.user.domain.entity.SysUser;
import com.zyt.flowerkisstao.user.infrastructure.mapper.SysUserMapper;
import com.zyt.flowerkisstao.user.web.dto.ProfileUpdateDTO;
import com.zyt.flowerkisstao.user.web.vo.AvatarUploadVO;
import com.zyt.flowerkisstao.user.web.vo.UserVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.nio.charset.StandardCharsets;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AfterCommitCacheInvalidator cacheInvalidator;
    private final RedisKey redisKey;
    private final ProfileFileService fileService;

    public UserProfileServiceImpl(SysUserMapper userMapper,
                                  PasswordEncoder passwordEncoder,
                                  AfterCommitCacheInvalidator cacheInvalidator,
                                  RedisKey redisKey,
                                  ProfileFileService fileService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.cacheInvalidator = cacheInvalidator;
        this.redisKey = redisKey;
        this.fileService = fileService;
    }

    @Override
    public UserVO current() {
        return toVO(requireUser(), CurrentUser.require().getRoles(), CurrentUser.require().getPermissions());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO update(ProfileUpdateDTO dto) {
        SysUser current = requireUserForUpdate();
        SysUser update = new SysUser();
        update.setId(current.getId());

        if (dto.getUsername() != null) {
            String username = dto.getUsername().trim();
            if (username.isEmpty()) {
                throw new BizException("用户名不能为空");
            }
            update.setUsername(username);
        }
        if (dto.getNickname() != null) {
            String nickname = dto.getNickname().trim();
            update.setNickname(nickname);
        }

        if (dto.getNewPassword() != null) {
            if (dto.getNewPassword().isBlank()) throw new BizException("新密码不能全部为空格");
            if (dto.getNewPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new BizException("新密码过长，请减少中文或特殊字符");
            }
            if (dto.getCurrentPassword() == null
                    || dto.getCurrentPassword().getBytes(StandardCharsets.UTF_8).length > 72
                    || !passwordEncoder.matches(dto.getCurrentPassword(), current.getPassword())) {
                throw new BizException(ErrorCode.LOGIN_FAILED, "当前密码不正确");
            }
            update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        } else if (dto.getCurrentPassword() != null && !dto.getCurrentPassword().isBlank()) {
            throw new BizException("填写当前密码后，请同时填写新密码");
        }

        if (update.getUsername() == null && update.getNickname() == null
                && update.getPassword() == null) {
            throw new BizException("没有需要保存的资料");
        }
        try {
            userMapper.updateById(update);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.USERNAME_TAKEN, "该用户名已被占用");
        }
        cacheInvalidator.delete(redisKey.authUser(current.getId()));
        SysUser saved = userMapper.selectById(current.getId());
        return toVO(saved, CurrentUser.require().getRoles(), CurrentUser.require().getPermissions());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AvatarUploadVO uploadAvatar(MultipartFile file) {
        SysUser current = requireUserForUpdate();
        String url = fileService.storeAvatar(file);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                fileService.deleteAvatar(status == STATUS_COMMITTED ? current.getAvatar() : url);
            }
        });
        SysUser update = new SysUser();
        update.setId(current.getId());
        update.setAvatar(url);
        userMapper.updateById(update);
        cacheInvalidator.delete(redisKey.authUser(current.getId()));
        return new AvatarUploadVO(url);
    }

    private SysUser requireUser() {
        SysUser user = userMapper.selectById(CurrentUser.requireUserId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return user;
    }

    private SysUser requireUserForUpdate() {
        SysUser user = userMapper.selectForUpdate(CurrentUser.requireUserId());
        if (user == null) throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        return user;
    }

    private UserVO toVO(SysUser user, Set<String> roles, Set<String> permissions) {
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
