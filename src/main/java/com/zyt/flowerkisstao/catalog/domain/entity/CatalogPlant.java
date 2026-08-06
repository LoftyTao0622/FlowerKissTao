package com.zyt.flowerkisstao.catalog.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 植物商品。
 *
 * <p>{@code autoResultMap = true} 不能省：没有它，matchTags / careTips 的 typeHandler
 * 只在写入时生效，查询时会静默返回 null——插入看着一切正常，页面上标签却是空的。
 */
@Data
@TableName(value = "catalog_plant", autoResultMap = true)
public class CatalogPlant implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对外的详情页标识，如 fiddle-leaf-fig。数据库唯一 */
    private String slug;

    private String name;

    private String latinName;

    private BigDecimal price;

    /** 形如 /images/fiddle-leaf-fig.webp，由前端 public 目录提供 */
    private String image;

    private String imageAlt;

    private String category;

    private String light;

    private String watering;

    private String size;

    /** 1 宠物友好 0 需隔离 */
    private Integer petFriendly;

    private String petNote;

    private String difficulty;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> matchTags;

    private String recommendationReason;

    private String shortDescription;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> careTips;

    /** 1 首页精选 0 普通 */
    private Integer featured;

    /** 1 上架 0 下架。下架后公开接口一律查不到 */
    private Integer status;

    /** 展示排序，值大者靠前 */
    private Integer sort;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
