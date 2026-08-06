package com.zyt.flowerkisstao.user.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** ROLE_USER / ROLE_OPERATOR / ROLE_ADMIN */
    private String code;

    private String name;

    private String description;

    private Integer sort;

    private LocalDateTime createdAt;
}
