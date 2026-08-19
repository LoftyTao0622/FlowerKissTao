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
import java.time.LocalDateTime;
import java.util.List;

/**
 * 植物品种，承载与"卖哪个规格"无关的知识属性。
 *
 * <p>每组环境条件都存两份：数值列（{@code lightMin}、{@code waterIntervalDays}…）
 * 供推荐算法做硬过滤与加权评分，{@code *Note} 列供页面展示。合成一列的话，
 * "明亮散射光"这种文案参与不了任何数值比较。
 *
 * <p>{@code autoResultMap = true} 不能省：没有它，matchTags / careTips 的 typeHandler
 * 只在写入时生效，查询时会静默返回 null——插入看着一切正常，页面上标签却是空的。
 */
@Data
@TableName(value = "catalog_species", autoResultMap = true)
public class CatalogSpecies implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 光照等级：低光。数值与 schema 注释一致，硬过滤按等级比较 */
    public static final int LIGHT_LOW = 1;
    /** 光照等级：充足直射 */
    public static final int LIGHT_FULL_SUN = 4;

    /** 养护难度：新手友好 */
    public static final int CARE_EASY = 1;
    /** 养护难度：进阶养护 */
    public static final int CARE_HARD = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对外的详情页标识，如 fiddle-leaf-fig。数据库唯一，前端路由用的就是它 */
    private String code;

    private String name;

    private String latinName;

    private String category;

    /** leaf 观叶 / flower 观花 */
    private String ornamentalType;

    /** 以下三项仅观花品种填写 */
    private String bloomSeason;

    private String bloomColor;

    private String fragrance;

    /** 可接受的最低光照等级 1-4 */
    private Integer lightMin;

    /** 可接受的最高光照等级 1-4，须不小于 lightMin */
    private Integer lightMax;

    /** 光照展示文案，如"明亮散射光" */
    private String lightNote;

    private Integer tempMin;

    private Integer tempMax;

    private Integer humidityMin;

    private Integer humidityMax;

    /** 浇水间隔天数，越小越费心。与用户"每周可浇水次数"比对 */
    private Integer waterIntervalDays;

    private String waterNote;

    /** 施肥间隔天数，购后养护任务按它排期 */
    private Integer fertilizeIntervalDays;

    private Integer repotIntervalMonths;

    /** 1 需定期修剪 0 不需要 */
    private Integer pruneNeeded;

    /** 1 新手友好 / 2 需要关注 / 3 进阶养护 */
    private Integer careLevel;

    /** 毒性分猫狗儿童三列存：猫毒与狗毒并非同一批植物，合成一列做不到按宠物类型精确排除 */
    private Integer toxicCat;

    private Integer toxicDog;

    private Integer toxicChild;

    private Integer pollenRisk;

    /** 成株高度厘米 */
    private Integer matureHeightCm;

    /** 成株冠幅厘米，小空间放不下大冠幅 */
    private Integer footprintCm;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> matchTags;

    private String recommendationReason;

    private String shortDescription;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> careTips;

    /** 1 启用 0 停用。停用后公开接口一律查不到 */
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
