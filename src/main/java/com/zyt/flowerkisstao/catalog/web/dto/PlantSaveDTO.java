package com.zyt.flowerkisstao.catalog.web.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 新建与编辑共用的入参。
 *
 * <p>imageAlt 是必填而非可选：产品的无障碍基线是 WCAG 2.1 AA，缺了替代文本，
 * 读屏用户在列表页就只能听到一串文件名。
 */
@Data
public class PlantSaveDTO {

    @NotBlank(message = "slug 不能为空")
    @Size(max = 80, message = "slug 最长 80 个字符")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
            message = "slug 只能是小写字母、数字和单个短横线，如 fiddle-leaf-fig")
    private String slug;

    @NotBlank(message = "名称不能为空")
    @Size(max = 50, message = "名称最长 50 个字符")
    private String name;

    @NotBlank(message = "拉丁学名不能为空")
    @Size(max = 100, message = "拉丁学名最长 100 个字符")
    private String latinName;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0", message = "价格不能为负数")
    private BigDecimal price;

    @NotBlank(message = "主图地址不能为空")
    @Size(max = 255, message = "主图地址最长 255 个字符")
    private String image;

    @NotBlank(message = "主图 alt 文案不能为空，无障碍要求必填")
    @Size(max = 120, message = "主图 alt 文案最长 120 个字符")
    private String imageAlt;

    @NotBlank(message = "分类不能为空")
    @Size(max = 20, message = "分类最长 20 个字符")
    private String category;

    @NotBlank(message = "光照条件不能为空")
    @Size(max = 20, message = "光照条件最长 20 个字符")
    private String light;

    @NotBlank(message = "浇水说明不能为空")
    @Size(max = 60, message = "浇水说明最长 60 个字符")
    private String watering;

    @NotBlank(message = "尺寸说明不能为空")
    @Size(max = 60, message = "尺寸说明最长 60 个字符")
    private String size;

    @NotNull(message = "请标明是否宠物友好")
    private Boolean petFriendly;

    @Size(max = 60, message = "宠物提示最长 60 个字符")
    private String petNote;

    @NotBlank(message = "养护难度不能为空")
    @Size(max = 20, message = "养护难度最长 20 个字符")
    private String difficulty;

    @Size(max = 8, message = "适配标签最多 8 个")
    private List<String> matchTags;

    @NotBlank(message = "推荐理由不能为空")
    @Size(max = 255, message = "推荐理由最长 255 个字符")
    private String recommendationReason;

    @NotBlank(message = "简介不能为空")
    @Size(max = 255, message = "简介最长 255 个字符")
    private String shortDescription;

    private String description;

    @Size(max = 10, message = "养护备忘最多 10 条")
    private List<String> careTips;

    private Boolean featured;

    private Integer sort;
}
