package com.zyt.flowerkisstao.care.domain.model;

import java.time.LocalDate;

/**
 * 计划出来的一条任务。还没落库，所以不带 id 与 archiveId。
 *
 * @param type        任务类型
 * @param title       标题
 * @param instruction 操作方法、用量或注意事项
 * @param dueDate     建议日期
 */
public record PlannedTask(CareTaskType type, String title, String instruction, LocalDate dueDate) {
}
