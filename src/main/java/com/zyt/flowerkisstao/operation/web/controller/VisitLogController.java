package com.zyt.flowerkisstao.operation.web.controller;

import com.zyt.flowerkisstao.operation.application.service.VisitLogService;
import com.zyt.flowerkisstao.operation.web.dto.VisitLogDTO;
import com.zyt.flowerkisstao.shared.web.R;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 页面访问埋点。游客也要记，所以这个路径需在 SecurityConfig 公开白名单里。
 * 接口不接受 userId，登录人由 SecurityContext 自动识别。
 */
@RestController
@RequestMapping("/api/operation/visits")
@Validated
public class VisitLogController {

    private final VisitLogService visitLogService;

    public VisitLogController(VisitLogService visitLogService) {
        this.visitLogService = visitLogService;
    }

    @PostMapping
    public R<Void> record(@Valid @RequestBody VisitLogDTO dto) {
        visitLogService.record(dto);
        return R.ok();
    }
}
