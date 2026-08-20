package com.zyt.flowerkisstao.knowledge.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.knowledge.application.service.KnowledgeAdminService;
import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeArticle;
import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeSearchMiss;
import com.zyt.flowerkisstao.knowledge.domain.model.ArticleStatus;
import com.zyt.flowerkisstao.knowledge.domain.service.ArticleTransition;
import com.zyt.flowerkisstao.knowledge.infrastructure.mapper.KnowledgeArticleMapper;
import com.zyt.flowerkisstao.knowledge.infrastructure.mapper.KnowledgeSearchMissMapper;
import com.zyt.flowerkisstao.knowledge.web.dto.ArticleSaveDTO;
import com.zyt.flowerkisstao.knowledge.web.vo.ArticleVO;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.redis.AfterCommitCacheInvalidator;
import com.zyt.flowerkisstao.shared.redis.RedisKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeAdminServiceImpl implements KnowledgeAdminService {

    /** slug 列宽 60，删除时要给 -del-<id> 后缀留位置 */
    private static final int SLUG_MAX = 60;

    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeSearchMissMapper searchMissMapper;
    private final AfterCommitCacheInvalidator cacheInvalidator;
    private final RedisKey redisKey;

    public KnowledgeAdminServiceImpl(KnowledgeArticleMapper articleMapper,
                                     KnowledgeSearchMissMapper searchMissMapper,
                                     AfterCommitCacheInvalidator cacheInvalidator,
                                     RedisKey redisKey) {
        this.articleMapper = articleMapper;
        this.searchMissMapper = searchMissMapper;
        this.cacheInvalidator = cacheInvalidator;
        this.redisKey = redisKey;
    }

    @Override
    public IPage<ArticleVO> page(IPage<?> page, Integer status, String keyword) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String trimmed = hasKeyword ? keyword.trim() : null;

        @SuppressWarnings("unchecked")
        IPage<KnowledgeArticle> result = articleMapper.selectPage((IPage<KnowledgeArticle>) page,
                Wrappers.<KnowledgeArticle>lambdaQuery()
                        .eq(status != null, KnowledgeArticle::getStatus, status)
                        .and(hasKeyword, w -> w
                                .like(KnowledgeArticle::getTitle, trimmed)
                                .or().like(KnowledgeArticle::getSlug, trimmed)
                                .or().like(KnowledgeArticle::getSummary, trimmed))
                        .orderByDesc(KnowledgeArticle::getUpdatedAt)
                        .orderByDesc(KnowledgeArticle::getId));

        ArticleTransition.Actor actor = currentActor();
        return result.convert(article -> KnowledgeConverter.toAdmin(article, actor));
    }

    @Override
    public ArticleVO get(Long id) {
        return KnowledgeConverter.toAdmin(requireArticle(id), currentActor());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ArticleSaveDTO dto) {
        requireSlugAvailable(dto.getSlug(), null);

        KnowledgeArticle article = new KnowledgeArticle();
        applyDto(article, dto);
        // 新建一律是草稿。想发布必须走流转接口，不能靠编辑表单一步到位
        article.setStatus(ArticleStatus.DRAFT.code());
        article.setAuthorId(CurrentUser.requireUserId());
        article.setViewCount(0);
        article.setUsefulCount(0);

        articleMapper.insert(article);
        cacheInvalidator.delete(redisKey.articleFacets());
        return article.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ArticleSaveDTO dto) {
        KnowledgeArticle existing = requireArticle(id);
        requireSlugAvailable(dto.getSlug(), id);

        KnowledgeArticle update = new KnowledgeArticle();
        update.setId(id);
        applyDto(update, dto);
        // status 不在这里改，避免编辑表单绕过审核直接发布
        articleMapper.updateById(update);
        cacheInvalidator.delete(redisKey.articleDetail(existing.getSlug()),
                redisKey.articleDetail(dto.getSlug()), redisKey.articleFacets());
    }

    /**
     * 删除。
     *
     * <p>uk_article_slug 是物理唯一索引，不认 deleted 标记：直接删掉后想用同一个 slug
     * 重建就会撞键。所以先改名腾位再标记删除，做法与 catalog 的 code、画像的 scene_name 一致。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        KnowledgeArticle existing = requireArticle(id);

        KnowledgeArticle rename = new KnowledgeArticle();
        rename.setId(id);
        rename.setSlug(truncateSlug(existing.getSlug(), id));
        articleMapper.updateById(rename);

        articleMapper.deleteById(id);
        cacheInvalidator.delete(redisKey.articleDetail(existing.getSlug()), redisKey.articleFacets());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transition(Long id, ArticleTransition.Action action) {
        KnowledgeArticle article = requireArticle(id);
        ArticleStatus current = ArticleStatus.of(article.getStatus());

        ArticleTransition.Result result = ArticleTransition.check(current, action);
        if (!result.allowed()) {
            throw new BizException(ErrorCode.ARTICLE_STATUS_INVALID, result.reason());
        }

        KnowledgeArticle update = new KnowledgeArticle();
        update.setId(id);
        update.setStatus(result.target().code());
        // 首次发布时记下发布时间；重新上线不覆盖，列表按首次发布排序才稳定
        if (result.target() == ArticleStatus.PUBLISHED && article.getPublishedAt() == null) {
            update.setPublishedAt(LocalDateTime.now());
        }
        articleMapper.updateById(update);
        cacheInvalidator.delete(redisKey.articleDetail(article.getSlug()), redisKey.articleFacets());
    }

    @Override
    public List<KnowledgeSearchMiss> searchMisses(int limit) {
        return searchMissMapper.selectList(Wrappers.<KnowledgeSearchMiss>lambdaQuery()
                // 搜得最多的排最前——这份清单就是给运营用来决定先补哪篇的
                .orderByDesc(KnowledgeSearchMiss::getHitCount)
                .orderByDesc(KnowledgeSearchMiss::getLastSearchedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }

    // ================================================================

    private KnowledgeArticle requireArticle(Long id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BizException(ErrorCode.ARTICLE_NOT_FOUND, "文章不存在");
        }
        return article;
    }

    /** 新建时 excludeId 传 null；编辑时传自身 id，否则会和自己撞车 */
    private void requireSlugAvailable(String slug, Long excludeId) {
        Long count = articleMapper.selectCount(Wrappers.<KnowledgeArticle>lambdaQuery()
                .eq(KnowledgeArticle::getSlug, slug)
                .ne(excludeId != null, KnowledgeArticle::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.ARTICLE_SLUG_TAKEN, "文章标识「" + slug + "」已被占用");
        }
    }

    /** 删除时给 slug 加后缀腾位，超长则先截断，保证加完不超列宽 */
    private String truncateSlug(String slug, Long id) {
        String suffix = "-del-" + id;
        int keep = SLUG_MAX - suffix.length();
        String base = slug.length() > keep ? slug.substring(0, keep) : slug;
        return base + suffix;
    }

    /**
     * 当前登录人在文章流转里的角色。
     *
     * <p>有发布权限的算审核人，只有写权限的算作者。ROLE_OPERATOR 两个权限都有，
     * 所以它两种动作都能做——但角色区分本身是有意义的，将来加一个"只能写不能发"的
     * 角色时，这里一行都不用改。
     */
    private ArticleTransition.Actor currentActor() {
        boolean canPublish = CurrentUser.get()
                .map(user -> user.getAuthorities().stream()
                        .anyMatch(a -> Perms.KNOWLEDGE_ARTICLE_PUBLISH.equals(a.getAuthority())))
                .orElse(false);
        return canPublish ? ArticleTransition.Actor.REVIEWER : ArticleTransition.Actor.AUTHOR;
    }

    private void applyDto(KnowledgeArticle article, ArticleSaveDTO dto) {
        article.setSlug(dto.getSlug());
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setCover(dto.getCover());
        article.setCategory(dto.getCategory());
        article.setDifficulty(dto.getDifficulty());
        article.setSeasons(dto.getSeasons());
        article.setTags(dto.getTags());
        article.setApplicable(dto.getApplicable());
        article.setFrequency(dto.getFrequency());
        article.setSteps(dto.getSteps());
        article.setMistakes(dto.getMistakes());
        article.setRisks(dto.getRisks());
        article.setRelatedTaskTypes(dto.getRelatedTaskTypes());
        article.setRelatedSpecies(dto.getRelatedSpecies());
        article.setSort(dto.getSort() == null ? 0 : dto.getSort());
    }
}
