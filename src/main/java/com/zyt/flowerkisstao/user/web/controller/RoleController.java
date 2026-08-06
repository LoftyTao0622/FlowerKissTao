package com.zyt.flowerkisstao.user.web.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import com.zyt.flowerkisstao.user.domain.entity.SysRole;
import com.zyt.flowerkisstao.user.infrastructure.mapper.SysRoleMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色列表，供分配角色的下拉框使用。
 */
@RestController
@RequestMapping("/api/admin/roles")
public class RoleController {

    private final SysRoleMapper roleMapper;

    public RoleController(SysRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.USER_ROLE_ASSIGN + "')")
    public R<List<SysRole>> list() {
        return R.ok(roleMapper.selectList(
                Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getSort)));
    }
}
