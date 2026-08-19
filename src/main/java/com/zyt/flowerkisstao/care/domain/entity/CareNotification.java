package com.zyt.flowerkisstao.care.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内提醒。方案原文："系统通过站内消息按时提醒。"
 *
 * <p>权限点 {@code care:reminder:manage-own} 早在第一步就预埋好了，这张表是它的落地。
 */
@Data
@TableName("care_notification")
public class CareNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务今天到期 */
    public static final String TYPE_TASK_DUE = "task_due";
    /** 任务已逾期 */
    public static final String TYPE_OVERDUE = "overdue";
    /** 健康反馈的排查结论 */
    public static final String TYPE_HEALTH = "health";
    /** 频率被重新评估过 */
    public static final String TYPE_RE_EVAL = "re_eval";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** task_due / overdue / health / re_eval */
    private String type;

    private String title;

    private String content;

    /** 关联档案，点击跳转用 */
    private Long archiveId;

    /** 关联任务，点击跳转用 */
    private Long taskId;

    private Integer readFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
