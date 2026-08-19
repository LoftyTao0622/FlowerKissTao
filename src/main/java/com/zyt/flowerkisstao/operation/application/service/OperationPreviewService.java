package com.zyt.flowerkisstao.operation.application.service;

import com.zyt.flowerkisstao.recommendation.web.dto.WeightPreviewDTO;
import com.zyt.flowerkisstao.recommendation.web.vo.RecommendationVO;

/** 推荐规则测试预览：不落配置，不存推荐快照。 */
public interface OperationPreviewService {

    RecommendationVO preview(WeightPreviewDTO dto);
}
