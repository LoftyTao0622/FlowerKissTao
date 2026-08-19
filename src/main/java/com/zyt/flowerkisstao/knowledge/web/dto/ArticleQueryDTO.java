package com.zyt.flowerkisstao.knowledge.web.dto;

import lombok.Data;

/**
 * 文章检索条件。方案原文："用户可按植物名称、问题症状、养护难度和季节检索。"
 *
 * <p>写成 {@code @Data} 类而不是一堆 {@code @RequestParam}：条件有七个，
 * 平铺在方法签名上以后加一个就要改三处。
 */
@Data
public class ArticleQueryDTO {

    /** 关键词，匹配标题、摘要、适用条件与标签 */
    private String keyword;

    private String category;

    /** 1 入门 / 2 进阶 / 3 专业 */
    private Integer difficulty;

    /** spring / summer / autumn / winter */
    private String season;

    private String tag;

    /**
     * 品种 code。商品详情页跳转用：查"专门讲这个品种"的文章。
     */
    private String speciesCode;

    /**
     * 养护任务类型。养护任务页跳转用：查"解释这类任务"的文章。
     */
    private String taskType;
}
