package com.zyt.flowerkisstao.care.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 一条养护任务。
 *
 * <p>方案原文："每项任务包含建议日期、操作方法、用量或注意事项。"
 * 三样分别落在 {@link #dueDate}、{@link #instruction} 上。
 *
 * <p>生成规则在 {@code CareTaskPlanner} 里，那是个不依赖 Spring、可脱离容器
 * 单测的纯函数。
 */
@Data
@TableName("care_task")
public class CareTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待办 */
    public static final int STATUS_PENDING = 0;
    /** 已完成 */
    public static final int STATUS_DONE = 1;
    /** 已跳过 */
    public static final int STATUS_SKIPPED = 2;
    /** 已逾期，由每日定时任务标记 */
    public static final int STATUS_OVERDUE = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long archiveId;

    /** water / fertilize / repot / prune / rotate / pest，见 CareTaskType */
    private String taskType;

    private String title;

    /** 操作方法、用量或注意事项 */
    private String instruction;

    /** 建议日期 */
    private LocalDate dueDate;

    /**
     * 计划实例的稳定日期。延后只改变 dueDate，不改变这个值，
     * 这样每日补任务不会把同一个计划重新生成一遍。
     */
    private LocalDate plannedDate;

    private Integer status;

    private LocalDateTime completedAt;

    /** 完成时附的文字记录 */
    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 待办或已逾期都算"没做完"，重评估时要一并重排 */
    public boolean isOpen() {
        return status != null && (status == STATUS_PENDING || status == STATUS_OVERDUE);
    }
}
