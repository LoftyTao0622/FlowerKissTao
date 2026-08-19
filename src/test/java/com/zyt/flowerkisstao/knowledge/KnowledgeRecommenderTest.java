package com.zyt.flowerkisstao.knowledge;

import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeArticle;
import com.zyt.flowerkisstao.knowledge.domain.service.KnowledgeRecommender;
import com.zyt.flowerkisstao.knowledge.domain.service.KnowledgeRecommender.Scored;
import com.zyt.flowerkisstao.knowledge.domain.service.KnowledgeRecommender.UserSignals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 知识内容个性化推荐的单元测试。
 *
 * <p>方案原文："系统依据用户已购植物和近期养护行为推荐相关内容。"这些断言就是
 * 那句话的可执行版本——不起容器，纯函数直接测。
 */
class KnowledgeRecommenderTest {

    @Test
    @DisplayName("讲你养的那个品种的文章排最前")
    void speciesMatchRanksFirst() {
        KnowledgeArticle generic = article(1L, "通用浇水", List.of("water"), List.of());
        KnowledgeArticle mine = article(2L, "琴叶榕专篇", List.of(), List.of("fiddle-leaf-fig"));

        List<Scored> result = KnowledgeRecommender.recommend(
                List.of(generic, mine),
                signals(Set.of("fiddle-leaf-fig"), Set.of("water"), Set.of(), Set.of()),
                5);

        assertEquals(2L, result.get(0).article().getId(),
                "讲用户自己那株植物的内容，相关度高于通用文章");
        assertEquals("与你养的植物直接相关", result.get(0).reason());
    }

    @Test
    @DisplayName("逾期任务对应的文章比近期任务的更靠前")
    void overdueTaskOutranksRecentTask() {
        KnowledgeArticle recent = article(1L, "施肥", List.of("fertilize"), List.of());
        KnowledgeArticle overdue = article(2L, "浇水", List.of("water"), List.of());

        List<Scored> result = KnowledgeRecommender.recommend(
                List.of(recent, overdue),
                signals(Set.of(), Set.of("fertilize"), Set.of("water"), Set.of()),
                5);

        assertEquals(2L, result.get(0).article().getId(),
                "任务没做完，往往是因为不知道为什么要做——这类文章最该被看到");
        assertEquals("你有一项相关养护任务还没完成", result.get(0).reason());
    }

    @Test
    @DisplayName("同一任务类型既逾期又近期时不重复加分")
    void sameTaskTypeIsNotDoubleCounted() {
        KnowledgeArticle both = article(1L, "浇水", List.of("water"), List.of());
        KnowledgeArticle onlyOverdue = article(2L, "浇水二", List.of("water"), List.of());

        List<Scored> bothSignals = KnowledgeRecommender.recommend(
                List.of(both),
                signals(Set.of(), Set.of("water"), Set.of("water"), Set.of()), 5);
        List<Scored> overdueOnly = KnowledgeRecommender.recommend(
                List.of(onlyOverdue),
                signals(Set.of(), Set.of(), Set.of("water"), Set.of()), 5);

        assertEquals(overdueOnly.get(0).score(), bothSignals.get(0).score(),
                "同一件事出现在两个集合里不该算两次分");
    }

    @Test
    @DisplayName("与用户毫无关系的文章不进推荐，除非要补位")
    void irrelevantArticlesAreExcluded() {
        KnowledgeArticle relevant = article(1L, "浇水", List.of("water"), List.of());
        KnowledgeArticle unrelated = article(2L, "花期管理", List.of("bloom"), List.of());

        List<Scored> result = KnowledgeRecommender.recommend(
                List.of(relevant, unrelated),
                signals(Set.of(), Set.of("water"), Set.of(), Set.of()),
                1);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).article().getId());
    }

    @Test
    @DisplayName("相关的不够时用热门补齐，推荐位不留空")
    void fillsUpWithPopularWhenNotEnoughRelevant() {
        KnowledgeArticle relevant = article(1L, "浇水", List.of("water"), List.of());
        KnowledgeArticle popular = article(2L, "热门", List.of("bloom"), List.of());
        popular.setUsefulCount(99);
        KnowledgeArticle cold = article(3L, "冷门", List.of("bloom"), List.of());

        List<Scored> result = KnowledgeRecommender.recommend(
                List.of(relevant, popular, cold),
                signals(Set.of(), Set.of("water"), Set.of(), Set.of()),
                3);

        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).article().getId(), "相关的仍排最前");
        assertEquals(2L, result.get(1).article().getId(), "补位时热门优先");
        assertEquals("热门养护知识", result.get(1).reason());
    }

    @Test
    @DisplayName("读过的文章降权但不排除——复习也有价值")
    void readArticlesAreDemotedNotRemoved() {
        KnowledgeArticle read = article(1L, "读过的浇水", List.of("water"), List.of());
        KnowledgeArticle fresh = article(2L, "没读过的浇水", List.of("water"), List.of());

        List<Scored> result = KnowledgeRecommender.recommend(
                List.of(read, fresh),
                signals(Set.of(), Set.of("water"), Set.of(), Set.of(1L)),
                5);

        assertEquals(2L, result.get(0).article().getId(), "没读过的应当排前");
        assertTrue(result.stream().anyMatch(s -> s.article().getId().equals(1L)),
                "读过的仍应出现，只是排后面");
        assertTrue(result.get(1).reason().contains("你读过"));
    }

    @Test
    @DisplayName("新用户没有任何信号时退回热门，而不是一片空白")
    void newUserGetsPopularArticles() {
        KnowledgeArticle hot = article(1L, "热门", List.of("water"), List.of());
        hot.setUsefulCount(50);
        KnowledgeArticle cold = article(2L, "冷门", List.of("water"), List.of());

        List<Scored> result = KnowledgeRecommender.recommend(
                List.of(cold, hot),
                signals(Set.of(), Set.of(), Set.of(), Set.of()),
                2);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).article().getId());
        assertEquals("热门养护知识", result.get(0).reason());
    }

    @Test
    @DisplayName("入门难度在相关度相同时略微靠前")
    void easierArticlesRankSlightlyHigher() {
        KnowledgeArticle easy = article(1L, "入门", List.of("water"), List.of());
        easy.setDifficulty(1);
        KnowledgeArticle hard = article(2L, "专业", List.of("water"), List.of());
        hard.setDifficulty(3);

        List<Scored> result = KnowledgeRecommender.recommend(
                List.of(hard, easy),
                signals(Set.of(), Set.of("water"), Set.of(), Set.of()),
                5);

        assertEquals(1L, result.get(0).article().getId(),
                "推给正在养植物的人，先给能立刻用上的");
    }

    @Test
    @DisplayName("每条推荐都带理由——不解释的推荐等于随机排列")
    void everyResultHasReason() {
        List<Scored> result = KnowledgeRecommender.recommend(
                List.of(article(1L, "浇水", List.of("water"), List.of())),
                signals(Set.of(), Set.of("water"), Set.of(), Set.of()),
                5);

        assertFalse(result.isEmpty());
        for (Scored scored : result) {
            assertFalse(scored.reason().isBlank());
        }
    }

    @Test
    @DisplayName("同样的输入推两次，结果完全一致")
    void isReproducible() {
        // 五篇除 id 外完全一样，逼同分兜底规则起作用
        List<KnowledgeArticle> same = List.of(
                article(1L, "甲", List.of("water"), List.of()),
                article(2L, "乙", List.of("water"), List.of()),
                article(3L, "丙", List.of("water"), List.of()),
                article(4L, "丁", List.of("water"), List.of()),
                article(5L, "戊", List.of("water"), List.of()));
        UserSignals signals = signals(Set.of(), Set.of("water"), Set.of(), Set.of());

        List<Scored> first = KnowledgeRecommender.recommend(same, signals, 3);
        List<Scored> second = KnowledgeRecommender.recommend(same, signals, 3);

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).article().getId(), second.get(i).article().getId(),
                    "第 " + i + " 名不稳定——同分兜底没定死");
            assertEquals(first.get(i).score(), second.get(i).score());
        }
    }

    @Test
    @DisplayName("候选为空或条数为零时返回空列表，不抛异常")
    void emptyInputsAreSafe() {
        UserSignals signals = signals(Set.of(), Set.of("water"), Set.of(), Set.of());
        assertTrue(KnowledgeRecommender.recommend(List.of(), signals, 5).isEmpty());
        assertTrue(KnowledgeRecommender.recommend(
                List.of(article(1L, "甲", List.of("water"), List.of())), signals, 0).isEmpty());
    }

    // ================================================================

    private static UserSignals signals(Set<String> species, Set<String> recent,
                                       Set<String> overdue, Set<Long> read) {
        return new UserSignals(species, recent, overdue, read);
    }

    private static KnowledgeArticle article(Long id, String title,
                                            List<String> taskTypes, List<String> species) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(id);
        article.setSlug("a-" + id);
        article.setTitle(title);
        article.setDifficulty(2);
        article.setRelatedTaskTypes(taskTypes);
        article.setRelatedSpecies(species);
        article.setViewCount(0);
        article.setUsefulCount(0);
        return article;
    }
}
