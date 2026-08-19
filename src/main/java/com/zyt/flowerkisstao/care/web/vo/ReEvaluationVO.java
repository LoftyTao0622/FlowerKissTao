package com.zyt.flowerkisstao.care.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 重新评估的结果。
 *
 * <p>方案原文："系统可重新评估后续频率并给出逐项排查建议。"两样都在这里：
 * {@link #checklist} 是排查建议，{@link #newInterval} 是调整后的频率。
 */
@Data
@Builder
public class ReEvaluationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 触发原因：missed 连续遗漏 / light 光照变化 / health 叶片发黄 / manual 手动 */
    private String trigger;

    private String triggerLabel;

    /** 频率有没有真的变。没变时前端不必弹"已调整"的提示 */
    private Boolean adjusted;

    private Integer oldInterval;

    private Integer newInterval;

    /** 一句话说明，如"光照由明亮散射变为低光，浇水间隔已从 10 天调整为 13 天" */
    private String summary;

    /** 逐项排查建议 */
    private List<String> checklist;

    /** 重排了多少条未来任务 */
    private Integer rescheduledCount;
}
