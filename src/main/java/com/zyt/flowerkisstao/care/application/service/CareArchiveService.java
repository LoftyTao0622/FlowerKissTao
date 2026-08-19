package com.zyt.flowerkisstao.care.application.service;

import com.zyt.flowerkisstao.care.web.dto.CareNoteDTO;
import com.zyt.flowerkisstao.care.web.dto.HealthReportDTO;
import com.zyt.flowerkisstao.care.web.dto.TaskActionDTO;
import com.zyt.flowerkisstao.care.web.vo.CareArchiveVO;
import com.zyt.flowerkisstao.care.web.vo.CareNoteVO;
import com.zyt.flowerkisstao.care.web.vo.CareTaskVO;
import com.zyt.flowerkisstao.care.web.vo.ReEvaluationVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 养护档案。方案模块 5 的主体。
 *
 * <p>排期规则不在这里，在 {@code CareTaskPlanner}——那是个不依赖 Spring 的纯函数。
 * 这一层只做编排：建档、落任务、记录操作、触发重评估。
 */
public interface CareArchiveService {

    /**
     * 确认收货后按订单建档。由 trade 模块在 {@code OrderServiceImpl.receive()} 调用。
     *
     * <p>方案原文："确认收货事件会把植物品种、购买时间和用户场景传递给养护模块，
     * 自动建立'我的植物'。"
     *
     * @return 新建的档案数。同一订单重复调用不会重复建档
     */
    int createFromOrder(Long userId, Long orderId);

    /** 我的植物列表 */
    List<CareArchiveVO> listMine();

    /** 档案详情，含任务与成长记录 */
    CareArchiveVO get(Long archiveId);

    /** 任务列表，日历视图的数据源。from/to 为空时取未来 30 天 */
    List<CareTaskVO> listTasks(Long archiveId, LocalDate from, LocalDate to);

    void completeTask(Long taskId, TaskActionDTO dto);

    void skipTask(Long taskId);

    void postponeTask(Long taskId, TaskActionDTO dto);

    /** 成长记录，支持文字与 base64 图片 */
    CareNoteVO addNote(Long archiveId, CareNoteDTO dto);

    /** 报告叶片发黄等症状，返回逐项排查建议 */
    ReEvaluationVO reportHealth(Long archiveId, HealthReportDTO dto);

    /** 手动重新评估。演示友好：改完场景光照点一下就能看到频率变化 */
    ReEvaluationVO reEvaluate(Long archiveId);
}
