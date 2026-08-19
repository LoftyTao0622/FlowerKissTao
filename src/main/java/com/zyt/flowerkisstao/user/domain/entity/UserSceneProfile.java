package com.zyt.flowerkisstao.user.domain.entity;

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
 * 用户场景画像，推荐算法的输入端。
 *
 * <p>一个用户可存多份：客厅、卧室、办公室、阳台的光照与空间条件完全不同，
 * 共用一份画像等于假装它们一样。
 *
 * <p>温湿度存 1/2/3 档而非精确数值——用户量不出自家空气湿度，逼他们填
 * "22.5℃"只会得到瞎猜的数据。档位与 {@code CatalogSpecies} 的 temp/humidity
 * 区间比对同样能算。
 *
 * <p>这里只存输入，不存推荐结果。方案原文："画像只负责提供标准化输入，
 * 不直接决定排序"。
 */
@Data
@TableName("user_scene_profile")
public class UserSceneProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 空间档位：桌面 */
    public static final int SPACE_DESKTOP = 1;
    /** 空间档位：层架 */
    public static final int SPACE_SHELF = 2;
    /** 空间档位：落地 */
    public static final int SPACE_FLOOR = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 sys_user.id。所有查询都必须带上它，否则会读到别人的画像 */
    private Long userId;

    /** 场景名，如"客厅"。同用户下唯一 */
    private String sceneName;

    /** 1 默认场景，每用户至多一个 */
    private Integer isDefault;

    // ===== 环境条件 =====

    /** living_room / bedroom / office / balcony / other */
    private String placement;

    /** 1 低光 / 2 柔和散射 / 3 明亮散射 / 4 充足直射，与 species.lightMin~lightMax 同刻度 */
    private Integer lightLevel;

    /** 1 偏冷 &lt;15℃ / 2 常温 15-28℃ / 3 偏热 &gt;28℃ */
    private Integer tempLevel;

    /** 1 干燥 &lt;40% / 2 适中 40-70% / 3 潮湿 &gt;70% */
    private Integer humidityLevel;

    /** 1 桌面 / 2 层架 / 3 落地 */
    private Integer spaceLevel;

    /**
     * 冠幅上限厘米，由 spaceLevel 派生并冗余存储。
     *
     * <p>存下来后硬过滤就是一句 {@code species.footprintCm <= profile.maxFootprintCm}，
     * 不必在 SQL 里写 CASE。派生规则见 SceneProfileServiceImpl#deriveMaxFootprint。
     */
    private Integer maxFootprintCm;

    /** 1 较差 / 2 一般 / 3 良好 */
    private Integer ventilation;

    private BigDecimal budgetMin;

    /** 预算上限。硬过滤据此排除超价商品 */
    private BigDecimal budgetMax;

    /** leaf 观叶 / flower 观花 / any 都行 */
    private String preferOrnamental;

    // ===== 安全条件，与 species 的 toxicChild / toxicCat / toxicDog 一一对应 =====

    private Integer hasChild;

    private Integer hasCat;

    private Integer hasDog;

    // ===== 养护能力 =====

    /** 1 新手 / 2 有一些 / 3 有经验 */
    private Integer experienceLevel;

    /** 每周可浇水次数 0-7，与 species.waterIntervalDays 比对 */
    private Integer waterTimesWeek;

    /** 1 很少 / 2 偶尔 / 3 频繁出差 */
    private Integer travelFrequency;

    /** 1 容易忘记养护 */
    private Integer forgetful;

    private Integer acceptRepot;

    private Integer acceptFertilize;

    private Integer acceptPrune;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
