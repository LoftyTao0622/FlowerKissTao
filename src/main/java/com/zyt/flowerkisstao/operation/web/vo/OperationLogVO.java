package com.zyt.flowerkisstao.operation.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/** 一条管理操作日志。前后值已经在记录时按白名单脱敏 */
@Data
@Builder
public class OperationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String targetType;
    private String targetId;
    private Map<String, Object> before;
    private Map<String, Object> after;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}
