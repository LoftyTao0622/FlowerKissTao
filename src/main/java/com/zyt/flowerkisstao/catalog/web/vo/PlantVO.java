package com.zyt.flowerkisstao.catalog.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 植物商品展示对象。
 *
 * <p>刻意不拆 summary / detail 两份：列表页、首页精选和详情页共用前端同一个
 * PlantCard 组件与同一个 TS 类型，拆开只会逼出第二套类型定义，而当前数据量下
 * 省掉的那点字段毫无意义。
 */
@Data
@Builder
public class PlantVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据库主键，管理端操作用 */
    private Long id;

    /** 对外标识，前端路由与购物车用的都是它 */
    private String slug;

    private String name;

    private String latinName;

    private BigDecimal price;

    private String image;

    private String imageAlt;

    private String category;

    private String light;

    private String watering;

    private String size;

    private Boolean petFriendly;

    private String petNote;

    private String difficulty;

    private List<String> matchTags;

    private String recommendationReason;

    private String shortDescription;

    private String description;

    private List<String> careTips;

    private Boolean featured;

    /** 1 上架 0 下架。公开接口恒为 1，管理端需要据此显示状态 */
    private Integer status;
}
