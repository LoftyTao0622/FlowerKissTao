package com.zyt.flowerkisstao.care.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 养护档案，即"我的植物"。
 *
 * <p>列表页与详情页共用：列表时 {@link #tasks} 只带最近几条待办，
 * {@link #notes} 为空。
 */
@Data
@Builder
public class CareArchiveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long speciesId;

    /** 品种 code，跳商品详情页用 */
    private String slug;

    private String plantName;

    private String plantImage;

    private String orderNo;

    private LocalDateTime adoptedAt;

    /** 养了多少天，卡片上显示"已陪伴 32 天" */
    private Long adoptedDays;

    /** 1 养护中 0 已归档 */
    private Integer status;

    // ===== 任务概况，列表卡片直接用 =====

    /** 待办任务数（含逾期） */
    private Integer pendingCount;

    /** 已逾期任务数，卡片上标红 */
    private Integer overdueCount;

    /** 下一个任务，卡片上显示"3 天后：检查盆土并浇水" */
    private CareTaskVO nextTask;

    // ===== 当前养护节奏，重评估后这里会变 =====

    /** 当前浇水间隔天数（已含季节与环境调整） */
    private Integer waterIntervalDays;

    /** 频率调整系数百分比，100 表示未调整 */
    private Integer waterFactor;

    /** 调整说明，如"因连续遗漏已放宽两成"。未调整时为 null */
    private String adjustmentNote;

    /** 连续遗漏计数 */
    private Integer missedCount;

    /** 建档时的光照档位，与当前场景比对可知有没有变过 */
    private Integer lightLevelSnapshot;

    /** 当前场景光照档位 */
    private Integer currentLightLevel;

    // ===== 详情页才带 =====

    private List<CareTaskVO> tasks;

    private List<CareNoteVO> notes;
}
