package com.zyt.flowerkisstao.operation.application.service.impl;

import com.zyt.flowerkisstao.operation.application.service.OperationDashboardService;
import com.zyt.flowerkisstao.operation.infrastructure.mapper.OperationDashboardMapper;
import com.zyt.flowerkisstao.operation.web.vo.DashboardVO;
import com.zyt.flowerkisstao.trade.domain.model.OrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperationDashboardServiceImpl implements OperationDashboardService {

    private static final int MAX_RANGE_DAYS = 366;

    private final OperationDashboardMapper dashboardMapper;

    public OperationDashboardServiceImpl(OperationDashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    @Override
    public DashboardVO dashboard(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            start = end.minusDays(MAX_RANGE_DAYS - 1L);
        }

        LocalDateTime fromTime = start.atStartOfDay();
        LocalDateTime toExclusive = end.plusDays(1).atStartOfDay();

        long normalUsers = dashboardMapper.countNormalUsers();
        long profileUsers = dashboardMapper.countProfileUsers();
        long recItems = dashboardMapper.countRecommendationItems(fromTime, toExclusive);
        long recClicks = dashboardMapper.countRecommendationClicks(fromTime, toExclusive);
        long paidOrders = dashboardMapper.countPaidOrders(fromTime, toExclusive);
        BigDecimal paidAmount = defaultZero(dashboardMapper.sumPaidAmount(fromTime, toExclusive));
        long handledTasks = dashboardMapper.countHandledCareTasks(fromTime, toExclusive);
        long completedTasks = dashboardMapper.countCompletedCareTasks(fromTime, toExclusive);

        DashboardVO.Overview overview = DashboardVO.Overview.builder()
                .profileCompletionRate(rate(profileUsers, normalUsers))
                .recommendationClickRate(rate(recClicks, recItems))
                .paidOrderCount(paidOrders)
                .paidAmount(paidAmount)
                .careTaskCompletionRate(rate(completedTasks, handledTasks))
                .normalUserCount(normalUsers)
                .profileUserCount(profileUsers)
                .recommendationItemCount(recItems)
                .recommendationClickedCount(recClicks)
                .handledCareTaskCount(handledTasks)
                .completedCareTaskCount(completedTasks)
                .build();

        return DashboardVO.builder()
                .from(start)
                .to(end)
                .overview(overview)
                .trends(buildTrends(start, end, fromTime, toExclusive))
                .orderBreakdown(buildOrderBreakdown(fromTime, toExclusive))
                .topPlants(buildTopPlants(fromTime, toExclusive))
                .topArticles(buildTopArticles())
                .careBreakdown(buildCareBreakdown(fromTime, toExclusive))
                .build();
    }

    private List<DashboardVO.TrendPoint> buildTrends(LocalDate start, LocalDate end,
                                                      LocalDateTime from, LocalDateTime to) {
        Map<LocalDate, long[]> counts = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> amounts = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            counts.put(date, new long[3]); // visits / recommendations / orders
            amounts.put(date, BigDecimal.ZERO);
        }
        for (Map<String, Object> row : dashboardMapper.selectVisitTrend(from, to)) {
            LocalDate date = asDate(row.get("date"));
            if (counts.containsKey(date)) counts.get(date)[0] = asLong(row.get("visits"));
        }
        for (Map<String, Object> row : dashboardMapper.selectRecommendationTrend(from, to)) {
            LocalDate date = asDate(row.get("date"));
            if (counts.containsKey(date)) counts.get(date)[1] = asLong(row.get("recommendations"));
        }
        for (Map<String, Object> row : dashboardMapper.selectPaidOrderTrend(from, to)) {
            LocalDate date = asDate(row.get("date"));
            if (counts.containsKey(date)) {
                counts.get(date)[2] = asLong(row.get("paidOrders"));
                amounts.put(date, asDecimal(row.get("paidAmount")));
            }
        }

        List<DashboardVO.TrendPoint> result = new ArrayList<>();
        counts.forEach((date, values) -> result.add(new DashboardVO.TrendPoint(
                date, values[0], values[1], values[2], amounts.get(date))));
        return result;
    }

    private List<DashboardVO.OrderBreakdown> buildOrderBreakdown(LocalDateTime from,
                                                                  LocalDateTime to) {
        return dashboardMapper.selectOrderBreakdown(from, to).stream()
                .map(row -> {
                    int status = asInt(row.get("status"));
                    return new DashboardVO.OrderBreakdown(status,
                            OrderStatus.of(status).label(), asLong(row.get("count")),
                            asDecimal(row.get("amount")));
                }).toList();
    }

    private List<DashboardVO.TopPlant> buildTopPlants(LocalDateTime from, LocalDateTime to) {
        return dashboardMapper.selectTopPlants(from, to).stream()
                .map(row -> new DashboardVO.TopPlant(asLong(row.get("speciesId")),
                        String.valueOf(row.get("name")), asLong(row.get("orderedQuantity")),
                        asDecimal(row.get("revenue"))))
                .toList();
    }

    private List<DashboardVO.TopArticle> buildTopArticles() {
        return dashboardMapper.selectTopArticles().stream()
                .map(row -> new DashboardVO.TopArticle(asLong(row.get("articleId")),
                        String.valueOf(row.get("slug")), String.valueOf(row.get("title")),
                        asInt(row.get("views")), asInt(row.get("usefulCount"))))
                .toList();
    }

    private List<DashboardVO.CareBreakdown> buildCareBreakdown(LocalDateTime from,
                                                                LocalDateTime to) {
        return dashboardMapper.selectCareBreakdown(from, to).stream()
                .map(row -> {
                    int status = asInt(row.get("status"));
                    return new DashboardVO.CareBreakdown(status, careStatusLabel(status),
                            asLong(row.get("count")));
                }).toList();
    }

    private static BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(numerator * 100.0 / denominator)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static String careStatusLabel(int status) {
        return switch (status) {
            case 0 -> "待办";
            case 1 -> "已完成";
            case 2 -> "已跳过";
            case 3 -> "已逾期";
            default -> "未知";
        };
    }

    private static LocalDate asDate(Object value) {
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        if (value instanceof LocalDate date) return date;
        if (value instanceof LocalDateTime dateTime) return dateTime.toLocalDate();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime().toLocalDate();
        return LocalDate.parse(String.valueOf(value));
    }

    private static int asInt(Object value) { return value == null ? 0 : ((Number) value).intValue(); }
    private static long asLong(Object value) { return value == null ? 0L : ((Number) value).longValue(); }
    private static BigDecimal asDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    }
    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
