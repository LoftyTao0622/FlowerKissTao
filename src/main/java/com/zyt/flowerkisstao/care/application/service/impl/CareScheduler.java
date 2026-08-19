package com.zyt.flowerkisstao.care.application.service.impl;

import com.zyt.flowerkisstao.care.application.service.CareMaintenanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 养护定时任务。方案架构章节："Spring Task 定时生成养护任务和站内提醒。"
 *
 * <p>这个类刻意只有一层壳，真正的逻辑在 {@link CareMaintenanceService} 里——
 * 定时任务没法在演示时等到第二天早上，把逻辑抽到 Service 之后，
 * 管理端可以随时手工触发同一段代码。
 */
@Component
@Slf4j
public class CareScheduler {

    private final CareMaintenanceService maintenanceService;

    public CareScheduler(CareMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    /**
     * 每天早上 7:37 跑一次。
     *
     * <p>时间挑在早高峰之前、整点之外：养护提醒适合在人起床后看到，
     * 而避开整点是因为一堆系统都爱在整点跑批。
     */
    @Scheduled(cron = "0 37 7 * * *")
    public void dailyMaintenance() {
        try {
            maintenanceService.runDailyMaintenance();
        } catch (Exception e) {
            // 定时任务里抛异常会被 Spring 吞掉且不再重试，必须自己记下来，
            // 否则某天提醒没发出去，日志里连线索都没有
            log.error("每日养护维护执行失败", e);
        }
    }
}
