package com.zyt.flowerkisstao.operation.application.service;

import com.zyt.flowerkisstao.operation.web.dto.VisitLogDTO;

/** 页面访问埋点。只记录安全白名单字段。 */
public interface VisitLogService {

    void record(VisitLogDTO dto);
}
