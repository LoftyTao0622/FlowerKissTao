package com.zyt.flowerkisstao.care.application.service.impl;

import com.zyt.flowerkisstao.care.domain.entity.CareArchive;
import com.zyt.flowerkisstao.care.domain.entity.CareNote;
import com.zyt.flowerkisstao.care.domain.entity.CareNotification;
import com.zyt.flowerkisstao.care.domain.entity.CareTask;
import com.zyt.flowerkisstao.care.domain.model.CareTaskType;
import com.zyt.flowerkisstao.care.web.vo.CareNoteVO;
import com.zyt.flowerkisstao.care.web.vo.CareNotificationVO;
import com.zyt.flowerkisstao.care.web.vo.CareTaskVO;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 养护模块的实体到展示对象映射。集中放一处，免得同一份文案在服务与控制器里各写一遍。
 */
final class CareConverter {

    private CareConverter() {
    }

    static CareTaskVO toVO(CareTask task, LocalDate today) {
        CareTaskType type = CareTaskType.of(task.getTaskType());
        return CareTaskVO.builder()
                .id(task.getId())
                .archiveId(task.getArchiveId())
                .taskType(task.getTaskType())
                .typeLabel(type.label())
                .icon(type.icon())
                .title(task.getTitle())
                .instruction(task.getInstruction())
                .dueDate(task.getDueDate())
                .status(task.getStatus())
                .statusLabel(statusLabel(task.getStatus()))
                .completedAt(task.getCompletedAt())
                .note(task.getNote())
                // 负数表示已过期几天，日历用它排紧急度
                .daysFromToday(ChronoUnit.DAYS.between(today, task.getDueDate()))
                .build();
    }

    /**
     * 成长记录。{@code withImage} 为 false 时不带 base64 内容——
     * 列表里几十条各带一张图，一个响应就是几 MB。
     */
    static CareNoteVO toVO(CareNote note, boolean withImage) {
        boolean hasImage = note.getImage() != null && !note.getImage().isBlank();
        return CareNoteVO.builder()
                .id(note.getId())
                .noteType(note.getNoteType())
                .content(note.getContent())
                .image(withImage ? note.getImage() : null)
                .hasImage(hasImage)
                .createdAt(note.getCreatedAt())
                .build();
    }

    static CareNotificationVO toVO(CareNotification notification) {
        return CareNotificationVO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .archiveId(notification.getArchiveId())
                .taskId(notification.getTaskId())
                .read(notification.getReadFlag() != null && notification.getReadFlag() == 1)
                .createdAt(notification.getCreatedAt())
                .build();
    }

    static String statusLabel(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CareTask.STATUS_PENDING -> "待办";
            case CareTask.STATUS_DONE -> "已完成";
            case CareTask.STATUS_SKIPPED -> "已跳过";
            case CareTask.STATUS_OVERDUE -> "已逾期";
            default -> null;
        };
    }

    /** 频率调整说明。未调整时返回 null，前端就不显示这一行 */
    static String adjustmentNote(CareArchive archive) {
        int factor = archive.getWaterFactor() == null
                ? CareArchive.FACTOR_BASE : archive.getWaterFactor();
        if (factor == CareArchive.FACTOR_BASE) {
            return null;
        }
        int diff = Math.abs(factor - CareArchive.FACTOR_BASE);
        return factor > CareArchive.FACTOR_BASE
                ? "浇水间隔已放宽 " + diff + "%，以贴合你的实际养护节奏"
                : "浇水间隔已收紧 " + diff + "%，当前环境失水较快";
    }
}
