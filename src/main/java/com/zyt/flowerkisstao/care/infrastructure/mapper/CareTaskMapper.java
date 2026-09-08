package com.zyt.flowerkisstao.care.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.flowerkisstao.care.domain.entity.CareTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CareTaskMapper extends BaseMapper<CareTask> {

    /** 并发补任务时由唯一键兜底，重复任务直接忽略。 */
    @Insert("INSERT IGNORE INTO care_task "
            + "(archive_id, task_type, title, instruction, due_date, planned_date, status) "
            + "VALUES (#{archiveId}, #{taskType}, #{title}, #{instruction}, #{dueDate}, #{plannedDate}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIgnore(CareTask task);

    /** 只有仍为待办的任务才能被标记逾期，保证多实例不会重复累计遗漏。 */
    @Update("UPDATE care_task SET status = 3 "
            + "WHERE id = #{id} AND status = 0 AND due_date < CURRENT_DATE")
    int markOverdue(Long id);

    /** 只有仍处于开放状态的任务才能完成，避免并发操作把终态改回待办。 */
    @Update("UPDATE care_task SET status = 1, completed_at = #{completedAt}, note = #{note} "
            + "WHERE id = #{id} AND status IN (0, 3)")
    int completeIfOpen(CareTask task);

    /** 跳过操作与完成操作共享同一个开放状态条件。 */
    @Update("UPDATE care_task SET status = 2 "
            + "WHERE id = #{id} AND status IN (0, 3)")
    int skipIfOpen(Long id);

    /** 延后只修改开放任务，并保留 planned_date 作为稳定的计划实例标识。 */
    @Update("UPDATE care_task SET due_date = #{dueDate}, status = 0 "
            + "WHERE id = #{id} AND status IN (0, 3)")
    int postponeIfOpen(CareTask task);
}
