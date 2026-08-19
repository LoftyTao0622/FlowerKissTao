package com.zyt.flowerkisstao.knowledge.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识文章。
 *
 * <p>方案要求详情"采用步骤化说明，突出适用条件、操作频次、常见误区与风险提示"——
 * 那描述的是结构而不是排版，所以四项各占一个字段，而不是塞进一段富文本。
 * 这样既不必引入 markdown 库与随之而来的 XSS 面，也强制作者把四项填齐：
 * 写成一大段时，"常见误区"是最容易被省略的一项。
 *
 * <p>{@code autoResultMap = true} 不能省：没有它，JSON 列的 typeHandler 只在写入时
 * 生效，查询时会静默返回 null——插入看着正常，页面上却是空的。
 */
@Data
@TableName(value = "knowledge_article", autoResultMap = true)
public class KnowledgeArticle implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对外标识，详情页路由用的就是它 */
    private String slug;

    private String title;

    /** 一句话摘要，列表卡片展示 */
    private String summary;

    private String cover;

    /** 见 {@code ArticleCategory} */
    private String category;

    /** 1 入门 / 2 进阶 / 3 专业 */
    private Integer difficulty;

    /** 适用季节 ["spring","summer"]，空表示四季通用 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> seasons;

    /** 自由标签，检索用 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    // ===== 方案点名的步骤化四件套 =====

    /** 适用条件：什么情况下该看这篇 */
    private String applicable;

    /** 操作频次：多久做一次 */
    private String frequency;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<ArticleStep> steps;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> mistakes;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> risks;

    // ===== 关联：三处跳转与个性化推荐的依据 =====

    /** 关联的养护任务类型，与 care_task.task_type 同域 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> relatedTaskTypes;

    /** 专门讲某几个品种时填其 code，空表示通用文章 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> relatedSpecies;

    /** 0 草稿 / 1 待审核 / 2 已发布 / 3 已下架 */
    private Integer status;

    private Long authorId;

    private LocalDateTime publishedAt;

    /** 冗余计数，热门排序与看板直接读 */
    private Integer viewCount;

    private Integer usefulCount;

    private Integer sort;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
