package com.zyt.flowerkisstao.shared.config;

import com.zyt.flowerkisstao.shared.security.AppUserDetailsService;
import com.zyt.flowerkisstao.shared.security.JwtAuthenticationFilter;
import com.zyt.flowerkisstao.shared.security.RedisRateLimitFilter;
import com.zyt.flowerkisstao.shared.security.RestAuthErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security 配置。
 *
 * <p>{@code @EnableMethodSecurity} 默认就开了 prePostEnabled，各模块接口用
 * {@code @PreAuthorize("hasAuthority(Perms.XXX)")} 控权。
 * 这里只负责放行公开路径，具体权限点不在此集中配置，避免和业务代码两头维护。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** 无需登录即可访问的路径 */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/register",
            // 页面访问埋点对游客开放，只接受 path/pageType/referrer 三个安全字段
            "/api/operation/visits",
    };

    /** 商品与知识内容对游客开放，仅限 GET */
    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/catalog/**",
            "/api/knowledge/**",
            "/api/home/**",
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RedisRateLimitFilter redisRateLimitFilter;
    private final RestAuthErrorHandler authErrorHandler;
    private final AppUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RedisRateLimitFilter redisRateLimitFilter,
                          RestAuthErrorHandler authErrorHandler,
                          AppUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.redisRateLimitFilter = redisRateLimitFilter;
        this.authErrorHandler = authErrorHandler;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // cost 10，与 data.sql 里 admin 的哈希一致
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // 关掉"用户不存在"与"密码错误"的区分，避免被用来枚举用户名
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return provider::authenticate;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 纯 token 认证，不依赖 Cookie，CSRF 无从谈起
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authErrorHandler)
                        .accessDeniedHandler(authErrorHandler))
                .authorizeHttpRequests(auth -> auth
                        // 预检请求不带 token，必须先放行
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        // 静态资源与前端路由交给 Vue 处理
                        .requestMatchers("/", "/index.html", "/assets/**", "/uploads/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(redisRateLimitFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 开发期跨域。前端 vite dev server 跑在 5173，preview 跑在 4173。
     * 生产走同源部署或网关，届时应收紧这里的 origin 列表。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173", "http://127.0.0.1:5173",
                "http://localhost:4173", "http://127.0.0.1:4173"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
