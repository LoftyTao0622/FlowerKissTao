package com.zyt.flowerkisstao.catalog.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 品种新建与编辑共用的入参。
 *
 * <p>结构化字段全部带范围校验：这些值会直接进推荐算法的加权计算，
 * 一个手滑填成 40 的光照等级不会报错，只会让评分悄悄失真。
 * 方案里"后台修改关键属性时需校验取值范围"说的就是这件事。
 */
@Data
public class SpeciesSaveDTO {

    @NotBlank(message = "code 不能为空")
    @Size(max = 80, message = "code 最长 80 个字符")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
            message = "code 只能是小写字母、数字和单个短横线，如 fiddle-leaf-fig")
    private String code;

    @NotBlank(message = "名称不能为空")
    @Size(max = 50, message = "名称最长 50 个字符")
    private String name;

    @NotBlank(message = "拉丁学名不能为空")
    @Size(max = 100, message = "拉丁学名最长 100 个字符")
    private String latinName;

    @NotBlank(message = "分类不能为空")
    @Size(max = 20, message = "分类最长 20 个字符")
    private String category;

    @NotBlank(message = "观赏类型不能为空")
    @Pattern(regexp = "^(leaf|flower)$", message = "观赏类型只能是 leaf 或 flower")
    private String ornamentalType;

    @Size(max = 30, message = "花期最长 30 个字符")
    private String bloomSeason;

    @Size(max = 30, message = "花色最长 30 个字符")
    private String bloomColor;

    @Size(max = 20, message = "香味最长 20 个字符")
    private String fragrance;

    @NotNull(message = "最低光照等级不能为空")
    @Min(value = 1, message = "光照等级只能是 1 到 4")
    @Max(value = 4, message = "光照等级只能是 1 到 4")
    private Integer lightMin;

    @NotNull(message = "最高光照等级不能为空")
    @Min(value = 1, message = "光照等级只能是 1 到 4")
    @Max(value = 4, message = "光照等级只能是 1 到 4")
    private Integer lightMax;

    @NotBlank(message = "光照说明不能为空")
    @Size(max = 20, message = "光照说明最长 20 个字符")
    private String lightNote;

    @NotNull(message = "最低耐受温度不能为空")
    @Min(value = -10, message = "温度需在 -10 到 50 摄氏度之间")
    @Max(value = 50, message = "温度需在 -10 到 50 摄氏度之间")
    private Integer tempMin;

    @NotNull(message = "最高耐受温度不能为空")
    @Min(value = -10, message = "温度需在 -10 到 50 摄氏度之间")
    @Max(value = 50, message = "温度需在 -10 到 50 摄氏度之间")
    private Integer tempMax;

    @NotNull(message = "最低适宜湿度不能为空")
    @Min(value = 0, message = "湿度需在 0 到 100 之间")
    @Max(value = 100, message = "湿度需在 0 到 100 之间")
    private Integer humidityMin;

    @NotNull(message = "最高适宜湿度不能为空")
    @Min(value = 0, message = "湿度需在 0 到 100 之间")
    @Max(value = 100, message = "湿度需在 0 到 100 之间")
    private Integer humidityMax;

    @NotNull(message = "浇水间隔不能为空")
    @Min(value = 1, message = "浇水间隔需在 1 到 90 天之间")
    @Max(value = 90, message = "浇水间隔需在 1 到 90 天之间")
    private Integer waterIntervalDays;

    @NotBlank(message = "浇水说明不能为空")
    @Size(max = 60, message = "浇水说明最长 60 个字符")
    private String waterNote;

    @Min(value = 1, message = "施肥间隔需在 1 到 365 天之间")
    @Max(value = 365, message = "施肥间隔需在 1 到 365 天之间")
    private Integer fertilizeIntervalDays;

    @Min(value = 1, message = "换盆间隔需在 1 到 120 个月之间")
    @Max(value = 120, message = "换盆间隔需在 1 到 120 个月之间")
    private Integer repotIntervalMonths;

    private Boolean pruneNeeded;

    @NotNull(message = "养护难度不能为空")
    @Min(value = 1, message = "养护难度只能是 1 新手友好 / 2 需要关注 / 3 进阶养护")
    @Max(value = 3, message = "养护难度只能是 1 新手友好 / 2 需要关注 / 3 进阶养护")
    private Integer careLevel;

    @NotNull(message = "请标明对猫是否有毒")
    private Boolean toxicCat;

    @NotNull(message = "请标明对狗是否有毒")
    private Boolean toxicDog;

    @NotNull(message = "请标明儿童误食是否有风险")
    private Boolean toxicChild;

    private Boolean pollenRisk;

    @NotNull(message = "成株高度不能为空")
    @Min(value = 1, message = "成株高度需在 1 到 1000 厘米之间")
    @Max(value = 1000, message = "成株高度需在 1 到 1000 厘米之间")
    private Integer matureHeightCm;

    @NotNull(message = "成株冠幅不能为空")
    @Min(value = 1, message = "成株冠幅需在 1 到 1000 厘米之间")
    @Max(value = 1000, message = "成株冠幅需在 1 到 1000 厘米之间")
    private Integer footprintCm;

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

    private Integer sort;
}
