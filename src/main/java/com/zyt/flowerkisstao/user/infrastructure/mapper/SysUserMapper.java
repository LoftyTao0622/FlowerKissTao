package com.zyt.flowerkisstao.user.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.flowerkisstao.user.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 五表 JOIN，取出该用户的全部权限点。登录与每次 token 校验都会走这里。
     */
    @Select("SELECT DISTINCT p.code "
            + "FROM sys_user u "
            + "JOIN sys_user_role ur ON ur.user_id = u.id "
            + "JOIN sys_role r ON r.id = ur.role_id "
            + "JOIN sys_role_permission rp ON rp.role_id = r.id "
            + "JOIN sys_permission p ON p.id = rp.permission_id "
            + "WHERE u.id = #{userId} AND u.deleted = 0")
    Set<String> selectPermissionCodes(@Param("userId") Long userId);

    /**
     * 该用户拥有的角色 code。
     */
    @Select("SELECT r.code "
            + "FROM sys_user_role ur "
            + "JOIN sys_role r ON r.id = ur.role_id "
            + "WHERE ur.user_id = #{userId}")
    Set<String> selectRoleCodes(@Param("userId") Long userId);
}
