package com.zyt.flowerkisstao.catalog.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.flowerkisstao.catalog.domain.entity.CatalogSpecies;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CatalogSpeciesMapper extends BaseMapper<CatalogSpecies> {

    /**
     * 筛选项来自库里真实存在、且至少有一个上架 SKU 的品种，而不是前端写死的枚举。
     * 这样下拉框永远不会给出一个筛完是空结果的选项。
     */
    @Select("SELECT DISTINCT s.category FROM catalog_species s "
            + "WHERE s.status = 1 AND s.deleted = 0 "
            + "AND EXISTS (SELECT 1 FROM catalog_sku k "
            + "            WHERE k.species_id = s.id AND k.status = 1 AND k.deleted = 0) "
            + "ORDER BY s.category")
    List<String> selectDistinctCategories();

    /**
     * 光照筛选项返回展示文案而非等级数值：前端下拉框展示的是"明亮散射光"，
     * 用户也是照着文案选的。等级数值只在推荐算法内部使用。
     */
    @Select("SELECT DISTINCT s.light_note FROM catalog_species s "
            + "WHERE s.status = 1 AND s.deleted = 0 "
            + "AND EXISTS (SELECT 1 FROM catalog_sku k "
            + "            WHERE k.species_id = s.id AND k.status = 1 AND k.deleted = 0) "
            + "ORDER BY s.light_note")
    List<String> selectDistinctLights();
}
