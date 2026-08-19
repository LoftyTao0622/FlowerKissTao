package com.zyt.flowerkisstao.care.application.service;

import com.zyt.flowerkisstao.care.web.vo.CareNotificationVO;

import java.util.List;

/**
 * 站内提醒。方案原文："系统通过站内消息按时提醒。"
 */
public interface CareNotificationService {

    /** 我的提醒，未读优先 */
    List<CareNotificationVO> listMine();

    /** 未读数，Header 铃铛角标用 */
    long unreadCount();

    void markRead(Long id);

    /** 全部标记已读 */
    void markAllRead();

    /**
     * 发一条提醒。供定时任务与重评估调用。
     *
     * @param archiveId 关联档案，可空
     * @param taskId    关联任务，可空
     */
    void push(Long userId, String type, String title, String content, Long archiveId, Long taskId);
}
