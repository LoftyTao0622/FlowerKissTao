package com.zyt.flowerkisstao.trade.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/** 与 {@code UserAddress} 对应 */
@Data
@Builder
public class AddressVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String receiver;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String detail;

    private Boolean isDefault;

    /** 省市区 + 详细地址拼好的一行，界面直接显示，不必在前端再拼一次 */
    private String fullAddress;
}
