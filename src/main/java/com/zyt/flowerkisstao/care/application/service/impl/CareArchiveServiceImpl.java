package com.zyt.flowerkisstao.care.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.care.application.service.CareArchiveService;
import com.zyt.flowerkisstao.care.application.service.CareNotificationService;
import com.zyt.flowerkisstao.care.domain.entity.CareArchive;
import com.zyt.flowerkisstao.care.domain.entity.CareNote;
import com.zyt.flowerkisstao.care.domain.entity.CareNotification;
import com.zyt.flowerkisstao.care.domain.entity.CareTask;
import com.zyt.flowerkisstao.care.domain.model.CarePlanInput;
import com.zyt.flowerkisstao.care.domain.model.CareTaskType;
import com.zyt.flowerkisstao.care.domain.model.PlannedTask;
import com.zyt.flowerkisstao.care.domain.service.CareTaskPlanner;
import com.zyt.flowerkisstao.care.domain.service.ReEvaluationRules;
import com.zyt.flowerkisstao.care.infrastructure.mapper.CareArchiveMapper;
import com.zyt.flowerkisstao.care.infrastructure.mapper.CareNoteMapper;
import com.zyt.flowerkisstao.care.infrastructure.mapper.CareTaskMapper;
import com.zyt.flowerkisstao.care.web.dto.CareNoteDTO;
import com.zyt.flowerkisstao.care.web.dto.HealthReportDTO;
import com.zyt.flowerkisstao.care.web.dto.TaskActionDTO;
import com.zyt.flowerkisstao.care.web.vo.CareArchiveVO;
import com.zyt.flowerkisstao.care.web.vo.CareNoteVO;
import com.zyt.flowerkisstao.care.web.vo.CareTaskVO;
import com.zyt.flowerkisstao.care.web.vo.ReEvaluationVO;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSpeciesMapper;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import com.zyt.flowerkisstao.trade.domain.entity.TradeOrder;
import com.zyt.flowerkisstao.trade.domain.entity.TradeOrderItem;
import com.zyt.flowerkisstao.trade.infrastructure.mapper.TradeOrderItemMapper;
import com.zyt.flowerkisstao.trade.infrastructure.mapper.TradeOrderMapper;
import com.zyt.flowerkisstao.user.domain.entity.UserSceneProfile;
import com.zyt.flowerkisstao.user.infrastructure.mapper.UserSceneProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 养护档案的编排层。
 *
 * <p>排期规则一行都不在这里，在 {@link CareTaskPlanner}；调频与排查规则在
 * {@link ReEvaluationRules}。两者都是不依赖 Spring 的纯函数，可脱离容器单测。
 * 这一层只做编排：建档、落任务、记录操作、触发重评估。
 */
@Service
public class CareArchiveServiceImpl implements CareArchiveService {

    /** 建档与每日补任务的时间窗长度 */
    private static final int PLAN_WINDOW_DAYS = 60;

    /** 日历默认展示未来多少天 */
    private static final int DEFAULT_CALENDAR_DAYS = 30;

    /** base64 图片大小上限。MEDIUMTEXT 放得下更多，但几十张高清图会让库很难看 */
    private static final int MAX_IMAGE_BYTES = 300 * 1024;

    private final CareArchiveMapper archiveMapper;
    private final CareTaskMapper taskMapper;
    private final CareNoteMapper noteMapper;
    private final CatalogSpeciesMapper speciesMapper;
    private final UserSceneProfileMapper profileMapper;
    private final TradeOrderMapper orderMapper;
    private final TradeOrderItemMapper orderItemMapper;
    private final CareNotificationService notificationService;

    public CareArchiveServiceImpl(CareArchiveMapper archiveMapper,
                                  CareTaskMapper taskMapper,
                                  CareNoteMapper noteMapper,
                                  CatalogSpeciesMapper speciesMapper,
                                  UserSceneProfileMapper profileMapper,
                                  TradeOrderMapper orderMapper,
                                  TradeOrderItemMapper orderItemMapper,
                                  CareNotificationService notificationService) {
        this.archiveMapper = archiveMapper;
        this.taskMapper = taskMapper;
        this.noteMapper = noteMapper;
        this.speciesMapper = speciesMapper;
        this.profileMapper = profileMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.notificationService = notificationService;
    }

    // ================================================================
    // 建档：确认收货的下游
    // ================================================================

    /**
     * 按订单建档。
     *
     * <p>同一订单里买了三株不同的植物就建三份档案——养护是按株进行的，
     * 合成一份就没法分别记录浇水与长势了。同品种买两盆只建一份：
     * 同一种植物放在同一个场景里，养护节奏完全一样，拆成两份只会让用户重复操作两遍。
     *
     * <p><b>用 REQUIRES_NEW 开独立事务</b>：调用方是"确认收货"，用户已经收到货了，
     * 这是既成事实。建档失败可以事后补，但不能反过来让收货操作回滚——那会把用户
     * 困在"运输中"状态里出不来。挂在同一个事务上的话，这里抛异常会把外层标成
     * rollback-only，调用方就算 catch 住也没用，提交时照样失败。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int createFromOrder(Long userId, Long orderId) {
        TradeOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return 0;
        }

        // 幂等：确认收货接口理论上只会成功一次，但重复建档的后果是用户看到两株
        // 一模一样的植物，代价太难看，这里再挡一道
        Long existing = archiveMapper.selectCount(Wrappers.<CareArchive>lambdaQuery()
                .eq(CareArchive::getOrderId, orderId));
        if (existing != null && existing > 0) {
            return 0;
        }

        List<TradeOrderItem> items = orderItemMapper.selectList(
                Wrappers.<TradeOrderItem>lambdaQuery().eq(TradeOrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            return 0;
        }

        UserSceneProfile scene = defaultScene(userId);
        LocalDateTime adoptedAt = order.getReceivedAt() == null
                ? LocalDateTime.now() : order.getReceivedAt();

        // 同品种去重
        Map<Long, TradeOrderItem> bySpecies = items.stream()
                .collect(Collectors.toMap(TradeOrderItem::getSpeciesId, Function.identity(),
                        (a, b) -> a));

        int created = 0;
        for (TradeOrderItem item : bySpecies.values()) {
            CatalogSpecies species = speciesMapper.selectById(item.getSpeciesId());
            if (species == null) {
                // 品种被删了。跳过而不是抛异常——不能因为养护建档失败就让用户收不了货
                continue;
            }

            CareArchive archive = new CareArchive();
            archive.setUserId(userId);
            archive.setSpeciesId(item.getSpeciesId());
            // 名字与图片存快照：商品会改名下架，但"我买过那株琴叶榕"是既成事实
            archive.setPlantName(item.getSpeciesName());
            archive.setPlantImage(item.getImage());
            archive.setOrderId(orderId);
            archive.setOrderNo(order.getOrderNo());
            archive.setSceneId(scene == null ? null : scene.getId());
            // 建档时的光照是"光照变化检测"的基准线
            archive.setLightLevelSnapshot(scene == null ? null : scene.getLightLevel());
            archive.setAdoptedAt(adoptedAt);
            archive.setMissedCount(0);
            archive.setWaterFactor(CareArchive.FACTOR_BASE);
            archive.setStatus(CareArchive.STATUS_ACTIVE);
            try {
                archiveMapper.insert(archive);
            } catch (DuplicateKeyException e) {
                // 并发确认收货时另一事务已经为该订单和品种建档，按幂等成功处理。
                continue;
            }

            LocalDate anchor = adoptedAt.toLocalDate();
            generateTasks(archive, species, scene, anchor, anchor, anchor.plusDays(PLAN_WINDOW_DAYS));
            created++;

            notificationService.push(userId, CareNotification.TYPE_TASK_DUE,
                    "养护档案已建立",
                    archive.getPlantName() + " 的养护计划已生成，未来 "
                            + PLAN_WINDOW_DAYS + " 天的任务都排好了。",
                    archive.getId(), null);
        }
        return created;
    }

    /**
     * 按窗口生成任务并落库。
     *
     * <p>会跳过窗口内已存在的同类型同日期任务——每日补任务时窗口会与上一次重叠，
     * 不去重就会出现两条一模一样的"检查盆土并浇水"。
     */
    int generateTasks(CareArchive archive, CatalogSpecies species, UserSceneProfile scene,
                      LocalDate anchor, LocalDate from, LocalDate to) {
        CarePlanInput input = CarePlanInput.of(species, scene,
                archive.getWaterFactor() == null ? CareArchive.FACTOR_BASE : archive.getWaterFactor());
        List<PlannedTask> planned = CareTaskPlanner.plan(input, anchor, from, to);
        if (planned.isEmpty()) {
            return 0;
        }

        List<CareTask> existing = taskMapper.selectList(Wrappers.<CareTask>lambdaQuery()
                .eq(CareTask::getArchiveId, archive.getId())
                .ge(CareTask::getDueDate, from)
                .lt(CareTask::getDueDate, to));
        java.util.Set<String> seen = existing.stream()
                .map(task -> task.getTaskType() + "@" + task.getDueDate())
                .collect(Collectors.toSet());

        int inserted = 0;
        for (PlannedTask plan : planned) {
            String key = plan.type().code() + "@" + plan.dueDate();
            if (!seen.add(key)) {
                continue;
            }
            CareTask task = new CareTask();
            task.setArchiveId(archive.getId());
            task.setTaskType(plan.type().code());
            task.setTitle(plan.title());
            task.setInstruction(plan.instruction());
            task.setDueDate(plan.dueDate());
            task.setStatus(CareTask.STATUS_PENDING);
            if (taskMapper.insertIgnore(task) > 0) {
                inserted++;
            }
        }
        return inserted;
    }

    // ================================================================
    // 查询
    // ================================================================

    @Override
    public List<CareArchiveVO> listMine() {
        Long userId = CurrentUser.requireUserId();
        List<CareArchive> archives = archiveMapper.selectList(Wrappers.<CareArchive>lambdaQuery()
                .eq(CareArchive::getUserId, userId)
                .eq(CareArchive::getStatus, CareArchive.STATUS_ACTIVE)
                .orderByDesc(CareArchive::getId));
        if (archives.isEmpty()) {
            return List.of();
        }

        // 一次把所有档案的未完成任务捞出来，避免逐个档案查的 N+1
        List<Long> archiveIds = archives.stream().map(CareArchive::getId).toList();
        Map<Long, List<CareTask>> openByArchive = taskMapper.selectList(
                        Wrappers.<CareTask>lambdaQuery()
                                .in(CareTask::getArchiveId, archiveIds)
                                .in(CareTask::getStatus,
                                        CareTask.STATUS_PENDING, CareTask.STATUS_OVERDUE)
                                .orderByAsc(CareTask::getDueDate)).stream()
                .collect(Collectors.groupingBy(CareTask::getArchiveId));

        LocalDate today = LocalDate.now();
        return archives.stream()
                .map(archive -> toSummaryVO(archive,
                        openByArchive.getOrDefault(archive.getId(), List.of()), today))
                .toList();
    }

    @Override
    public CareArchiveVO get(Long archiveId) {
        CareArchive archive = requireOwnedArchive(archiveId);
        LocalDate today = LocalDate.now();

        List<CareTask> open = taskMapper.selectList(Wrappers.<CareTask>lambdaQuery()
                .eq(CareTask::getArchiveId, archiveId)
                .in(CareTask::getStatus, CareTask.STATUS_PENDING, CareTask.STATUS_OVERDUE)
                .orderByAsc(CareTask::getDueDate));

        CareArchiveVO vo = toSummaryVO(archive, open, today);

        // 详情带未来 30 天的全部任务（含已完成的，日历上要能看到做过什么）
        vo.setTasks(taskMapper.selectList(Wrappers.<CareTask>lambdaQuery()
                        .eq(CareTask::getArchiveId, archiveId)
                        .ge(CareTask::getDueDate, today.minusDays(7))
                        .lt(CareTask::getDueDate, today.plusDays(DEFAULT_CALENDAR_DAYS))
                        .orderByAsc(CareTask::getDueDate)).stream()
                .map(task -> CareConverter.toVO(task, today))
                .toList());

        // 成长记录不带 base64 图片内容，只给 hasImage 标记
        vo.setNotes(noteMapper.selectList(Wrappers.<CareNote>lambdaQuery()
                        .eq(CareNote::getArchiveId, archiveId)
                        .orderByDesc(CareNote::getId)).stream()
                .map(note -> CareConverter.toVO(note, false))
                .toList());

        return vo;
    }

    @Override
    public List<CareTaskVO> listTasks(Long archiveId, LocalDate from, LocalDate to) {
        requireOwnedArchive(archiveId);
        LocalDate today = LocalDate.now();
        LocalDate start = from == null ? today : from;
        LocalDate end = to == null ? start.plusDays(DEFAULT_CALENDAR_DAYS) : to;

        return taskMapper.selectList(Wrappers.<CareTask>lambdaQuery()
                        .eq(CareTask::getArchiveId, archiveId)
                        .ge(CareTask::getDueDate, start)
                        .le(CareTask::getDueDate, end)
                        .orderByAsc(CareTask::getDueDate)).stream()
                .map(task -> CareConverter.toVO(task, today))
                .toList();
    }

    // ================================================================
    // 任务操作
    // ================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, TaskActionDTO dto) {
        CareTask task = requireOpenTask(taskId);
        CareArchive archive = requireOwnedArchive(task.getArchiveId());

        CareTask update = new CareTask();
        update.setId(taskId);
        update.setStatus(CareTask.STATUS_DONE);
        update.setCompletedAt(LocalDateTime.now());
        update.setNote(dto == null ? null : dto.getNote());
        taskMapper.updateById(update);

        // 完成任意一个任务就把连续遗漏清零——"连续"的语义在这里
        if (archive.getMissedCount() != null && archive.getMissedCount() > 0) {
            CareArchive reset = new CareArchive();
            reset.setId(archive.getId());
            reset.setMissedCount(0);
            archiveMapper.updateById(reset);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void skipTask(Long taskId) {
        CareTask task = requireOpenTask(taskId);
        CareArchive archive = requireOwnedArchive(task.getArchiveId());

        CareTask update = new CareTask();
        update.setId(taskId);
        update.setStatus(CareTask.STATUS_SKIPPED);
        taskMapper.updateById(update);

        // 主动跳过与被动逾期一样计入连续遗漏：两者都说明当前频率跟不上
        bumpMissed(archive);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postponeTask(Long taskId, TaskActionDTO dto) {
        CareTask task = requireOpenTask(taskId);
        requireOwnedArchive(task.getArchiveId());

        int days = dto == null || dto.getDays() == null ? 1 : dto.getDays();
        CareTask update = new CareTask();
        update.setId(taskId);
        update.setDueDate(task.getDueDate().plusDays(days));
        // 延后的任务重新回到待办：逾期状态下延后应当恢复正常，否则它永远标红
        update.setStatus(CareTask.STATUS_PENDING);
        taskMapper.updateById(update);
    }

    // ================================================================
    // 成长记录
    // ================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CareNoteVO addNote(Long archiveId, CareNoteDTO dto) {
        requireOwnedArchive(archiveId);

        String image = normalizeImage(dto.getImage());
        if ((dto.getContent() == null || dto.getContent().isBlank()) && image == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "写点什么或选一张图片吧");
        }

        CareNote note = new CareNote();
        note.setArchiveId(archiveId);
        note.setNoteType(image == null ? CareNote.TYPE_TEXT : CareNote.TYPE_PHOTO);
        note.setContent(dto.getContent());
        note.setImage(image);
        noteMapper.insert(note);

        return CareConverter.toVO(note, true);
    }

    /** 校验 base64 图片大小。超限直接拒绝，而不是悄悄截断存半张坏图 */
    private String normalizeImage(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        // 前端可能带 data:image/png;base64, 前缀，按原样存即可，只按实际字节算大小
        int commaAt = raw.indexOf(',');
        String payload = commaAt > 0 && raw.startsWith("data:") ? raw.substring(commaAt + 1) : raw;
        // base64 每 4 个字符表示 3 字节
        long bytes = (long) (payload.length() * 3L / 4);
        if (bytes > MAX_IMAGE_BYTES) {
            throw new BizException(ErrorCode.CARE_IMAGE_TOO_LARGE,
                    "图片不能超过 300KB，当前约 " + (bytes / 1024) + "KB，请压缩后再上传");
        }
        return raw;
    }

    // ================================================================
    // 重新评估
    // ================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReEvaluationVO reportHealth(Long archiveId, HealthReportDTO dto) {
        CareArchive archive = requireOwnedArchive(archiveId);
        UserSceneProfile scene = sceneOf(archive);
        LocalDate today = LocalDate.now();

        // 排查清单要结合实际养护记录，而不是给一份通用说明
        boolean recentSkipped = taskMapper.selectCount(Wrappers.<CareTask>lambdaQuery()
                .eq(CareTask::getArchiveId, archiveId)
                .eq(CareTask::getTaskType, CareTaskType.WATER.code())
                .in(CareTask::getStatus, CareTask.STATUS_SKIPPED, CareTask.STATUS_OVERDUE)
                .ge(CareTask::getDueDate, today.minusDays(30))) > 0;
        boolean dimLight = scene != null && scene.getLightLevel() != null && scene.getLightLevel() <= 2;
        boolean recentRepot = taskMapper.selectCount(Wrappers.<CareTask>lambdaQuery()
                .eq(CareTask::getArchiveId, archiveId)
                .eq(CareTask::getTaskType, CareTaskType.REPOT.code())
                .eq(CareTask::getStatus, CareTask.STATUS_DONE)
                .ge(CareTask::getDueDate, today.minusDays(30))) > 0;

        List<String> checklist = ReEvaluationRules.checklistFor(
                dto.getSymptom(), recentSkipped, dimLight, recentRepot);

        // 排查结论落成一条成长记录，方案要求"保留既有养护历史"
        CareNote note = new CareNote();
        note.setArchiveId(archiveId);
        note.setNoteType(CareNote.TYPE_HEALTH);
        note.setContent(symptomLabel(dto.getSymptom())
                + (dto.getDetail() == null || dto.getDetail().isBlank() ? "" : "：" + dto.getDetail()));
        noteMapper.insert(note);

        notificationService.push(archive.getUserId(), CareNotification.TYPE_HEALTH,
                archive.getPlantName() + " 的排查建议已生成",
                "针对「" + symptomLabel(dto.getSymptom()) + "」给出了 " + checklist.size() + " 条排查建议。",
                archiveId, null);

        return ReEvaluationVO.builder()
                .trigger(ReEvaluationRules.Trigger.HEALTH.code())
                .triggerLabel(ReEvaluationRules.Trigger.HEALTH.label())
                .adjusted(false)
                .checklist(checklist)
                .summary("已根据你的反馈与最近的养护记录生成排查建议。")
                .rescheduledCount(0)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReEvaluationVO reEvaluate(Long archiveId) {
        CareArchive archive = requireOwnedArchive(archiveId);
        return doReEvaluate(archive, ReEvaluationRules.Trigger.MANUAL);
    }

    /**
     * 重新评估的共用实现。三条触发源（连续遗漏、光照变化、手动）都走这里，
     * 分开实现的话调频规则必然会分叉。
     */
    ReEvaluationVO doReEvaluate(CareArchive archive, ReEvaluationRules.Trigger trigger) {
        CatalogSpecies species = speciesMapper.selectById(archive.getSpeciesId());
        if (species == null) {
            throw new BizException(ErrorCode.CARE_ARCHIVE_NOT_FOUND, "这株植物的品种资料已不存在");
        }
        UserSceneProfile scene = sceneOf(archive);
        LocalDate today = LocalDate.now();

        int oldFactor = archive.getWaterFactor() == null
                ? CareArchive.FACTOR_BASE : archive.getWaterFactor();
        int oldInterval = CareTaskPlanner.currentWaterInterval(
                CarePlanInput.of(species, scene, oldFactor), today);

        int newFactor = oldFactor;
        List<String> checklist = new ArrayList<>();
        String summary;

        Integer currentLight = scene == null ? null : scene.getLightLevel();
        boolean lightChanged = archive.getLightLevelSnapshot() != null
                && currentLight != null
                && !currentLight.equals(archive.getLightLevelSnapshot());

        if (trigger == ReEvaluationRules.Trigger.MISSED) {
            newFactor = ReEvaluationRules.relaxForMissed(oldFactor);
            checklist.add("已放宽浇水频率，让计划贴近你的实际节奏。");
            checklist.add("如果是因为出差，可在场景问卷里把出差频率调高，系统会一并考虑。");
            summary = "检测到连续遗漏任务，已自动放宽养护频率。";
        } else if (lightChanged) {
            newFactor = ReEvaluationRules.adjustForLight(oldFactor,
                    archive.getLightLevelSnapshot(), currentLight);
            checklist.add("光照变化会直接影响盆土干燥速度，浇水频率已同步调整。");
            if (currentLight < archive.getLightLevelSnapshot()) {
                checklist.add("光线变弱后蒸腾减慢，务必等盆土干到位再浇，否则容易烂根。");
            } else {
                checklist.add("光线变强后失水加快，注意观察叶片有无萎蔫或焦边。");
            }
            summary = "光照由「" + lightLabel(archive.getLightLevelSnapshot()) + "」变为「"
                    + lightLabel(currentLight) + "」，浇水频率已重新评估。";
        } else {
            checklist.add("当前环境与建档时一致，养护频率保持不变。");
            checklist.add("如果调整过摆放位置，记得先更新场景问卷再重新评估。");
            summary = "环境没有明显变化，维持当前养护节奏。";
        }

        int rescheduled = 0;
        if (newFactor != oldFactor) {
            CareArchive update = new CareArchive();
            update.setId(archive.getId());
            update.setWaterFactor(newFactor);
            // 光照变化后要把基准线也更新，否则每天都会重复判定"变了"
            if (lightChanged) {
                update.setLightLevelSnapshot(currentLight);
            }
            if (trigger == ReEvaluationRules.Trigger.MISSED) {
                update.setMissedCount(0);
            }
            archiveMapper.updateById(update);
            archive.setWaterFactor(newFactor);

            rescheduled = rescheduleFuture(archive, species, scene, today);
        } else if (lightChanged) {
            CareArchive update = new CareArchive();
            update.setId(archive.getId());
            update.setLightLevelSnapshot(currentLight);
            archiveMapper.updateById(update);
        }

        int newInterval = CareTaskPlanner.currentWaterInterval(
                CarePlanInput.of(species, scene, newFactor), today);

        if (newFactor != oldFactor) {
            notificationService.push(archive.getUserId(), CareNotification.TYPE_RE_EVAL,
                    archive.getPlantName() + " 的养护频率已调整",
                    "浇水间隔从 " + oldInterval + " 天调整为 " + newInterval + " 天。" + summary,
                    archive.getId(), null);
        }

        return ReEvaluationVO.builder()
                .trigger(trigger.code())
                .triggerLabel(trigger.label())
                .adjusted(newFactor != oldFactor)
                .oldInterval(oldInterval)
                .newInterval(newInterval)
                .summary(summary)
                .checklist(checklist)
                .rescheduledCount(rescheduled)
                .build();
    }

    /**
     * 按新频率重排未来任务。
     *
     * <p>只删未完成的将来任务，**已完成与已跳过的一律保留**——方案原文明确要求
     * "同时保留既有养护历史"。把历史一并删掉重排，用户的养护记录就消失了。
     */
    private int rescheduleFuture(CareArchive archive, CatalogSpecies species,
                                 UserSceneProfile scene, LocalDate today) {
        taskMapper.delete(Wrappers.<CareTask>lambdaQuery()
                .eq(CareTask::getArchiveId, archive.getId())
                .gt(CareTask::getDueDate, today)
                .in(CareTask::getStatus, CareTask.STATUS_PENDING, CareTask.STATUS_OVERDUE));

        // 从明天起按新系数重排。锚点仍用入手日期，保证周期节奏连续
        return generateTasks(archive, species, scene,
                archive.getAdoptedAt().toLocalDate(),
                today.plusDays(1), today.plusDays(PLAN_WINDOW_DAYS));
    }

    /** 连续遗漏加一，达到阈值就调频。定时任务标逾期时也会调它 */
    void bumpMissed(CareArchive archive) {
        int missed = (archive.getMissedCount() == null ? 0 : archive.getMissedCount()) + 1;
        CareArchive update = new CareArchive();
        update.setId(archive.getId());
        update.setMissedCount(missed);
        archiveMapper.updateById(update);
        archive.setMissedCount(missed);

        if (missed >= CareArchive.MISSED_THRESHOLD) {
            doReEvaluate(archive, ReEvaluationRules.Trigger.MISSED);
        }
    }

    // ================================================================
    // 工具
    // ================================================================

    private CareArchiveVO toSummaryVO(CareArchive archive, List<CareTask> openTasks, LocalDate today) {
        CatalogSpecies species = speciesMapper.selectById(archive.getSpeciesId());
        UserSceneProfile scene = sceneOf(archive);

        int overdue = (int) openTasks.stream()
                .filter(task -> task.getDueDate().isBefore(today)).count();
        CareTask next = openTasks.stream()
                .min(Comparator.comparing(CareTask::getDueDate)).orElse(null);

        Integer interval = species == null ? null : CareTaskPlanner.currentWaterInterval(
                CarePlanInput.of(species, scene, archive.getWaterFactor() == null
                        ? CareArchive.FACTOR_BASE : archive.getWaterFactor()), today);

        return CareArchiveVO.builder()
                .id(archive.getId())
                .speciesId(archive.getSpeciesId())
                .slug(species == null ? null : species.getCode())
                .plantName(archive.getPlantName())
                .plantImage(archive.getPlantImage())
                .orderNo(archive.getOrderNo())
                .adoptedAt(archive.getAdoptedAt())
                .adoptedDays(ChronoUnit.DAYS.between(archive.getAdoptedAt().toLocalDate(), today))
                .status(archive.getStatus())
                .pendingCount(openTasks.size())
                .overdueCount(overdue)
                .nextTask(next == null ? null : CareConverter.toVO(next, today))
                .waterIntervalDays(interval)
                .waterFactor(archive.getWaterFactor())
                .adjustmentNote(CareConverter.adjustmentNote(archive))
                .missedCount(archive.getMissedCount())
                .lightLevelSnapshot(archive.getLightLevelSnapshot())
                .currentLightLevel(scene == null ? null : scene.getLightLevel())
                .build();
    }

    /**
     * 取档案并确认归属。
     *
     * <p>不属于当前用户时抛 CARE_ARCHIVE_NOT_FOUND 而非 FORBIDDEN：返回 403 等于确认
     * "这个档案确实存在"。与订单、画像、推荐同一口径。
     */
    private CareArchive requireOwnedArchive(Long archiveId) {
        CareArchive archive = archiveMapper.selectById(archiveId);
        if (archive == null || !archive.getUserId().equals(CurrentUser.requireUserId())) {
            throw new BizException(ErrorCode.CARE_ARCHIVE_NOT_FOUND, "这株植物的养护档案不存在");
        }
        if (archive.getStatus() != null && archive.getStatus() == CareArchive.STATUS_CLOSED) {
            throw new BizException(ErrorCode.CARE_ARCHIVE_CLOSED, "这份档案已归档");
        }
        return archive;
    }

    /** 取任务并确认它还能操作。已完成或已跳过的不能重复操作 */
    private CareTask requireOpenTask(Long taskId) {
        CareTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.CARE_TASK_NOT_FOUND, "任务不存在");
        }
        if (!task.isOpen()) {
            throw new BizException(ErrorCode.CARE_TASK_CLOSED,
                    "这条任务已" + CareConverter.statusLabel(task.getStatus()) + "，不能重复操作");
        }
        return task;
    }

    /** 档案绑定的场景；场景被删了就退回用户当前默认场景 */
    private UserSceneProfile sceneOf(CareArchive archive) {
        if (archive.getSceneId() != null) {
            UserSceneProfile scene = profileMapper.selectById(archive.getSceneId());
            if (scene != null) {
                return scene;
            }
        }
        return defaultScene(archive.getUserId());
    }

    private UserSceneProfile defaultScene(Long userId) {
        UserSceneProfile scene = profileMapper.selectOne(Wrappers.<UserSceneProfile>lambdaQuery()
                .eq(UserSceneProfile::getUserId, userId)
                .eq(UserSceneProfile::getIsDefault, 1)
                .last("LIMIT 1"));
        if (scene != null) {
            return scene;
        }
        return profileMapper.selectOne(Wrappers.<UserSceneProfile>lambdaQuery()
                .eq(UserSceneProfile::getUserId, userId)
                .orderByAsc(UserSceneProfile::getId)
                .last("LIMIT 1"));
    }

    private static String lightLabel(Integer level) {
        if (level == null) {
            return "未知";
        }
        return switch (level) {
            case 1 -> "低光";
            case 2 -> "柔和散射";
            case 3 -> "明亮散射";
            case 4 -> "充足直射";
            default -> "未知";
        };
    }

    private static String symptomLabel(String symptom) {
        return switch (symptom == null ? "" : symptom) {
            case "yellowing" -> "叶片发黄";
            case "wilting" -> "植株萎蔫";
            case "spots" -> "叶片出现斑点";
            case "dropping" -> "掉叶";
            case "pest" -> "疑似虫害";
            default -> "健康反馈";
        };
    }
}
