package com.zyt.flowerkisstao.operation.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 运营看板的完整数据切片。所有区块共享同一 from/to 日期范围。 */
@Data
@Builder
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate from;
    private LocalDate to;
    private Overview overview;
    private List<TrendPoint> trends;
    private List<OrderBreakdown> orderBreakdown;
    private List<TopPlant> topPlants;
    private List<TopArticle> topArticles;
    private List<CareBreakdown> careBreakdown;

    /** 五个 KPI。单值用 stat tile，而不是一根柱子的图。 */
    @Data
    @Builder
    public static class Overview implements Serializable {
        private BigDecimal profileCompletionRate;
        private BigDecimal recommendationClickRate;
        private Long paidOrderCount;
        private BigDecimal paidAmount;
        private BigDecimal careTaskCompletionRate;
        /** 每个比例的分母，界面可展示统计口径而非只有孤立数字 */
        private Long normalUserCount;
        private Long profileUserCount;
        private Long recommendationItemCount;
        private Long recommendationClickedCount;
        private Long handledCareTaskCount;
        private Long completedCareTaskCount;
    }

    @Data
    @AllArgsConstructor
    public static class TrendPoint implements Serializable {
        private LocalDate date;
        private Long visits;
        private Long recommendations;
        private Long paidOrders;
        private BigDecimal paidAmount;
    }

    @Data
    @AllArgsConstructor
    public static class OrderBreakdown implements Serializable {
        private Integer status;
        private String statusLabel;
        private Long count;
        private BigDecimal amount;
    }

    @Data
    @AllArgsConstructor
    public static class TopPlant implements Serializable {
        private Long speciesId;
        private String name;
        private Long orderedQuantity;
        private BigDecimal revenue;
    }

    @Data
    @AllArgsConstructor
    public static class TopArticle implements Serializable {
        private Long articleId;
        private String slug;
        private String title;
        private Integer views;
        private Integer usefulCount;
    }

    @Data
    @AllArgsConstructor
    public static class CareBreakdown implements Serializable {
        private Integer status;
        private String statusLabel;
        private Long count;
    }
}
