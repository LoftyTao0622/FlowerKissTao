package com.zyt.flowerkisstao.care.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.care.application.service.CareNotificationService;
import com.zyt.flowerkisstao.care.domain.entity.CareNotification;
import com.zyt.flowerkisstao.care.infrastructure.mapper.CareNotificationMapper;
import com.zyt.flowerkisstao.care.web.vo.CareNotificationVO;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CareNotificationServiceImpl implements CareNotificationService {

    /** 列表最多返回这么多条。提醒是流水，翻很久以前的没有意义 */
    private static final int MAX_LIST = 50;

    private final CareNotificationMapper notificationMapper;

    public CareNotificationServiceImpl(CareNotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public List<CareNotificationVO> listMine() {
        return notificationMapper.selectList(Wrappers.<CareNotification>lambdaQuery()
                        .eq(CareNotification::getUserId, CurrentUser.requireUserId())
                        // 未读排最前，同状态内按时间倒序
                        .orderByAsc(CareNotification::getReadFlag)
                        .orderByDesc(CareNotification::getId)
                        .last("LIMIT " + MAX_LIST)).stream()
                .map(CareConverter::toVO)
                .toList();
    }

    @Override
    public long unreadCount() {
        return notificationMapper.selectCount(Wrappers.<CareNotification>lambdaQuery()
                .eq(CareNotification::getUserId, CurrentUser.requireUserId())
                .eq(CareNotification::getReadFlag, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        // 带 userId 条件更新而不是先查再改：别人的提醒直接更新不到，
        // 既不用抛异常也不会误改
        notificationMapper.update(null, Wrappers.<CareNotification>lambdaUpdate()
                .eq(CareNotification::getId, id)
                .eq(CareNotification::getUserId, CurrentUser.requireUserId())
                .set(CareNotification::getReadFlag, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        notificationMapper.update(null, Wrappers.<CareNotification>lambdaUpdate()
                .eq(CareNotification::getUserId, CurrentUser.requireUserId())
                .eq(CareNotification::getReadFlag, 0)
                .set(CareNotification::getReadFlag, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void push(Long userId, String type, String title, String content,
                     Long archiveId, Long taskId) {
        CareNotification notification = new CareNotification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setArchiveId(archiveId);
        notification.setTaskId(taskId);
        notification.setReadFlag(0);
        notificationMapper.insertIgnore(notification);
    }
}
