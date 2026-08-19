package com.zyt.flowerkisstao.recommendation.application.service;

import com.zyt.flowerkisstao.recommendation.web.vo.RecommendationVO;

/**
 * 环境适配推荐。方案里的模块 3，本项目的核心。
 *
 * <p>这一层只做编排：读画像 → 读候选 → 调 {@code RecommendationEngine} → 存快照。
 * 算法本身在引擎里，那是个不依赖 Spring 的纯函数，可以脱离容器单测。
 */
public interface RecommendationService {

    /**
     * 按指定画像生成推荐并存快照。
     *
     * @param profileId 为空时取当前用户的默认场景
     */
    RecommendationVO generate(Long profileId);

    /** 我最近一次推荐。从没推荐过时返回 null，由前端引导去填问卷 */
    RecommendationVO latest();

    /** 某次推荐的详情。不属于当前用户时按"不存在"处理 */
    RecommendationVO get(Long resultId);

    /** 记录一次点击，第⑦步运营看板算推荐点击率 */
    void markClicked(Long resultId, Long itemId);
}
