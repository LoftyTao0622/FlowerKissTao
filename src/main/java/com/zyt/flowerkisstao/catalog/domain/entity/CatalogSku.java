package com.zyt.flowerkisstao.catalog.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 植物商品 SKU，承载销售属性。
 *
 * <p>一个品种可挂多个 SKU：同一株琴叶榕的 75 厘米陶盆版与 110 厘米水泥盆版
 * 价格与库存都不同，但"喜明亮散射光、猫狗有毒"是品种层的事。
 *
 * <p>库存放在这一层。第④步下单扣的是这里的 {@code stock}，不是品种。
 */
@Data
@TableName("catalog_sku")
public class CatalogSku implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属品种 catalog_species.id。不设数据库外键，引用完整性在 Service 层校验 */
    private Long speciesId;

    /** 商品编码，如 fiddle-leaf-fig-75。数据库唯一 */
    private String skuCode;

    /** 规格文案，如"中大型 · 约 75 厘米" */
    private String spec;

    /** 盆器，如"米白陶盆" */
    private String pot;

    private BigDecimal price;

    /** 可售库存，下单时事务内扣减 */
    private Integer stock;

    /** 形如 /images/fiddle-leaf-fig.webp，由前端 public 目录提供 */
    private String image;

    private String imageAlt;

    /** 1 首页精选 0 普通 */
    private Integer featured;

    /** 1 上架 0 下架 */
    private Integer status;

    /** 同品种内排序，值大者为默认 SKU */
    private Integer sort;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
