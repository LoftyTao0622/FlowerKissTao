package com.zyt.flowerkisstao.care.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 成长记录，即界面上的成长时间线。
 *
 * <p>方案原文："用户可标记完成、延后、跳过并上传文字或图片记录"、
 * "成长时间线保存植物状态"。
 *
 * <p>图片以 base64 存 MEDIUMTEXT。方案里写的对象存储本项目没有搭，而
 * "能拍张照记录长势"是这个模块最直观的价值，用 base64 落库是当前基础设施下
 * 唯一能跑通的做法。单张限 300KB 由 Service 层校验。
 */
@Data
@TableName("care_note")
public class CareNote implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 纯文字 */
    public static final String TYPE_TEXT = "text";
    /** 带图 */
    public static final String TYPE_PHOTO = "photo";
    /** 健康反馈，如叶片发黄的排查结论 */
    public static final String TYPE_HEALTH = "health";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long archiveId;

    /** text / photo / health */
    private String noteType;

    private String content;

    /** base64 图片，单张限 300KB */
    private String image;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
