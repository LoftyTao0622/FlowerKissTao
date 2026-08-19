package com.zyt.flowerkisstao.recommendation.application.service;

import com.zyt.flowerkisstao.recommendation.domain.model.WeightSet;
import com.zyt.flowerkisstao.recommendation.web.dto.WeightConfigDTO;

/**
 * 推荐权重配置的读写。管理员可调，调完对新推荐生效，历史推荐仍按各自的快照解释。
 */
public interface WeightConfigService {

    /** 当前生效的权重。表里那一行若意外缺失，退回方案默认口径而不是让推荐算不出来 */
    WeightSet current();

    void update(WeightConfigDTO dto);
}
