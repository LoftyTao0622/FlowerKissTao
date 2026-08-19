package com.zyt.flowerkisstao.care.domain.model;

/**
 * 六类养护任务。方案原文点名的就是这六项："生成浇水检查、施肥、换盆、修剪、
 * 转向补光及病虫害预防任务"。
 *
 * <p>{@code code} 存进 {@code care_task.task_type}，改了会与存量数据错位。
 */
public enum CareTaskType {

    /** 浇水检查。周期取品种的 waterIntervalDays，是唯一会被环境与季节调整的一类 */
    WATER("water", "浇水检查", "💧"),

    /** 施肥。周期取品种的 fertilizeIntervalDays */
    FERTILIZE("fertilize", "施肥", "🌿"),

    /** 换盆。周期取品种的 repotIntervalMonths */
    REPOT("repot", "换盆", "🪴"),

    /** 修剪。只有 pruneNeeded=1 的品种才生成 */
    PRUNE("prune", "修剪", "✂️"),

    /** 转向补光。让株形均匀受光，与品种无关，固定周期 */
    ROTATE("rotate", "转向补光", "🔄"),

    /** 病虫害预防。夏季加密，虫害在高温高湿季最活跃 */
    PEST("pest", "病虫害预防", "🔍");

    private final String code;
    private final String label;
    private final String icon;

    CareTaskType(String code, String label, String icon) {
        this.code = code;
        this.label = label;
        this.icon = icon;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String icon() {
        return icon;
    }

    /** 数据库里的值转回枚举。未知值直接抛，比静默当成浇水安全 */
    public static CareTaskType of(String code) {
        for (CareTaskType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的任务类型：" + code);
    }
}
