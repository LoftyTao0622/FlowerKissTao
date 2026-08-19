package com.zyt.flowerkisstao.care.web.controller;

import com.zyt.flowerkisstao.care.application.service.CareArchiveService;
import com.zyt.flowerkisstao.care.application.service.CareMaintenanceService;
import com.zyt.flowerkisstao.care.web.dto.CareNoteDTO;
import com.zyt.flowerkisstao.care.web.dto.HealthReportDTO;
import com.zyt.flowerkisstao.care.web.dto.TaskActionDTO;
import com.zyt.flowerkisstao.care.web.vo.CareArchiveVO;
import com.zyt.flowerkisstao.care.web.vo.CareNoteVO;
import com.zyt.flowerkisstao.care.web.vo.CareTaskVO;
import com.zyt.flowerkisstao.care.web.vo.ReEvaluationVO;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 养护档案，即"我的植物"。
 *
 * <p>路径挂 /api/care，不落 SecurityConfig 的公开 GET 白名单前缀，自动要求登录。
 * 越权访问一律按"不存在"处理——返回 403 等于确认这份档案真实存在。
 *
 * <p>权限点沿用早已预埋的 care:plan:manage-own，ROLE_USER 默认就有。
 */
@RestController
@RequestMapping("/api/care")
@Validated
public class CareArchiveController {

    private final CareArchiveService careArchiveService;
    private final CareMaintenanceService maintenanceService;

    public CareArchiveController(CareArchiveService careArchiveService,
                                 CareMaintenanceService maintenanceService) {
        this.careArchiveService = careArchiveService;
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/archives")
    @PreAuthorize("hasAuthority('" + Perms.CARE_PLAN_MANAGE_OWN + "')")
    public R<List<CareArchiveVO>> listMine() {
        return R.ok(careArchiveService.listMine());
    }

    @GetMapping("/archives/{id}")
    @PreAuthorize("hasAuthority('" + Perms.CARE_PLAN_MANAGE_OWN + "')")
    public R<CareArchiveVO> get(@PathVariable Long id) {
        return R.ok(careArchiveService.get(id));
    }

    /** 任务列表，日历视图的数据源。不传日期时取未来 30 天 */
    @GetMapping("/archives/{id}/tasks")
    @PreAuthorize("hasAuthority('" + Perms.CARE_PLAN_MANAGE_OWN + "')")
    public R<List<CareTaskVO>> listTasks(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(careArchiveService.listTasks(id, from, to));
    }

    /**
     * 手动重新评估。
     *
     * <p>方案要求光照变化时自动调频，那由每日定时任务做。这个接口是给用户主动触发的：
     * 改完场景问卷立刻就想看到养护计划跟着变，而不是等到第二天早上。
     */
    @PostMapping("/archives/{id}/re-evaluate")
    @PreAuthorize("hasAuthority('" + Perms.CARE_PLAN_MANAGE_OWN + "')")
    public R<ReEvaluationVO> reEvaluate(@PathVariable Long id) {
        return R.ok(careArchiveService.reEvaluate(id));
    }

    /** 报告叶片发黄等症状，返回逐项排查建议 */
    @PostMapping("/archives/{id}/health-report")
    @PreAuthorize("hasAuthority('" + Perms.CARE_PLAN_MANAGE_OWN + "')")
    public R<ReEvaluationVO> reportHealth(@PathVariable Long id,
                                          @Valid @RequestBody HealthReportDTO dto) {
        return R.ok(careArchiveService.reportHealth(id, dto));
    }

    /** 成长记录，支持文字与 base64 图片 */
    @PostMapping("/archives/{id}/notes")
    @PreAuthorize("hasAuthority('" + Perms.CARE_PLAN_MANAGE_OWN + "')")
    public R<CareNoteVO> addNote(@PathVariable Long id, @Valid @RequestBody CareNoteDTO dto) {
        return R.ok(careArchiveService.addNote(id, dto));
    }

    @PostMapping("/tasks/{id}/complete")
    @PreAuthorize("hasAuthority('" + Perms.CARE_PLAN_MANAGE_OWN + "')")
    public R<Void> completeTask(@PathVariable Long id,
                                @RequestBody(required = false) TaskActionDTO dto) {
        careArchiveService.completeTask(id, dto);
        return R.ok();
    }

    @PostMapping("/tasks/{id}/skip")
    @PreAuthorize("hasAuthority('" + Perms.CARE_PLAN_MANAGE_OWN + "')")
    public R<Void> skipTask(@PathVariable Long id) {
        careArchiveService.skipTask(id);
        return R.ok();
    }

    @PutMapping("/tasks/{id}/postpone")
    @PreAuthorize("hasAuthority('" + Perms.CARE_PLAN_MANAGE_OWN + "')")
    public R<Void> postponeTask(@PathVariable Long id,
                                @RequestBody(required = false) TaskActionDTO dto) {
        careArchiveService.postponeTask(id, dto);
        return R.ok();
    }

    /**
     * 手工触发每日维护，与定时任务跑的是同一段代码。
     *
     * <p>挂管理端权限：它会给所有用户发提醒、改所有档案的频率，不是普通用户能碰的。
     * 存在的意义是演示——总不能为了看一眼提醒效果等到第二天早上七点。
     */
    @PostMapping("/maintenance/run")
    @PreAuthorize("hasAuthority('" + Perms.CARE_TEMPLATE_MANAGE + "')")
    public R<String> runMaintenance() {
        return R.ok(maintenanceService.runDailyMaintenance());
    }
}
