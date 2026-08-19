package com.zyt.flowerkisstao.care.domain.service;

import com.zyt.flowerkisstao.care.domain.model.CarePlanInput;
import com.zyt.flowerkisstao.care.domain.model.CareTaskType;
import com.zyt.flowerkisstao.care.domain.model.PlannedTask;
import com.zyt.flowerkisstao.care.domain.model.Season;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 养护任务生成器。
 *
 * <p><b>这个类不依赖 Spring，也不碰数据库。</b>入参是品种养护字段 + 场景 + 时间窗，
 * 出参是任务列表，同样的输入必然得到同样的输出。与 {@code RecommendationEngine}、
 * {@code OrderTransition} 同一套路，理由也一样：排期规则是这个模块最容易出错、
 * 也最容易测的地方，抽成纯函数之后，出问题时能立刻分清是规则错了还是接线错了。
 *
 * <p>方案原文要求依据"植物品种、用户场景、当前季节和养护难度"生成六类任务：
 * 浇水检查、施肥、换盆、修剪、转向补光及病虫害预防。
 */
public final class CareTaskPlanner {

    /** 转向补光的固定周期。与品种无关，让株形均匀受光 */
    private static final int ROTATE_INTERVAL_DAYS = 14;

    /** 病虫害预防的基准周期，夏季会减半 */
    private static final int PEST_INTERVAL_DAYS = 30;

    /** 修剪周期。只有 pruneNeeded 的品种才排 */
    private static final int PRUNE_INTERVAL_DAYS = 60;

    /** 浇水间隔的下限与上限。再勤不能天天浇，再懒也不能半年不管 */
    private static final int MIN_WATER_INTERVAL = 2;
    private static final int MAX_WATER_INTERVAL = 60;

    private CareTaskPlanner() {
    }

    /**
     * 排出 [from, toExclusive) 窗口内的全部任务。
     *
     * <p>窗口化而非无限生成：一株浇水间隔 5 天的植物一年就是 73 条浇水任务，
     * 全量生成既浪费又让日历没法看。建档时排未来 60 天，之后由每日定时任务补足。
     *
     * @param input       品种与场景
     * @param anchor      起算基准日，通常是入手日期。周期从它开始数
     * @param from        窗口起点（含）
     * @param toExclusive 窗口终点（不含）
     */
    public static List<PlannedTask> plan(CarePlanInput input, LocalDate anchor,
                                         LocalDate from, LocalDate toExclusive) {
        List<PlannedTask> tasks = new ArrayList<>();

        for (CareTaskType type : CareTaskType.values()) {
            if (type == CareTaskType.PRUNE && !input.pruneNeeded()) {
                // 不需要修剪的品种不排修剪任务。给虎尾兰排"修剪"只会让用户困惑
                continue;
            }
            appendPeriodic(tasks, input, type, anchor, from, toExclusive);
        }

        // 同一天多个任务时按类型声明序排，保证同样输入的输出顺序一致
        tasks.sort(Comparator.comparing(PlannedTask::dueDate)
                .thenComparing(task -> task.type().ordinal()));
        return tasks;
    }

    /**
     * 按周期把某一类任务铺满窗口。
     *
     * <p>周期是逐次重算的，不是一次算好再等距铺开：浇水间隔随季节变（夏 0.85 冬 1.35），
     * 一个跨季的窗口里，前半段和后半段的间隔本来就该不一样。
     */
    private static void appendPeriodic(List<PlannedTask> tasks, CarePlanInput input,
                                       CareTaskType type, LocalDate anchor,
                                       LocalDate from, LocalDate toExclusive) {
        LocalDate cursor = anchor;
        // 先把游标推进到窗口起点附近，避免从入手日期开始一天天空转
        int guard = 0;
        while (cursor.isBefore(from) && guard++ < 10_000) {
            cursor = cursor.plusDays(intervalOf(input, type, cursor));
        }

        while (cursor.isBefore(toExclusive) && guard++ < 10_000) {
            if (!cursor.isBefore(from)) {
                tasks.add(new PlannedTask(type, titleOf(type), instructionOf(input, type, cursor), cursor));
            }
            cursor = cursor.plusDays(intervalOf(input, type, cursor));
        }
    }

    /** 某一类任务在某个日期上的间隔天数。浇水与病虫害会随季节和环境变 */
    private static int intervalOf(CarePlanInput input, CareTaskType type, LocalDate on) {
        Season season = Season.of(on);
        return switch (type) {
            case WATER -> waterInterval(input, season);
            case FERTILIZE -> input.fertilizeIntervalDays();
            // 换盆按月存，换算成天。一个月按 30 天算足够——换盆差几天没有影响
            case REPOT -> Math.max(30, input.repotIntervalMonths() * 30);
            case PRUNE -> PRUNE_INTERVAL_DAYS;
            case ROTATE -> ROTATE_INTERVAL_DAYS;
            case PEST -> Math.max(7, (int) Math.round(PEST_INTERVAL_DAYS * season.pestFactor()));
        };
    }

    /**
     * 浇水间隔，六类里唯一被环境与季节共同调整的一类。
     *
     * <p>每一项系数都对应一条实际原因：
     * <ul>
     *   <li><b>季节</b>：夏季蒸发快要勤浇，冬季半休眠必须减——冬天按夏天的频率浇是最常见的烂根原因</li>
     *   <li><b>光照偏暗</b>：光弱则蒸腾慢，盆土干得慢，浇太勤同样烂根</li>
     *   <li><b>健忘 / 常出差</b>：主动把间隔拉长，让用户"做得到"，而不是排一堆必然做不到的任务</li>
     *   <li><b>进阶品种</b>：养护要求精细，间隔略缩短以提高检查频次</li>
     * </ul>
     */
    private static int waterInterval(CarePlanInput input, Season season) {
        double days = input.waterIntervalDays() * season.waterFactor();

        if (input.lightLevel() <= 1) {
            days *= 1.25;
        }
        if (input.forgetful()) {
            days *= 1.2;
        }
        if (input.travelFrequent()) {
            days *= 1.3;
        }
        if (input.careLevel() >= CatalogSpecies.CARE_HARD) {
            days *= 0.9;
        }
        // 档案上的调整系数：连续遗漏或光照变化后由重评估写入
        days = days * input.waterFactorPercent() / 100.0;

        return (int) Math.max(MIN_WATER_INTERVAL, Math.min(MAX_WATER_INTERVAL, Math.round(days)));
    }

    /** 当前生效的浇水间隔，重评估要拿它做前后对比的文案 */
    public static int currentWaterInterval(CarePlanInput input, LocalDate on) {
        return waterInterval(input, Season.of(on));
    }

    private static String titleOf(CareTaskType type) {
        return switch (type) {
            case WATER -> "检查盆土并浇水";
            case FERTILIZE -> "施薄肥一次";
            case REPOT -> "检查根系并换盆";
            case PRUNE -> "修剪枯黄与徒长枝";
            case ROTATE -> "转盆并检查受光";
            case PEST -> "检查叶背与新芽";
        };
    }

    /** 方案要求每项任务带"操作方法、用量或注意事项"，就是这一段 */
    private static String instructionOf(CarePlanInput input, CareTaskType type, LocalDate on) {
        Season season = Season.of(on);
        return switch (type) {
            case WATER -> input.waterNote() + "。浇到盆底出水为止，托盘积水及时倒掉。";
            case FERTILIZE -> season == Season.WINTER
                    ? "冬季生长缓慢，本次可减半或跳过。薄肥少施，切忌浓肥烧根。"
                    : "用通用型液肥按说明稀释一倍，沿盆边浇施，避免直接接触根颈。";
            case REPOT -> "换比原盆大一号的容器，用疏松透气的介质，换盆后一周内不要施肥。";
            case PRUNE -> "剪去枯黄叶与过长枝条，刀口斜切并保持工具洁净。";
            case ROTATE -> "将花盆转动四分之一圈，让各面均匀受光，避免株形偏斜。";
            case PEST -> season == Season.SUMMER
                    ? "高温高湿是虫害高发期。重点检查叶背、叶腋与新芽，发现虫体先用清水冲洗。"
                    : "检查叶背与新芽是否有蚜虫、红蜘蛛或介壳虫，早发现处理起来最省事。";
        };
    }
}
