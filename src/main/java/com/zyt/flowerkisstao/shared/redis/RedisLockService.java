package com.zyt.flowerkisstao.shared.redis;

import com.zyt.flowerkisstao.shared.config.RedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Service
@Slf4j
public class RedisLockService {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisProperties properties;

    public RedisLockService(StringRedisTemplate redisTemplate, RedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public LockAttempt tryLock(String key, Duration lease) {
        if (!properties.isEnabled()) {
            return LockAttempt.unavailable(key);
        }
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, lease);
            return Boolean.TRUE.equals(acquired)
                    ? LockAttempt.acquired(key, token)
                    : LockAttempt.contended(key);
        } catch (RuntimeException e) {
            log.warn("Redis 分布式锁不可用 key={}, error={}", key, e.getClass().getSimpleName());
            return LockAttempt.unavailable(key);
        }
    }

    public boolean unlock(LockAttempt attempt) {
        if (attempt == null || !attempt.acquired()) {
            return false;
        }
        try {
            Long result = redisTemplate.execute(UNLOCK_SCRIPT,
                    Collections.singletonList(attempt.key()), attempt.token());
            return Long.valueOf(1L).equals(result);
        } catch (RuntimeException e) {
            log.warn("Redis 分布式锁释放失败，等待租约过期 key={}, error={}",
                    attempt.key(), e.getClass().getSimpleName());
            return false;
        }
    }

    public record LockAttempt(String key, String token, Status status) {
        public enum Status { ACQUIRED, CONTENDED, UNAVAILABLE }

        public static LockAttempt acquired(String key, String token) {
            return new LockAttempt(key, token, Status.ACQUIRED);
        }

        public static LockAttempt contended(String key) {
            return new LockAttempt(key, null, Status.CONTENDED);
        }

        public static LockAttempt unavailable(String key) {
            return new LockAttempt(key, null, Status.UNAVAILABLE);
        }

        public boolean acquired() {
            return status == Status.ACQUIRED;
        }
    }
}
