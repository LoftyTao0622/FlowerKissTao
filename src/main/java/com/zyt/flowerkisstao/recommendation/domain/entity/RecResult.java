package com.zyt.flowerkisstao.recommendation.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 一次推荐的快照。方案要求推荐"可解释、可复现"，这张表就是那句话的落地。
 *
 * <p>只存品种 id 列表做不到可复现：管理员改过权重之后，历史推荐再也解释不清
 * 当时为什么是这个排序。所以 {@link #weightSnapshot} 把当时的七项权重一起存下来，
 * 展示时按快照解释而不是按当前配置。
 *
 * <p>{@code autoResultMap = true} 不能省：没有它，JSON 列的 typeHandler 只在写入时
 * 生效，查询时会静默返回 null——插入看着正常，读出来却是空的。
 *
 * <p>没有 {@code deleted}：快照是历史事实，不提供删除。
 */
@Data
@TableName(value = "rec_result", autoResultMap = true)
public class RecResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户。查询一律带上它，越权访问按"不存在"处理，与画像模块一致 */
    private Long userId;

    /** 输入画像 user_scene_profile.id */
    private Long profileId;

    /** 当时的场景名。画像改名或删除后，历史记录仍显示当时那个名字 */
    private String sceneNameSnapshot;

    /**
     * 当时的七项权重与 topN，形如 {@code {"light":30,"temp":15,...,"topN":3}}。
     *
     * <p>不存这个，"可复现"就是空话。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Integer> weightSnapshot;

    /** 参与筛选的在售品种总数 */
    private Integer totalCount;

    /** 硬过滤后剩余的候选数。为 0 时前端展示诊断而不是空白 */
    private Integer candidateCount;

    /**
     * 各条硬规则各排除了多少株，形如 {@code {"toxic":10,"light":1,"space":0,...}}。
     *
     * <p>无候选时前端要能说出"是预算卡掉了 6 株"，而不是干巴巴一句"没有结果"。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Integer> filteredJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
