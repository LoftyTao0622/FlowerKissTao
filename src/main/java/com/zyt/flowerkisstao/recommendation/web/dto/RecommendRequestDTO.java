package com.zyt.flowerkisstao.recommendation.web.dto;

import lombok.Data;

/**
 * 生成推荐的入参。
 *
 * <p>只有一个可选字段：不传 {@code profileId} 就用当前用户的默认场景。
 * 没有 userId 参数——一律取当前登录人，前端传不进来别人的 id。
 */
@Data
public class RecommendRequestDTO {

    /** 用哪份画像算。为空时取默认场景 */
    private Long profileId;
}
