package com.zyt.flowerkisstao.knowledge;

import com.zyt.flowerkisstao.knowledge.domain.model.ArticleStatus;
import com.zyt.flowerkisstao.knowledge.domain.service.ArticleTransition;
import com.zyt.flowerkisstao.knowledge.domain.service.ArticleTransition.Action;
import com.zyt.flowerkisstao.knowledge.domain.service.ArticleTransition.Actor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文章审核流转的单元测试。不起 Spring 容器：规则是纯函数。
 */
class ArticleTransitionTest {

    @Test
    @DisplayName("主链路：草稿 → 待审核 → 已发布")
    void happyPath() {
        ArticleTransition.Result submitted =
                ArticleTransition.check(ArticleStatus.DRAFT, Action.SUBMIT);
        assertTrue(submitted.allowed());
        assertEquals(ArticleStatus.PENDING, submitted.target());

        ArticleTransition.Result published =
                ArticleTransition.check(ArticleStatus.PENDING, Action.PUBLISH);
        assertTrue(published.allowed());
        assertEquals(ArticleStatus.PUBLISHED, published.target());
    }

    @Test
    @DisplayName("草稿不能直接发布，必须先过审")
    void draftCannotPublishDirectly() {
        ArticleTransition.Result result =
                ArticleTransition.check(ArticleStatus.DRAFT, Action.PUBLISH);
        assertFalse(result.allowed(), "跳过审核直接发布会让'审核'这一步形同虚设");
        assertNotNull(result.reason());
    }

    @Test
    @DisplayName("退回与撤回都回到草稿")
    void rejectAndWithdrawBothReturnToDraft() {
        assertEquals(ArticleStatus.DRAFT,
                ArticleTransition.check(ArticleStatus.PENDING, Action.REJECT).target());
        assertEquals(ArticleStatus.DRAFT,
                ArticleTransition.check(ArticleStatus.PENDING, Action.WITHDRAW).target());
    }

    @Test
    @DisplayName("已下架的文章改完可以重新提交")
    void offlineCanBeResubmitted() {
        ArticleTransition.Result result =
                ArticleTransition.check(ArticleStatus.OFFLINE, Action.SUBMIT);
        assertTrue(result.allowed(), "内容改好后应当能重新上线，而不是只能新建一篇");
        assertEquals(ArticleStatus.PENDING, result.target());
    }

    @Test
    @DisplayName("只有已发布的能下架")
    void onlyPublishedCanGoOffline() {
        assertTrue(ArticleTransition.check(ArticleStatus.PUBLISHED, Action.OFFLINE).allowed());
        for (ArticleStatus status : List.of(ArticleStatus.DRAFT, ArticleStatus.PENDING,
                ArticleStatus.OFFLINE)) {
            assertFalse(ArticleTransition.check(status, Action.OFFLINE).allowed(),
                    status.label() + "的文章本来就不在线上，谈不上下架");
        }
    }

    @Test
    @DisplayName("穷举 4 状态 × 5 动作，每一格的结论都与预期一致")
    void everyCombinationIsDecided() {
        Set<String> expectedAllowed = Set.of(
                key(ArticleStatus.DRAFT, Action.SUBMIT),
                key(ArticleStatus.OFFLINE, Action.SUBMIT),
                key(ArticleStatus.PENDING, Action.WITHDRAW),
                key(ArticleStatus.PENDING, Action.PUBLISH),
                key(ArticleStatus.PENDING, Action.REJECT),
                key(ArticleStatus.PUBLISHED, Action.OFFLINE));

        int checked = 0;
        for (ArticleStatus status : ArticleStatus.values()) {
            for (Action action : Action.values()) {
                boolean expected = expectedAllowed.contains(key(status, action));
                ArticleTransition.Result result = ArticleTransition.check(status, action);
                assertEquals(expected, result.allowed(),
                        String.format("%s + %s 的结论不对", status.label(), action.label()));
                if (result.allowed()) {
                    assertNotNull(result.target());
                    assertNull(result.reason());
                } else {
                    assertNotNull(result.reason(), "拒绝时必须说明原因");
                    assertFalse(result.reason().isBlank());
                }
                checked++;
            }
        }
        assertEquals(20, checked, "4 状态 × 5 动作");
    }

    @Test
    @DisplayName("按角色给出可用动作：作者看不到发布按钮")
    void actionsAreScopedByActor() {
        assertEquals(List.of(Action.SUBMIT),
                ArticleTransition.availableActions(ArticleStatus.DRAFT, Actor.AUTHOR));
        assertTrue(ArticleTransition.availableActions(ArticleStatus.DRAFT, Actor.REVIEWER).isEmpty(),
                "草稿阶段审核人无事可做");

        List<Action> reviewerOnPending =
                ArticleTransition.availableActions(ArticleStatus.PENDING, Actor.REVIEWER);
        assertEquals(2, reviewerOnPending.size());
        assertTrue(reviewerOnPending.contains(Action.PUBLISH));
        assertTrue(reviewerOnPending.contains(Action.REJECT));

        assertEquals(List.of(Action.WITHDRAW),
                ArticleTransition.availableActions(ArticleStatus.PENDING, Actor.AUTHOR));
    }

    @Test
    @DisplayName("状态码与枚举双向对应")
    void statusCodesRoundTrip() {
        for (ArticleStatus status : ArticleStatus.values()) {
            assertEquals(status, ArticleStatus.of(status.code()));
        }
        // 数值存进数据库，改了会让存量数据整体错位
        assertEquals(0, ArticleStatus.DRAFT.code());
        assertEquals(1, ArticleStatus.PENDING.code());
        assertEquals(2, ArticleStatus.PUBLISHED.code());
        assertEquals(3, ArticleStatus.OFFLINE.code());

        assertTrue(ArticleStatus.PUBLISHED.isPublic());
        assertFalse(ArticleStatus.DRAFT.isPublic());
        assertFalse(ArticleStatus.OFFLINE.isPublic());
    }

    @Test
    @DisplayName("未知状态码直接抛")
    void unknownStatusThrows() {
        assertThrows(IllegalArgumentException.class, () -> ArticleStatus.of(9));
        assertThrows(IllegalArgumentException.class, () -> ArticleStatus.of(null));
    }

    private static String key(ArticleStatus status, Action action) {
        return status.name() + "|" + action.name();
    }
}
