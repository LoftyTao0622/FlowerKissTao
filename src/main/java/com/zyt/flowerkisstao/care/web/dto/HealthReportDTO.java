package com.zyt.flowerkisstao.care.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 健康反馈入参。方案原文："反馈叶片发黄…系统可重新评估后续频率并给出逐项排查建议。"
 */
@Data
public class HealthReportDTO {

    /** yellowing 叶片发黄 / wilting 萎蔫 / spots 斑点 / dropping 落叶 / pest 疑似虫害 */
    @NotBlank(message = "请选择症状")
    private String symptom;

    @Size(max = 500, message = "补充说明最长 500 字")
    private String detail;
}
