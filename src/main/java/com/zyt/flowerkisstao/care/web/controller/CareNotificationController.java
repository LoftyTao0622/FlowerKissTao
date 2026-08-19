package com.zyt.flowerkisstao.care.web.controller;

import com.zyt.flowerkisstao.care.application.service.CareNotificationService;
import com.zyt.flowerkisstao.care.web.vo.CareNotificationVO;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 站内提醒。方案原文："系统通过站内消息按时提醒。"
 *
 * <p>权限点沿用 care:reminder:manage-own，第一步就预埋好了。
 */
@RestController
@RequestMapping("/api/care/notifications")
@Validated
public class CareNotificationController {

    private final CareNotificationService notificationService;

    public CareNotificationController(CareNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.CARE_REMINDER_MANAGE_OWN + "')")
    public R<List<CareNotificationVO>> listMine() {
        return R.ok(notificationService.listMine());
    }

    /** 未读数，Header 铃铛角标用。单独一个接口，免得为了一个数字拉整个列表 */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('" + Perms.CARE_REMINDER_MANAGE_OWN + "')")
    public R<Long> unreadCount() {
        return R.ok(notificationService.unreadCount());
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('" + Perms.CARE_REMINDER_MANAGE_OWN + "')")
    public R<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return R.ok();
    }

    @PutMapping("/read-all")
    @PreAuthorize("hasAuthority('" + Perms.CARE_REMINDER_MANAGE_OWN + "')")
    public R<Void> markAllRead() {
        notificationService.markAllRead();
        return R.ok();
    }
}
