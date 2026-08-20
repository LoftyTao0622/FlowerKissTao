package com.zyt.flowerkisstao.knowledge.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 检索筛选项。与 catalog 的 PlantFacets 同一思路：
 * 只返回真实存在已发布文章的取值，筛选框里永远不会出现筛完是空结果的选项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleFacetsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Option> categories;

    private List<Option> difficulties;

    private List<Option> seasons;

    private List<String> tags;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Option implements Serializable {

        private static final long serialVersionUID = 1L;

        private String value;

        private String label;
    }
}
