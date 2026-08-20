package com.zyt.flowerkisstao.user.application.service.impl;

import com.zyt.flowerkisstao.shared.security.AppUserDetails;

import java.util.Set;

public record CachedAuthUser(Long userId,
                             String username,
                             Integer status,
                             Set<String> roles,
                             Set<String> permissions) {

    public AppUserDetails toUserDetails() {
        return new AppUserDetails(userId, username, "", status, roles, permissions);
    }

    public static CachedAuthUser from(AppUserDetails details) {
        return new CachedAuthUser(details.getUserId(), details.getUsername(), details.getStatus(),
                Set.copyOf(details.getRoles()), Set.copyOf(details.getPermissions()));
    }
}
