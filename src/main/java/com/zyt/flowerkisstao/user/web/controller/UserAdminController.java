package com.zyt.flowerkisstao.user.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import com.zyt.flowerkisstao.user.application.service.UserAdminService;
import com.zyt.flowerkisstao.user.web.vo.UserVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户与角色管理，仅管理员可用。
 *
 * <p>注解里写的是权限点而非角色：将来新增角色只需在 sys_role_permission 配数据，
 * 这些接口一行都不用改。Perms 常量是编译期常量，可直接参与注解里的字符串拼接。
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.USER_ACCOUNT_READ + "')")
    public R<IPage<UserVO>> page(@RequestParam(defaultValue = "1") long current,
                                 @RequestParam(defaultValue = "10") long size,
                                 @RequestParam(required = false) String keyword) {
        return R.ok(userAdminService.page(new Page<>(current, size), keyword));
    }

    /** 封禁或解封 */
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('" + Perms.USER_ACCOUNT_BAN + "')")
    public R<Void> changeStatus(@PathVariable Long userId, @RequestParam Integer status) {
        userAdminService.changeStatus(userId, status);
        return R.ok();
    }

    /** 全量覆盖该用户的角色 */
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('" + Perms.USER_ROLE_ASSIGN + "')")
    public R<Void> assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        userAdminService.assignRoles(userId, roleIds);
        return R.ok();
    }
}
