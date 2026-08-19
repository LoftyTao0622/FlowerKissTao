package com.zyt.flowerkisstao.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {

    /**
     * 分类分面：只返回真实存在已发布文章的分类。
     *
     * <p>与 catalog 的 selectDistinctCategories 同一思路——筛选框里永远不会出现
     * 一个筛完是空结果的选项。
     */
    @Select("SELECT DISTINCT category FROM knowledge_article "
            + "WHERE status = 2 AND deleted = 0 ORDER BY category")
    List<String> selectDistinctCategories();

    /**
     * 标签分面。
     *
     * <p>标签存在 JSON 数组列里，MySQL 8 的 JSON_TABLE 能把它展开成行。
     * 比在 Java 里把所有文章读出来再去重干净，也不必为标签单开两张表。
     */
    @Select("SELECT DISTINCT jt.tag FROM knowledge_article a, "
            + "JSON_TABLE(a.tags, '$[*]' COLUMNS (tag VARCHAR(40) PATH '$')) jt "
            + "WHERE a.status = 2 AND a.deleted = 0 AND jt.tag IS NOT NULL "
            + "ORDER BY jt.tag")
    List<String> selectDistinctTags();

    /**
     * 浏览数 +1。
     *
     * <p>用 SQL 自增而不是"读出来加一再写回"：详情页是并发最高的接口，
     * 后者会丢计数。这里也刻意不走 updateById，免得顺带把 updated_at 刷了——
     * 浏览一次不该让文章看起来像刚被编辑过。
     */
    @Update("UPDATE knowledge_article SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    /** 有用数增减。delta 传 1 或 -1，同样用自增避免并发丢计数 */
    @Update("UPDATE knowledge_article SET useful_count = GREATEST(useful_count + #{delta}, 0) "
            + "WHERE id = #{id}")
    int addUsefulCount(@Param("id") Long id, @Param("delta") int delta);
}
