package com.zyt.flowerkisstao.recommendation;

import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.recommendation.domain.model.FilterRule;
import com.zyt.flowerkisstao.recommendation.domain.model.PlantCandidate;
import com.zyt.flowerkisstao.recommendation.domain.model.RecommendationOutcome;
import com.zyt.flowerkisstao.recommendation.domain.model.ScoreDimension;
import com.zyt.flowerkisstao.recommendation.domain.model.ScoredPlant;
import com.zyt.flowerkisstao.recommendation.domain.model.WeightSet;
import com.zyt.flowerkisstao.recommendation.domain.service.RecommendationEngine;
import com.zyt.flowerkisstao.user.domain.entity.UserSceneProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 打分引擎的单元测试。
 *
 * <p>不起 Spring 容器：引擎是纯函数，测它不需要数据库。这也正是把引擎独立出来的
 * 理由——算法出问题时能立刻和"接线出问题"区分开。
 */
class RecommendationEngineTest {

    // ================================================================
    // 硬过滤
    // ================================================================

    @Test
    @DisplayName("养猫时，对猫有毒的品种一定被排除，且计在宠物安全一栏")
    void excludesCatToxicWhenUserHasCat() {
        UserSceneProfile profile = baseProfile();
        profile.setHasCat(1);

        CatalogSpecies toxic = species(1L, "龟背竹");
        toxic.setToxicCat(1);
        CatalogSpecies safe = species(2L, "波士顿蕨");

        RecommendationOutcome outcome = RecommendationEngine.recommend(
                profile, List.of(candidate(toxic), candidate(safe)), WeightSet.DEFAULT);

        assertEquals(1, outcome.candidateCount());
        assertEquals("波士顿蕨", outcome.items().get(0).species().getName());
        assertEquals(1, outcome.filtered().get(FilterRule.TOXIC_PET.code()));
    }

    @Test
    @DisplayName("猫毒与狗毒分开判：只养狗时，对猫有毒的品种不该被排除")
    void catToxicityDoesNotAffectDogOwner() {
        UserSceneProfile profile = baseProfile();
        profile.setHasCat(0);
        profile.setHasDog(1);

        CatalogSpecies catOnly = species(1L, "龟背竹");
        catOnly.setToxicCat(1);

        RecommendationOutcome outcome = RecommendationEngine.recommend(
                profile, List.of(candidate(catOnly)), WeightSet.DEFAULT);

        assertEquals(1, outcome.candidateCount(),
                "猫毒不该影响只养狗的用户，否则毒性分三列存就没有意义了");
    }

    @Test
    @DisplayName("超出预算上限的品种出局；同品种的低价 SKU 仍能把它救回来")
    void budgetFiltersBySkuNotSpecies() {
        UserSceneProfile profile = baseProfile();
        profile.setBudgetMax(new BigDecimal("200.00"));

        CatalogSpecies expensive = species(1L, "琴叶榕");
        PlantCandidate allTooPricey = new PlantCandidate(expensive,
                List.of(sku(11L, "458.00"), sku(12L, "268.00")));

        CatalogSpecies mixed = species(2L, "散尾葵");
        PlantCandidate hasCheapOne = new PlantCandidate(mixed,
                List.of(sku(21L, "458.00"), sku(22L, "158.00")));

        RecommendationOutcome outcome = RecommendationEngine.recommend(
                profile, List.of(allTooPricey, hasCheapOne), WeightSet.DEFAULT);

        assertEquals(1, outcome.candidateCount());
        assertEquals("散尾葵", outcome.items().get(0).species().getName());
        assertEquals(1, outcome.filtered().get(FilterRule.BUDGET.code()));
    }

    @Test
    @DisplayName("经常出差时，浇水间隔短于 7 天的品种被排除")
    void excludesThirstyPlantsForFrequentTravelers() {
        UserSceneProfile profile = baseProfile();
        profile.setTravelFrequency(3);

        CatalogSpecies thirsty = species(1L, "网纹草");
        thirsty.setWaterIntervalDays(3);
        CatalogSpecies drought = species(2L, "虎尾兰");
        drought.setWaterIntervalDays(14);

        RecommendationOutcome outcome = RecommendationEngine.recommend(
                profile, List.of(candidate(thirsty), candidate(drought)), WeightSet.DEFAULT);

        assertEquals(1, outcome.candidateCount());
        assertEquals("虎尾兰", outcome.items().get(0).species().getName());
        assertEquals(1, outcome.filtered().get(FilterRule.WATER_FREQUENCY.code()));
    }

    // ================================================================
    // 权重
    // ================================================================

    @Test
    @DisplayName("光照权重调到 100 时，排序完全由光照贴合度决定")
    void lightWeightDominatesWhenSetToHundred() {
        UserSceneProfile profile = baseProfile();
        profile.setLightLevel(3);
        profile.setPreferOrnamental("flower");

        // 光照精确命中，但其余各维都很差：偏好不符、养护难度高于经验
        CatalogSpecies lightPerfect = species(1L, "光照满分株");
        lightPerfect.setLightMin(3);
        lightPerfect.setLightMax(3);
        lightPerfect.setOrnamentalType("leaf");
        lightPerfect.setCareLevel(3);
        lightPerfect.setTempMin(35);
        lightPerfect.setTempMax(40);

        // 光照勉强通过（区间宽），其余各维都很好
        CatalogSpecies allRounder = species(2L, "样样不错株");
        allRounder.setLightMin(1);
        allRounder.setLightMax(4);
        allRounder.setOrnamentalType("flower");
        allRounder.setCareLevel(1);

        List<PlantCandidate> candidates = List.of(candidate(lightPerfect), candidate(allRounder));

        // 默认权重下，样样不错的那株应当靠前
        RecommendationOutcome balanced = RecommendationEngine.recommend(
                profile, candidates, WeightSet.DEFAULT);
        assertEquals("样样不错株", balanced.items().get(0).species().getName(),
                "默认权重下，综合表现好的应当排前");

        // 光照 100 其余归 0：两株的光照分都是 100（都在区间内），此时应当靠
        // 同分排序的第一顺位——养护难度低者优先——分出胜负
        WeightSet lightOnly = new WeightSet(100, 0, 0, 0, 0, 0, 0, 3);
        RecommendationOutcome onlyLight = RecommendationEngine.recommend(
                profile, candidates, lightOnly);
        assertEquals(100.0, onlyLight.items().get(0).totalScore(), 0.001,
                "光照权重独占时，落在区间内的品种应当拿满分");
        assertEquals("样样不错株", onlyLight.items().get(0).species().getName(),
                "总分相同时按养护难度排序，难度 1 应排在难度 3 之前");
    }

    @Test
    @DisplayName("总分等于七项分项按权重的加权平均")
    void totalScoreMatchesWeightedAverageOfBreakdown() {
        UserSceneProfile profile = baseProfile();
        WeightSet weights = WeightSet.DEFAULT;

        ScoredPlant item = RecommendationEngine
                .recommend(profile, List.of(candidate(species(1L, "绿萝"))), weights)
                .items().get(0);

        double expected = 0;
        for (ScoreDimension dimension : ScoreDimension.values()) {
            expected += item.scores().get(dimension.code()) * dimension.weightIn(weights);
        }
        expected /= weights.sum();

        assertEquals(expected, item.totalScore(), 0.01,
                "总分与分项对不上的话，前端画出来的分项条就是在骗人");
    }

    // ================================================================
    // 排序
    // ================================================================

    @Test
    @DisplayName("同分时养护难度低者优先")
    void tiesBreakByCareLevel() {
        UserSceneProfile profile = baseProfile();
        // 让养护维不影响总分，否则难度差异会直接体现在分数上而非同分排序上
        WeightSet noCareWeight = new WeightSet(50, 15, 10, 0, 10, 10, 5, 3);

        CatalogSpecies hard = species(1L, "进阶株");
        hard.setCareLevel(3);
        CatalogSpecies easy = species(2L, "新手株");
        easy.setCareLevel(1);

        RecommendationOutcome outcome = RecommendationEngine.recommend(
                profile, List.of(candidate(hard), candidate(easy)), noCareWeight);

        assertEquals(outcome.items().get(0).totalScore(), outcome.items().get(1).totalScore(), 0.001,
                "前提：两株总分应当相同");
        assertEquals("新手株", outcome.items().get(0).species().getName());
    }

    @Test
    @DisplayName("只返回 topN 条，但候选数报的是过滤后的真实数量")
    void respectsTopN() {
        UserSceneProfile profile = baseProfile();
        List<PlantCandidate> five = List.of(
                candidate(species(1L, "甲")), candidate(species(2L, "乙")),
                candidate(species(3L, "丙")), candidate(species(4L, "丁")),
                candidate(species(5L, "戊")));

        RecommendationOutcome outcome = RecommendationEngine.recommend(
                profile, five, new WeightSet(30, 15, 10, 20, 10, 10, 5, 2));

        assertEquals(2, outcome.items().size());
        assertEquals(5, outcome.totalCount(), "totalCount 是参与筛选的总数，不受 topN 影响");
        assertEquals(5, outcome.candidateCount(),
                "候选数是硬过滤后的真实数量。取 items.size() 的话，"
                        + "前端就会把'筛出 5 株展示 2 株'说成'只筛出 2 株'");
        assertTrue(outcome.isFunnelConsistent());
    }

    @Test
    @DisplayName("有排除也有候选时，漏斗恒等式仍然成立")
    void funnelIsConsistentWhenBothExcludedAndSurvivorsExist() {
        UserSceneProfile profile = baseProfile();
        profile.setHasCat(1);

        CatalogSpecies toxicA = species(1L, "猫毒甲");
        toxicA.setToxicCat(1);
        CatalogSpecies toxicB = species(2L, "猫毒乙");
        toxicB.setToxicCat(1);
        CatalogSpecies needsSun = species(3L, "喜光株");
        needsSun.setLightMin(4);
        needsSun.setLightMax(4);

        RecommendationOutcome outcome = RecommendationEngine.recommend(
                profile,
                List.of(toxicA, toxicB, needsSun, species(4L, "甲"), species(5L, "乙"))
                        .stream().map(RecommendationEngineTest::candidate).toList(),
                new WeightSet(30, 15, 10, 20, 10, 10, 5, 1));

        assertEquals(5, outcome.totalCount());
        assertEquals(2, outcome.candidateCount(), "5 株减去 2 株猫毒、1 株光照不符");
        assertEquals(1, outcome.items().size(), "topN=1 只展示一条");
        assertTrue(outcome.isFunnelConsistent(),
                "总数应当等于候选数加各规则排除数之和：" + outcome.filtered());
    }

    // ================================================================
    // 无候选诊断
    // ================================================================

    @Test
    @DisplayName("无候选时，各规则排除数之和等于总数")
    void diagnosticCountsAreConserved() {
        UserSceneProfile profile = baseProfile();
        profile.setHasCat(1);
        profile.setBudgetMax(new BigDecimal("50.00"));
        profile.setLightLevel(1);

        CatalogSpecies toxic = species(1L, "有毒株");
        toxic.setToxicCat(1);
        CatalogSpecies pricey = species(2L, "昂贵株");
        CatalogSpecies needsSun = species(3L, "喜光株");
        needsSun.setLightMin(4);
        needsSun.setLightMax(4);

        RecommendationOutcome outcome = RecommendationEngine.recommend(
                profile, List.of(candidate(toxic), candidate(pricey), candidate(needsSun)),
                WeightSet.DEFAULT);

        assertEquals(0, outcome.candidateCount());
        int excludedTotal = outcome.filtered().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(outcome.totalCount() - outcome.candidateCount(), excludedTotal,
                "一株同时违反多条规则时若重复计数，诊断里的数字就对不上总数");
        assertTrue(outcome.isFunnelConsistent());
    }

    @Test
    @DisplayName("放宽建议里绝不出现安全约束")
    void neverSuggestsRelaxingSafetyConstraints() {
        UserSceneProfile profile = baseProfile();
        profile.setHasCat(1);
        profile.setHasChild(1);
        profile.setBudgetMax(new BigDecimal("50.00"));

        CatalogSpecies catToxic = species(1L, "猫毒株");
        catToxic.setToxicCat(1);
        CatalogSpecies childToxic = species(2L, "童毒株");
        childToxic.setToxicChild(1);
        CatalogSpecies pricey = species(3L, "昂贵株");

        RecommendationOutcome outcome = RecommendationEngine.recommend(
                profile, List.of(candidate(catToxic), candidate(childToxic), candidate(pricey)),
                WeightSet.DEFAULT);

        assertEquals(0, outcome.candidateCount());
        assertFalse(outcome.suggestions().isEmpty());
        for (String suggestion : outcome.suggestions()) {
            assertFalse(suggestion.contains("宠物") || suggestion.contains("有毒")
                            || suggestion.contains("儿童") || suggestion.contains("安全条件"),
                    "建议用户放宽毒性约束等于拿宠物或孩子的安全换一条推荐结果：" + suggestion);
        }
        assertTrue(outcome.suggestions().stream().anyMatch(s -> s.contains("预算")),
                "预算是可放宽的，应当出现在建议里");
    }

    @Test
    @DisplayName("全部因安全约束出局时，给出诚实的说明而非硬凑一条放宽建议")
    void allSafetyExcludedGivesHonestMessage() {
        UserSceneProfile profile = baseProfile();
        profile.setHasCat(1);

        CatalogSpecies toxic = species(1L, "猫毒株");
        toxic.setToxicCat(1);

        RecommendationOutcome outcome = RecommendationEngine.recommend(
                profile, List.of(candidate(toxic)), WeightSet.DEFAULT);

        assertEquals(0, outcome.candidateCount());
        assertEquals(1, outcome.suggestions().size());
        assertTrue(outcome.suggestions().get(0).contains("安全条件"));
    }

    @Test
    @DisplayName("有候选时不给放宽建议")
    void noSuggestionsWhenCandidatesExist() {
        RecommendationOutcome outcome = RecommendationEngine.recommend(
                baseProfile(), List.of(candidate(species(1L, "绿萝"))), WeightSet.DEFAULT);

        assertEquals(1, outcome.candidateCount());
        assertTrue(outcome.suggestions().isEmpty());
    }

    // ================================================================
    // 可复现性与理由
    // ================================================================

    @Test
    @DisplayName("同样的输入跑两次，结果完全一致")
    void isReproducible() {
        UserSceneProfile profile = baseProfile();
        // 五株除 id 与名字外完全一样，逼同分排序的兜底规则起作用
        List<PlantCandidate> identical = List.of(
                candidate(species(1L, "甲")), candidate(species(2L, "乙")),
                candidate(species(3L, "丙")), candidate(species(4L, "丁")),
                candidate(species(5L, "戊")));

        RecommendationOutcome first = RecommendationEngine.recommend(
                profile, identical, WeightSet.DEFAULT);
        RecommendationOutcome second = RecommendationEngine.recommend(
                profile, identical, WeightSet.DEFAULT);

        assertEquals(first.items().size(), second.items().size());
        for (int i = 0; i < first.items().size(); i++) {
            assertEquals(first.items().get(i).species().getId(), second.items().get(i).species().getId(),
                    "第 " + i + " 名不稳定——同分兜底没定死，可复现就是空话");
            assertEquals(first.items().get(i).totalScore(), second.items().get(i).totalScore(), 0.0);
            assertEquals(first.items().get(i).reasons(), second.items().get(i).reasons());
        }
    }

    @Test
    @DisplayName("每条结果都有三条理由，七维齐全")
    void everyItemHasReasonsAndFullBreakdown() {
        ScoredPlant item = RecommendationEngine
                .recommend(baseProfile(), List.of(candidate(species(1L, "绿萝"))), WeightSet.DEFAULT)
                .items().get(0);

        assertEquals(3, item.reasons().size());
        assertEquals(ScoreDimension.values().length, item.scores().size());
        for (ScoreDimension dimension : ScoreDimension.values()) {
            Integer score = item.scores().get(dimension.code());
            assertNotNull(score, dimension.code() + " 缺分项得分");
            assertTrue(score >= 0 && score <= 100, dimension.code() + " 分项得分越界：" + score);
        }
        assertNotNull(item.sku(), "每条结果都要挂一个 SKU，否则前端跳不到详情页");
    }

    @Test
    @DisplayName("七维都高分时不硬安风险提示")
    void noRiskWhenAllDimensionsAreStrong() {
        UserSceneProfile profile = baseProfile();
        profile.setPreferOrnamental("leaf");

        CatalogSpecies perfect = species(1L, "完美株");
        perfect.setOrnamentalType("leaf");
        perfect.setCareLevel(1);

        ScoredPlant item = RecommendationEngine
                .recommend(profile, List.of(candidate(perfect)), WeightSet.DEFAULT)
                .items().get(0);

        assertEquals(null, item.risk(), "没有短板却硬安一条风险，只会稀释真正的警告");
    }

    @Test
    @DisplayName("有明显短板时给出风险提示")
    void reportsRiskForWeakDimension() {
        UserSceneProfile profile = baseProfile();
        profile.setExperienceLevel(1);
        profile.setWaterTimesWeek(0);
        profile.setForgetful(1);
        profile.setAcceptRepot(0);
        profile.setAcceptFertilize(0);
        profile.setAcceptPrune(0);

        CatalogSpecies demanding = species(1L, "娇气株");
        demanding.setCareLevel(3);
        demanding.setWaterIntervalDays(3);
        demanding.setPruneNeeded(1);

        ScoredPlant item = RecommendationEngine
                .recommend(profile, List.of(candidate(demanding)), WeightSet.DEFAULT)
                .items().get(0);

        assertTrue(item.scores().get(ScoreDimension.CARE.code()) < 60,
                "新手 + 从不浇水 + 健忘 + 什么都不接受，配上娇气品种，养护分应当很低");
        assertNotNull(item.risk());
    }

    // ================================================================
    // 夹具
    // ================================================================

    /** 一份宽松的画像：不养宠物、无幼童、明亮散射光、落地空间、预算 0-300 */
    private static UserSceneProfile baseProfile() {
        UserSceneProfile profile = new UserSceneProfile();
        profile.setId(1L);
        profile.setUserId(1L);
        profile.setSceneName("客厅");
        profile.setPlacement("living_room");
        profile.setLightLevel(3);
        profile.setTempLevel(2);
        profile.setHumidityLevel(2);
        profile.setSpaceLevel(3);
        profile.setMaxFootprintCm(150);
        profile.setVentilation(2);
        profile.setBudgetMin(BigDecimal.ZERO);
        profile.setBudgetMax(new BigDecimal("300.00"));
        profile.setPreferOrnamental("any");
        profile.setHasChild(0);
        profile.setHasCat(0);
        profile.setHasDog(0);
        profile.setExperienceLevel(2);
        profile.setWaterTimesWeek(3);
        profile.setTravelFrequency(1);
        profile.setForgetful(0);
        profile.setAcceptRepot(1);
        profile.setAcceptFertilize(1);
        profile.setAcceptPrune(1);
        return profile;
    }

    /** 一株各方面都无可挑剔的品种，测试按需覆盖某几个字段 */
    private static CatalogSpecies species(Long id, String name) {
        CatalogSpecies species = new CatalogSpecies();
        species.setId(id);
        species.setCode("test-" + id);
        species.setName(name);
        species.setOrnamentalType("leaf");
        species.setLightMin(2);
        species.setLightMax(4);
        species.setLightNote("明亮散射光");
        species.setTempMin(15);
        species.setTempMax(28);
        species.setHumidityMin(40);
        species.setHumidityMax(70);
        species.setWaterIntervalDays(7);
        species.setWaterNote("表土干透再浇");
        species.setCareLevel(1);
        species.setToxicCat(0);
        species.setToxicDog(0);
        species.setToxicChild(0);
        species.setPruneNeeded(0);
        species.setFootprintCm(80);
        species.setStatus(1);
        return species;
    }

    private static PlantCandidate candidate(CatalogSpecies species) {
        return new PlantCandidate(species, List.of(sku(species.getId() * 10, "128.00")));
    }

    private static CatalogSku sku(Long id, String price) {
        CatalogSku sku = new CatalogSku();
        sku.setId(id);
        sku.setSkuCode("sku-" + id);
        sku.setPrice(new BigDecimal(price));
        sku.setStock(10);
        sku.setStatus(1);
        return sku;
    }
}
