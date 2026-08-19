package com.zyt.flowerkisstao.operation.infrastructure.mapper;

import com.zyt.flowerkisstao.operation.web.vo.DashboardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 看板聚合 SQL。不把所有业务行读进 Java 再统计，数据库擅长的聚合留给数据库。
 */
@Mapper
public interface OperationDashboardMapper {

    @Select("SELECT COUNT(*) FROM sys_user WHERE status = 1 AND deleted = 0")
    long countNormalUsers();

    @Select("SELECT COUNT(DISTINCT user_id) FROM user_scene_profile WHERE deleted = 0")
    long countProfileUsers();

    @Select("SELECT COUNT(*) FROM rec_result_item i JOIN rec_result r ON r.id = i.result_id "
            + "WHERE r.created_at >= #{from} AND r.created_at < #{to}")
    long countRecommendationItems(@Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(*) FROM rec_result_item i JOIN rec_result r ON r.id = i.result_id "
            + "WHERE i.clicked = 1 AND r.created_at >= #{from} AND r.created_at < #{to}")
    long countRecommendationClicks(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(*) FROM rec_result WHERE created_at >= #{from} AND created_at < #{to}")
    long countRecommendations(@Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(*) FROM trade_order WHERE status IN (1,2,3,5) "
            + "AND created_at >= #{from} AND created_at < #{to}")
    long countPaidOrders(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COALESCE(SUM(total_amount),0) FROM trade_order WHERE status IN (1,2,3,5) "
            + "AND created_at >= #{from} AND created_at < #{to}")
    BigDecimal sumPaidAmount(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(*) FROM care_task WHERE status IN (1,2) "
            + "AND created_at >= #{from} AND created_at < #{to}")
    long countHandledCareTasks(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(*) FROM care_task WHERE status = 1 "
            + "AND created_at >= #{from} AND created_at < #{to}")
    long countCompletedCareTasks(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(*) FROM operation_visit_log "
            + "WHERE created_at >= #{from} AND created_at < #{to}")
    long countVisits(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT status AS `status`, COUNT(*) AS `count`, COALESCE(SUM(total_amount),0) AS amount "
            + "FROM trade_order WHERE created_at >= #{from} AND created_at < #{to} "
            + "GROUP BY status ORDER BY status")
    List<Map<String, Object>> selectOrderBreakdown(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);

    @Select("SELECT i.species_id AS speciesId, i.species_name AS name, "
            + "SUM(i.quantity) AS orderedQuantity, SUM(i.subtotal) AS revenue "
            + "FROM trade_order_item i JOIN trade_order o ON o.id = i.order_id "
            + "WHERE o.status IN (1,2,3,5) AND o.created_at >= #{from} AND o.created_at < #{to} "
            + "GROUP BY i.species_id, i.species_name ORDER BY orderedQuantity DESC, revenue DESC LIMIT 5")
    List<Map<String, Object>> selectTopPlants(@Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);

    @Select("SELECT id AS articleId, slug, title, view_count AS views, useful_count AS usefulCount "
            + "FROM knowledge_article WHERE status=2 AND deleted=0 "
            + "ORDER BY view_count DESC, useful_count DESC, id ASC LIMIT 5")
    List<Map<String, Object>> selectTopArticles();

    @Select("SELECT status AS `status`, COUNT(*) AS `count` FROM care_task "
            + "WHERE created_at >= #{from} AND created_at < #{to} GROUP BY status ORDER BY status")
    List<Map<String, Object>> selectCareBreakdown(@Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to);

    @Select("SELECT DATE(created_at) AS `date`, COUNT(*) AS visits FROM operation_visit_log "
            + "WHERE created_at >= #{from} AND created_at < #{to} GROUP BY DATE(created_at)")
    List<Map<String, Object>> selectVisitTrend(@Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

    @Select("SELECT DATE(created_at) AS `date`, COUNT(*) AS recommendations FROM rec_result "
            + "WHERE created_at >= #{from} AND created_at < #{to} GROUP BY DATE(created_at)")
    List<Map<String, Object>> selectRecommendationTrend(@Param("from") LocalDateTime from,
                                                        @Param("to") LocalDateTime to);

    @Select("SELECT DATE(created_at) AS `date`, COUNT(*) AS paidOrders, "
            + "COALESCE(SUM(total_amount),0) AS paidAmount FROM trade_order "
            + "WHERE status IN (1,2,3,5) AND created_at >= #{from} AND created_at < #{to} "
            + "GROUP BY DATE(created_at)")
    List<Map<String, Object>> selectPaidOrderTrend(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);
}
