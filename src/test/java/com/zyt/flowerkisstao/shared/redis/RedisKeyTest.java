package com.zyt.flowerkisstao.shared.redis;

import com.zyt.flowerkisstao.shared.config.RedisProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeyTest {

    @Test
    void shouldBuildVersionedKeysAndNormalizeSensitiveInput() {
        RedisProperties properties = new RedisProperties();
        RedisKey key = new RedisKey(properties);

        assertThat(key.authUser(42L)).isEqualTo("flowerkisstao:v1:auth:user:42");
        assertThat(key.hash("  User  Name ")).isEqualTo(key.hash("user name"));
        assertThat(key.rate("login:ip", "192.0.2.1", 123L))
                .startsWith("flowerkisstao:v1:rate:login:ip:")
                .doesNotContain("192.0.2.1");
    }
}
