package com.zyt.flowerkisstao.knowledge.domain.model;

import java.util.Arrays;
import java.util.List;

/**
 * 文章分类。十个取值逐条对应方案点名的主题："内容涵盖植物习性、花期管理、
 * 光照判断、浇水方法、基质与花盆选择、施肥修剪、季节养护、宠物安全和常见病虫害等主题。"
 *
 * <p>{@link #taskType} 把分类挂到养护任务类型上，这是"从养护任务跳到指南"的依据。
 * 没有对应任务类型的分类（如宠物安全、基质选择）返回 null。
 */
public enum ArticleCategory {

    WATERING("watering", "浇水方法", "water"),
    LIGHT("light", "光照判断", "rotate"),
    FEEDING("feeding", "施肥", "fertilize"),
    REPOTTING("repotting", "换盆", "repot"),
    PRUNING("pruning", "修剪", "prune"),
    PEST("pest", "病虫害防治", "pest"),
    MEDIUM("medium", "基质与花盆", null),
    SEASON("season", "季节养护", null),
    PET_SAFETY("pet-safety", "宠物与儿童安全", null),
    BLOOM("bloom", "花期管理", null);

    private final String code;
    private final String label;
    private final String taskType;

    ArticleCategory(String code, String label, String taskType) {
        this.code = code;
        this.label = label;
        this.taskType = taskType;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /** 对应的养护任务类型，没有则为 null */
    public String taskType() {
        return taskType;
    }

    public static ArticleCategory of(String code) {
        for (ArticleCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }

    /** 中文名，找不到时原样返回 code，免得界面上出现空白 */
    public static String labelOf(String code) {
        ArticleCategory category = of(code);
        return category == null ? code : category.label();
    }

    public static List<ArticleCategory> all() {
        return Arrays.asList(values());
    }
}
