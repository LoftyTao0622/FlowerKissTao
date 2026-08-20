package com.zyt.flowerkisstao.shared.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyt.flowerkisstao.shared.config.RedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class RedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisProperties properties;

    public RedisCacheService(StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper,
                             RedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        return get(key, objectMapper.getTypeFactory().constructType(type));
    }

    public <T> Optional<T> get(String key, JavaType type) {
        if (!available()) {
            return Optional.empty();
        }
        try {
            String json = redisTemplate.opsForValue().get(key);
            return json == null ? Optional.empty() : Optional.ofNullable(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.warn("Redis 缓存内容损坏，按未命中处理并删除 key={}", key);
            delete(key);
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("Redis 读取失败，按未命中回源 key={}, error={}", key, e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public void set(String key, Object value, Duration ttl) {
        if (!available() || value == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), jitter(ttl));
        } catch (JsonProcessingException e) {
            log.warn("Redis 缓存对象序列化失败 key={}, type={}", key, value.getClass().getSimpleName());
        } catch (RuntimeException e) {
            log.warn("Redis 写入失败，不影响业务结果 key={}, error={}", key, e.getClass().getSimpleName());
        }
    }

    public void delete(String... keys) {
        if (!available() || keys == null) {
            return;
        }
        for (String key : keys) {
            try {
                redisTemplate.delete(key);
            } catch (RuntimeException e) {
                log.warn("Redis 删除失败，等待 TTL 兜底 key={}, error={}", key, e.getClass().getSimpleName());
            }
        }
    }

    public Long increment(String key, Duration ttl) {
        if (!available()) {
            return null;
        }
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (Long.valueOf(1L).equals(value)) {
                redisTemplate.expire(key, ttl);
            }
            return value;
        } catch (RuntimeException e) {
            log.warn("Redis 计数失败 key={}, error={}", key, e.getClass().getSimpleName());
            return null;
        }
    }

    private boolean available() {
        return properties.isEnabled() && properties.isCacheEnabled();
    }

    private Duration jitter(Duration ttl) {
        long bound = Math.max(0, properties.getTtlJitterSeconds());
        return bound == 0 ? ttl : ttl.plusSeconds(ThreadLocalRandom.current().nextLong(bound + 1));
    }
}
