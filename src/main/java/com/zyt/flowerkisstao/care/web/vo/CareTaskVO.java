package com.zyt.flowerkisstao.care.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 一条养护任务。方案要求每项含建议日期、操作方法与注意事项 */
@Data
@Builder
public class CareTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long archiveId;

    /** water / fertilize / repot / prune / rotate / pest */
    private String taskType;

    private String typeLabel;

    /** 图标 emoji，日历上一眼区分任务类型 */
    private String icon;

    private String title;

    /** 操作方法、用量或注意事项 */
    private String instruction;

    private LocalDate dueDate;

    /** 0 待办 1 已完成 2 已跳过 3 已逾期 */
    private Integer status;

    private String statusLabel;

    private LocalDateTime completedAt;

    private String note;

    /** 距今天还有几天。负数表示已过期几天，日历上用它排紧急度 */
    private Long daysFromToday;
}
