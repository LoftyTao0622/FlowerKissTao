package com.zyt.flowerkisstao.care.domain.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 重新评估的规则部分。
 *
 * <p>与 {@code CareTaskPlanner} 一样是纯函数：入参是触发原因与当前状态，出参是
 * 新的调整系数与排查清单。方案原文："若用户连续遗漏任务、反馈叶片发黄或修改了
 * 光照环境，系统可重新评估后续频率并给出逐项排查建议。"
 */
public final class ReEvaluationRules {

    /** 触发原因 */
    public enum Trigger {
        /** 连续遗漏任务 */
        MISSED("missed", "连续遗漏任务"),
        /** 场景光照变化 */
        LIGHT("light", "光照环境变化"),
        /** 用户反馈健康问题 */
        HEALTH("health", "植物状态反馈"),
        /** 用户手动触发 */
        MANUAL("manual", "手动重新评估");

        private final String code;
        private final String label;

        Trigger(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String code() {
            return code;
        }

        public String label() {
            return label;
        }
    }

    /** 连续遗漏时每次放宽的幅度 */
    private static final int MISSED_RELAX_STEP = 20;

    /** 调整系数的上下限。放得再宽也不能变成不管，收得再紧也不能天天浇 */
    private static final int MIN_FACTOR = 70;
    private static final int MAX_FACTOR = 200;

    private ReEvaluationRules() {
    }

    /**
     * 连续遗漏后的新系数：间隔放宽两成。
     *
     * <p>用户连着漏两次浇水，说明这个频率他做不到。与其继续排一堆做不到的任务，
     * 不如把节奏调到他跟得上的程度——方案要的是"持续养护"，不是"持续提醒失败"。
     */
    public static int relaxForMissed(int currentFactor) {
        return clamp(currentFactor + MISSED_RELAX_STEP);
    }

    /**
     * 光照变化后的新系数。
     *
     * <p>光照变暗 → 蒸腾变慢 → 盆土干得慢 → 必须拉长间隔，否则烂根；
     * 变亮则相反。每差一档调整 15%。
     */
    public static int adjustForLight(int currentFactor, int oldLevel, int newLevel) {
        int delta = oldLevel - newLevel;
        return clamp(currentFactor + delta * 15);
    }

    /**
     * 叶片发黄等症状的逐项排查清单。
     *
     * <p>清单是"按可能性从大到小"排的：黄叶最常见的原因是浇水过多而非缺水，
     * 把浇水排查放在最前，能避免用户凭直觉"再多浇点"把植物彻底浇死。
     *
     * @param symptom       症状代码
     * @param recentSkipped 最近是否跳过过浇水任务
     * @param dimLight      场景光照是否偏暗
     * @param recentRepot   最近 30 天内是否换过盆
     */
    public static List<String> checklistFor(String symptom, boolean recentSkipped,
                                            boolean dimLight, boolean recentRepot) {
        List<String> items = new ArrayList<>();

        switch (symptom == null ? "" : symptom) {
            case "yellowing" -> {
                items.add("先摸盆土：土湿而叶黄多半是浇水过多，此时应停浇并加强通风，"
                        + "而不是继续补水——黄叶最常见的原因是涝而不是旱。");
                if (recentSkipped) {
                    items.add("检测到你最近跳过了浇水任务。若土已干透至盆底，则可能是缺水，"
                            + "先浇透一次再观察三天。");
                }
                if (dimLight) {
                    items.add("当前场景光照偏暗，长期弱光会让老叶发黄脱落。"
                            + "可以挪到离窗更近的位置，或增加转盆频率。");
                }
                if (recentRepot) {
                    items.add("最近换过盆，短期黄叶可能是缓苗反应。保持稳定环境、暂缓施肥，"
                            + "两周内通常会自行恢复。");
                }
                items.add("检查是否只有底部老叶变黄——那属于正常代谢，摘掉即可。"
                        + "若新叶也黄，问题更可能出在根系或光照。");
            }
            case "wilting" -> {
                items.add("先确认盆土干湿：干透萎蔫浇透即可恢复；土湿仍萎蔫多半是根系受损，"
                        + "需脱盆检查有无烂根并修剪。");
                items.add("检查是否被空调、暖气或冷风直吹，位置不当会造成短时间大量失水。");
                if (recentRepot) {
                    items.add("刚换过盆的植株常有短暂萎蔫，属于缓苗，避免此时施肥或再次移动。");
                }
            }
            case "spots" -> {
                items.add("观察斑点是否有黄晕或扩散趋势：扩散通常是真菌或细菌病害，"
                        + "应剪除病叶并隔离，避免叶面积水。");
                items.add("检查是否浇水时淋到叶面、通风是否不足——闷湿环境最易诱发叶斑。");
            }
            case "dropping" -> {
                items.add("检查近期是否挪动过位置或温度骤变，多数植物落叶是对环境突变的应激反应。");
                items.add("确认盆土是否长期过湿，根系受损时会先表现为落叶。");
                if (dimLight) {
                    items.add("光照偏暗时植株会主动舍弃下部叶片以减少消耗，可考虑改善采光。");
                }
            }
            case "pest" -> {
                items.add("重点检查叶背、叶腋与新芽：蚜虫、红蜘蛛、介壳虫都藏在这些位置。");
                items.add("发现虫体先用清水冲洗或用棉签蘸酒精擦除，虫口密度低时不必立即用药。");
                items.add("处理后与其他植物隔离两周，防止扩散。");
            }
            default -> items.add("请先描述具体症状，以便给出针对性的排查建议。");
        }

        // 方案原文："复杂问题会提示咨询商家或专业人员。"
        items.add("若按以上步骤处理两周后仍无改善，建议联系商家或花卉养护专业人员进一步诊断。");
        return items;
    }

    private static int clamp(int factor) {
        return Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, factor));
    }
}
