package com.zyt.flowerkisstao.knowledge.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.care.domain.entity.CareArchive;
import com.zyt.flowerkisstao.care.domain.entity.CareTask;
import com.zyt.flowerkisstao.care.infrastructure.mapper.CareArchiveMapper;
import com.zyt.flowerkisstao.care.infrastructure.mapper.CareTaskMapper;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import com.zyt.flowerkisstao.catalog.infrastructure.mapper.CatalogSpeciesMapper;
import com.zyt.flowerkisstao.knowledge.application.service.KnowledgeQueryService;
import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeArticle;
import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeFeedback;
import com.zyt.flowerkisstao.knowledge.domain.model.ArticleCategory;
import com.zyt.flowerkisstao.knowledge.domain.model.ArticleStatus;
import com.zyt.flowerkisstao.knowledge.domain.service.KnowledgeRecommender;
import com.zyt.flowerkisstao.knowledge.infrastructure.mapper.KnowledgeArticleMapper;
import com.zyt.flowerkisstao.knowledge.infrastructure.mapper.KnowledgeFeedbackMapper;
import com.zyt.flowerkisstao.knowledge.infrastructure.mapper.KnowledgeSearchMissMapper;
import com.zyt.flowerkisstao.knowledge.web.dto.ArticleQueryDTO;
import com.zyt.flowerkisstao.knowledge.web.vo.ArticleFacetsVO;
import com.zyt.flowerkisstao.knowledge.web.vo.ArticleVO;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.security.AppUserDetails;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库的公开读取。
 *
 * <p>检索写法沿用 {@code PlantQueryServiceImpl}：状态用 `.eq()` 在最前兜死、
 * 关键词用 `.and()` 包一组 OR（**不能裸 `.or()`，那会漏过状态过滤把草稿放出去**）、
 * 未发布文章按"不存在"处理，不泄露它曾经存在。
 */
@Service
@Slf4j
public class KnowledgeQueryServiceImpl implements KnowledgeQueryService {

    /** 无结果关键词记进清单的长度上限，与表的列宽一致 */
    private static final int MAX_KEYWORD_LENGTH = 60;

    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeFeedbackMapper feedbackMapper;
    private final KnowledgeSearchMissMapper searchMissMapper;
    private final CareArchiveMapper careArchiveMapper;
    private final CareTaskMapper careTaskMapper;
    private final CatalogSpeciesMapper speciesMapper;

    public KnowledgeQueryServiceImpl(KnowledgeArticleMapper articleMapper,
                                     KnowledgeFeedbackMapper feedbackMapper,
                                     KnowledgeSearchMissMapper searchMissMapper,
                                     CareArchiveMapper careArchiveMapper,
                                     CareTaskMapper careTaskMapper,
                                     CatalogSpeciesMapper speciesMapper) {
        this.articleMapper = articleMapper;
        this.feedbackMapper = feedbackMapper;
        this.searchMissMapper = searchMissMapper;
        this.careArchiveMapper = careArchiveMapper;
        this.careTaskMapper = careTaskMapper;
        this.speciesMapper = speciesMapper;
    }

    // ================================================================
    // 检索
    // ================================================================

    @Override
    public IPage<ArticleVO> page(IPage<?> page, ArticleQueryDTO query) {
        String keyword = trimToNull(query.getKeyword());

        @SuppressWarnings("unchecked")
        IPage<KnowledgeArticle> result = articleMapper.selectPage(
                (IPage<KnowledgeArticle>) page, publicQuery(query, keyword));

        // 方案原文："高频搜索但无结果的关键词会进入后台内容需求清单。"
        // 只在"用户确实搜了词且一条都没搜到"时记——按分类筛出空结果不算内容缺口
        if (keyword != null && result.getTotal() == 0) {
            recordSearchMiss(keyword);
        }

        return result.convert(KnowledgeConverter::toSummary);
    }

    /** 公开检索的条件。所有分支都挂在同一个 wrapper 上，状态过滤永远在最外层 */
    private LambdaQueryWrapper<KnowledgeArticle> publicQuery(ArticleQueryDTO query, String keyword) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = Wrappers.<KnowledgeArticle>lambdaQuery()
                // 已发布在这里兜死，任何调用方都绕不过去
                .eq(KnowledgeArticle::getStatus, ArticleStatus.PUBLISHED.code())
                .eq(hasText(query.getCategory()), KnowledgeArticle::getCategory, query.getCategory())
                .eq(query.getDifficulty() != null,
                        KnowledgeArticle::getDifficulty, query.getDifficulty());

        // 关键词必须整组包在 and 里。写成裸 .or() 的话，OR 会跟状态条件平级，
        // 结果是"已发布 或 标题匹配"——草稿会被搜出来
        if (keyword != null) {
            wrapper.and(w -> w
                    .like(KnowledgeArticle::getTitle, keyword)
                    .or().like(KnowledgeArticle::getSummary, keyword)
                    .or().like(KnowledgeArticle::getApplicable, keyword)
                    .or().apply("CAST(tags AS CHAR) LIKE CONCAT('%', {0}, '%')", keyword));
        }

        // JSON 数组列的包含判断。参数化传值，不拼字符串
        if (hasText(query.getSeason())) {
            wrapper.apply("(seasons IS NULL OR JSON_LENGTH(seasons) = 0 "
                    + "OR JSON_CONTAINS(seasons, JSON_QUOTE({0})))", query.getSeason());
        }
        if (hasText(query.getTag())) {
            wrapper.apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", query.getTag());
        }
        if (hasText(query.getSpeciesCode())) {
            wrapper.apply("JSON_CONTAINS(related_species, JSON_QUOTE({0}))", query.getSpeciesCode());
        }
        if (hasText(query.getTaskType())) {
            wrapper.apply("JSON_CONTAINS(related_task_types, JSON_QUOTE({0}))", query.getTaskType());
        }

        return wrapper
                .orderByDesc(KnowledgeArticle::getSort)
                .orderByDesc(KnowledgeArticle::getPublishedAt)
                .orderByAsc(KnowledgeArticle::getId);
    }

    /**
     * 记一次无结果搜索。
     *
     * <p>整段 try 住：这是埋点，绝不能因为它失败而让用户的搜索请求报错。
     */
    private void recordSearchMiss(String keyword) {
        try {
            String trimmed = keyword.length() > MAX_KEYWORD_LENGTH
                    ? keyword.substring(0, MAX_KEYWORD_LENGTH) : keyword;
            searchMissMapper.recordMiss(trimmed);
        } catch (Exception e) {
            log.warn("记录无结果搜索词失败：{}", keyword, e);
        }
    }

    // ================================================================
    // 详情
    // ================================================================

    @Override
    public ArticleVO getBySlug(String slug) {
        KnowledgeArticle article = articleMapper.selectOne(Wrappers.<KnowledgeArticle>lambdaQuery()
                .eq(KnowledgeArticle::getSlug, slug)
                .eq(KnowledgeArticle::getStatus, ArticleStatus.PUBLISHED.code())
                .last("LIMIT 1"));
        // 草稿、待审、已下架与压根不存在，对外是同一句话——不泄露"这篇曾经存在"
        if (article == null) {
            throw new BizException(ErrorCode.ARTICLE_NOT_FOUND, "文章不存在或已下架");
        }

        // 浏览数用 SQL 自增，不读出来加一再写回：详情页并发最高，后者会丢计数
        articleMapper.incrementViewCount(article.getId());
        article.setViewCount(safe(article.getViewCount()) + 1);

        Long userId = currentUserIdOrNull();
        boolean marked = false;
        boolean favorited = false;
        if (userId != null) {
            Set<String> types = feedbackTypesOf(userId, article.getId());
            marked = types.contains(KnowledgeFeedback.TYPE_USEFUL);
            favorited = types.contains(KnowledgeFeedback.TYPE_FAVORITE);
        }
        return KnowledgeConverter.toDetail(article, marked, favorited);
    }

    // ================================================================
    // 筛选项
    // ================================================================

    @Override
    public ArticleFacetsVO facets() {
        List<ArticleFacetsVO.Option> categories = articleMapper.selectDistinctCategories().stream()
                .map(code -> new ArticleFacetsVO.Option(code, ArticleCategory.labelOf(code)))
                .toList();

        List<ArticleFacetsVO.Option> difficulties = List.of(
                new ArticleFacetsVO.Option("1", "入门"),
                new ArticleFacetsVO.Option("2", "进阶"),
                new ArticleFacetsVO.Option("3", "专业"));

        List<ArticleFacetsVO.Option> seasons = KnowledgeConverter.allSeasons().stream()
                .map(code -> new ArticleFacetsVO.Option(code, KnowledgeConverter.seasonLabel(code)))
                .toList();

        return ArticleFacetsVO.builder()
                .categories(categories)
                .difficulties(difficulties)
                .seasons(seasons)
                .tags(articleMapper.selectDistinctTags())
                .build();
    }

    // ================================================================
    // 个性化推荐
    // ================================================================

    /**
     * 方案原文："系统依据用户已购植物和近期养护行为推荐相关内容。"
     *
     * <p>这里只负责把信号从 care 模块查出来，挑选规则在
     * {@link KnowledgeRecommender} 里——那是个可脱离容器单测的纯函数。
     */
    @Override
    public List<ArticleVO> recommended(int limit) {
        List<KnowledgeArticle> candidates = articleMapper.selectList(
                Wrappers.<KnowledgeArticle>lambdaQuery()
                        .eq(KnowledgeArticle::getStatus, ArticleStatus.PUBLISHED.code()));
        if (candidates.isEmpty()) {
            return List.of();
        }

        KnowledgeRecommender.UserSignals signals = collectSignals();
        return KnowledgeRecommender.recommend(candidates, signals, limit).stream()
                .map(scored -> {
                    ArticleVO vo = KnowledgeConverter.toSummary(scored.article());
                    vo.setRecommendReason(scored.reason());
                    return vo;
                })
                .toList();
    }

    /** 从 care 模块收集用户信号。未登录时返回空信号，推荐会退回热门 */
    private KnowledgeRecommender.UserSignals collectSignals() {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            return new KnowledgeRecommender.UserSignals(Set.of(), Set.of(), Set.of(), Set.of());
        }

        List<CareArchive> archives = careArchiveMapper.selectList(
                Wrappers.<CareArchive>lambdaQuery()
                        .eq(CareArchive::getUserId, userId)
                        .eq(CareArchive::getStatus, CareArchive.STATUS_ACTIVE));
        if (archives.isEmpty()) {
            return new KnowledgeRecommender.UserSignals(Set.of(), Set.of(), Set.of(),
                    readArticleIds(userId));
        }

        // 已购品种：档案存的是 speciesId，文章关联的是 code，这里换一次
        List<Long> speciesIds = archives.stream()
                .map(CareArchive::getSpeciesId).distinct().toList();
        Set<String> speciesCodes = speciesMapper.selectBatchIds(speciesIds).stream()
                .map(CatalogSpecies::getCode)
                .collect(Collectors.toSet());

        List<Long> archiveIds = archives.stream().map(CareArchive::getId).toList();
        LocalDate today = LocalDate.now();

        // 近期任务：往前 14 天到往后 14 天，正好覆盖"刚做过"与"马上要做"
        List<CareTask> tasks = careTaskMapper.selectList(Wrappers.<CareTask>lambdaQuery()
                .in(CareTask::getArchiveId, archiveIds)
                .ge(CareTask::getDueDate, today.minusDays(14))
                .le(CareTask::getDueDate, today.plusDays(14)));

        Set<String> recentTypes = new HashSet<>();
        Set<String> overdueTypes = new HashSet<>();
        for (CareTask task : tasks) {
            recentTypes.add(task.getTaskType());
            // 逾期或被跳过的最需要解释"为什么要做"
            if (task.getStatus() != null
                    && (task.getStatus() == CareTask.STATUS_OVERDUE
                    || task.getStatus() == CareTask.STATUS_SKIPPED)) {
                overdueTypes.add(task.getTaskType());
            }
        }

        return new KnowledgeRecommender.UserSignals(
                speciesCodes, recentTypes, overdueTypes, readArticleIds(userId));
    }

    /** 用收藏与"有用"近似"读过"：真正的阅读历史要另建表，收益不值这张表 */
    private Set<Long> readArticleIds(Long userId) {
        return feedbackMapper.selectList(Wrappers.<KnowledgeFeedback>lambdaQuery()
                        .eq(KnowledgeFeedback::getUserId, userId)).stream()
                .map(KnowledgeFeedback::getArticleId)
                .collect(Collectors.toSet());
    }

    // ================================================================

    private Set<String> feedbackTypesOf(Long userId, Long articleId) {
        return feedbackMapper.selectList(Wrappers.<KnowledgeFeedback>lambdaQuery()
                        .eq(KnowledgeFeedback::getUserId, userId)
                        .eq(KnowledgeFeedback::getArticleId, articleId)).stream()
                .map(KnowledgeFeedback::getType)
                .collect(Collectors.toSet());
    }

    /** 当前登录人；游客返回 null。知识库对游客开放，不能直接用 requireUserId */
    private Long currentUserIdOrNull() {
        Optional<AppUserDetails> user = CurrentUser.get();
        return user.map(AppUserDetails::getUserId).orElse(null);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
