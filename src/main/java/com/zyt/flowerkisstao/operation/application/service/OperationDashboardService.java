package com.zyt.flowerkisstao.operation.application.service;

import com.zyt.flowerkisstao.operation.web.vo.DashboardVO;

import java.time.LocalDate;

/** 运营数据聚合。 */
public interface OperationDashboardService {

    DashboardVO dashboard(LocalDate from, LocalDate to);
}
