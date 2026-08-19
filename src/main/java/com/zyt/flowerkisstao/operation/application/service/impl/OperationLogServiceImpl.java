package com.zyt.flowerkisstao.operation.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.operation.application.service.OperationLogService;
import com.zyt.flowerkisstao.operation.domain.entity.SysOperationLog;
import com.zyt.flowerkisstao.operation.infrastructure.mapper.SysOperationLogMapper;
import com.zyt.flowerkisstao.operation.web.vo.OperationLogVO;
import com.zyt.flowerkisstao.shared.security.AppUserDetails;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Slf4j
public class OperationLogServiceImpl implements OperationLogService {

    private static final int MAX_USER_AGENT = 255;

    private final SysOperationLogMapper logMapper;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public OperationLogServiceImpl(SysOperationLogMapper logMapper,
                                   ObjectProvider<HttpServletRequest> requestProvider) {
        this.logMapper = logMapper;
        this.requestProvider = requestProvider;
    }

    /**
     * 独立事务记录，失败只写应用日志，绝不回滚主业务。
     *
     * <p>如果权重已经改成功，只因审计表临时不可用就告诉管理员“修改失败”，
     * 管理员会再点一次，反而造成更多混乱。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String module, String action, String targetType, Object targetId,
                       Map<String, Object> before, Map<String, Object> after) {
        try {
            AppUserDetails user = CurrentUser.get().orElse(null);
            HttpServletRequest request = requestProvider.getIfAvailable();

            SysOperationLog entry = new SysOperationLog();
            entry.setUserId(user == null ? null : user.getUserId());
            entry.setUsernameSnapshot(user == null ? null : user.getUsername());
            entry.setModule(module);
            entry.setAction(action);
            entry.setTargetType(targetType);
            entry.setTargetId(targetId == null ? null : String.valueOf(targetId));
            entry.setBeforeJson(before);
            entry.setAfterJson(after);
            entry.setIp(request == null ? null : clientIp(request));
            entry.setUserAgent(request == null ? null : truncate(request.getHeader("User-Agent")));
            logMapper.insert(entry);
        } catch (Exception e) {
            log.error("记录管理操作日志失败：module={}, action={}, target={}",
                    module, action, targetId, e);
        }
    }

    @Override
    public IPage<OperationLogVO> page(IPage<?> page, String module, String action) {
        @SuppressWarnings("unchecked")
        IPage<SysOperationLog> result = logMapper.selectPage((IPage<SysOperationLog>) page,
                Wrappers.<SysOperationLog>lambdaQuery()
                        .eq(hasText(module), SysOperationLog::getModule, module)
                        .eq(hasText(action), SysOperationLog::getAction, action)
                        .orderByDesc(SysOperationLog::getId));
        return result.convert(entry -> OperationLogVO.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .username(entry.getUsernameSnapshot())
                .module(entry.getModule())
                .action(entry.getAction())
                .targetType(entry.getTargetType())
                .targetId(entry.getTargetId())
                .before(entry.getBeforeJson())
                .after(entry.getAfterJson())
                .ip(entry.getIp())
                .userAgent(entry.getUserAgent())
                .createdAt(entry.getCreatedAt())
                .build());
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_USER_AGENT) {
            return value;
        }
        return value.substring(0, MAX_USER_AGENT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
