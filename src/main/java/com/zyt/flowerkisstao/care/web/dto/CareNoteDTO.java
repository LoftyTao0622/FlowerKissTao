package com.zyt.flowerkisstao.care.web.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 成长记录入参。
 *
 * <p>图片以 base64 传，由 Service 层校验大小上限——方案里写的对象存储本项目没有搭，
 * 而"能拍张照记录长势"是这个模块最直观的价值。
 */
@Data
public class CareNoteDTO {

    @Size(max = 500, message = "记录内容最长 500 字")
    private String content;

    /** base64 图片（可带 data:image/... 前缀），限 300KB */
    private String image;
}
