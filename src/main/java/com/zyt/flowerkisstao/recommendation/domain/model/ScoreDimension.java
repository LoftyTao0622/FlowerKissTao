package com.zyt.flowerkisstao.recommendation.domain.model;

/**
 * 七个评分维度。名字与 {@code score_json} 的键、{@link WeightSet} 的字段一一对应。
 *
 * <p>做成枚举而不是七个散落的常量，是为了让"生成理由"与"挑风险项"能统一按
 * 维度遍历——每加一维只改这里，不必再去补三处 switch。
 */
public enum ScoreDimension {

    LIGHT("light", "光照"),
    TEMP("temp", "温度"),
    HUMIDITY("humidity", "湿度"),
    CARE("care", "养护"),
    SPACE("space", "空间"),
    BUDGET("budget", "预算"),
    PREFERENCE("preference", "偏好");

    private final String code;
    private final String label;

    ScoreDimension(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /** 该维度在这组权重里占几分 */
    public int weightIn(WeightSet weights) {
        return switch (this) {
            case LIGHT -> weights.light();
            case TEMP -> weights.temp();
            case HUMIDITY -> weights.humidity();
            case CARE -> weights.care();
            case SPACE -> weights.space();
            case BUDGET -> weights.budget();
            case PREFERENCE -> weights.preference();
        };
    }
}
