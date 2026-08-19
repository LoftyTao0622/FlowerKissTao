package com.zyt.flowerkisstao.operation.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyt.flowerkisstao.operation.application.service.OperationLogService;
import com.zyt.flowerkisstao.operation.web.vo.OperationLogVO;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理操作日志查询。 */
@RestController
@RequestMapping("/api/admin/operation/logs")
@Validated
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.OPERATION_LOG_READ + "')")
    public R<IPage<OperationLogVO>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String module,
                                         @RequestParam(required = false) String action) {
        return R.ok(operationLogService.page(new Page<>(current, size), module, action));
    }
}
