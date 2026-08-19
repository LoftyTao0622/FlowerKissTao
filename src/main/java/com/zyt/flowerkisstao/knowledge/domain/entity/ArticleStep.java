package com.zyt.flowerkisstao.knowledge.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文章里的一个步骤。方案要求详情"采用步骤化说明"，这是其中一步。
 *
 * <p>拆成 title + detail 而不是一整句：列表上只显示 title 就能扫完全流程，
 * 展开才看 detail。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleStep implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 这一步做什么，一句话 */
    private String title;

    /** 具体怎么做、用量与注意事项 */
    private String detail;
}
