package com.zyt.flowerkisstao.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyt.flowerkisstao.shared.security.AppUserDetails;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.user.domain.entity.SysUser;
import com.zyt.flowerkisstao.user.infrastructure.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.redis.cache-enabled=false", "app.redis.rate-limit-enabled=false"})
@AutoConfigureMockMvc
@Transactional
class UserProfileIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired SysUserMapper users;
    @Autowired PasswordEncoder encoder;
    private SysUser account;
    private AppUserDetails principal;

    @BeforeEach
    void createAccount() {
        account = new SysUser();
        account.setUsername("test_" + UUID.randomUUID().toString().substring(0, 8));
        account.setNickname("测试资料");
        account.setPassword(encoder.encode("BeforePass123"));
        account.setStatus(1);
        account.setDeleted(0);
        users.insert(account);
        principal = new AppUserDetails(account.getId(), account.getUsername(), "", 1,
                Set.of("ROLE_USER"), Set.of(Perms.USER_PROFILE_READ_OWN, Perms.USER_PROFILE_UPDATE_OWN));
    }

    @Test
    void changedUsernameNicknameAndPasswordPersistAndNewCredentialsWork() throws Exception {
        String renamed = "new_" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(put("/api/user/profile").with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", renamed, "nickname", "新昵称"))))
                .andExpect(status().isOk()).andExpect(jsonPath("code").value(0))
                .andExpect(jsonPath("data.username").value(renamed))
                .andExpect(jsonPath("data.nickname").value("新昵称"))
                .andExpect(jsonPath("data.password").doesNotExist());
        mvc.perform(put("/api/user/profile").with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("currentPassword", "BeforePass123", "newPassword", "AfterPass123"))))
                .andExpect(jsonPath("code").value(0));
        assertThat(encoder.matches("AfterPass123", users.selectById(account.getId()).getPassword())).isTrue();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", renamed, "password", "BeforePass123"))))
                .andExpect(jsonPath("code").value(2001));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", renamed, "password", "AfterPass123"))))
                .andExpect(jsonPath("code").value(0)).andExpect(jsonPath("data.user.id").value(account.getId()));
    }

    @Test
    void uploadedAvatarIsSavedAndPubliclyReadableAsImage() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", bytes);
        String body = mvc.perform(multipart("/api/user/profile/avatar").file(
                        new MockMultipartFile("file", "avatar.png", "image/png", bytes.toByteArray())).with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("code").value(0))
                .andReturn().getResponse().getContentAsString();
        String url = json.readTree(body).path("data").path("url").asText();
        assertThat(users.selectById(account.getId()).getAvatar()).isEqualTo(url);
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void anonymousAndPermissionlessUsersCannotModifyProfile() throws Exception {
        mvc.perform(put("/api/user/profile").contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"blocked\"}"))
                .andExpect(status().isUnauthorized());
        AppUserDetails noPermissions = new AppUserDetails(account.getId(), account.getUsername(), "", 1, Set.of(), Set.of());
        mvc.perform(put("/api/user/profile").with(user(noPermissions)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"blocked\"}"))
                .andExpect(status().isForbidden());
        assertThat(users.selectById(account.getId()).getNickname()).isEqualTo("测试资料");
    }
}
