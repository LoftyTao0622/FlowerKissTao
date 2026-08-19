package com.zyt.flowerkisstao.operation.application.service.impl;

import com.zyt.flowerkisstao.operation.application.service.VisitLogService;
import com.zyt.flowerkisstao.operation.domain.entity.VisitLog;
import com.zyt.flowerkisstao.operation.infrastructure.mapper.VisitLogMapper;
import com.zyt.flowerkisstao.operation.web.dto.VisitLogDTO;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Slf4j
public class VisitLogServiceImpl implements VisitLogService {

    /** 只允许这些页面类型，防止把任意用户输入当维度塞进看板 */
    private static final Set<String> PAGE_TYPES = Set.of(
            "home", "catalog", "recommendation", "knowledge", "care", "trade", "admin");

    private final VisitLogMapper visitLogMapper;

    public VisitLogServiceImpl(VisitLogMapper visitLogMapper) {
        this.visitLogMapper = visitLogMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(VisitLogDTO dto) {
        try {
            if (!PAGE_TYPES.contains(dto.getPageType())) {
                return;
            }
            VisitLog logEntry = new VisitLog();
            logEntry.setUserId(CurrentUser.get().map(user -> user.getUserId()).orElse(null));
            // 只保留路径，不存 query——搜索词、redirect 等不进入分析表
            String path = dto.getPath();
            int queryAt = path.indexOf('?');
            logEntry.setPath(queryAt >= 0 ? path.substring(0, queryAt) : path);
            logEntry.setPageType(dto.getPageType());
            logEntry.setReferrer(dto.getReferrer());
            visitLogMapper.insert(logEntry);
        } catch (Exception e) {
            // 埋点失败不影响页面访问
            log.debug("记录访问日志失败：{}", dto.getPath(), e);
        }
    }
}
