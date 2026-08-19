package com.zyt.flowerkisstao.recommendation.domain.model;

import com.zyt.flowerkisstao.recommendation.domain.entity.RecWeightConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 七维权重的取值，打分引擎的入参之一。
 *
 * <p>单独定义成 record 而不直接用 {@link RecWeightConfig} 实体，是为了让引擎
 * 不依赖持久化类型：管理端的"预览"接口要用一组临时权重试算而不落库，
 * 单元测试也要能随手造一组极端权重（比如光照 100 其余 0）验证权重确实生效。
 *
 * @param light      光照
 * @param temp       温度
 * @param humidity   湿度
 * @param care       养护能力
 * @param space      空间
 * @param budget     预算贴合
 * @param preference 观赏偏好
 * @param topN       返回条数
 */
public record WeightSet(int light, int temp, int humidity, int care,
                        int space, int budget, int preference, int topN) {

    /** 方案架构章节给出的默认口径 */
    public static final WeightSet DEFAULT = new WeightSet(30, 15, 10, 20, 10, 10, 5, 3);

    /**
     * 七项之和。必须等于 {@link RecWeightConfig#WEIGHT_SUM}，否则加权总分
     * 不在 0-100 刻度上，前端的百分比进度条会画错。
     */
    public int sum() {
        return light + temp + humidity + care + space + budget + preference;
    }

    public static WeightSet from(RecWeightConfig config) {
        return new WeightSet(config.getWLight(), config.getWTemp(), config.getWHumidity(),
                config.getWCare(), config.getWSpace(), config.getWBudget(),
                config.getWPreference(), config.getTopN());
    }

    /**
     * 存进 {@code rec_result.weight_snapshot} 的形态。
     *
     * <p>用 LinkedHashMap 保序：快照读出来展示时，七项的顺序应当每次一致。
     */
    public Map<String, Integer> toSnapshot() {
        Map<String, Integer> snapshot = new LinkedHashMap<>();
        snapshot.put("light", light);
        snapshot.put("temp", temp);
        snapshot.put("humidity", humidity);
        snapshot.put("care", care);
        snapshot.put("space", space);
        snapshot.put("budget", budget);
        snapshot.put("preference", preference);
        snapshot.put("topN", topN);
        return snapshot;
    }
}
