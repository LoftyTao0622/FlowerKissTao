package com.zyt.flowerkisstao.knowledge.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 无结果搜索词。方案原文："高频搜索但无结果的关键词会进入后台内容需求清单。"
 *
 * <p>这张表就是那份清单——它把"用户想知道什么而我们没写"变成可排序的待办，
 * 比运营凭感觉选题可靠。
 *
 * <p>keyword 唯一 + {@link #hitCount} 累加，而不是每次搜索插一行：清单要看的是
 * "哪个词被搜得最多"，存流水还得再聚合一次。
 */
@Data
@TableName("knowledge_search_miss")
public class KnowledgeSearchMiss implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 搜索词，已 trim */
    private String keyword;

    /** 被搜索次数，越高越该补内容 */
    private Integer hitCount;

    private LocalDateTime lastSearchedAt;

    private LocalDateTime createdAt;
}
