package com.zyt.flowerkisstao.shared.redis;

import com.zyt.flowerkisstao.shared.config.RedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class RedisRateLimiter {

    private static final DefaultRedisScript<List> LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "local ttl = redis.call('TTL', KEYS[1]); return {count, ttl};", List.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisProperties properties;

    public RedisRateLimiter(StringRedisTemplate redisTemplate, RedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public RateLimitResult check(String key, int limit, Duration window, boolean failOpen) {
        if (!properties.isEnabled()) {
            return RateLimitResult.unavailable(failOpen);
        }
        try {
            List<?> result = redisTemplate.execute(LIMIT_SCRIPT, Collections.singletonList(key),
                    String.valueOf(Math.max(1, window.toSeconds())));
            long count = ((Number) result.get(0)).longValue();
            long retryAfter = Math.max(0, ((Number) result.get(1)).longValue());
            return new RateLimitResult(count <= limit, count, retryAfter, true);
        } catch (RuntimeException e) {
            log.warn("Redis 限流不可用 key={}, failOpen={}, error={}",
                    key, failOpen, e.getClass().getSimpleName());
            return RateLimitResult.unavailable(failOpen);
        }
    }

    public record RateLimitResult(boolean allowed, long count, long retryAfterSeconds, boolean available) {
        public static RateLimitResult unavailable(boolean failOpen) {
            return new RateLimitResult(failOpen, 0, 0, false);
        }
    }
}
