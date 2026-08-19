package com.zyt.flowerkisstao.recommendation.domain.service;

import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSku;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.recommendation.domain.model.FilterRule;
import com.zyt.flowerkisstao.recommendation.domain.model.PlantCandidate;
import com.zyt.flowerkisstao.recommendation.domain.model.RecommendationOutcome;
import com.zyt.flowerkisstao.recommendation.domain.model.ScoreDimension;
import com.zyt.flowerkisstao.recommendation.domain.model.ScoredPlant;
import com.zyt.flowerkisstao.recommendation.domain.model.WeightSet;
import com.zyt.flowerkisstao.user.domain.entity.UserSceneProfile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 环境适配推荐的打分引擎。
 *
 * <p><b>这个类刻意不依赖 Spring，也不碰数据库。</b>入参是画像 + 候选 + 权重，
 * 出参是排序结果，同样的输入必然得到同样的输出。这样做有两个好处：单元测试
 * 不必起容器就能覆盖算法本身；出问题时能立刻分清是算法错了还是接线错了。
 * 方案原文要求推荐"可复现、便于测试"，落点就在这里。
 *
 * <p>整体分两段，与方案原文的描述同构：
 * <ol>
 *   <li><b>硬过滤</b>——安全与物理条件不满足的直接出局，并记录每条规则各排除了多少株</li>
 *   <li><b>加权评分</b>——对活下来的候选按七维打分，"预算等软评分仅用于合格候选之间的排序"</li>
 * </ol>
 *
 * <p>硬过滤与软评分的边界是有意为之：预算超标是硬出局（买不起就是买不起），
 * 但预算贴合度只在合格候选之间比较——不能因为一株便宜就把它排到环境更匹配的
 * 那株前面去。
 */
public final class RecommendationEngine {

    // ===== 画像档位到代表值的换算 =====
    // 用户答的是"偏冷/常温/偏热"，品种存的是 temp_min/temp_max 的摄氏度区间。
    // 两者要比较，必须先把档位换算成一个代表温度。取各档中位数。

    /** 温度档位 1/2/3 对应的代表温度（℃），下标 0 占位 */
    private static final int[] TEMP_OF_LEVEL = {0, 12, 22, 32};

    /** 湿度档位 1/2/3 对应的代表湿度（%），下标 0 占位 */
    private static final int[] HUMIDITY_OF_LEVEL = {0, 30, 55, 80};

    /** 光照每偏离一级扣的分。偏两级即 100-70=30 分，仍留一点余地给权重去博弈 */
    private static final int LIGHT_PENALTY_PER_LEVEL = 35;

    /** 温度每偏离一度扣的分 */
    private static final double TEMP_PENALTY_PER_DEGREE = 6.0;

    /** 湿度每偏离一个百分点扣的分 */
    private static final double HUMIDITY_PENALTY_PER_PERCENT = 2.0;

    /** 出差频繁的判定档位，与 user_scene_profile.travel_frequency 同刻度 */
    private static final int TRAVEL_FREQUENT = 3;

    /** 出差频繁时不能接受的浇水间隔下限（天）。低于它意味着人不在家植物就渴死 */
    private static final int MIN_INTERVAL_WHEN_TRAVELING = 7;

    /** 理由取前几条。七条全给等于没有重点 */
    private static final int REASON_COUNT = 3;

    /** 低于这个分的维度才值得提示风险。都高于它说明没有明显短板 */
    private static final int RISK_THRESHOLD = 60;

    private RecommendationEngine() {
    }

    /**
     * 跑一次完整推荐。
     *
     * @param profile    用户的场景画像，推荐的输入端
     * @param candidates 在售品种连同其 SKU
     * @param weights    七维权重与 topN
     * @return 排序结果与漏斗诊断；无候选时 items 为空但 filtered 完整
     */
    public static RecommendationOutcome recommend(UserSceneProfile profile,
                                                  List<PlantCandidate> candidates,
                                                  WeightSet weights) {
        Map<FilterRule, Integer> excluded = new EnumMap<>(FilterRule.class);
        for (FilterRule rule : FilterRule.values()) {
            excluded.put(rule, 0);
        }

        List<PlantCandidate> survivors = new ArrayList<>();
        for (PlantCandidate candidate : candidates) {
            FilterRule rejectedBy = firstFailingRule(profile, candidate);
            if (rejectedBy == null) {
                survivors.add(candidate);
            } else {
                // 只记第一条命中的规则。一株植物同时违反毒性和预算时算两次的话，
                // 各项之和就会大于"被排除的总数"，诊断里的数字对不上。
                excluded.merge(rejectedBy, 1, Integer::sum);
            }
        }

        List<ScoredPlant> scored = new ArrayList<>();
        for (PlantCandidate candidate : survivors) {
            scored.add(score(profile, candidate, weights));
        }
        scored.sort(rankingOrder(profile));

        List<ScoredPlant> top = scored.size() > weights.topN()
                ? new ArrayList<>(scored.subList(0, weights.topN()))
                : scored;

        return new RecommendationOutcome(
                top,
                candidates.size(),
                // 候选数取 survivors 而非 top：后者被 topN 截断过。诊断要说的是
                // "筛出来 7 株，展示前 3 株"，用 top.size() 会让漏斗对不上总数
                survivors.size(),
                toCodeMap(excluded),
                survivors.isEmpty() ? relaxSuggestions(excluded) : List.of());
    }

    // ================================================================
    // 硬过滤
    // ================================================================

    /**
     * 返回第一条把这株植物挡下来的规则，全部通过则返回 null。
     *
     * <p>顺序不是随意的：安全约束排在最前，这样诊断里"因安全原因排除"的计数
     * 不会被后面的预算规则抢走——用户看到"6 株因预算被排除"却其中 4 株本来
     * 就对猫有毒，那个建议就是错的。
     */
    private static FilterRule firstFailingRule(UserSceneProfile profile, PlantCandidate candidate) {
        CatalogSpecies species = candidate.species();

        // 1. 宠物毒性。猫毒与狗毒不是同一批植物，所以分开判
        if ((isYes(profile.getHasCat()) && isYes(species.getToxicCat()))
                || (isYes(profile.getHasDog()) && isYes(species.getToxicDog()))) {
            return FilterRule.TOXIC_PET;
        }

        // 2. 儿童误食
        if (isYes(profile.getHasChild()) && isYes(species.getToxicChild())) {
            return FilterRule.TOXIC_CHILD;
        }

        // 3. 光照：等级必须落在品种可接受区间内
        int light = profile.getLightLevel();
        if (light < species.getLightMin() || light > species.getLightMax()) {
            return FilterRule.LIGHT;
        }

        // 4. 空间：冠幅不能超过档位上限
        if (species.getFootprintCm() != null
                && species.getFootprintCm() > profile.getMaxFootprintCm()) {
            return FilterRule.SPACE;
        }

        // 5. 预算：至少要有一个 SKU 买得起。注意判的是 SKU 而非品种——
        //    同一株植物的小规格进得来、大规格进不来是常态
        if (affordableSkus(profile, candidate).isEmpty()) {
            return FilterRule.BUDGET;
        }

        // 6. 出差频繁时排除浇水太勤的品种。人不在家，三天一浇的植物必死
        if (profile.getTravelFrequency() != null
                && profile.getTravelFrequency() >= TRAVEL_FREQUENT
                && species.getWaterIntervalDays() != null
                && species.getWaterIntervalDays() < MIN_INTERVAL_WHEN_TRAVELING) {
            return FilterRule.WATER_FREQUENCY;
        }

        return null;
    }

    /** 预算内的 SKU。预算下限只用于打分，不参与硬过滤——便宜从来不是出局的理由 */
    private static List<CatalogSku> affordableSkus(UserSceneProfile profile, PlantCandidate candidate) {
        BigDecimal max = profile.getBudgetMax();
        return candidate.skus().stream()
                .filter(sku -> sku.getPrice() != null && sku.getPrice().compareTo(max) <= 0)
                .toList();
    }

    // ================================================================
    // 七维评分
    // ================================================================

    private static ScoredPlant score(UserSceneProfile profile, PlantCandidate candidate, WeightSet weights) {
        CatalogSpecies species = candidate.species();
        CatalogSku sku = pickBestSku(profile, candidate);

        Map<ScoreDimension, Integer> dimensionScores = new EnumMap<>(ScoreDimension.class);
        dimensionScores.put(ScoreDimension.LIGHT, lightScore(profile, species));
        dimensionScores.put(ScoreDimension.TEMP, tempScore(profile, species));
        dimensionScores.put(ScoreDimension.HUMIDITY, humidityScore(profile, species));
        dimensionScores.put(ScoreDimension.CARE, careScore(profile, species));
        dimensionScores.put(ScoreDimension.SPACE, spaceScore(profile, species));
        dimensionScores.put(ScoreDimension.BUDGET, budgetScore(profile, sku));
        dimensionScores.put(ScoreDimension.PREFERENCE, preferenceScore(profile, species));

        double weighted = 0;
        for (Map.Entry<ScoreDimension, Integer> entry : dimensionScores.entrySet()) {
            weighted += entry.getValue() * entry.getKey().weightIn(weights);
        }
        // 除以权重实际之和而非写死的 100：管理端预览可能传一组和不为 100 的权重，
        // 那时仍应落在 0-100 刻度上，否则前端进度条会溢出
        int weightTotal = weights.sum();
        double total = weightTotal == 0 ? 0 : weighted / weightTotal;

        return new ScoredPlant(
                species,
                sku,
                round2(total),
                toCodeMap2(dimensionScores),
                buildReasons(dimensionScores, species),
                buildRisk(dimensionScores, species));
    }

    /**
     * 光照。落在区间内满分，每偏离一级扣 {@link #LIGHT_PENALTY_PER_LEVEL}。
     *
     * <p>实际上偏离的品种在硬过滤阶段就出局了，这里的衰减分支只在管理端预览
     * 或将来放宽光照约束时才走到。留着是为了让这一维的语义完整。
     */
    private static int lightScore(UserSceneProfile profile, CatalogSpecies species) {
        int light = profile.getLightLevel();
        int deviation = 0;
        if (light < species.getLightMin()) {
            deviation = species.getLightMin() - light;
        } else if (light > species.getLightMax()) {
            deviation = light - species.getLightMax();
        }
        return clamp(100 - deviation * LIGHT_PENALTY_PER_LEVEL);
    }

    /** 温度。把画像档位换算成代表温度再与品种区间比，偏离按度数线性衰减 */
    private static int tempScore(UserSceneProfile profile, CatalogSpecies species) {
        if (species.getTempMin() == null || species.getTempMax() == null) {
            // 品种没填温度区间时给中性分，不因数据缺失就判它不合适
            return 80;
        }
        int actual = TEMP_OF_LEVEL[clampLevel(profile.getTempLevel(), TEMP_OF_LEVEL.length - 1)];
        return rangeScore(actual, species.getTempMin(), species.getTempMax(), TEMP_PENALTY_PER_DEGREE);
    }

    private static int humidityScore(UserSceneProfile profile, CatalogSpecies species) {
        if (species.getHumidityMin() == null || species.getHumidityMax() == null) {
            return 80;
        }
        int actual = HUMIDITY_OF_LEVEL[clampLevel(profile.getHumidityLevel(), HUMIDITY_OF_LEVEL.length - 1)];
        return rangeScore(actual, species.getHumidityMin(), species.getHumidityMax(),
                HUMIDITY_PENALTY_PER_PERCENT);
    }

    /** 落在 [min,max] 内满分，否则按偏离量乘以每单位罚分线性衰减 */
    private static int rangeScore(int actual, int min, int max, double penaltyPerUnit) {
        if (actual >= min && actual <= max) {
            return 100;
        }
        int deviation = actual < min ? min - actual : actual - max;
        return clamp((int) Math.round(100 - deviation * penaltyPerUnit));
    }

    /**
     * 养护能力，七维里最复杂的一维——它要回答"这个人养不养得活"。
     *
     * <p>从满分往下扣，每一项对应画像里一个真实约束：
     * <ul>
     *   <li><b>难度超出经验</b>：新手碰进阶养护品种，扣得最狠</li>
     *   <li><b>浇水频率不匹配</b>：品种要求的频次高于用户每周能浇的次数</li>
     *   <li><b>健忘</b>：浇水间隔越短，忘一次的代价越大</li>
     *   <li><b>不接受修剪/换盆/施肥</b>：品种需要而用户不愿意做</li>
     * </ul>
     */
    private static int careScore(UserSceneProfile profile, CatalogSpecies species) {
        int score = 100;

        // 养护难度 1-3 对经验 1-3。难度高于经验时按差值扣，每级 20 分
        int careLevel = species.getCareLevel() == null
                ? CatalogSpecies.CARE_EASY : species.getCareLevel();
        int gap = careLevel - profile.getExperienceLevel();
        if (gap > 0) {
            score -= gap * 20;
        }

        Integer intervalDays = species.getWaterIntervalDays();
        if (intervalDays != null && intervalDays > 0) {
            // 品种每周需要浇几次。间隔 3 天即每周约 2.3 次
            double neededPerWeek = 7.0 / intervalDays;
            int canDoPerWeek = profile.getWaterTimesWeek() == null ? 0 : profile.getWaterTimesWeek();
            if (neededPerWeek > canDoPerWeek) {
                // 差半次扣 8 分，差两次扣 32 分
                score -= (int) Math.round((neededPerWeek - canDoPerWeek) * 16);
            }
            // 健忘的人配上勤浇水的植物是最常见的养死组合
            if (isYes(profile.getForgetful()) && intervalDays < MIN_INTERVAL_WHEN_TRAVELING) {
                score -= 15;
            }
        }

        if (isYes(species.getPruneNeeded()) && !isYes(profile.getAcceptPrune())) {
            score -= 10;
        }
        // 换盆与施肥每个品种都要，只在用户明确不接受时扣
        if (!isYes(profile.getAcceptRepot())) {
            score -= 5;
        }
        if (!isYes(profile.getAcceptFertilize())) {
            score -= 5;
        }

        return clamp(score);
    }

    /**
     * 空间贴合度。
     *
     * <p>不是"越小越好"：桌面档位上摆一株冠幅 5 厘米的多肉确实放得下，但会显得
     * 空荡。理想是占到上限的 40%-90%，太小也略扣。
     */
    private static int spaceScore(UserSceneProfile profile, CatalogSpecies species) {
        if (species.getFootprintCm() == null || profile.getMaxFootprintCm() == null
                || profile.getMaxFootprintCm() <= 0) {
            return 80;
        }
        double ratio = (double) species.getFootprintCm() / profile.getMaxFootprintCm();
        if (ratio > 1.0) {
            // 硬过滤已排除，这里只在放宽约束时走到
            return clamp((int) Math.round(100 - (ratio - 1.0) * 200));
        }
        if (ratio >= 0.4) {
            return 100;
        }
        // 0.4 以下线性衰减到 70 分：放得下但撑不起空间
        return clamp((int) Math.round(70 + ratio / 0.4 * 30));
    }

    /**
     * 预算贴合度。价格落在 [budgetMin, budgetMax] 内满分，低于下限按差距衰减。
     *
     * <p>方案原文："预算等软评分仅用于合格候选之间的排序"——所以超预算的
     * 在硬过滤就出局了，这里不会出现价格高于上限的入参。
     */
    private static int budgetScore(UserSceneProfile profile, CatalogSku sku) {
        if (sku == null || sku.getPrice() == null) {
            return 60;
        }
        BigDecimal price = sku.getPrice();
        BigDecimal min = profile.getBudgetMin() == null ? BigDecimal.ZERO : profile.getBudgetMin();
        if (price.compareTo(min) >= 0) {
            return 100;
        }
        // 低于预算下限：用户可能觉得"太便宜是不是不好"，但这不是硬伤，
        // 最多扣到 75 分。差距按下限的比例算
        if (min.compareTo(BigDecimal.ZERO) <= 0) {
            return 100;
        }
        double shortfall = min.subtract(price).doubleValue() / min.doubleValue();
        return clamp((int) Math.round(100 - shortfall * 25));
    }

    /** 观赏偏好。一致满分，用户选"都行"给 80——不该和精确命中拿一样的分 */
    private static int preferenceScore(UserSceneProfile profile, CatalogSpecies species) {
        String prefer = profile.getPreferOrnamental();
        if (prefer == null || "any".equals(prefer)) {
            return 80;
        }
        return prefer.equals(species.getOrnamentalType()) ? 100 : 50;
    }

    /**
     * 挑一个 SKU 代表这个品种。
     *
     * <p>取预算内价格最高的那个：同一株植物，预算允许的前提下用户通常想要更大
     * 更成熟的规格。预算内一个都没有时（只会发生在放宽约束的场景）退回最便宜的。
     */
    private static CatalogSku pickBestSku(UserSceneProfile profile, PlantCandidate candidate) {
        List<CatalogSku> affordable = affordableSkus(profile, candidate);
        if (!affordable.isEmpty()) {
            return affordable.stream()
                    .max(Comparator.comparing(CatalogSku::getPrice))
                    .orElseThrow();
        }
        return candidate.skus().stream()
                .min(Comparator.comparing(CatalogSku::getPrice))
                .orElse(null);
    }

    // ================================================================
    // 排序、理由、诊断
    // ================================================================

    /**
     * 排序规则。方案原文："同分结果再按养护难度、价格贴合度和库存状态排序"。
     *
     * <p>末位加了 speciesId 兜底：前四项全都相同时若不定序，不同 JDK 的排序实现
     * 可能给出不同顺序，"同样输入必得同样输出"就不成立了。
     */
    private static Comparator<ScoredPlant> rankingOrder(UserSceneProfile profile) {
        return Comparator
                .comparingDouble(ScoredPlant::totalScore).reversed()
                .thenComparingInt((ScoredPlant p) -> p.species().getCareLevel() == null
                        ? CatalogSpecies.CARE_HARD : p.species().getCareLevel())
                .thenComparing(Comparator.comparingInt(
                        (ScoredPlant p) -> p.scores().getOrDefault(ScoreDimension.BUDGET.code(), 0)).reversed())
                .thenComparing(Comparator.comparingInt(
                        (ScoredPlant p) -> p.sku() == null || p.sku().getStock() == null
                                ? 0 : p.sku().getStock()).reversed())
                .thenComparing(p -> p.species().getId());
    }

    /**
     * 生成推荐理由：取得分最高的三维各一句。
     *
     * <p>七维全输出等于没有重点。用户想知道的是"为什么推它给我"，不是一份评分表。
     */
    private static List<String> buildReasons(Map<ScoreDimension, Integer> scores, CatalogSpecies species) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<ScoreDimension, Integer>comparingByValue().reversed()
                        // 同分时按枚举声明序，保证可复现
                        .thenComparing(e -> e.getKey().ordinal()))
                .limit(REASON_COUNT)
                .map(e -> reasonText(e.getKey(), e.getValue(), species))
                .toList();
    }

    private static String reasonText(ScoreDimension dimension, int score, CatalogSpecies species) {
        return switch (dimension) {
            case LIGHT -> score >= 90
                    ? "光照条件正合适：它需要" + noteOr(species.getLightNote(), "散射光") + "，与你的环境一致"
                    : "光照大致可行，注意留意叶片状态";
            case TEMP -> score >= 90
                    ? "室温落在它的适宜区间（" + species.getTempMin() + "-" + species.getTempMax() + "℃）"
                    : "温度略有偏差，季节交替时留意";
            case HUMIDITY -> score >= 90
                    ? "湿度适宜，不必额外加湿"
                    : "湿度偏离适宜区间，可考虑喷雾或加湿";
            case CARE -> score >= 90
                    ? "养护强度与你的经验和时间匹配：" + noteOr(species.getWaterNote(), "按时浇水即可")
                    : "养护上需要多花一点心思";
            case SPACE -> score >= 90
                    ? "株型与你的摆放空间相称（成株冠幅约 " + species.getFootprintCm() + " 厘米）"
                    : "尺寸与空间尚可，摆放时留出生长余量";
            case BUDGET -> score >= 90
                    ? "价格落在你的预算区间内"
                    : "价格低于预算下限，性价比高";
            case PREFERENCE -> score >= 90
                    ? ("flower".equals(species.getOrnamentalType()) ? "属于观花类，符合你的偏好" : "属于观叶类，符合你的偏好")
                    : "观赏类型与偏好略有出入";
        };
    }

    /**
     * 主要风险：得分最低那一维，且必须低于 {@link #RISK_THRESHOLD}。
     *
     * <p>不设阈值的话，七维全是 95 分的完美候选也会被硬安一条"风险"，
     * 那种提示只会稀释真正的警告。
     */
    private static String buildRisk(Map<ScoreDimension, Integer> scores, CatalogSpecies species) {
        Map.Entry<ScoreDimension, Integer> weakest = scores.entrySet().stream()
                .min(Map.Entry.<ScoreDimension, Integer>comparingByValue()
                        .thenComparing(e -> e.getKey().ordinal()))
                .orElse(null);
        if (weakest == null || weakest.getValue() >= RISK_THRESHOLD) {
            return null;
        }
        return switch (weakest.getKey()) {
            case LIGHT -> "光照与它的偏好有差距，长期可能徒长或叶色变淡";
            case TEMP -> "当前温度不在适宜区间，冬夏两季需要挪位置";
            case HUMIDITY -> "湿度不足容易出现叶尖干枯，建议配合加湿";
            case CARE -> "养护要求偏高：" + noteOr(species.getWaterNote(), "需要规律浇水")
                    + "，忙起来容易疏忽";
            case SPACE -> "株型相对你的空间偏大，成株后可能显得局促";
            case BUDGET -> "价格与你的预算区间有偏差";
            case PREFERENCE -> "观赏类型与你选的偏好不同，介意的话可以换一株";
        };
    }

    /**
     * 由已存下来的漏斗重建放宽建议。
     *
     * <p>读历史快照时用。建议本身不入库——它完全由 {@code filtered_json} 决定，
     * 存下来只会多出一份可能与漏斗对不上的冗余数据。
     *
     * @param filtered 键为 {@link FilterRule#code()} 的排除计数
     */
    public static List<String> suggestionsFrom(Map<String, Integer> filtered) {
        Map<FilterRule, Integer> excluded = new EnumMap<>(FilterRule.class);
        for (FilterRule rule : FilterRule.values()) {
            excluded.put(rule, filtered == null ? 0 : filtered.getOrDefault(rule.code(), 0));
        }
        return relaxSuggestions(excluded);
    }

    /**
     * 无候选时的放宽建议。
     *
     * <p>只挑排除数最多的那几条**可放宽**规则。毒性与儿童安全永远不出现在这里——
     * 方案原文："无候选时仅建议放宽非安全约束"。这不是措辞问题：建议用户
     * "要不要接受一株对猫有毒的植物"是拿宠物的命换一条推荐结果。
     */
    private static List<String> relaxSuggestions(Map<FilterRule, Integer> excluded) {
        List<String> suggestions = excluded.entrySet().stream()
                .filter(e -> e.getKey().relaxable() && e.getValue() > 0)
                .sorted(Map.Entry.<FilterRule, Integer>comparingByValue().reversed()
                        .thenComparing(e -> e.getKey().ordinal()))
                .map(e -> switch (e.getKey()) {
                    case LIGHT -> "调整光照条件：有 " + e.getValue() + " 株因光照不匹配被排除，"
                            + "换一个靠窗的位置可能就合适了";
                    case SPACE -> "放宽摆放空间：有 " + e.getValue() + " 株因株型偏大被排除，"
                            + "改成层架或落地摆放能多出不少选择";
                    case BUDGET -> "提高预算上限：有 " + e.getValue() + " 株因超出预算被排除";
                    case WATER_FREQUENCY -> "有 " + e.getValue() + " 株因浇水太勤被排除，"
                            + "配一个自动滴灌或选更耐旱的品类会好一些";
                    default -> null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        if (suggestions.isEmpty()) {
            // 全部因安全约束出局。这时唯一诚实的说法是"当前条件下没有安全的选择"，
            // 而不是憋出一条放宽建议
            return List.of("当前的安全条件（宠物或儿童）下暂时没有合适的品种，"
                    + "我们会继续补充无毒品类");
        }
        return suggestions;
    }

    // ================================================================
    // 工具
    // ================================================================

    private static Map<String, Integer> toCodeMap(Map<FilterRule, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (FilterRule rule : FilterRule.values()) {
            result.put(rule.code(), source.getOrDefault(rule, 0));
        }
        return result;
    }

    private static Map<String, Integer> toCodeMap2(Map<ScoreDimension, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (ScoreDimension dimension : ScoreDimension.values()) {
            result.put(dimension.code(), source.getOrDefault(dimension, 0));
        }
        return result;
    }

    private static boolean isYes(Integer flag) {
        return flag != null && flag == 1;
    }

    private static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    /** 把画像档位夹到换算表的合法下标内，防越界 */
    private static int clampLevel(Integer level, int max) {
        if (level == null) {
            return 2;
        }
        return Math.max(1, Math.min(max, level));
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String noteOr(String note, String fallback) {
        return note == null || note.isBlank() ? fallback : note;
    }
}
