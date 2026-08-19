package com.zyt.flowerkisstao.user.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 场景画像展示对象。
 *
 * <p>除原始档位外还返回一组展示文案（{@code *Label}）与养护能力标签：
 * 首页的档案卡片和问卷的回顾页都要显示"明亮散射光"而不是"3"，
 * 把这层映射放在后端，前端与将来的推荐理由文案才不会各写一套说法。
 */
@Data
@Builder
public class SceneProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String sceneName;

    private Boolean isDefault;

    // ===== 环境条件 =====

    private String placement;

    private String placementLabel;

    private Integer lightLevel;

    private String lightLabel;

    private Integer tempLevel;

    private String tempLabel;

    private Integer humidityLevel;

    private String humidityLabel;

    private Integer spaceLevel;

    private String spaceLabel;

    /** 冠幅上限厘米，由 spaceLevel 派生 */
    private Integer maxFootprintCm;

    private Integer ventilation;

    private String ventilationLabel;

    private BigDecimal budgetMin;

    private BigDecimal budgetMax;

    private String preferOrnamental;

    private String preferOrnamentalLabel;

    // ===== 安全条件 =====

    private Boolean hasChild;

    private Boolean hasCat;

    private Boolean hasDog;

    // ===== 养护能力 =====

    private Integer experienceLevel;

    private String experienceLabel;

    private Integer waterTimesWeek;

    private Integer travelFrequency;

    private String travelLabel;

    private Boolean forgetful;

    private Boolean acceptRepot;

    private Boolean acceptFertilize;

    private Boolean acceptPrune;

    /**
     * 养护能力标签，如"新手友好"、"经常出差"、"低频浇水"。
     *
     * <p>方案要求画像"生成可编辑的环境画像与养护能力标签"，这就是那组标签。
     * 第③步的推荐理由可以直接引用它们说明"为什么这株适合你"。
     */
    private List<String> careTags;
}
