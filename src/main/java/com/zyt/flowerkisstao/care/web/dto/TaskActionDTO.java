package com.zyt.flowerkisstao.care.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 完成任务时可附一句记录；延后时给天数 */
@Data
public class TaskActionDTO {

    @Size(max = 255, message = "记录最长 255 字")
    private String note;

    /** 延后天数，仅 postpone 用 */
    @Min(value = 1, message = "至少延后 1 天")
    @Max(value = 30, message = "最多延后 30 天")
    private Integer days;
}
