package com.zyt.flowerkisstao.user.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 场景画像新建与编辑共用的入参。
 *
 * <p>每个字段都带范围校验：这些值会直接进推荐算法的硬过滤与加权计算，
 * 一个手滑填成 9 的光照等级不会报错，只会让整份推荐悄悄失真。
 *
 * <p>跨字段的矛盾（预算下限高于上限、浇水 0 次却说不健忘）单字段注解拦不住，
 * 由 SceneProfileServiceImpl 在保存前统一校验。
 */
@Data
public class SceneProfileSaveDTO {

    @NotBlank(message = "场景名称不能为空")
    @Size(max = 20, message = "场景名称最长 20 个字符")
    private String sceneName;

    @NotBlank(message = "摆放位置不能为空")
    @Pattern(regexp = "^(living_room|bedroom|office|balcony|other)$",
            message = "摆放位置只能是客厅、卧室、办公室、阳台或其他")
    private String placement;

    @NotNull(message = "请选择光照条件")
    @Min(value = 1, message = "光照条件只能是 1 到 4")
    @Max(value = 4, message = "光照条件只能是 1 到 4")
    private Integer lightLevel;

    @NotNull(message = "请选择温度档位")
    @Min(value = 1, message = "温度档位只能是 1 偏冷 / 2 常温 / 3 偏热")
    @Max(value = 3, message = "温度档位只能是 1 偏冷 / 2 常温 / 3 偏热")
    private Integer tempLevel;

    @NotNull(message = "请选择湿度档位")
    @Min(value = 1, message = "湿度档位只能是 1 干燥 / 2 适中 / 3 潮湿")
    @Max(value = 3, message = "湿度档位只能是 1 干燥 / 2 适中 / 3 潮湿")
    private Integer humidityLevel;

    @NotNull(message = "请选择摆放空间")
    @Min(value = 1, message = "摆放空间只能是 1 桌面 / 2 层架 / 3 落地")
    @Max(value = 3, message = "摆放空间只能是 1 桌面 / 2 层架 / 3 落地")
    private Integer spaceLevel;

    @NotNull(message = "请选择通风状况")
    @Min(value = 1, message = "通风状况只能是 1 较差 / 2 一般 / 3 良好")
    @Max(value = 3, message = "通风状况只能是 1 较差 / 2 一般 / 3 良好")
    private Integer ventilation;

    @NotNull(message = "预算下限不能为空")
    @DecimalMin(value = "0", message = "预算不能为负数")
    private BigDecimal budgetMin;

    @NotNull(message = "预算上限不能为空")
    @DecimalMin(value = "0", message = "预算不能为负数")
    private BigDecimal budgetMax;

    @NotBlank(message = "请选择观赏偏好")
    @Pattern(regexp = "^(leaf|flower|any)$", message = "观赏偏好只能是观叶、观花或都可以")
    private String preferOrnamental;

    @NotNull(message = "请标明家中是否有幼童")
    private Boolean hasChild;

    @NotNull(message = "请标明家中是否养猫")
    private Boolean hasCat;

    @NotNull(message = "请标明家中是否养狗")
    private Boolean hasDog;

    @NotNull(message = "请选择养护经验")
    @Min(value = 1, message = "养护经验只能是 1 新手 / 2 有一些 / 3 有经验")
    @Max(value = 3, message = "养护经验只能是 1 新手 / 2 有一些 / 3 有经验")
    private Integer experienceLevel;

    @NotNull(message = "请填写每周可浇水次数")
    @Min(value = 0, message = "每周浇水次数需在 0 到 7 之间")
    @Max(value = 7, message = "每周浇水次数需在 0 到 7 之间")
    private Integer waterTimesWeek;

    @NotNull(message = "请选择出差频率")
    @Min(value = 1, message = "出差频率只能是 1 很少 / 2 偶尔 / 3 频繁")
    @Max(value = 3, message = "出差频率只能是 1 很少 / 2 偶尔 / 3 频繁")
    private Integer travelFrequency;

    @NotNull(message = "请标明是否容易忘记养护")
    private Boolean forgetful;

    @NotNull(message = "请标明是否接受换盆")
    private Boolean acceptRepot;

    @NotNull(message = "请标明是否接受施肥")
    private Boolean acceptFertilize;

    @NotNull(message = "请标明是否接受修剪")
    private Boolean acceptPrune;
}
