package com.example.sd009thuan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThongKeSoSanhDTO {

    private String compareMode; // "month", "quarter", "year", "custom"
    private Integer targetYear;
    private Integer startYear;
    private Integer endYear;
    private String brandName;
    private Long brandId;
    private String productKeyword;
    private SummaryKpiDTO summary;
    private List<PeriodItemDTO> periods;
    private List<TopProductComparisonDTO> topProducts;

    public ThongKeSoSanhDTO() {
        super();
        this.compareMode = "month";
        this.summary = new SummaryKpiDTO();
        this.periods = new ArrayList<>();
        this.topProducts = new ArrayList<>();
    }

    public ThongKeSoSanhDTO(String compareMode, Integer targetYear, Integer startYear, Integer endYear,
                            String brandName, Long brandId, String productKeyword,
                            SummaryKpiDTO summary, List<PeriodItemDTO> periods,
                            List<TopProductComparisonDTO> topProducts) {
        super();
        this.compareMode = compareMode != null ? compareMode : "month";
        this.targetYear = targetYear;
        this.startYear = startYear;
        this.endYear = endYear;
        this.brandName = brandName;
        this.brandId = brandId;
        this.productKeyword = productKeyword;
        this.summary = summary != null ? summary : new SummaryKpiDTO();
        this.periods = periods != null ? periods : new ArrayList<>();
        this.topProducts = topProducts != null ? topProducts : new ArrayList<>();
    }

    public String getCompareMode() { return compareMode; }
    public void setCompareMode(String compareMode) { this.compareMode = compareMode; }

    public Integer getTargetYear() { return targetYear; }
    public void setTargetYear(Integer targetYear) { this.targetYear = targetYear; }

    public Integer getStartYear() { return startYear; }
    public void setStartYear(Integer startYear) { this.startYear = startYear; }

    public Integer getEndYear() { return endYear; }
    public void setEndYear(Integer endYear) { this.endYear = endYear; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }

    public String getProductKeyword() { return productKeyword; }
    public void setProductKeyword(String productKeyword) { this.productKeyword = productKeyword; }

    public SummaryKpiDTO getSummary() { return summary; }
    public void setSummary(SummaryKpiDTO summary) { this.summary = summary; }

    public List<PeriodItemDTO> getPeriods() { return periods; }
    public void setPeriods(List<PeriodItemDTO> periods) { this.periods = periods; }

    public List<TopProductComparisonDTO> getTopProducts() { return topProducts; }
    public void setTopProducts(List<TopProductComparisonDTO> topProducts) { this.topProducts = topProducts; }

    // ==========================================
    // 1. SUMMARY KPI DTO
    // ==========================================
    public static class SummaryKpiDTO {
        private BigDecimal totalRevenue;
        private BigDecimal cashRevenue;
        private BigDecimal transferRevenue;
        private long totalOrders;
        private BigDecimal aov;
        private long totalItemsSold;
        private double avgGrowthRate;

        public SummaryKpiDTO() {
            super();
            this.totalRevenue = BigDecimal.ZERO;
            this.cashRevenue = BigDecimal.ZERO;
            this.transferRevenue = BigDecimal.ZERO;
            this.totalOrders = 0;
            this.aov = BigDecimal.ZERO;
            this.totalItemsSold = 0;
            this.avgGrowthRate = 0.0;
        }

        public SummaryKpiDTO(BigDecimal totalRevenue, BigDecimal cashRevenue, BigDecimal transferRevenue,
                             long totalOrders, BigDecimal aov, long totalItemsSold, double avgGrowthRate) {
            super();
            this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
            this.cashRevenue = cashRevenue != null ? cashRevenue : BigDecimal.ZERO;
            this.transferRevenue = transferRevenue != null ? transferRevenue : BigDecimal.ZERO;
            this.totalOrders = totalOrders;
            this.aov = aov != null ? aov : BigDecimal.ZERO;
            this.totalItemsSold = totalItemsSold;
            this.avgGrowthRate = avgGrowthRate;
        }

        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

        public BigDecimal getCashRevenue() { return cashRevenue; }
        public void setCashRevenue(BigDecimal cashRevenue) { this.cashRevenue = cashRevenue; }

        public BigDecimal getTransferRevenue() { return transferRevenue; }
        public void setTransferRevenue(BigDecimal transferRevenue) { this.transferRevenue = transferRevenue; }

        public long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }

        public BigDecimal getAov() { return aov; }
        public void setAov(BigDecimal aov) { this.aov = aov; }

        public long getTotalItemsSold() { return totalItemsSold; }
        public void setTotalItemsSold(long totalItemsSold) { this.totalItemsSold = totalItemsSold; }

        public double getAvgGrowthRate() { return avgGrowthRate; }
        public void setAvgGrowthRate(double avgGrowthRate) { this.avgGrowthRate = avgGrowthRate; }
    }

    // ==========================================
    // 2. PERIOD ITEM DTO (Từng kỳ đối chiếu)
    // ==========================================
    public static class PeriodItemDTO {
        private String periodKey;   // "T1", "Q1", "2024"
        private String periodName;  // "Tháng 01", "Quý 1", "Năm 2024"
        private String dateRangeText; // "01/01/2026 - 31/01/2026"
        private LocalDate fromDate;
        private LocalDate toDate;
        private BigDecimal totalRevenue;
        private BigDecimal cashRevenue;
        private BigDecimal transferRevenue;
        private long orderCount;
        private BigDecimal aov;
        private long itemsSold;
        private BigDecimal diffAmount; // Chênh lệch so với kỳ liền trước
        private double growthRate;     // % Tăng trưởng so với kỳ liền trước
        private boolean highest;
        private boolean lowest;

        public PeriodItemDTO() {
            super();
            this.totalRevenue = BigDecimal.ZERO;
            this.cashRevenue = BigDecimal.ZERO;
            this.transferRevenue = BigDecimal.ZERO;
            this.diffAmount = BigDecimal.ZERO;
            this.aov = BigDecimal.ZERO;
        }

        public PeriodItemDTO(String periodKey, String periodName, String dateRangeText,
                             LocalDate fromDate, LocalDate toDate,
                             BigDecimal totalRevenue, BigDecimal cashRevenue, BigDecimal transferRevenue,
                             long orderCount, BigDecimal aov, long itemsSold,
                             BigDecimal diffAmount, double growthRate,
                             boolean highest, boolean lowest) {
            super();
            this.periodKey = periodKey;
            this.periodName = periodName;
            this.dateRangeText = dateRangeText;
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
            this.cashRevenue = cashRevenue != null ? cashRevenue : BigDecimal.ZERO;
            this.transferRevenue = transferRevenue != null ? transferRevenue : BigDecimal.ZERO;
            this.orderCount = orderCount;
            this.aov = aov != null ? aov : BigDecimal.ZERO;
            this.itemsSold = itemsSold;
            this.diffAmount = diffAmount != null ? diffAmount : BigDecimal.ZERO;
            this.growthRate = growthRate;
            this.highest = highest;
            this.lowest = lowest;
        }

        public String getPeriodKey() { return periodKey; }
        public void setPeriodKey(String periodKey) { this.periodKey = periodKey; }

        public String getPeriodName() { return periodName; }
        public void setPeriodName(String periodName) { this.periodName = periodName; }

        public String getDateRangeText() { return dateRangeText; }
        public void setDateRangeText(String dateRangeText) { this.dateRangeText = dateRangeText; }

        public LocalDate getFromDate() { return fromDate; }
        public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

        public LocalDate getToDate() { return toDate; }
        public void setToDate(LocalDate toDate) { this.toDate = toDate; }

        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

        public BigDecimal getCashRevenue() { return cashRevenue; }
        public void setCashRevenue(BigDecimal cashRevenue) { this.cashRevenue = cashRevenue; }

        public BigDecimal getTransferRevenue() { return transferRevenue; }
        public void setTransferRevenue(BigDecimal transferRevenue) { this.transferRevenue = transferRevenue; }

        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }

        public BigDecimal getAov() { return aov; }
        public void setAov(BigDecimal aov) { this.aov = aov; }

        public long getItemsSold() { return itemsSold; }
        public void setItemsSold(long itemsSold) { this.itemsSold = itemsSold; }

        public BigDecimal getDiffAmount() { return diffAmount; }
        public void setDiffAmount(BigDecimal diffAmount) { this.diffAmount = diffAmount; }

        public double getGrowthRate() { return growthRate; }
        public void setGrowthRate(double growthRate) { this.growthRate = growthRate; }

        public boolean isHighest() { return highest; }
        public void setHighest(boolean highest) { this.highest = highest; }

        public boolean isLowest() { return lowest; }
        public void setLowest(boolean lowest) { this.lowest = lowest; }
    }

    // ==========================================
    // 3. TOP PRODUCT COMPARISON DTO
    // ==========================================
    public static class TopProductComparisonDTO {
        private Long productId;
        private String productCode;
        private String productName;
        private String brandName;
        private String imageUrl;
        private long totalSold;
        private BigDecimal totalRevenue;
        private Map<String, Long> periodSoldMap;
        private Map<String, BigDecimal> periodRevenueMap;
        private Double growthTrend; // % Tăng/giảm giữa 2 kỳ gần nhất có doanh thu
        private String trend;       // "up", "down", "flat"

        public TopProductComparisonDTO() {
            super();
            this.totalRevenue = BigDecimal.ZERO;
            this.periodSoldMap = new HashMap<>();
            this.periodRevenueMap = new HashMap<>();
            this.growthTrend = 0.0;
            this.trend = "flat";
        }

        public TopProductComparisonDTO(Long productId, String productCode, String productName,
                                       String brandName, String imageUrl,
                                       long totalSold, BigDecimal totalRevenue,
                                       Map<String, Long> periodSoldMap,
                                       Map<String, BigDecimal> periodRevenueMap,
                                       Double growthTrend, String trend) {
            super();
            this.productId = productId;
            this.productCode = productCode;
            this.productName = productName;
            this.brandName = brandName;
            this.imageUrl = imageUrl;
            this.totalSold = totalSold;
            this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
            this.periodSoldMap = periodSoldMap != null ? periodSoldMap : new HashMap<>();
            this.periodRevenueMap = periodRevenueMap != null ? periodRevenueMap : new HashMap<>();
            this.growthTrend = growthTrend != null ? growthTrend : 0.0;
            this.trend = trend != null ? trend : "flat";
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getBrandName() { return brandName; }
        public void setBrandName(String brandName) { this.brandName = brandName; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public long getTotalSold() { return totalSold; }
        public void setTotalSold(long totalSold) { this.totalSold = totalSold; }

        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

        public Map<String, Long> getPeriodSoldMap() { return periodSoldMap; }
        public void setPeriodSoldMap(Map<String, Long> periodSoldMap) { this.periodSoldMap = periodSoldMap; }

        public Map<String, BigDecimal> getPeriodRevenueMap() { return periodRevenueMap; }
        public void setPeriodRevenueMap(Map<String, BigDecimal> periodRevenueMap) { this.periodRevenueMap = periodRevenueMap; }

        public Double getGrowthTrend() { return growthTrend; }
        public void setGrowthTrend(Double growthTrend) { this.growthTrend = growthTrend; }

        public String getTrend() { return trend; }
        public void setTrend(String trend) { this.trend = trend; }
    }
}

