package com.zyt.flowerkisstao.user.web.controller;

import com.zyt.flowerkisstao.shared.web.R;
import com.zyt.flowerkisstao.user.application.service.AuthService;
import com.zyt.flowerkisstao.user.web.dto.LoginDTO;
import com.zyt.flowerkisstao.user.web.dto.RegisterDTO;
import com.zyt.flowerkisstao.user.web.vo.TokenVO;
import com.zyt.flowerkisstao.user.web.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证接口。login 与 register 在 SecurityConfig 中放行。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public R<TokenVO> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(authService.register(dto));
    }

    @PostMapping("/login")
    public R<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    /** 前端刷新页面后用它恢复登录态与权限 */
    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(authService.currentUser());
    }

    /**
     * 登出。JWT 无状态，服务端不持有会话，令牌由前端自行丢弃。
     * 保留该接口是为了前端调用统一，以及后续接入黑名单时不必改前端。
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }
}
