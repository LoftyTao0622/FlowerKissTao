package com.zyt.flowerkisstao.recommendation.web.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 一条硬过滤规则的排除计数，无候选时的诊断依据。
 *
 * <p>{@link #relaxable} 是给前端用的：不可放宽的两条（宠物、儿童）在界面上标注
 * "不建议放宽"并禁掉调整入口，避免用户顺手把安全约束关掉。
 */
@Data
@AllArgsConstructor
public class FilterDiagnosticVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** pet / child / light / space / budget / water */
    private String code;

    /** 中文名，如"宠物安全" */
    private String label;

    /** 这条规则排除了多少株 */
    private Integer excluded;

    /** false 表示安全约束，界面上应标注"不建议放宽" */
    private Boolean relaxable;
}
