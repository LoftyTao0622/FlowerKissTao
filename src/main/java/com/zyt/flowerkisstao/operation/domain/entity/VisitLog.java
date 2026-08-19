package com.zyt.flowerkisstao.operation.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 页面访问日志。只记页面类型与时间，不记录请求 body 或查询词，避免把个人数据
 * 带进分析表。游客 userId 可为空。
 */
@Data
@TableName("operation_visit_log")
public class VisitLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String path;

    /** home/catalog/recommendation/knowledge/care/trade/admin */
    private String pageType;

    private String referrer;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
