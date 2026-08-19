package com.zyt.flowerkisstao.catalog.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * SKU 新建与编辑共用的入参。
 *
 * <p>imageAlt 是必填而非可选：产品的无障碍基线是 WCAG 2.1 AA，缺了替代文本，
 * 读屏用户在列表页就只能听到一串文件名。
 */
@Data
public class SkuSaveDTO {

    @NotNull(message = "所属品种不能为空")
    private Long speciesId;

    @NotBlank(message = "商品编码不能为空")
    @Size(max = 80, message = "商品编码最长 80 个字符")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
            message = "商品编码只能是小写字母、数字和单个短横线，如 fiddle-leaf-fig-75")
    private String skuCode;

    @NotBlank(message = "规格不能为空")
    @Size(max = 60, message = "规格最长 60 个字符")
    private String spec;

    @Size(max = 40, message = "盆器最长 40 个字符")
    private String pot;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0", message = "价格不能为负数")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @NotBlank(message = "主图地址不能为空")
    @Size(max = 255, message = "主图地址最长 255 个字符")
    private String image;

    @NotBlank(message = "主图 alt 文案不能为空，无障碍要求必填")
    @Size(max = 120, message = "主图 alt 文案最长 120 个字符")
    private String imageAlt;

    private Boolean featured;

    private Integer sort;
}
