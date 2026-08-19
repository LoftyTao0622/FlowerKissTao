package com.zyt.flowerkisstao.operation.web.controller;

import com.zyt.flowerkisstao.operation.application.service.OperationDashboardService;
import com.zyt.flowerkisstao.operation.web.vo.DashboardVO;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 运营看板。 */
@RestController
@RequestMapping("/api/admin/operation/dashboard")
@Validated
public class OperationDashboardController {

    private final OperationDashboardService dashboardService;

    public OperationDashboardController(OperationDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.OPERATION_DASHBOARD_READ + "')")
    public R<DashboardVO> dashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(dashboardService.dashboard(from, to));
    }
}
