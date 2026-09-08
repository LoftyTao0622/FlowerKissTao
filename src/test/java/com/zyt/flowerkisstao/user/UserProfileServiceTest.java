package com.zyt.flowerkisstao.user;

import com.zyt.flowerkisstao.shared.config.RedisProperties;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.redis.AfterCommitCacheInvalidator;
import com.zyt.flowerkisstao.shared.redis.RedisKey;
import com.zyt.flowerkisstao.shared.security.AppUserDetails;
import com.zyt.flowerkisstao.user.application.service.impl.ProfileFileService;
import com.zyt.flowerkisstao.user.application.service.impl.UserProfileServiceImpl;
import com.zyt.flowerkisstao.user.domain.entity.SysUser;
import com.zyt.flowerkisstao.user.infrastructure.mapper.SysUserMapper;
import com.zyt.flowerkisstao.user.web.dto.ProfileUpdateDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserProfileServiceTest {
    private SysUserMapper mapper;
    private UserProfileServiceImpl service;
    private AfterCommitCacheInvalidator invalidator;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeEach
    void setUp() {
        mapper = mock(SysUserMapper.class);
        invalidator = mock(AfterCommitCacheInvalidator.class);
        service = new UserProfileServiceImpl(mapper, encoder, invalidator,
                new RedisKey(new RedisProperties()), mock(ProfileFileService.class));
        SysUser user = new SysUser();
        user.setId(7L);
        user.setUsername("tester");
        user.setNickname("原昵称");
        user.setPassword(encoder.encode("oldPassword"));
        user.setStatus(1);
        when(mapper.selectForUpdate(7L)).thenReturn(user);
        when(mapper.selectById(7L)).thenReturn(user);
        AppUserDetails principal = new AppUserDetails(7L, "tester", "", 1, Set.of("ROLE_USER"), Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void rejectsWrongCurrentPasswordWithoutSavingAnyFields() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setUsername("updated");
        dto.setNewPassword("newPassword");
        dto.setCurrentPassword("wrong");
        assertThatThrownBy(() -> service.update(dto)).isInstanceOf(BizException.class).hasMessage("当前密码不正确");
        verify(mapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void passwordChangeUsesHashAndOnlyCurrentAccount() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setNewPassword("newPassword");
        dto.setCurrentPassword("oldPassword");
        service.update(dto);
        ArgumentCaptor<SysUser> saved = ArgumentCaptor.forClass(SysUser.class);
        verify(mapper).updateById(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(7L);
        assertThat(saved.getValue().getUsername()).isNull();
        assertThat(saved.getValue().getPassword()).isNotEqualTo("newPassword");
        assertThat(encoder.matches("newPassword", saved.getValue().getPassword())).isTrue();
        verify(invalidator).delete(anyString());
    }

    @Test
    void allowsClearingNicknameWithoutChangingPassword() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setNickname("   ");
        service.update(dto);
        ArgumentCaptor<SysUser> saved = ArgumentCaptor.forClass(SysUser.class);
        verify(mapper).updateById(saved.capture());
        assertThat(saved.getValue().getNickname()).isEmpty();
        assertThat(saved.getValue().getPassword()).isNull();
    }

    @Test
    void rejectsBlankPassword() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setUsername("updated");
        dto.setNewPassword("        ");
        assertThatThrownBy(() -> service.update(dto)).isInstanceOf(BizException.class);
        verify(mapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void rejectsPasswordBeyondBcryptByteLimit() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setCurrentPassword("oldPassword");
        dto.setNewPassword("密".repeat(25));
        assertThatThrownBy(() -> service.update(dto)).isInstanceOf(BizException.class).hasMessageContaining("新密码过长");
        verify(mapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void duplicateUsernameReturnsBusinessConflict() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setUsername("existing");
        when(mapper.updateById(any(SysUser.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertThatThrownBy(() -> service.update(dto)).isInstanceOfSatisfying(BizException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.USERNAME_TAKEN));
    }
}
