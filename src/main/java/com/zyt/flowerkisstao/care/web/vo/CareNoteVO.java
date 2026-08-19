package com.zyt.flowerkisstao.care.web.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 一条成长记录 */
@Data
@Builder
public class CareNoteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** text 文字 / photo 带图 / health 健康反馈 */
    private String noteType;

    private String content;

    /** base64 图片。列表接口不返回它，只在详情里带，否则一页几 MB */
    private String image;

    /** 有没有图，列表接口据此显示缩略图占位 */
    private Boolean hasImage;

    private LocalDateTime createdAt;
}
