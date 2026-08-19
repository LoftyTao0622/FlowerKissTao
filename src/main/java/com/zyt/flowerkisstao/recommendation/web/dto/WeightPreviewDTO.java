package com.zyt.flowerkisstao.recommendation.web.dto;

import lombok.Data;

/**
 * 推荐权重测试预览。
 *
 * <p>profileId 为空时使用当前登录人的默认画像；weights 为空时使用当前数据库配置。
 * 临时权重只参与本次计算，不落 rec_weight_config，也不创建 rec_result 快照。
 */
@Data
public class WeightPreviewDTO {

    private Long profileId;

    private WeightConfigDTO weights;
}
