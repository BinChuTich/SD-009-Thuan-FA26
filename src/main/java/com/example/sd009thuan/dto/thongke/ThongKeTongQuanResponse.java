package com.example.sd009thuan.dto.thongke;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThongKeTongQuanResponse {
    private BigDecimal totalRevenue;
    private BigDecimal cashRevenue;
    private BigDecimal transferRevenue;
    private Long totalOrders;
    private BigDecimal aov;
    private Long totalItemsSold;

    @Builder.Default
    private List<DailySalesItem> dailySales = new ArrayList<>();

    @Builder.Default
    private List<BrandStatsItem> brandStats = new ArrayList<>();

    private OrderStatusStats orderStatusStats;

    @Builder.Default
    private List<TimeSummaryRow> timeSummary = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySalesItem {
        private String date;
        private String label;
        private Long value;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrandStatsItem {
        private String name;
        private BigDecimal revenue;
        private Long quantity;
        private Double percentage;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderStatusStats {
        private Long completed;
        private Long processing;
        private Long cancelled;
        private Long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSummaryRow {
        private String periodKey;
        private String periodName;
        private BigDecimal totalRevenue;
        private BigDecimal cash;
        private BigDecimal transfer;
        private Long orderCount;
        private BigDecimal aov;
        private Double growthRate;
    }
}

