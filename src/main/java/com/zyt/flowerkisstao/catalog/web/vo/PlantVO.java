package com.zyt.flowerkisstao.catalog.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 植物商品展示对象，把品种与默认 SKU 拍平成一份。
 *
 * <p>刻意不拆 summary / detail 两份：列表页、首页精选和详情页共用前端同一个
 * PlantCard 组件与同一个 TS 类型，拆开只会逼出第二套类型定义，而当前数据量下
 * 省掉的那点字段毫无意义。
 *
 * <p>品种/商品拆表后，上半部分字段名与拆表前逐一保持一致（{@code slug} 仍是
 * 品种 code、{@code light} 仍是展示文案），前端因此不需要任何改动。
 * 下半部分是新增的结构化属性，供第③步推荐算法与第④步下单使用。
 */
@Data
@Builder
public class PlantVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== 以下字段与拆表前一致，前端已在消费，不可改名或删除 =====

    /** 品种主键，管理端操作用 */
    private Long id;

    /** 对外标识，取自 species.code。前端路由与购物车用的都是它 */
    private String slug;

    private String name;

    private String latinName;

    /** 默认 SKU 的售价 */
    private BigDecimal price;

    /** 默认 SKU 的主图 */
    private String image;

    private String imageAlt;

    private String category;

    /** 光照展示文案，取自 species.lightNote */
    private String light;

    /** 浇水展示文案，取自 species.waterNote */
    private String watering;

    /** 规格文案，取自默认 SKU 的 spec */
    private String size;

    /** 猫狗均无毒时为 true */
    private Boolean petFriendly;

    /** petFriendly 为 false 时的隔离提示，按毒性组合派生 */
    private String petNote;

    /** 养护难度文案，由 careLevel 映射 */
    private String difficulty;

    private List<String> matchTags;

    private String recommendationReason;

    private String shortDescription;

    private String description;

    private List<String> careTips;

    /** 默认 SKU 是否为首页精选 */
    private Boolean featured;

    /** 1 启用 0 停用。公开接口恒为 1，管理端需要据此显示状态 */
    private Integer status;

    // ===== 以下为拆表后新增，前端暂未消费 =====

    /** 默认 SKU 的主键，加购与下单提交的是它 */
    private Long defaultSkuId;

    /** 该品种下全部上架 SKU，供详情页切换规格 */
    private List<SkuVO> skus;

    /** leaf 观叶 / flower 观花 */
    private String ornamentalType;

    private String bloomSeason;

    private String bloomColor;

    private String fragrance;

    /** 可接受光照等级区间 1-4，推荐算法的硬过滤依据 */
    private Integer lightMin;

    private Integer lightMax;

    private Integer tempMin;

    private Integer tempMax;

    private Integer humidityMin;

    private Integer humidityMax;

    /** 浇水间隔天数，越小越费心 */
    private Integer waterIntervalDays;

    private Integer fertilizeIntervalDays;

    private Integer repotIntervalMonths;

    private Boolean pruneNeeded;

    /** 1 新手友好 / 2 需要关注 / 3 进阶养护 */
    private Integer careLevel;

    /** 毒性分开返回，推荐算法按用户实际养的宠物类型精确过滤 */
    private Boolean toxicCat;

    private Boolean toxicDog;

    private Boolean toxicChild;

    private Boolean pollenRisk;

    private Integer matureHeightCm;

    /** 成株冠幅厘米，空间硬过滤依据 */
    private Integer footprintCm;
}
