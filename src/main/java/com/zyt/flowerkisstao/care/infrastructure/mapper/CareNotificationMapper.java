package com.zyt.flowerkisstao.care.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.flowerkisstao.care.domain.entity.CareNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface CareNotificationMapper extends BaseMapper<CareNotification> {

    /** 同一任务同一类型的提醒只允许成功写入一次。 */
    @Insert("INSERT IGNORE INTO care_notification "
            + "(user_id, type, title, content, archive_id, task_id, read_flag) "
            + "VALUES (#{userId}, #{type}, #{title}, #{content}, #{archiveId}, #{taskId}, #{readFlag})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIgnore(CareNotification notification);
}
