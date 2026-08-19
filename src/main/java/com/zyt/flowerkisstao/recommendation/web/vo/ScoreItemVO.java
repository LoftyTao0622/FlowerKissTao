package com.zyt.flowerkisstao.recommendation.web.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 一个评分维度的得分与权重。
 *
 * <p>把权重和得分一起返回，前端才能画出"光照 100 分 × 30%"这种解释。
 * 只给得分的话，用户看到七个百分比却不知道哪个更要紧。
 */
@Data
@AllArgsConstructor
public class ScoreItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** light / temp / humidity / care / space / budget / preference */
    private String code;

    /** 中文名，如"光照" */
    private String label;

    /** 该维得分 0-100 */
    private Integer score;

    /** 该维在本次推荐中的权重百分比，取自快照而非当前配置 */
    private Integer weight;
}
