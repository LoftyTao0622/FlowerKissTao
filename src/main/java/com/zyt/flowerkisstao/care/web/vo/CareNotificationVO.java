package com.zyt.flowerkisstao.care.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 一条站内提醒 */
@Data
@Builder
public class CareNotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** task_due / overdue / health / re_eval */
    private String type;

    private String title;

    private String content;

    /** 点击跳转用 */
    private Long archiveId;

    private Long taskId;

    private Boolean read;

    private LocalDateTime createdAt;
}
