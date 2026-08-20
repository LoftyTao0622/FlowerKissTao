package com.zyt.flowerkisstao.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyt.flowerkisstao.shared.security.AppUserDetails;
import com.zyt.flowerkisstao.user.application.service.impl.CachedAuthUser;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CachedAuthUserTest {

    @Test
    void shouldNotPutPasswordHashIntoCachedJson() throws Exception {
        AppUserDetails details = new AppUserDetails(1L, "demo", "secret-hash", 1,
                Set.of("ROLE_USER"), Set.of("catalog:plant:read"));

        String json = new ObjectMapper().writeValueAsString(CachedAuthUser.from(details));

        assertThat(json).doesNotContain("secret-hash").doesNotContain("password");
    }
}
