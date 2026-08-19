package com.zyt.flowerkisstao.trade.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址的新建与编辑入参。
 *
 * <p>没有 isDefault：设默认是单独一个接口，免得编辑表单顺手把默认地址改掉。
 */
@Data
public class AddressSaveDTO {

    @NotBlank(message = "收件人不能为空")
    @Size(max = 30, message = "收件人姓名最长 30 个字符")
    private String receiver;

    @NotBlank(message = "联系电话不能为空")
    // 手机号与座机都允许：填座机的多是办公室收货，卡死手机号会把这类人挡在门外
    @Pattern(regexp = "^[0-9-]{7,20}$", message = "请填写有效的联系电话")
    private String phone;

    @NotBlank(message = "请选择省份")
    @Size(max = 30)
    private String province;

    @NotBlank(message = "请选择城市")
    @Size(max = 30)
    private String city;

    @NotBlank(message = "请选择区县")
    @Size(max = 30)
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 120, message = "详细地址最长 120 个字符")
    private String detail;
}
