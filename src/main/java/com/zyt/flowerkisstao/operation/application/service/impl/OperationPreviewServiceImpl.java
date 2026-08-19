package com.zyt.flowerkisstao.operation.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSkuMapper;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSpeciesMapper;
import com.zyt.flowerkisstao.operation.application.service.OperationLogService;
import com.zyt.flowerkisstao.operation.application.service.OperationPreviewService;
import com.zyt.flowerkisstao.recommendation.application.service.WeightConfigService;
import com.zyt.flowerkisstao.recommendation.application.service.impl.RecommendationConverter;
import com.zyt.flowerkisstao.recommendation.domain.entity.RecResult;
import com.zyt.flowerkisstao.recommendation.domain.entity.RecResultItem;
import com.zyt.flowerkisstao.recommendation.domain.model.PlantCandidate;
import com.zyt.flowerkisstao.recommendation.domain.model.RecommendationOutcome;
import com.zyt.flowerkisstao.recommendation.domain.model.ScoredPlant;
import com.zyt.flowerkisstao.recommendation.domain.model.WeightSet;
import com.zyt.flowerkisstao.recommendation.domain.service.RecommendationEngine;
import com.zyt.flowerkisstao.recommendation.web.dto.WeightConfigDTO;
import com.zyt.flowerkisstao.recommendation.web.dto.WeightPreviewDTO;
import com.zyt.flowerkisstao.recommendation.web.vo.RecommendationVO;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import com.zyt.flowerkisstao.user.domain.entity.UserSceneProfile;
import com.zyt.flowerkisstao.user.infrastructure.mapper.UserSceneProfileMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 推荐权重测试预览。算法复用 RecommendationEngine，但不落任何业务表。 */
@Service
public class OperationPreviewServiceImpl implements OperationPreviewService {

    private final UserSceneProfileMapper profileMapper;
    private final CatalogSpeciesMapper speciesMapper;
    private final CatalogSkuMapper skuMapper;
    private final WeightConfigService weightConfigService;
    private final OperationLogService operationLogService;

    public OperationPreviewServiceImpl(UserSceneProfileMapper profileMapper,
                                       CatalogSpeciesMapper speciesMapper,
                                       CatalogSkuMapper skuMapper,
                                       WeightConfigService weightConfigService,
                                       OperationLogService operationLogService) {
        this.profileMapper = profileMapper;
        this.speciesMapper = speciesMapper;
        this.skuMapper = skuMapper;
        this.weightConfigService = weightConfigService;
        this.operationLogService = operationLogService;
    }

    @Override
    public RecommendationVO preview(WeightPreviewDTO dto) {
        Long profileId = dto == null ? null : dto.getProfileId();
        UserSceneProfile profile = resolveProfile(profileId);
        WeightSet weights = dto == null || dto.getWeights() == null
                ? weightConfigService.current() : toWeightSet(dto.getWeights());

        RecommendationOutcome outcome = RecommendationEngine.recommend(profile, loadCandidates(), weights);

        // 构造仅供 Converter 使用的内存对象：不 insert，不产生 resultId，不污染推荐历史
        RecResult pseudo = new RecResult();
        pseudo.setId(null);
        pseudo.setUserId(CurrentUser.requireUserId());
        pseudo.setProfileId(profile.getId());
        pseudo.setSceneNameSnapshot(profile.getSceneName());
        pseudo.setWeightSnapshot(weights.toSnapshot());
        pseudo.setTotalCount(outcome.totalCount());
        pseudo.setCandidateCount(outcome.candidateCount());
        pseudo.setFilteredJson(outcome.filtered());
        pseudo.setCreatedAt(LocalDateTime.now());

        List<RecResultItem> items = new ArrayList<>();
        Map<Long, CatalogSpecies> speciesById = new LinkedHashMap<>();
        Map<Long, CatalogSku> skuById = new LinkedHashMap<>();
        int rank = 1;
        for (ScoredPlant scored : outcome.items()) {
            RecResultItem item = new RecResultItem();
            item.setId(null);
            item.setSpeciesId(scored.species().getId());
            item.setSkuId(scored.sku() == null ? null : scored.sku().getId());
            item.setTotalScore(RecommendationConverter.scale2(scored.totalScore()));
            item.setScoreJson(scored.scores());
            item.setReasonsJson(scored.reasons());
            item.setRisk(scored.risk());
            item.setRankNo(rank++);
            items.add(item);
            speciesById.put(scored.species().getId(), scored.species());
            if (scored.sku() != null) skuById.put(scored.sku().getId(), scored.sku());
        }

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("profileId", profile.getId());
        after.put("sceneName", profile.getSceneName());
        after.put("weights", weights.toSnapshot());
        after.put("candidateCount", outcome.candidateCount());
        after.put("topSpeciesIds", outcome.items().stream()
                .map(item -> item.species().getId()).toList());
        operationLogService.record("recommendation", "PREVIEW", "scene_profile",
                profile.getId(), null, after);

        return RecommendationConverter.toVO(pseudo, items, speciesById, skuById,
                outcome.suggestions());
    }

    private UserSceneProfile resolveProfile(Long profileId) {
        if (profileId != null) {
            UserSceneProfile profile = profileMapper.selectById(profileId);
            if (profile == null) throw new BizException(ErrorCode.PROFILE_NOT_FOUND, "场景画像不存在");
            return profile;
        }
        // 运营预览可选择任意现有画像；不指定时优先 demo 默认画像，再取全库第一条
        UserSceneProfile profile = profileMapper.selectOne(Wrappers.<UserSceneProfile>lambdaQuery()
                .eq(UserSceneProfile::getIsDefault, 1)
                .orderByAsc(UserSceneProfile::getId)
                .last("LIMIT 1"));
        if (profile == null) {
            profile = profileMapper.selectOne(Wrappers.<UserSceneProfile>lambdaQuery()
                    .orderByAsc(UserSceneProfile::getId).last("LIMIT 1"));
        }
        if (profile == null) throw new BizException(ErrorCode.REC_NO_PROFILE, "没有可用于预览的场景画像");
        return profile;
    }

    private WeightSet toWeightSet(WeightConfigDTO dto) {
        if (dto.sum() != 100) {
            throw new BizException(ErrorCode.REC_WEIGHT_SUM_INVALID,
                    "七项权重之和必须等于 100，当前是 " + dto.sum());
        }
        return new WeightSet(dto.getLight(), dto.getTemp(), dto.getHumidity(), dto.getCare(),
                dto.getSpace(), dto.getBudget(), dto.getPreference(), dto.getTopN());
    }

    private List<PlantCandidate> loadCandidates() {
        List<CatalogSpecies> species = speciesMapper.selectList(Wrappers.<CatalogSpecies>lambdaQuery()
                .eq(CatalogSpecies::getStatus, 1));
        if (species.isEmpty()) return List.of();
        List<Long> ids = species.stream().map(CatalogSpecies::getId).toList();
        Map<Long, List<CatalogSku>> bySpecies = skuMapper.selectList(Wrappers.<CatalogSku>lambdaQuery()
                        .eq(CatalogSku::getStatus, 1)
                        .in(CatalogSku::getSpeciesId, ids)).stream()
                .collect(Collectors.groupingBy(CatalogSku::getSpeciesId));
        return species.stream()
                .filter(item -> !bySpecies.getOrDefault(item.getId(), List.of()).isEmpty())
                .map(item -> new PlantCandidate(item, bySpecies.get(item.getId())))
                .toList();
    }
}
