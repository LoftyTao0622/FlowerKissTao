package com.zyt.flowerkisstao.recommendation.domain.model;

/**
 * 硬过滤规则。每条对应方案原文里的一句筛选要求。
 *
 * <p>{@link #relaxable} 是这个枚举存在的理由：无候选时要给用户"放宽哪一条"的
 * 建议，但绝不能建议"要不要接受一株对猫有毒的植物"。方案原文写得很明确——
 * "无候选时仅建议放宽非安全约束"。把可否放宽做成规则自带的属性，而不是在
 * 生成建议的地方写一串 if，漏掉一条的风险就没了。
 */
public enum FilterRule {

    /** 家里养猫狗，排除对应有毒的品种 */
    TOXIC_PET("pet", "宠物安全", false),

    /** 家里有幼童，排除误食有风险的品种 */
    TOXIC_CHILD("child", "儿童安全", false),

    /** 光照等级不在品种可接受区间内 */
    LIGHT("light", "光照条件", true),

    /** 冠幅超过空间上限 */
    SPACE("space", "摆放空间", true),

    /** 没有任何一个 SKU 的价格落在预算内 */
    BUDGET("budget", "预算上限", true),

    /** 经常出差，排除浇水间隔过短的品种 */
    WATER_FREQUENCY("water", "浇水频率", true);

    private final String code;
    private final String label;
    private final boolean relaxable;

    FilterRule(String code, String label, boolean relaxable) {
        this.code = code;
        this.label = label;
        this.relaxable = relaxable;
    }

    /** 存进 filtered_json 的键，也是前端读诊断的键 */
    public String code() {
        return code;
    }

    /** 中文名，直接用于诊断文案 */
    public String label() {
        return label;
    }

    /** 安全约束返回 false，永远不出现在放宽建议里 */
    public boolean relaxable() {
        return relaxable;
    }
}
