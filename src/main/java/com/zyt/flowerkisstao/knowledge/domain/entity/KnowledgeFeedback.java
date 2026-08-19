package com.zyt.flowerkisstao.knowledge.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏与有用性反馈。
 *
 * <p>两种行为共用一张表靠 {@link #type} 区分：数据结构完全相同，拆两张表只会让
 * "我对这篇做过什么"要查两次。
 *
 * <p>唯一键 (user_id, article_id, type) 是关键：没有它，用户连点两次"有用"
 * 就会让计数翻倍。应用层"先查有没有点过"在并发下拦不住，唯一索引能。
 */
@Data
@TableName("knowledge_feedback")
public class KnowledgeFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 有用性反馈 */
    public static final String TYPE_USEFUL = "useful";
    /** 收藏 */
    public static final String TYPE_FAVORITE = "favorite";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long articleId;

    /** useful / favorite */
    private String type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
