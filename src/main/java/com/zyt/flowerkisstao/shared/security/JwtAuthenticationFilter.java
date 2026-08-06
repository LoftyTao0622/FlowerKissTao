package com.zyt.flowerkisstao.shared.security;

import io.jsonwebtoken.Claims;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 每个请求解析一次 JWT，成功则把 Authentication 放进 SecurityContext。
 *
 * <p>解析失败不在这里抛异常，直接放行到后续过滤器：需要登录的接口会由
 * AuthenticationEntryPoint 兜底返回 401，公开接口则正常通过。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   AppUserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        // 已有认证信息就不重复解析
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = tokenProvider.resolveToken(request.getHeader(tokenProvider.getHeader()));
            if (token != null) {
                Claims claims = tokenProvider.parse(token);
                if (claims != null) {
                    authenticate(request, claims);
                }
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, Claims claims) {
        Long userId;
        try {
            userId = tokenProvider.getUserId(claims);
        } catch (NumberFormatException e) {
            return;
        }
        // 每次按 ID 查库，拿到的是最新角色与权限；改权限后旧 token 立即生效
        AppUserDetails user = userDetailsService.loadByUserId(userId);
        // 用户被删或被封禁，当作未登录，交给 EntryPoint 返回 401
        if (user == null || !user.isEnabled()) {
            return;
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
