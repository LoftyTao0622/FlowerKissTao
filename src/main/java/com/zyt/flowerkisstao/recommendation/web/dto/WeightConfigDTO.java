package com.zyt.flowerkisstao.recommendation.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 权重配置的入参。
 *
 * <p>字段名与 GET 返回的 {@code WeightSet} 逐一对应（light/temp/humidity/…），
 * 管理端读回来什么形状就能改什么形状送回去。<b>不要改成 wLight 这类命名</b>：
 * Lombok 生成的 getter 是 {@code getWLight()}，Jackson 按 Bean 规范会把它解析成
 * {@code wlight}，前端照着字段名传 {@code wLight} 会静默绑不上——所有值都是 null，
 * 报出来的却是"光照权重不能为空"，排查起来很费时间。
 *
 * <p>单字段注解只能拦住"某一项越界"，拦不住"七项之和不是 100"——那是跨字段
 * 约束，由 WeightConfigServiceImpl 在落库前统一校验，数据库的 ck_rec_weight_sum
 * 是最后一道防线。
 */
@Data
public class WeightConfigDTO {

    @NotNull(message = "光照权重不能为空")
    @Min(value = 0, message = "权重只能是 0 到 100")
    @Max(value = 100, message = "权重只能是 0 到 100")
    private Integer light;

    @NotNull(message = "温度权重不能为空")
    @Min(value = 0, message = "权重只能是 0 到 100")
    @Max(value = 100, message = "权重只能是 0 到 100")
    private Integer temp;

    @NotNull(message = "湿度权重不能为空")
    @Min(value = 0, message = "权重只能是 0 到 100")
    @Max(value = 100, message = "权重只能是 0 到 100")
    private Integer humidity;

    @NotNull(message = "养护权重不能为空")
    @Min(value = 0, message = "权重只能是 0 到 100")
    @Max(value = 100, message = "权重只能是 0 到 100")
    private Integer care;

    @NotNull(message = "空间权重不能为空")
    @Min(value = 0, message = "权重只能是 0 到 100")
    @Max(value = 100, message = "权重只能是 0 到 100")
    private Integer space;

    @NotNull(message = "预算权重不能为空")
    @Min(value = 0, message = "权重只能是 0 到 100")
    @Max(value = 100, message = "权重只能是 0 到 100")
    private Integer budget;

    @NotNull(message = "偏好权重不能为空")
    @Min(value = 0, message = "权重只能是 0 到 100")
    @Max(value = 100, message = "权重只能是 0 到 100")
    private Integer preference;

    @NotNull(message = "返回条数不能为空")
    @Min(value = 1, message = "返回条数只能是 1 到 20")
    @Max(value = 20, message = "返回条数只能是 1 到 20")
    private Integer topN;

    /** 七项之和。跨字段校验用，注解拦不住它 */
    public int sum() {
        return light + temp + humidity + care + space + budget + preference;
    }
}
