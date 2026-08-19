package com.zyt.flowerkisstao.recommendation.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 一次推荐里的一条结果。
 *
 * <p>{@link #scoreJson} 存七项分项得分，前端据此画分项条。只给一个总分的话，
 * 用户没法判断"85 分"是因为光照贴合还是因为便宜——方案原文要求"展示总分、
 * 分项匹配度、适合场景、养护难度、主要风险及推荐理由"，分项是其中一项。
 */
@Data
@TableName(value = "rec_result_item", autoResultMap = true)
public class RecResultItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resultId;

    private Long speciesId;

    /** 价格最贴合预算的那个 SKU，点击直达详情页 */
    private Long skuId;

    /** 加权总分，0-100，两位小数 */
    private BigDecimal totalScore;

    /** 七项分项得分，各项 0-100，键为 light/temp/humidity/care/space/budget/preference */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Integer> scoreJson;

    /** 推荐理由文案，取得分最高的三维各一句 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> reasonsJson;

    /** 主要风险提示，由得分最低那一维生成。七维都高分时为 null */
    private String risk;

    /** 名次，从 1 起 */
    private Integer rankNo;

    /** 1 用户点进过详情页。第⑦步运营看板算"推荐点击率" */
    private Integer clicked;
}
