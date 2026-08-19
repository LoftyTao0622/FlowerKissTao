package com.zyt.flowerkisstao.trade.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 取消订单、申请售后、驳回售后共用：都只需要一句理由 */
@Data
public class ReasonDTO {

    @NotBlank(message = "请填写原因")
    @Size(max = 200, message = "原因最长 200 个字符")
    private String reason;
}
