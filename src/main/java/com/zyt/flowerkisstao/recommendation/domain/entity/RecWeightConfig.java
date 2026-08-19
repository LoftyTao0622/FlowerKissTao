package com.zyt.flowerkisstao.recommendation.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推荐算法的七维权重配置。整张表只有一行，{@code id} 恒为 {@link #SINGLETON_ID}。
 *
 * <p>七个权重存在同一行而不是拆成七行键值对：它们必须同时满足"和为 100"，
 * 拆开后没有任何一条数据库约束能拦住"只改了一半"的中间状态——那时算出来的
 * 分不在 0-100 刻度上，前端的百分比进度条会画错，却看不出是哪里错了。
 *
 * <p>这里没有 {@code deleted}：单行配置不存在删除语义，删掉推荐就没法算了。
 */
@Data
@TableName("rec_weight_config")
public class RecWeightConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 单行配置的固定主键 */
    public static final long SINGLETON_ID = 1L;

    /** 七项权重之和必须等于它，与 schema 的 ck_rec_weight_sum 同一口径 */
    public static final int WEIGHT_SUM = 100;

    /** 不带 AUTO：这一行由 data.sql 种下，应用层只更新不插入 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 光照权重。默认 30，是七维里最高的一维——光照不对的植物养不活 */
    private Integer wLight;

    private Integer wTemp;

    private Integer wHumidity;

    /** 养护能力权重，默认 20。方案里"养护成本"是仅次于光照的决定因素 */
    private Integer wCare;

    private Integer wSpace;

    private Integer wBudget;

    private Integer wPreference;

    /** 返回条数，默认 3 */
    private Integer topN;

    /** 最后修改人 sys_user.id */
    private Long updatedBy;

    /** 由数据库 ON UPDATE CURRENT_TIMESTAMP 维护，不参与 FieldFill */
    private LocalDateTime updatedAt;

    /** 七项之和，Service 层落库前先自查一遍——低于 8.0.16 的 MySQL 会忽略 CHECK */
    public int weightSum() {
        return wLight + wTemp + wHumidity + wCare + wSpace + wBudget + wPreference;
    }
}
