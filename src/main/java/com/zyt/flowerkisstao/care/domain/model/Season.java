package com.zyt.flowerkisstao.care.domain.model;

import java.time.LocalDate;

/**
 * 季节。方案原文要求依据"当前季节"生成养护任务。
 *
 * <p>按北半球月份粗分四季即可——这个产品面向的是家庭室内植物，
 * 精确到节气对浇水频率没有实际影响，反而让规则难以解释。
 */
public enum Season {

    /** 春 3-5 月，生长季开始 */
    SPRING("春季"),

    /** 夏 6-8 月，蒸发快、虫害活跃 */
    SUMMER("夏季"),

    /** 秋 9-11 月 */
    AUTUMN("秋季"),

    /** 冬 12-2 月，多数植物半休眠，浇水要减 */
    WINTER("冬季");

    private final String label;

    Season(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Season of(LocalDate date) {
        return switch (date.getMonthValue()) {
            case 3, 4, 5 -> SPRING;
            case 6, 7, 8 -> SUMMER;
            case 9, 10, 11 -> AUTUMN;
            default -> WINTER;
        };
    }

    /**
     * 浇水间隔的季节系数。
     *
     * <p>夏季蒸发快，间隔缩短；冬季多数植物半休眠，间隔明显拉长——
     * 冬天按夏天的频率浇水是最常见的烂根原因。
     */
    public double waterFactor() {
        return switch (this) {
            case SUMMER -> 0.85;
            case WINTER -> 1.35;
            case SPRING, AUTUMN -> 1.0;
        };
    }

    /** 病虫害预防的季节系数。夏季高温高湿虫害最活跃，检查加密一倍 */
    public double pestFactor() {
        return this == SUMMER ? 0.5 : 1.0;
    }
}
