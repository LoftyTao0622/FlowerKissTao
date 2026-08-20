package com.zyt.flowerkisstao.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyt.flowerkisstao.shared.config.RedisProperties;
import com.zyt.flowerkisstao.shared.redis.RedisKey;
import com.zyt.flowerkisstao.shared.redis.RedisRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisRateLimitFilterTest {
    private RedisRateLimiter limiter;
    private RedisRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        limiter = mock(RedisRateLimiter.class);
        RedisProperties properties = new RedisProperties();
        filter = new RedisRateLimitFilter(limiter, new RedisKey(properties), properties, new ObjectMapper());
    }

    @Test
    void shouldReturn429WhenLoginLimitExceeded() throws Exception {
        when(limiter.check(anyString(), anyInt(), any(Duration.class), anyBoolean()))
                .thenReturn(new RedisRateLimiter.RateLimitResult(false, 11, 37, true));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("37");
        assertThat(response.getContentAsString()).contains("\"code\":1429");
    }

    @Test
    void shouldFailOpenForSearchWhenRedisUnavailable() throws Exception {
        when(limiter.check(anyString(), anyInt(), any(Duration.class), anyBoolean()))
                .thenReturn(RedisRateLimiter.RateLimitResult.unavailable(true));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/catalog/plants");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldFailClosedForPaymentWhenRedisUnavailable() throws Exception {
        when(limiter.check(anyString(), anyInt(), any(Duration.class), anyBoolean()))
                .thenReturn(RedisRateLimiter.RateLimitResult.unavailable(false));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders/12/pay");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("\"code\":1002");
    }
}
