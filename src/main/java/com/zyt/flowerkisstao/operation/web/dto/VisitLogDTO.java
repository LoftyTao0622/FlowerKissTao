package com.zyt.flowerkisstao.operation.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 轻量页面访问埋点。不接受请求 body 或自定义用户 id，避免带入个人数据。 */
@Data
public class VisitLogDTO {

    @NotBlank(message = "path 不能为空")
    @Size(max = 255)
    private String path;

    @NotBlank(message = "pageType 不能为空")
    @Size(max = 40)
    private String pageType;

    @Size(max = 255)
    private String referrer;
}
