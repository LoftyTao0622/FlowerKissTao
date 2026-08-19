package com.zyt.flowerkisstao.care.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.care.application.service.CareMaintenanceService;
import com.zyt.flowerkisstao.care.application.service.CareNotificationService;
import com.zyt.flowerkisstao.care.domain.entity.CareArchive;
import com.zyt.flowerkisstao.care.domain.entity.CareNotification;
import com.zyt.flowerkisstao.care.domain.entity.CareTask;
import com.zyt.flowerkisstao.care.domain.model.CareTaskType;
import com.zyt.flowerkisstao.care.domain.service.ReEvaluationRules;
import com.zyt.flowerkisstao.care.infrastructure.mapper.CareArchiveMapper;
import com.zyt.flowerkisstao.care.infrastructure.mapper.CareTaskMapper;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSpeciesMapper;
import com.zyt.flowerkisstao.user.domain.entity.UserSceneProfile;
import com.zyt.flowerkisstao.user.infrastructure.mapper.UserSceneProfileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日养护维护。
 *
 * <p>方案架构章节："Spring Task 定时生成养护任务和站内提醒。"这个类是那句话的落地，
 * 由 {@link CareScheduler} 每天调一次。
 *
 * <p>做成 Service 而不是把逻辑写在 {@code @Scheduled} 方法里，是为了能手工触发——
 * 答辩演示时不可能等到第二天早上七点才看到提醒。
 */
@Service
@Slf4j
public class CareMaintenanceServiceImpl implements CareMaintenanceService {

    /** 补任务的时间窗，与建档时一致 */
    private static final int PLAN_WINDOW_DAYS = 60;

    private final CareArchiveMapper archiveMapper;
    private final CareTaskMapper taskMapper;
    private final CatalogSpeciesMapper speciesMapper;
    private final UserSceneProfileMapper profileMapper;
    private final CareNotificationService notificationService;
    private final CareArchiveServiceImpl archiveService;

    public CareMaintenanceServiceImpl(CareArchiveMapper archiveMapper,
                                      CareTaskMapper taskMapper,
                                      CatalogSpeciesMapper speciesMapper,
                                      UserSceneProfileMapper profileMapper,
                                      CareNotificationService notificationService,
                                      CareArchiveServiceImpl archiveService) {
        this.archiveMapper = archiveMapper;
        this.taskMapper = taskMapper;
        this.speciesMapper = speciesMapper;
        this.profileMapper = profileMapper;
        this.notificationService = notificationService;
        this.archiveService = archiveService;
    }

    @Override
    public String runDailyMaintenance() {
        // 顺序有讲究：先标逾期再发提醒，否则今天刚过期的任务不会出现在提醒里；
        // 先补任务再发提醒，否则窗口边缘新生成的任务会漏掉当天的提醒
        int topped = topUpTasks();
        int overdue = markOverdue();
        int reminded = pushDueReminders();
        int relit = detectLightChanges();

        String summary = String.format("补任务 %d 条，标记逾期 %d 条，发送提醒 %d 条，光照调频 %d 株",
                topped, overdue, reminded, relit);
        log.info("每日养护维护完成：{}", summary);
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int topUpTasks() {
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(PLAN_WINDOW_DAYS);
        int total = 0;

        for (CareArchive archive : activeArchives()) {
            CatalogSpecies species = speciesMapper.selectById(archive.getSpeciesId());
            if (species == null) {
                continue;
            }
            UserSceneProfile scene = sceneOf(archive);
            // 从今天补到 60 天后。generateTasks 会跳过窗口内已存在的同类型同日期任务，
            // 所以每天调都不会产生重复
            total += archiveService.generateTasks(archive, species, scene,
                    archive.getAdoptedAt().toLocalDate(), today, horizon);
        }
        return total;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markOverdue() {
        LocalDate today = LocalDate.now();

        List<CareTask> overdue = taskMapper.selectList(Wrappers.<CareTask>lambdaQuery()
                .lt(CareTask::getDueDate, today)
                .eq(CareTask::getStatus, CareTask.STATUS_PENDING));
        if (overdue.isEmpty()) {
            return 0;
        }

        for (CareTask task : overdue) {
            CareTask update = new CareTask();
            update.setId(task.getId());
            update.setStatus(CareTask.STATUS_OVERDUE);
            taskMapper.updateById(update);

            CareArchive archive = archiveMapper.selectById(task.getArchiveId());
            if (archive == null || archive.getStatus() == null
                    || archive.getStatus() != CareArchive.STATUS_ACTIVE) {
                continue;
            }
            // 逾期计入连续遗漏，达到阈值时 bumpMissed 内部会自动调频
            archiveService.bumpMissed(archive);

            notificationService.push(archive.getUserId(), CareNotification.TYPE_OVERDUE,
                    archive.getPlantName() + " 有一项养护任务已逾期",
                    task.getTitle() + "（原定 " + task.getDueDate() + "）还没完成，"
                            + "现在补做仍然来得及。",
                    archive.getId(), task.getId());
        }
        return overdue.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int pushDueReminders() {
        LocalDate today = LocalDate.now();

        List<CareTask> due = taskMapper.selectList(Wrappers.<CareTask>lambdaQuery()
                .eq(CareTask::getDueDate, today)
                .eq(CareTask::getStatus, CareTask.STATUS_PENDING));
        int sent = 0;

        for (CareTask task : due) {
            CareArchive archive = archiveMapper.selectById(task.getArchiveId());
            if (archive == null || archive.getStatus() == null
                    || archive.getStatus() != CareArchive.STATUS_ACTIVE) {
                continue;
            }
            CareTaskType type = CareTaskType.of(task.getTaskType());
            notificationService.push(archive.getUserId(), CareNotification.TYPE_TASK_DUE,
                    type.icon() + " " + archive.getPlantName() + "：" + task.getTitle(),
                    task.getInstruction(),
                    archive.getId(), task.getId());
            sent++;
        }
        return sent;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int detectLightChanges() {
        int affected = 0;

        for (CareArchive archive : activeArchives()) {
            if (archive.getLightLevelSnapshot() == null) {
                continue;
            }
            UserSceneProfile scene = sceneOf(archive);
            if (scene == null || scene.getLightLevel() == null) {
                continue;
            }
            if (scene.getLightLevel().equals(archive.getLightLevelSnapshot())) {
                continue;
            }
            // 光照变了：doReEvaluate 内部会调频、重排未来任务、更新基准线并发提醒。
            // 更新基准线这一步不能少，否则每天都会重复判定"变了"
            archiveService.doReEvaluate(archive, ReEvaluationRules.Trigger.LIGHT);
            affected++;
        }
        return affected;
    }

    // ================================================================

    private List<CareArchive> activeArchives() {
        return archiveMapper.selectList(Wrappers.<CareArchive>lambdaQuery()
                .eq(CareArchive::getStatus, CareArchive.STATUS_ACTIVE));
    }

    /** 档案绑定的场景；场景被删了就退回用户当前默认场景 */
    private UserSceneProfile sceneOf(CareArchive archive) {
        if (archive.getSceneId() != null) {
            UserSceneProfile scene = profileMapper.selectById(archive.getSceneId());
            if (scene != null) {
                return scene;
            }
        }
        UserSceneProfile fallback = profileMapper.selectOne(
                Wrappers.<UserSceneProfile>lambdaQuery()
                        .eq(UserSceneProfile::getUserId, archive.getUserId())
                        .eq(UserSceneProfile::getIsDefault, 1)
                        .last("LIMIT 1"));
        if (fallback != null) {
            return fallback;
        }
        return profileMapper.selectOne(Wrappers.<UserSceneProfile>lambdaQuery()
                .eq(UserSceneProfile::getUserId, archive.getUserId())
                .orderByAsc(UserSceneProfile::getId)
                .last("LIMIT 1"));
    }
}
