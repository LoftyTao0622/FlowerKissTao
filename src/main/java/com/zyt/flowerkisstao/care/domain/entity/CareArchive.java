package com.zyt.flowerkisstao.care.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 养护档案，也就是用户界面上的"我的植物"。
 *
 * <p>确认收货时自动创建。方案原文："订单确认收货后，系统依据购买的植物品种、
 * 用户场景、当前季节和养护难度自动创建个人养护档案。"
 *
 * <p>植物名与图片存快照：商品会改名、下架、删除，但"我三个月前买的那株琴叶榕"
 * 是既成事实，档案不该跟着商品一起变或一起消失。
 */
@Data
@TableName("care_archive")
public class CareArchive implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 养护中 */
    public static final int STATUS_ACTIVE = 1;
    /** 已归档（植物送人了、养死了） */
    public static final int STATUS_CLOSED = 0;

    /** 连续遗漏到这个数就触发频率重评估 */
    public static final int MISSED_THRESHOLD = 2;

    /** 频率系数基准值。100 表示按品种原始周期 */
    public static final int FACTOR_BASE = 100;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long speciesId;

    /** 品种名快照 */
    private String plantName;

    /** 主图快照，卡片展示用 */
    private String plantImage;

    private Long orderId;

    private String orderNo;

    /** 建档时的场景 user_scene_profile.id */
    private Long sceneId;

    /**
     * 建档时的光照档位，是"光照变化检测"的基准线。
     *
     * <p>没有这个基准就只能知道现在光照是多少，判断不出它变没变过——
     * 而方案要求的正是"若用户…修改了光照环境，系统可重新评估后续频率"。
     */
    private Integer lightLevelSnapshot;

    /** 入手时间，等于确认收货时间 */
    private LocalDateTime adoptedAt;

    /**
     * 连续遗漏任务数，达到 {@link #MISSED_THRESHOLD} 触发调频。
     *
     * <p>关键在"连续"：完成任意一个任务就清零。现算区分不了连续与累计。
     */
    private Integer missedCount;

    /** 浇水间隔调整系数（百分比）。120 表示间隔比品种原始周期长两成 */
    private Integer waterFactor;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
