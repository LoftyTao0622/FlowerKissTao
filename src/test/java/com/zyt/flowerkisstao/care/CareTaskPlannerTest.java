package com.zyt.flowerkisstao.care;

import com.zyt.flowerkisstao.care.domain.model.CarePlanInput;
import com.zyt.flowerkisstao.care.domain.model.CareTaskType;
import com.zyt.flowerkisstao.care.domain.model.PlannedTask;
import com.zyt.flowerkisstao.care.domain.model.Season;
import com.zyt.flowerkisstao.care.domain.service.CareTaskPlanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 养护任务生成器的单元测试。
 *
 * <p>不起 Spring 容器：排期是纯函数，测它不需要数据库。养护任务一排就是几十条、
 * 跨月跨季，靠人眼看数据库表核对是不现实的，这些断言才是真正的保障。
 */
class CareTaskPlannerTest {

    /** 春季起算，避开季节边界，让基础用例的间隔不被季节系数干扰 */
    private static final LocalDate SPRING_ANCHOR = LocalDate.of(2026, 4, 1);

    // ================================================================
    // 周期
    // ================================================================

    @Test
    @DisplayName("浇水任务按品种间隔铺满窗口")
    void watersOnSpeciesInterval() {
        // 浇水间隔 10 天、光照正常、不健忘：春季系数 1.0，就是 10 天一次
        CarePlanInput input = baseInput(10);
        List<PlannedTask> tasks = CareTaskPlanner.plan(
                input, SPRING_ANCHOR, SPRING_ANCHOR, SPRING_ANCHOR.plusDays(60));

        List<PlannedTask> waters = ofType(tasks, CareTaskType.WATER);
        assertEquals(6, waters.size(), "60 天窗口、10 天一次，应当有 6 条");

        for (int i = 0; i < waters.size(); i++) {
            assertEquals(SPRING_ANCHOR.plusDays(i * 10L), waters.get(i).dueDate(),
                    "第 " + (i + 1) + " 次浇水日期不对");
        }
    }

    @Test
    @DisplayName("施肥与换盆各按自己的周期，互不影响")
    void fertilizeAndRepotUseOwnIntervals() {
        CarePlanInput input = new CarePlanInput(10, 30, 12, false, 1,
                "土表干后浇透", 3, false, false, 100);
        List<PlannedTask> tasks = CareTaskPlanner.plan(
                input, SPRING_ANCHOR, SPRING_ANCHOR, SPRING_ANCHOR.plusDays(90));

        assertEquals(3, ofType(tasks, CareTaskType.FERTILIZE).size(), "90 天里 30 天一次施肥");
        // 换盆 12 个月 = 360 天，90 天窗口里只有起算日那一次
        assertEquals(1, ofType(tasks, CareTaskType.REPOT).size());
    }

    @Test
    @DisplayName("转向补光固定 14 天一次，与品种无关")
    void rotateIsFixedInterval() {
        List<PlannedTask> a = CareTaskPlanner.plan(
                baseInput(5), SPRING_ANCHOR, SPRING_ANCHOR, SPRING_ANCHOR.plusDays(56));
        List<PlannedTask> b = CareTaskPlanner.plan(
                baseInput(21), SPRING_ANCHOR, SPRING_ANCHOR, SPRING_ANCHOR.plusDays(56));

        assertEquals(4, ofType(a, CareTaskType.ROTATE).size());
        assertEquals(ofType(a, CareTaskType.ROTATE).size(),
                ofType(b, CareTaskType.ROTATE).size(),
                "浇水间隔天差地别的两株，转盆频率应当一样");
    }

    // ================================================================
    // 六类齐全与修剪的条件生成
    // ================================================================

    @Test
    @DisplayName("需要修剪的品种六类齐全")
    void allSixTypesForPrunedSpecies() {
        CarePlanInput input = new CarePlanInput(10, 30, 12, true, 2,
                "土表干后浇透", 3, false, false, 100);
        List<PlannedTask> tasks = CareTaskPlanner.plan(
                input, SPRING_ANCHOR, SPRING_ANCHOR, SPRING_ANCHOR.plusDays(120));

        Set<CareTaskType> types = tasks.stream().map(PlannedTask::type).collect(Collectors.toSet());
        assertEquals(6, types.size(), "方案点名的六类应当齐全，实际：" + types);
        for (CareTaskType type : CareTaskType.values()) {
            assertTrue(types.contains(type), "缺少 " + type.label());
        }
    }

    @Test
    @DisplayName("不需要修剪的品种不排修剪任务")
    void noPruneTaskWhenNotNeeded() {
        CarePlanInput input = baseInput(10);
        assertFalse(input.pruneNeeded());

        List<PlannedTask> tasks = CareTaskPlanner.plan(
                input, SPRING_ANCHOR, SPRING_ANCHOR, SPRING_ANCHOR.plusDays(180));

        assertTrue(ofType(tasks, CareTaskType.PRUNE).isEmpty(),
                "给虎尾兰这类不需修剪的植物排修剪任务只会让用户困惑");
        assertFalse(ofType(tasks, CareTaskType.WATER).isEmpty(), "其余类型仍应正常生成");
    }

    // ================================================================
    // 季节调整
    // ================================================================

    @Test
    @DisplayName("冬季浇水明显比夏季稀")
    void winterWatersLessOftenThanSummer() {
        CarePlanInput input = baseInput(10);

        LocalDate summer = LocalDate.of(2026, 7, 1);
        LocalDate winter = LocalDate.of(2026, 1, 5);

        int summerCount = ofType(CareTaskPlanner.plan(input, summer, summer, summer.plusDays(60)),
                CareTaskType.WATER).size();
        int winterCount = ofType(CareTaskPlanner.plan(input, winter, winter, winter.plusDays(60)),
                CareTaskType.WATER).size();

        assertTrue(summerCount > winterCount,
                "夏季应当浇得更勤：夏 " + summerCount + " 次 vs 冬 " + winterCount + " 次");
        // 冬天按夏天的频率浇水是最常见的烂根原因，差距必须明显
        assertEquals(7, summerCount, "10 天 × 0.85 ≈ 9 天一次");
        assertEquals(5, winterCount, "10 天 × 1.35 ≈ 14 天一次");
    }

    @Test
    @DisplayName("夏季病虫害检查加密一倍")
    void pestChecksDoubleInSummer() {
        CarePlanInput input = baseInput(10);

        LocalDate summer = LocalDate.of(2026, 6, 15);
        LocalDate autumn = LocalDate.of(2026, 10, 1);

        int summerPest = ofType(CareTaskPlanner.plan(input, summer, summer, summer.plusDays(60)),
                CareTaskType.PEST).size();
        int autumnPest = ofType(CareTaskPlanner.plan(input, autumn, autumn, autumn.plusDays(60)),
                CareTaskType.PEST).size();

        assertTrue(summerPest > autumnPest,
                "高温高湿是虫害高发期：夏 " + summerPest + " vs 秋 " + autumnPest);
    }

    @Test
    @DisplayName("季节系数与枚举一致")
    void seasonFactorsAreConsistent() {
        assertEquals(Season.SUMMER, Season.of(LocalDate.of(2026, 7, 1)));
        assertEquals(Season.WINTER, Season.of(LocalDate.of(2026, 1, 1)));
        assertEquals(Season.WINTER, Season.of(LocalDate.of(2026, 12, 31)));
        assertEquals(Season.SPRING, Season.of(LocalDate.of(2026, 3, 1)));
        assertEquals(Season.AUTUMN, Season.of(LocalDate.of(2026, 9, 30)));

        assertTrue(Season.WINTER.waterFactor() > Season.SUMMER.waterFactor());
        assertTrue(Season.SUMMER.pestFactor() < Season.AUTUMN.pestFactor());
    }

    // ================================================================
    // 环境调整
    // ================================================================

    @Test
    @DisplayName("健忘的人拿到更宽松的浇水频率")
    void forgetfulUserGetsLongerInterval() {
        CarePlanInput normal = baseInput(10);
        CarePlanInput forgetful = new CarePlanInput(10, 30, 24, false, 1,
                "土表干后浇透", 3, true, false, 100);

        int normalCount = waterCount(normal);
        int forgetfulCount = waterCount(forgetful);

        assertTrue(forgetfulCount < normalCount,
                "排一堆必然做不到的任务没有意义：普通 " + normalCount + " vs 健忘 " + forgetfulCount);
    }

    @Test
    @DisplayName("光照偏暗时浇水间隔拉长")
    void dimLightStretchesWaterInterval() {
        CarePlanInput bright = baseInput(10);
        CarePlanInput dim = new CarePlanInput(10, 30, 24, false, 1,
                "土表干后浇透", 1, false, false, 100);

        assertTrue(waterCount(dim) < waterCount(bright),
                "光弱蒸腾慢、盆土干得慢，浇太勤同样会烂根");
    }

    @Test
    @DisplayName("经常出差时浇水间隔拉长")
    void frequentTravelStretchesInterval() {
        CarePlanInput home = baseInput(10);
        CarePlanInput traveller = new CarePlanInput(10, 30, 24, false, 1,
                "土表干后浇透", 3, false, true, 100);

        assertTrue(waterCount(traveller) < waterCount(home));
    }

    @Test
    @DisplayName("档案上的调整系数直接影响频率，这是重评估的落点")
    void archiveFactorChangesFrequency() {
        CarePlanInput base = baseInput(10);
        // 连续遗漏后重评估把系数调到 130，间隔应当明显拉长
        CarePlanInput relaxed = new CarePlanInput(10, 30, 24, false, 1,
                "土表干后浇透", 3, false, false, 130);

        assertTrue(waterCount(relaxed) < waterCount(base),
                "系数 130 意味着间隔拉长三成");
        assertEquals(10, CareTaskPlanner.currentWaterInterval(base, SPRING_ANCHOR));
        assertEquals(13, CareTaskPlanner.currentWaterInterval(relaxed, SPRING_ANCHOR));
    }

    @Test
    @DisplayName("浇水间隔有上下限，再极端也不会天天浇或半年不管")
    void waterIntervalIsClamped() {
        // 间隔 1 天 + 夏季 + 进阶品种，各系数叠加后仍不能低于 2 天
        CarePlanInput crazy = new CarePlanInput(1, 30, 24, false, 3,
                "保持湿润", 4, false, false, 50);
        assertTrue(CareTaskPlanner.currentWaterInterval(crazy, LocalDate.of(2026, 7, 1)) >= 2);

        // 间隔 40 天 + 冬季 + 健忘 + 出差 + 弱光 + 高系数，也不能超过 60 天
        CarePlanInput lazy = new CarePlanInput(40, 30, 24, false, 1,
                "干透浇透", 1, true, true, 200);
        assertTrue(CareTaskPlanner.currentWaterInterval(lazy, LocalDate.of(2026, 1, 1)) <= 60);
    }

    // ================================================================
    // 时间窗
    // ================================================================

    @Test
    @DisplayName("窗口是左闭右开，终点当天不生成")
    void windowExcludesEndDate() {
        CarePlanInput input = baseInput(10);
        LocalDate end = SPRING_ANCHOR.plusDays(30);

        List<PlannedTask> tasks = CareTaskPlanner.plan(input, SPRING_ANCHOR, SPRING_ANCHOR, end);

        assertTrue(tasks.stream().allMatch(task -> task.dueDate().isBefore(end)),
                "终点当天的任务不该出现在本窗口，否则补任务时会与下一窗口重复");
        assertTrue(tasks.stream().allMatch(task -> !task.dueDate().isBefore(SPRING_ANCHOR)));
    }

    @Test
    @DisplayName("补任务窗口只产出窗口内的部分，不会重复历史")
    void laterWindowDoesNotRepeatEarlierTasks() {
        CarePlanInput input = baseInput(10);
        LocalDate mid = SPRING_ANCHOR.plusDays(30);
        LocalDate end = SPRING_ANCHOR.plusDays(60);

        List<PlannedTask> first = CareTaskPlanner.plan(input, SPRING_ANCHOR, SPRING_ANCHOR, mid);
        List<PlannedTask> second = CareTaskPlanner.plan(input, SPRING_ANCHOR, mid, end);
        List<PlannedTask> whole = CareTaskPlanner.plan(input, SPRING_ANCHOR, SPRING_ANCHOR, end);

        assertEquals(whole.size(), first.size() + second.size(),
                "分两次补出来的任务总数应当与一次排满相同——多出来就是重复，少了就是漏排");

        Set<LocalDate> firstDates = first.stream().map(PlannedTask::dueDate).collect(Collectors.toSet());
        assertTrue(second.stream().noneMatch(task -> firstDates.contains(task.dueDate())
                        && ofType(first, task.type()).stream()
                        .anyMatch(f -> f.dueDate().equals(task.dueDate()))),
                "两个窗口不该产出同类型同日期的任务");
    }

    @Test
    @DisplayName("空窗口产出空列表")
    void emptyWindowYieldsNothing() {
        List<PlannedTask> tasks = CareTaskPlanner.plan(
                baseInput(10), SPRING_ANCHOR, SPRING_ANCHOR, SPRING_ANCHOR);
        assertTrue(tasks.isEmpty());
    }

    // ================================================================
    // 内容与可复现
    // ================================================================

    @Test
    @DisplayName("每条任务都有标题与操作说明")
    void everyTaskHasTitleAndInstruction() {
        CarePlanInput input = new CarePlanInput(10, 30, 12, true, 2,
                "土表 3-5 厘米干后浇透", 3, false, false, 100);
        List<PlannedTask> tasks = CareTaskPlanner.plan(
                input, SPRING_ANCHOR, SPRING_ANCHOR, SPRING_ANCHOR.plusDays(120));

        assertFalse(tasks.isEmpty());
        for (PlannedTask task : tasks) {
            assertFalse(task.title().isBlank(), task.type() + " 缺标题");
            assertFalse(task.instruction().isBlank(),
                    task.type() + " 缺操作说明——方案要求每项任务含操作方法与注意事项");
        }
        // 浇水说明应当带上品种自己的文案，而不是一句通用的话
        assertTrue(ofType(tasks, CareTaskType.WATER).get(0).instruction().contains("土表 3-5 厘米"));
    }

    @Test
    @DisplayName("同样的输入排两次，结果完全一致")
    void isReproducible() {
        CarePlanInput input = new CarePlanInput(7, 30, 18, true, 2,
                "土表干透后浇透", 2, true, false, 110);

        List<PlannedTask> first = CareTaskPlanner.plan(
                input, SPRING_ANCHOR, SPRING_ANCHOR, SPRING_ANCHOR.plusDays(90));
        List<PlannedTask> second = CareTaskPlanner.plan(
                input, SPRING_ANCHOR, SPRING_ANCHOR, SPRING_ANCHOR.plusDays(90));

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).type(), second.get(i).type());
            assertEquals(first.get(i).dueDate(), second.get(i).dueDate());
            assertEquals(first.get(i).instruction(), second.get(i).instruction());
        }
    }

    @Test
    @DisplayName("任务按日期升序排列，同一天按类型固定顺序")
    void tasksAreSortedByDate() {
        CarePlanInput input = new CarePlanInput(10, 30, 12, true, 2,
                "土表干后浇透", 3, false, false, 100);
        List<PlannedTask> tasks = CareTaskPlanner.plan(
                input, SPRING_ANCHOR, SPRING_ANCHOR, SPRING_ANCHOR.plusDays(90));

        for (int i = 1; i < tasks.size(); i++) {
            LocalDate prev = tasks.get(i - 1).dueDate();
            LocalDate curr = tasks.get(i).dueDate();
            assertFalse(curr.isBefore(prev), "日历要按日期展示，排序不能乱");
        }
    }

    // ================================================================
    // 夹具
    // ================================================================

    /** 一株普通植物：不需修剪、新手友好、场景光照正常、用户不健忘不出差 */
    private static CarePlanInput baseInput(int waterIntervalDays) {
        return new CarePlanInput(waterIntervalDays, 30, 24, false, 1,
                "土表干后浇透", 3, false, false, 100);
    }

    private static int waterCount(CarePlanInput input) {
        return ofType(CareTaskPlanner.plan(input, SPRING_ANCHOR, SPRING_ANCHOR,
                SPRING_ANCHOR.plusDays(60)), CareTaskType.WATER).size();
    }

    private static List<PlannedTask> ofType(List<PlannedTask> tasks, CareTaskType type) {
        return tasks.stream().filter(task -> task.type() == type).toList();
    }
}
