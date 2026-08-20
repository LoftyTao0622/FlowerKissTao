package com.zyt.flowerkisstao.shared.redis;

import com.zyt.flowerkisstao.shared.config.RedisProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class RedisKey {

    private final String prefix;

    public RedisKey(RedisProperties properties) {
        this.prefix = properties.getKeyPrefix().replaceAll(":+$", "");
    }

    public String of(String... segments) {
        return prefix + ":" + String.join(":", segments);
    }

    public String authUser(Long userId) {
        return of("auth", "user", String.valueOf(userId));
    }

    public String careDailyLock(String date) {
        return of("lock", "care", "daily", date);
    }

    public String cart(Long userId) {
        return of("cart", "user", String.valueOf(userId));
    }

    public String plantDetail(String slug) {
        return of("catalog", "plant", "detail", hash(slug));
    }

    public String plantFacets() {
        return of("catalog", "facets", "current");
    }

    public String articleDetail(String slug) {
        return of("knowledge", "article", "detail", hash(slug));
    }

    public String articleFacets() {
        return of("knowledge", "facets", "current");
    }

    public String recommendWeights() {
        return of("recommendation", "weights", "current");
    }

    public String recommendCandidates() {
        return of("recommendation", "candidates", "active");
    }

    public String rate(String scope, String identity, long window) {
        return of("rate", scope, hash(identity), String.valueOf(window));
    }

    public String hash(String input) {
        String normalized = input == null ? "" : input.trim().replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }
}
