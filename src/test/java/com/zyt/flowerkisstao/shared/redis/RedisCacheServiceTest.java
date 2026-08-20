package com.zyt.flowerkisstao.shared.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyt.flowerkisstao.shared.config.RedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisCacheService cacheService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RedisProperties properties = new RedisProperties();
        properties.setTtlJitterSeconds(0);
        cacheService = new RedisCacheService(redisTemplate, new ObjectMapper(), properties);
    }

    @Test
    void shouldSerializeAsJsonAndUseExplicitTtl() {
        cacheService.set("test:key", new Sample("value"), Duration.ofSeconds(60));

        verify(valueOperations).set(eq("test:key"), eq("{\"name\":\"value\"}"),
                eq(Duration.ofSeconds(60)));
    }

    @Test
    void shouldTreatRedisFailureAsCacheMiss() {
        when(valueOperations.get("test:key"))
                .thenThrow(new RedisConnectionFailureException("offline"));

        Optional<Sample> result = cacheService.get("test:key", Sample.class);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldDeleteBrokenJsonAndTreatItAsCacheMiss() {
        when(valueOperations.get("test:key")).thenReturn("not-json");

        Optional<Sample> result = cacheService.get("test:key", Sample.class);

        assertThat(result).isEmpty();
        verify(redisTemplate).delete("test:key");
    }

    private record Sample(String name) {
    }
}
