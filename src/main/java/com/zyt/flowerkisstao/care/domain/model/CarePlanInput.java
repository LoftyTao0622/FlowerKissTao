package com.zyt.flowerkisstao.care.domain.model;

import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.user.domain.entity.UserSceneProfile;

/**
 * 排任务需要的全部输入，打成一个包传给 {@code CareTaskPlanner}。
 *
 * <p>单独定义而不是让 Planner 直接收实体，是为了让它不依赖持久化类型——
 * 单元测试要能随手造一株"浇水间隔 5 天、需要修剪"的假植物，
 * 不必先构造一整个 CatalogSpecies。
 *
 * @param waterIntervalDays     品种浇水间隔天数
 * @param fertilizeIntervalDays 品种施肥间隔天数
 * @param repotIntervalMonths   品种换盆间隔月数
 * @param pruneNeeded           是否需要定期修剪
 * @param careLevel             1 新手友好 / 2 需要关注 / 3 进阶养护
 * @param waterNote             品种的浇水文案，直接用作任务说明
 * @param lightLevel            场景光照档位 1-4，偏暗会拉长浇水间隔
 * @param forgetful             用户是否容易忘记养护
 * @param travelFrequent        是否经常出差
 * @param waterFactorPercent    档案上的浇水调整系数百分比，100 为不调整
 */
public record CarePlanInput(int waterIntervalDays,
                            int fertilizeIntervalDays,
                            int repotIntervalMonths,
                            boolean pruneNeeded,
                            int careLevel,
                            String waterNote,
                            int lightLevel,
                            boolean forgetful,
                            boolean travelFrequent,
                            int waterFactorPercent) {

    /** 从品种与场景拼出输入。场景可为空——用户可能还没填过问卷 */
    public static CarePlanInput of(CatalogSpecies species, UserSceneProfile scene, int waterFactorPercent) {
        return new CarePlanInput(
                orDefault(species.getWaterIntervalDays(), 7),
                orDefault(species.getFertilizeIntervalDays(), 30),
                orDefault(species.getRepotIntervalMonths(), 24),
                isYes(species.getPruneNeeded()),
                orDefault(species.getCareLevel(), CatalogSpecies.CARE_EASY),
                species.getWaterNote() == null ? "检查盆土湿度后按需浇透" : species.getWaterNote(),
                scene == null ? 3 : orDefault(scene.getLightLevel(), 3),
                scene != null && isYes(scene.getForgetful()),
                scene != null && scene.getTravelFrequency() != null && scene.getTravelFrequency() >= 3,
                waterFactorPercent);
    }

    private static int orDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static boolean isYes(Integer flag) {
        return flag != null && flag == 1;
    }
}
