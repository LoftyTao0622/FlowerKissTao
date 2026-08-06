package com.zyt.flowerkisstao.shared.security;

import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 读取当前登录人的工具类，省去在每个 Controller 上写 @AuthenticationPrincipal。
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AppUserDetails> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails)) {
            return Optional.empty();
        }
        return Optional.of((AppUserDetails) authentication.getPrincipal());
    }

    /** 用于已确定需要登录的场景，取不到直接抛 401 */
    public static AppUserDetails require() {
        return get().orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED, "请先登录"));
    }

    public static Long requireUserId() {
        return require().getUserId();
    }
}
