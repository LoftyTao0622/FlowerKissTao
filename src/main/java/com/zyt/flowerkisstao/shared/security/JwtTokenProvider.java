package com.zyt.flowerkisstao.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 的签发与解析。
 *
 * <p>令牌里只放 userId 与 username，<b>不放权限点</b>。权限改动后旧 token 立刻生效，
 * 代价是每次请求要查一次库（后续可在 UserDetailsService 上加缓存）。
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        byte[] secretBytes = properties.getSecret() == null
                ? new byte[0]
                : properties.getSecret().getBytes(StandardCharsets.UTF_8);
        // HS256 要求密钥不短于 256 位，配置写短了这里直接启动失败，好过上线后才发现
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret 至少需要 32 字节，当前 " + secretBytes.length + " 字节");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    /** 签发访问令牌，subject 为 userId */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getExpireMinutes() * 60_000L);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析并验签，任何非法 token 一律返回 null，由调用方当作未登录处理 */
    public Claims parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.debug("token 已过期: {}", e.getMessage());
        } catch (Exception e) {
            log.debug("token 非法: {}", e.getMessage());
        }
        return null;
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    /** 从请求头值里剥掉 Bearer 前缀 */
    public String resolveToken(String headerValue) {
        if (headerValue == null) {
            return null;
        }
        String prefix = properties.getPrefix();
        if (prefix != null && !prefix.isEmpty() && headerValue.startsWith(prefix)) {
            return headerValue.substring(prefix.length()).trim();
        }
        return null;
    }

    public long getExpireMinutes() {
        return properties.getExpireMinutes();
    }

    public String getHeader() {
        return properties.getHeader();
    }
}
