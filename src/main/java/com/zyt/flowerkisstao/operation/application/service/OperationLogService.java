package com.zyt.flowerkisstao.operation.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zyt.flowerkisstao.operation.web.vo.OperationLogVO;

import java.util.Map;

/** 管理操作审计。只接受调用方显式挑选过的非敏感字段。 */
public interface OperationLogService {

    /**
     * 记录一条操作。内部独立事务且失败不抛回主业务。
     *
     * @param before 修改前的白名单字段，可空
     * @param after  修改后的白名单字段，可空
     */
    void record(String module, String action, String targetType, Object targetId,
                Map<String, Object> before, Map<String, Object> after);

    IPage<OperationLogVO> page(IPage<?> page, String module, String action);
}
