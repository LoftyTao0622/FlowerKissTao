package com.zyt.flowerkisstao.catalog.web.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 逛植物页的筛选项。取自库中上架商品的实际取值，避免前端硬编码枚举后与数据漂移。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantFacetsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> categories;

    private List<String> lights;
}
