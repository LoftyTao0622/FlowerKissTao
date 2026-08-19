package com.zyt.flowerkisstao.recommendation.web.controller;

import com.zyt.flowerkisstao.operation.application.service.OperationPreviewService;
import com.zyt.flowerkisstao.recommendation.application.service.WeightConfigService;
import com.zyt.flowerkisstao.recommendation.domain.model.WeightSet;
import com.zyt.flowerkisstao.recommendation.web.dto.WeightConfigDTO;
import com.zyt.flowerkisstao.recommendation.web.dto.WeightPreviewDTO;
import com.zyt.flowerkisstao.recommendation.web.vo.RecommendationVO;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 推荐权重配置，管理端。
 *
 * <p>权限点 {@code recommendation:rule:manage} 只授予管理员与运营，普通用户拿不到。
 * 改动只对新推荐生效——历史推荐按各自快照里的权重解释，这是方案要求的"可复现"。
 */
@RestController
@RequestMapping("/api/admin/rec-weights")
@Validated
public class WeightConfigController {

    private final WeightConfigService weightConfigService;
    private final OperationPreviewService previewService;

    public WeightConfigController(WeightConfigService weightConfigService,
                                  OperationPreviewService previewService) {
        this.weightConfigService = weightConfigService;
        this.previewService = previewService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.RECOMMENDATION_RULE_MANAGE + "')")
    public R<WeightSet> current() {
        return R.ok(weightConfigService.current());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('" + Perms.RECOMMENDATION_RULE_MANAGE + "')")
    public R<Void> update(@Valid @RequestBody WeightConfigDTO dto) {
        weightConfigService.update(dto);
        return R.ok();
    }

    /**
     * 测试画像预览：临时权重只参与本次计算，不写配置、不存推荐快照。
     */
    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('" + Perms.RECOMMENDATION_RULE_MANAGE + "')")
    public R<RecommendationVO> preview(@Valid @RequestBody(required = false) WeightPreviewDTO dto) {
        return R.ok(previewService.preview(dto));
    }
}
