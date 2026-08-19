package com.zyt.flowerkisstao.operation.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理操作日志。
 *
 * <p>采用显式记录而不是全局 AOP 自动序列化响应：响应中可能包含密码、地址或
 * base64 图片，自动切面很容易把敏感数据写进审计日志。
 */
@Data
@TableName(value = "sys_operation_log", autoResultMap = true)
public class SysOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 用户名快照，用户后来被删了也看得出谁操作的 */
    private String usernameSnapshot;

    /** catalog/trade/knowledge/recommendation/user/care */
    private String module;

    /** CREATE/UPDATE/DELETE/PUBLISH/SHIP/REFUND/CONFIG/PREVIEW */
    private String action;

    private String targetType;

    private String targetId;

    /** 只放经过白名单挑选的业务字段 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> beforeJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> afterJson;

    private String ip;

    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
