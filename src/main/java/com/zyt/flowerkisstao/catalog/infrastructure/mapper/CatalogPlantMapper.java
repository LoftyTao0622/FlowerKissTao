package com.zyt.flowerkisstao.catalog.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogPlant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CatalogPlantMapper extends BaseMapper<CatalogPlant> {

    /**
     * 筛选项来自库里真实存在的上架商品，而不是前端写死的枚举。
     * 这样下拉框永远不会给出一个筛完是空结果的选项。
     */
    @Select("SELECT DISTINCT category FROM catalog_plant "
            + "WHERE status = 1 AND deleted = 0 ORDER BY category")
    List<String> selectDistinctCategories();

    @Select("SELECT DISTINCT light FROM catalog_plant "
            + "WHERE status = 1 AND deleted = 0 ORDER BY light")
    List<String> selectDistinctLights();
}
