package com.zyt.flowerkisstao.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.flowerkisstao.knowledge.domain.entity.KnowledgeSearchMiss;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeSearchMissMapper extends BaseMapper<KnowledgeSearchMiss> {

    /**
     * 记一次无结果搜索：没有就插，有就把次数加一。
     *
     * <p>用 {@code ON DUPLICATE KEY UPDATE} 一条语句搞定，而不是"先查再决定插还是改"——
     * 后者在并发下两个请求会同时查到"没有"，然后一个插成功、另一个撞唯一键报错。
     * 这是个纯粹的埋点，绝不该因为撞键把用户的搜索请求搞失败。
     */
    @Insert("INSERT INTO knowledge_search_miss (keyword, hit_count, last_searched_at) "
            + "VALUES (#{keyword}, 1, NOW()) "
            + "ON DUPLICATE KEY UPDATE hit_count = hit_count + 1, last_searched_at = NOW()")
    int recordMiss(@Param("keyword") String keyword);
}
