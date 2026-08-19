package com.zyt.flowerkisstao.recommendation.web.controller;

import com.zyt.flowerkisstao.recommendation.application.service.RecommendationService;
import com.zyt.flowerkisstao.recommendation.web.dto.RecommendRequestDTO;
import com.zyt.flowerkisstao.recommendation.web.vo.RecommendationVO;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 环境适配推荐，顾客侧。
 *
 * <p>路径挂在 /api/recommendations，不落 SecurityConfig 的公开 GET 白名单前缀——
 * 推荐结果是私有数据，游客不该看到别人的。
 *
 * <p>没有任何接口带 userId 参数：一律取当前登录人。越权访问按"不存在"处理，
 * 与画像模块口径一致。
 */
@RestController
@RequestMapping("/api/recommendations")
@Validated
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * 生成一次推荐并存快照。
     *
     * <p>用 POST 而不是 GET：它有副作用（写一条快照），而且方案要求推荐可回溯，
     * 每次生成都该留痕。body 可以整个不传，那就用默认场景。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('" + Perms.RECOMMENDATION_RESULT_GENERATE + "')")
    public R<RecommendationVO> generate(@RequestBody(required = false) RecommendRequestDTO dto) {
        Long profileId = dto == null ? null : dto.getProfileId();
        return R.ok(recommendationService.generate(profileId));
    }

    /** 我最近一次推荐。没有推荐过时 data 为 null，前端据此引导去填问卷 */
    @GetMapping("/latest")
    @PreAuthorize("hasAuthority('" + Perms.RECOMMENDATION_RESULT_READ_OWN + "')")
    public R<RecommendationVO> latest() {
        return R.ok(recommendationService.latest());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.RECOMMENDATION_RESULT_READ_OWN + "')")
    public R<RecommendationVO> get(@PathVariable Long id) {
        return R.ok(recommendationService.get(id));
    }

    /** 记录一次点击，第⑦步运营看板算推荐点击率 */
    @PutMapping("/{id}/items/{itemId}/click")
    @PreAuthorize("hasAuthority('" + Perms.RECOMMENDATION_RESULT_READ_OWN + "')")
    public R<Void> markClicked(@PathVariable Long id, @PathVariable Long itemId) {
        recommendationService.markClicked(id, itemId);
        return R.ok();
    }
}
