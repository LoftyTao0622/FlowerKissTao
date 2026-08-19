package com.zyt.flowerkisstao.knowledge.domain.service;

import com.zyt.flowerkisstao.knowledge.domain.model.ArticleStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 文章状态流转规则。
 *
 * <p>与 {@code OrderTransition} 同一套路：不依赖 Spring、不碰数据库，纯函数。
 * 方案原文："文章由管理员编辑、分类、打标签并审核发布。"审核这条线上的每一步
 * 谁能做、能从哪来到哪去，都在这里说清楚。
 */
public final class ArticleTransition {

    /** 谁能发起 */
    public enum Actor {
        /** 有 knowledge:article:write 的人 */
        AUTHOR,
        /** 有 knowledge:article:publish 的人 */
        REVIEWER
    }

    /** 可执行的动作 */
    public enum Action {

        /** 提交审核。草稿或已下架的文章改完都能再交一次 */
        SUBMIT("提交审核", Actor.AUTHOR,
                Set.of(ArticleStatus.DRAFT, ArticleStatus.OFFLINE), ArticleStatus.PENDING),

        /** 撤回。提交后发现还想改 */
        WITHDRAW("撤回", Actor.AUTHOR,
                Set.of(ArticleStatus.PENDING), ArticleStatus.DRAFT),

        /** 审核通过并发布 */
        PUBLISH("发布", Actor.REVIEWER,
                Set.of(ArticleStatus.PENDING), ArticleStatus.PUBLISHED),

        /** 退回草稿。审核不通过 */
        REJECT("退回", Actor.REVIEWER,
                Set.of(ArticleStatus.PENDING), ArticleStatus.DRAFT),

        /** 下架。内容过时或有误时撤下 */
        OFFLINE("下架", Actor.REVIEWER,
                Set.of(ArticleStatus.PUBLISHED), ArticleStatus.OFFLINE);

        private final String label;
        private final Actor actor;
        private final Set<ArticleStatus> allowedFrom;
        private final ArticleStatus target;

        Action(String label, Actor actor, Set<ArticleStatus> allowedFrom, ArticleStatus target) {
            this.label = label;
            this.actor = actor;
            this.allowedFrom = allowedFrom;
            this.target = target;
        }

        public String label() {
            return label;
        }

        public Actor actor() {
            return actor;
        }

        public Set<ArticleStatus> allowedFrom() {
            return allowedFrom;
        }

        public ArticleStatus target() {
            return target;
        }
    }

    /** 流转结果。allowed 为 false 时 target 无意义，reason 是给用户看的话 */
    public record Result(boolean allowed, ArticleStatus target, String reason) {

        static Result ok(ArticleStatus target) {
            return new Result(true, target, null);
        }

        static Result reject(String reason) {
            return new Result(false, null, reason);
        }
    }

    private ArticleTransition() {
    }

    /** 能不能从 current 执行 action，能的话落到哪个状态 */
    public static Result check(ArticleStatus current, Action action) {
        if (!action.allowedFrom().contains(current)) {
            return Result.reject("当前是" + current.label() + "状态，不能" + action.label());
        }
        return Result.ok(action.target());
    }

    /**
     * 当前状态下这个角色能做哪些动作。
     *
     * <p>由后端算好给前端，界面上就不会出现点下去必然报错的按钮——
     * 与订单模块同一处理。
     */
    public static List<Action> availableActions(ArticleStatus current, Actor actor) {
        List<Action> actions = new ArrayList<>();
        for (Action action : Action.values()) {
            if (action.actor() == actor && action.allowedFrom().contains(current)) {
                actions.add(action);
            }
        }
        return actions;
    }
}
