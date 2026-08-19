package com.zyt.flowerkisstao.knowledge.web.dto;

import com.zyt.flowerkisstao.knowledge.domain.entity.ArticleStep;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 文章新建与编辑共用的入参。
 *
 * <p>{@link #applicable}、{@link #frequency}、{@link #steps} 都是必填——方案要求详情
 * "突出适用条件、操作频次"并"采用步骤化说明"，把它们做成必填是让这条要求真正落地的
 * 唯一办法。写成可选的话，第二篇文章开始就会有人只填正文。
 *
 * <p>没有 status：状态只能通过流转接口改，不能在编辑表单里直接设成"已发布"绕过审核。
 */
@Data
public class ArticleSaveDTO {

    @NotBlank(message = "文章标识不能为空")
    @Size(max = 60, message = "文章标识最长 60 个字符")
    // 只允许小写字母、数字和连字符：它要出现在 URL 里
    @Pattern(regexp = "^[a-z0-9-]+$", message = "文章标识只能包含小写字母、数字和连字符")
    private String slug;

    @NotBlank(message = "标题不能为空")
    @Size(max = 80, message = "标题最长 80 个字符")
    private String title;

    @NotBlank(message = "摘要不能为空")
    @Size(max = 200, message = "摘要最长 200 个字符")
    private String summary;

    @Size(max = 255)
    private String cover;

    @NotBlank(message = "请选择分类")
    private String category;

    @NotNull(message = "请选择难度")
    @Min(value = 1, message = "难度只能是 1 入门 / 2 进阶 / 3 专业")
    @Max(value = 3, message = "难度只能是 1 入门 / 2 进阶 / 3 专业")
    private Integer difficulty;

    /** 空表示四季通用 */
    private List<String> seasons;

    private List<String> tags;

    @NotBlank(message = "适用条件不能为空——读者要先判断这篇是否适用于自己")
    @Size(max = 300, message = "适用条件最长 300 个字符")
    private String applicable;

    @NotBlank(message = "操作频次不能为空")
    @Size(max = 300, message = "操作频次最长 300 个字符")
    private String frequency;

    @NotEmpty(message = "至少写一个操作步骤")
    private List<ArticleStep> steps;

    private List<String> mistakes;

    private List<String> risks;

    private List<String> relatedTaskTypes;

    private List<String> relatedSpecies;

    private Integer sort;
}
