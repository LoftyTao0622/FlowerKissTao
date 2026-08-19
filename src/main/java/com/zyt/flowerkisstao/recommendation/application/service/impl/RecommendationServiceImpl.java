package com.zyt.flowerkisstao.recommendation.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSkuMapper;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSpeciesMapper;
import com.zyt.flowerkisstao.recommendation.application.service.RecommendationService;
import com.zyt.flowerkisstao.recommendation.application.service.WeightConfigService;
import com.zyt.flowerkisstao.recommendation.domain.entity.RecResult;
import com.zyt.flowerkisstao.recommendation.domain.entity.RecResultItem;
import com.zyt.flowerkisstao.recommendation.domain.model.PlantCandidate;
import com.zyt.flowerkisstao.recommendation.domain.model.RecommendationOutcome;
import com.zyt.flowerkisstao.recommendation.domain.model.ScoredPlant;
import com.zyt.flowerkisstao.recommendation.domain.model.WeightSet;
import com.zyt.flowerkisstao.recommendation.domain.service.RecommendationEngine;
import com.zyt.flowerkisstao.recommendation.infrastructure.mapper.RecResultItemMapper;
import com.zyt.flowerkisstao.recommendation.infrastructure.mapper.RecResultMapper;
import com.zyt.flowerkisstao.recommendation.web.vo.RecommendationVO;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import com.zyt.flowerkisstao.user.domain.entity.UserSceneProfile;
import com.zyt.flowerkisstao.user.infrastructure.mapper.UserSceneProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 推荐的编排层：读画像 → 读候选 → 调引擎 → 存快照 → 拼展示对象。
 *
 * <p>算法一行都不在这里。它在 {@link RecommendationEngine}——那是个不依赖 Spring
 * 的纯函数，可以脱离容器单测。这样分层的实际收益是：推荐结果不对时，先跑单测，
 * 绿了就说明是取数或落库的问题，红了就是算法的问题，不必两头猜。
 */
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final UserSceneProfileMapper profileMapper;
    private final CatalogSpeciesMapper speciesMapper;
    private final CatalogSkuMapper skuMapper;
    private final RecResultMapper resultMapper;
    private final RecResultItemMapper itemMapper;
    private final WeightConfigService weightConfigService;

    public RecommendationServiceImpl(UserSceneProfileMapper profileMapper,
                                     CatalogSpeciesMapper speciesMapper,
                                     CatalogSkuMapper skuMapper,
                                     RecResultMapper resultMapper,
                                     RecResultItemMapper itemMapper,
                                     WeightConfigService weightConfigService) {
        this.profileMapper = profileMapper;
        this.speciesMapper = speciesMapper;
        this.skuMapper = skuMapper;
        this.resultMapper = resultMapper;
        this.itemMapper = itemMapper;
        this.weightConfigService = weightConfigService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecommendationVO generate(Long profileId) {
        Long userId = CurrentUser.requireUserId();
        UserSceneProfile profile = resolveProfile(userId, profileId);
        WeightSet weights = weightConfigService.current();

        List<PlantCandidate> candidates = loadCandidates();
        RecommendationOutcome outcome = RecommendationEngine.recommend(profile, candidates, weights);

        RecResult result = new RecResult();
        result.setUserId(userId);
        result.setProfileId(profile.getId());
        // 存当时的场景名：画像改名或删除后，历史记录仍显示当时那个名字
        result.setSceneNameSnapshot(profile.getSceneName());
        result.setWeightSnapshot(weights.toSnapshot());
        result.setTotalCount(outcome.totalCount());
        result.setCandidateCount(outcome.candidateCount());
        result.setFilteredJson(outcome.filtered());
        resultMapper.insert(result);

        List<RecResultItem> items = new ArrayList<>();
        int rank = 1;
        for (ScoredPlant scored : outcome.items()) {
            RecResultItem item = new RecResultItem();
            item.setResultId(result.getId());
            item.setSpeciesId(scored.species().getId());
            item.setSkuId(scored.sku() == null ? null : scored.sku().getId());
            item.setTotalScore(RecommendationConverter.scale2(scored.totalScore()));
            item.setScoreJson(scored.scores());
            item.setReasonsJson(scored.reasons());
            item.setRisk(scored.risk());
            item.setRankNo(rank++);
            item.setClicked(0);
            itemMapper.insert(item);
            items.add(item);
        }

        return RecommendationConverter.toVO(result, items,
                indexSpecies(outcome), indexSku(outcome), outcome.suggestions());
    }

    @Override
    public RecommendationVO latest() {
        Long userId = CurrentUser.requireUserId();
        RecResult result = resultMapper.selectOne(Wrappers.<RecResult>lambdaQuery()
                .eq(RecResult::getUserId, userId)
                .orderByDesc(RecResult::getCreatedAt)
                .orderByDesc(RecResult::getId)
                .last("LIMIT 1"));
        // 从没推荐过：返回 null 由前端引导去填问卷，而不是造一份空结果
        return result == null ? null : assemble(result);
    }

    @Override
    public RecommendationVO get(Long resultId) {
        return assemble(requireOwnedResult(resultId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markClicked(Long resultId, Long itemId) {
        // 先校归属，否则任何人都能给别人的推荐刷点击，第⑦步看板的数据就废了
        requireOwnedResult(resultId);

        RecResultItem item = itemMapper.selectById(itemId);
        if (item == null || !item.getResultId().equals(resultId)) {
            throw new BizException(ErrorCode.REC_RESULT_NOT_FOUND, "推荐记录不存在");
        }
        if (item.getClicked() != null && item.getClicked() == 1) {
            // 已点过就不重复写，点击率统计的是"有多少条被点开过"而非点击次数
            return;
        }
        RecResultItem update = new RecResultItem();
        update.setId(itemId);
        update.setClicked(1);
        itemMapper.updateById(update);
    }

    // ================================================================
    // 取数
    // ================================================================

    /**
     * 取候选：所有上架品种，各自挂上在售 SKU。
     *
     * <p>两次查询而不是逐个品种查 SKU：18 个品种就是 19 次查询，数据量再大些
     * 就成了典型的 N+1。这里一次拿全，在内存里按 speciesId 分组。
     *
     * <p>无在售 SKU 的品种不进候选——推荐一株买不到的植物没有意义。
     */
    private List<PlantCandidate> loadCandidates() {
        List<CatalogSpecies> speciesList = speciesMapper.selectList(
                Wrappers.<CatalogSpecies>lambdaQuery().eq(CatalogSpecies::getStatus, 1));
        if (speciesList.isEmpty()) {
            return List.of();
        }

        List<Long> speciesIds = speciesList.stream().map(CatalogSpecies::getId).toList();
        List<CatalogSku> skus = skuMapper.selectList(Wrappers.<CatalogSku>lambdaQuery()
                .eq(CatalogSku::getStatus, 1)
                .in(CatalogSku::getSpeciesId, speciesIds));
        Map<Long, List<CatalogSku>> skusBySpecies = skus.stream()
                .collect(Collectors.groupingBy(CatalogSku::getSpeciesId));

        List<PlantCandidate> candidates = new ArrayList<>();
        for (CatalogSpecies species : speciesList) {
            List<CatalogSku> own = skusBySpecies.getOrDefault(species.getId(), List.of());
            if (!own.isEmpty()) {
                candidates.add(new PlantCandidate(species, own));
            }
        }
        return candidates;
    }

    /**
     * 定位要用的画像。
     *
     * <p>不传 id 时取默认场景；没有任何画像时抛 5003，让前端引导去填问卷——
     * 方案原文："缺失关键条件时引导补充问卷"。
     */
    private UserSceneProfile resolveProfile(Long userId, Long profileId) {
        if (profileId != null) {
            UserSceneProfile profile = profileMapper.selectById(profileId);
            // 越权一律按"不存在"处理：返回 403 等于告诉对方这个 id 确实存在，
            // 只是不属于他。与画像模块口径一致
            if (profile == null || !profile.getUserId().equals(userId)) {
                throw new BizException(ErrorCode.PROFILE_NOT_FOUND, "场景画像不存在");
            }
            return profile;
        }

        UserSceneProfile preferred = profileMapper.selectOne(Wrappers.<UserSceneProfile>lambdaQuery()
                .eq(UserSceneProfile::getUserId, userId)
                .eq(UserSceneProfile::getIsDefault, 1)
                .last("LIMIT 1"));
        if (preferred != null) {
            return preferred;
        }
        // 有画像但没标默认（历史数据可能这样），退回最早的一份
        UserSceneProfile fallback = profileMapper.selectOne(Wrappers.<UserSceneProfile>lambdaQuery()
                .eq(UserSceneProfile::getUserId, userId)
                .orderByAsc(UserSceneProfile::getId)
                .last("LIMIT 1"));
        if (fallback == null) {
            throw new BizException(ErrorCode.REC_NO_PROFILE,
                    "还没有填写环境问卷，先告诉我们你的摆放环境才能推荐");
        }
        return fallback;
    }

    private RecResult requireOwnedResult(Long resultId) {
        Long userId = CurrentUser.requireUserId();
        RecResult result = resultMapper.selectById(resultId);
        if (result == null || !result.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.REC_RESULT_NOT_FOUND, "推荐记录不存在");
        }
        return result;
    }

    /** 由快照重建展示对象。商品信息按当前数据补，分数与理由一律取快照里的 */
    private RecommendationVO assemble(RecResult result) {
        List<RecResultItem> items = itemMapper.selectList(Wrappers.<RecResultItem>lambdaQuery()
                .eq(RecResultItem::getResultId, result.getId())
                .orderByAsc(RecResultItem::getRankNo));

        Map<Long, CatalogSpecies> speciesById = Collections.emptyMap();
        Map<Long, CatalogSku> skuById = Collections.emptyMap();
        if (!items.isEmpty()) {
            List<Long> speciesIds = items.stream().map(RecResultItem::getSpeciesId).distinct().toList();
            speciesById = speciesMapper.selectBatchIds(speciesIds).stream()
                    .collect(Collectors.toMap(CatalogSpecies::getId, Function.identity()));

            List<Long> skuIds = items.stream()
                    .map(RecResultItem::getSkuId).filter(java.util.Objects::nonNull).distinct().toList();
            if (!skuIds.isEmpty()) {
                skuById = skuMapper.selectBatchIds(skuIds).stream()
                        .collect(Collectors.toMap(CatalogSku::getId, Function.identity()));
            }
        }

        // 放宽建议不入库——它完全由 filtered_json 决定，存下来只会多一份
        // 可能与漏斗对不上的冗余。读的时候按同一套规则重算
        List<String> suggestions = items.isEmpty()
                ? RecommendationEngine.suggestionsFrom(result.getFilteredJson())
                : List.of();

        return RecommendationConverter.toVO(result, items, speciesById, skuById, suggestions);
    }

    private Map<Long, CatalogSpecies> indexSpecies(RecommendationOutcome outcome) {
        return outcome.items().stream()
                .collect(Collectors.toMap(p -> p.species().getId(), ScoredPlant::species,
                        (a, b) -> a));
    }

    private Map<Long, CatalogSku> indexSku(RecommendationOutcome outcome) {
        return outcome.items().stream()
                .filter(p -> p.sku() != null)
                .collect(Collectors.toMap(p -> p.sku().getId(), ScoredPlant::sku, (a, b) -> a));
    }
}
