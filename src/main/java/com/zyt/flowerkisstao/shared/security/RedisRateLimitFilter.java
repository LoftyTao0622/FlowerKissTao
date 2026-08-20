package com.zyt.flowerkisstao.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyt.flowerkisstao.shared.config.RedisProperties;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.redis.RedisKey;
import com.zyt.flowerkisstao.shared.redis.RedisRateLimiter;
import com.zyt.flowerkisstao.shared.web.R;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/** 统一入口限流。客户端 IP 只取连接地址，避免无条件信任伪造的转发头。 */
@Component
public class RedisRateLimitFilter extends OncePerRequestFilter {
    private final RedisRateLimiter limiter;
    private final RedisKey redisKey;
    private final RedisProperties properties;
    private final ObjectMapper objectMapper;

    public RedisRateLimitFilter(RedisRateLimiter limiter, RedisKey redisKey,
                                RedisProperties properties, ObjectMapper objectMapper) {
        this.limiter = limiter;
        this.redisKey = redisKey;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Rule rule = rule(request);
        if (rule == null || !properties.isRateLimitEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        String identity = identity(request, rule.scope());
        long window = Instant.now().getEpochSecond() / rule.windowSeconds();
        RedisRateLimiter.RateLimitResult result = limiter.check(
                redisKey.rate(rule.scope(), identity, window), rule.limit(),
                Duration.ofSeconds(rule.windowSeconds()), rule.failOpen());
        if (!result.available() && !rule.failOpen()) {
            write(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    ErrorCode.SERVER_ERROR, "限流服务暂时不可用，请稍后重试", 1);
        } else if (!result.allowed()) {
            long retry = Math.max(1, result.retryAfterSeconds());
            write(response, 429, ErrorCode.TOO_MANY_REQUESTS, "请求过于频繁，请稍后重试", retry);
        } else {
            chain.doFilter(request, response);
        }
    }

    private String identity(HttpServletRequest request, String scope) {
        if ((scope.equals("recommendation") || scope.equals("payment"))
                && SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof AppUserDetails user) {
            return "user:" + user.getUserId();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private Rule rule(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equals(method) && "/api/auth/login".equals(path)) return new Rule("login", 60, properties.getLoginLimit(), false);
        if ("POST".equals(method) && "/api/auth/register".equals(path)) return new Rule("register", 3600, properties.getRegisterLimit(), false);
        if ("GET".equals(method) && ("/api/catalog/plants".equals(path) || "/api/knowledge/articles".equals(path))) return new Rule("search", 60, properties.getSearchLimit(), true);
        if ("POST".equals(method) && "/api/recommendations".equals(path)) return new Rule("recommendation", 60, properties.getRecommendationLimit(), true);
        if ("POST".equals(method) && path.matches("/api/orders/[^/]+/pay")) return new Rule("payment", 60, properties.getPaymentLimit(), false);
        return null;
    }

    private void write(HttpServletResponse response, int status, int code, String message, long retryAfter) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(code, message)));
    }

    private record Rule(String scope, long windowSeconds, int limit, boolean failOpen) { }
}
