package com.zyt.flowerkisstao.care.application.service;

/**
 * 每日养护维护。定时任务与手工触发的接口共用同一份实现——
 * 演示时不必等到第二天早上才能看到效果。
 */
public interface CareMaintenanceService {

    /** 一次跑完四件事，返回一句结果摘要 */
    String runDailyMaintenance();

    /** 补足所有养护中档案未来 60 天的任务，返回新增条数 */
    int topUpTasks();

    /** 标记逾期任务并累计连续遗漏，返回标记条数 */
    int markOverdue();

    /** 给今天到期的任务各发一条提醒，返回发送条数 */
    int pushDueReminders();

    /** 检测场景光照变化并调频，返回受影响的档案数 */
    int detectLightChanges();
}
