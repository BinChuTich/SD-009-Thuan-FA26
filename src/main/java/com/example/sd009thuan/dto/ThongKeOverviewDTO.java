package com.example.sd009thuan.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ThongKeOverviewDTO {
    private KpiDTO kpi;
    private List<BarChartItemDTO> barChart;
    private List<BrandDonutItemDTO> brandDonut;
    private OrderStatusDonutDTO orderStatusDonut;
    private List<TimeSummaryRowDTO> timeSummary;
    private List<ProductStatDTO> products;
    private List<InvoiceStatDTO> invoices = new ArrayList<>();

    public ThongKeOverviewDTO() {
        super();
        this.kpi = new KpiDTO();
        this.barChart = new ArrayList<>();
        this.brandDonut = new ArrayList<>();
        this.orderStatusDonut = new OrderStatusDonutDTO();
        this.timeSummary = new ArrayList<>();
        this.products = new ArrayList<>();
    }

    public ThongKeOverviewDTO(KpiDTO kpi, List<BarChartItemDTO> barChart, List<BrandDonutItemDTO> brandDonut,
                              OrderStatusDonutDTO orderStatusDonut, List<TimeSummaryRowDTO> timeSummary,
                              List<ProductStatDTO> products) {
        super();
        this.kpi = kpi != null ? kpi : new KpiDTO();
        this.barChart = barChart != null ? barChart : new ArrayList<>();
        this.brandDonut = brandDonut != null ? brandDonut : new ArrayList<>();
        this.orderStatusDonut = orderStatusDonut != null ? orderStatusDonut : new OrderStatusDonutDTO();
        this.timeSummary = timeSummary != null ? timeSummary : new ArrayList<>();
        this.products = products != null ? products : new ArrayList<>();
    }

    public KpiDTO getKpi() {
        return kpi;
    }

    public void setKpi(KpiDTO kpi) {
        this.kpi = kpi;
    }

    public List<BarChartItemDTO> getBarChart() {
        return barChart;
    }

    public void setBarChart(List<BarChartItemDTO> barChart) {
        this.barChart = barChart;
    }

    public List<BrandDonutItemDTO> getBrandDonut() {
        return brandDonut;
    }

    public void setBrandDonut(List<BrandDonutItemDTO> brandDonut) {
        this.brandDonut = brandDonut;
    }

    public OrderStatusDonutDTO getOrderStatusDonut() {
        return orderStatusDonut;
    }

    public void setOrderStatusDonut(OrderStatusDonutDTO orderStatusDonut) {
        this.orderStatusDonut = orderStatusDonut;
    }

    public List<TimeSummaryRowDTO> getTimeSummary() {
        return timeSummary;
    }

    public void setTimeSummary(List<TimeSummaryRowDTO> timeSummary) {
        this.timeSummary = timeSummary;
    }

    public List<ProductStatDTO> getProducts() {
        return products;
    }

    public void setProducts(List<ProductStatDTO> products) {
        this.products = products;
    }

    public List<InvoiceStatDTO> getInvoices() {
        return invoices;
    }

    public void setInvoices(List<InvoiceStatDTO> invoices) {
        this.invoices = invoices != null ? invoices : new ArrayList<>();
    }

    public static class InvoiceStatDTO {
        private Long id;
        private String maHoaDon;
        private java.time.Instant ngayTao;
        private String tenKhachHang;
        private String soDienThoaiKhachHang;
        private String tenNhanVien;
        private Integer loaiDon;
        private BigDecimal tongTien;
        private BigDecimal tienSauGiamGia;
        private String hinhThucThanhToan;
        private Integer trangThai;
        private Integer trangThaiThanhToan;

        public InvoiceStatDTO() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getMaHoaDon() { return maHoaDon; }
        public void setMaHoaDon(String maHoaDon) { this.maHoaDon = maHoaDon; }
        public java.time.Instant getNgayTao() { return ngayTao; }
        public void setNgayTao(java.time.Instant ngayTao) { this.ngayTao = ngayTao; }
        public String getTenKhachHang() { return tenKhachHang; }
        public void setTenKhachHang(String tenKhachHang) { this.tenKhachHang = tenKhachHang; }
        public String getSoDienThoaiKhachHang() { return soDienThoaiKhachHang; }
        public void setSoDienThoaiKhachHang(String soDienThoaiKhachHang) { this.soDienThoaiKhachHang = soDienThoaiKhachHang; }
        public String getTenNhanVien() { return tenNhanVien; }
        public void setTenNhanVien(String tenNhanVien) { this.tenNhanVien = tenNhanVien; }
        public Integer getLoaiDon() { return loaiDon; }
        public void setLoaiDon(Integer loaiDon) { this.loaiDon = loaiDon; }
        public BigDecimal getTongTien() { return tongTien; }
        public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
        public BigDecimal getTienSauGiamGia() { return tienSauGiamGia; }
        public void setTienSauGiamGia(BigDecimal tienSauGiamGia) { this.tienSauGiamGia = tienSauGiamGia; }
        public String getHinhThucThanhToan() { return hinhThucThanhToan; }
        public void setHinhThucThanhToan(String hinhThucThanhToan) { this.hinhThucThanhToan = hinhThucThanhToan; }
        public Integer getTrangThai() { return trangThai; }
        public void setTrangThai(Integer trangThai) { this.trangThai = trangThai; }
        public Integer getTrangThaiThanhToan() { return trangThaiThanhToan; }
        public void setTrangThaiThanhToan(Integer trangThaiThanhToan) { this.trangThaiThanhToan = trangThaiThanhToan; }
    }

    public void setOldProducts(List<ProductStatDTO> products) {
        this.products = products;
    }

    // ==========================================
    // 1. KPI DTO
    // ==========================================
    public static class KpiDTO {
        private BigDecimal totalRevenue;
        private BigDecimal cashRevenue;
        private BigDecimal transferRevenue;
        private long totalOrders;
        private long completedOrders;
        private long cancelledOrders;
        private long processingOrders;
        private BigDecimal aov;
        private long totalItemsSold;

        public KpiDTO() {
            super();
            this.totalRevenue = BigDecimal.ZERO;
            this.cashRevenue = BigDecimal.ZERO;
            this.transferRevenue = BigDecimal.ZERO;
            this.totalOrders = 0;
            this.completedOrders = 0;
            this.cancelledOrders = 0;
            this.processingOrders = 0;
            this.aov = BigDecimal.ZERO;
            this.totalItemsSold = 0;
        }

        public KpiDTO(BigDecimal totalRevenue, BigDecimal cashRevenue, BigDecimal transferRevenue,
                      long totalOrders, BigDecimal aov, long totalItemsSold) {
            super();
            this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
            this.cashRevenue = cashRevenue != null ? cashRevenue : BigDecimal.ZERO;
            this.transferRevenue = transferRevenue != null ? transferRevenue : BigDecimal.ZERO;
            this.totalOrders = totalOrders;
            this.completedOrders = totalOrders;
            this.cancelledOrders = 0;
            this.processingOrders = 0;
            this.aov = aov != null ? aov : BigDecimal.ZERO;
            this.totalItemsSold = totalItemsSold;
        }

        public long getCompletedOrders() {
            return completedOrders;
        }

        public void setCompletedOrders(long completedOrders) {
            this.completedOrders = completedOrders;
        }

        public long getCancelledOrders() {
            return cancelledOrders;
        }

        public void setCancelledOrders(long cancelledOrders) {
            this.cancelledOrders = cancelledOrders;
        }

        public long getProcessingOrders() {
            return processingOrders;
        }

        public void setProcessingOrders(long processingOrders) {
            this.processingOrders = processingOrders;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public BigDecimal getCashRevenue() {
            return cashRevenue;
        }

        public void setCashRevenue(BigDecimal cashRevenue) {
            this.cashRevenue = cashRevenue;
        }

        public BigDecimal getTransferRevenue() {
            return transferRevenue;
        }

        public void setTransferRevenue(BigDecimal transferRevenue) {
            this.transferRevenue = transferRevenue;
        }

        public long getTotalOrders() {
            return totalOrders;
        }

        public void setTotalOrders(long totalOrders) {
            this.totalOrders = totalOrders;
        }

        public BigDecimal getAov() {
            return aov;
        }

        public void setAov(BigDecimal aov) {
            this.aov = aov;
        }

        public long getTotalItemsSold() {
            return totalItemsSold;
        }

        public void setTotalItemsSold(long totalItemsSold) {
            this.totalItemsSold = totalItemsSold;
        }
    }

    // ==========================================
    // 2. Bar Chart Item DTO
    // ==========================================
    public static class BarChartItemDTO {
        private String date;
        private String label;
        private long value;
        private BigDecimal revenue;

        public BarChartItemDTO() {
            super();
            this.value = 0;
            this.revenue = BigDecimal.ZERO;
        }

        public BarChartItemDTO(String date, String label, long value, BigDecimal revenue) {
            super();
            this.date = date;
            this.label = label;
            this.value = value;
            this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }
    }

    // ==========================================
    // 3. Brand Donut Item DTO
    // ==========================================
    public static class BrandDonutItemDTO {
        private Long id;
        private String name;
        private BigDecimal revenue;
        private long quantity;
        private String color;

        public BrandDonutItemDTO() {
            super();
            this.revenue = BigDecimal.ZERO;
            this.quantity = 0;
        }

        public BrandDonutItemDTO(Long id, String name, BigDecimal revenue, long quantity, String color) {
            super();
            this.id = id;
            this.name = name;
            this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
            this.quantity = quantity;
            this.color = color;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }

        public long getQuantity() {
            return quantity;
        }

        public void setQuantity(long quantity) {
            this.quantity = quantity;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    // ==========================================
    // 4. Order Status Donut DTO
    // ==========================================
    public static class OrderStatusDonutDTO {
        private long completed;
        private long processing;
        private long cancelled;
        private long total;
        private double completedPercent;
        private double processingPercent;
        private double cancelledPercent;

        public OrderStatusDonutDTO() {
            super();
            this.completed = 0;
            this.processing = 0;
            this.cancelled = 0;
            this.total = 0;
            this.completedPercent = 0.0;
            this.processingPercent = 0.0;
            this.cancelledPercent = 0.0;
        }

        public OrderStatusDonutDTO(long completed, long processing, long cancelled, long total,
                                   double completedPercent, double processingPercent, double cancelledPercent) {
            super();
            this.completed = completed;
            this.processing = processing;
            this.cancelled = cancelled;
            this.total = total;
            this.completedPercent = completedPercent;
            this.processingPercent = processingPercent;
            this.cancelledPercent = cancelledPercent;
        }

        public long getCompleted() {
            return completed;
        }

        public void setCompleted(long completed) {
            this.completed = completed;
        }

        public long getProcessing() {
            return processing;
        }

        public void setProcessing(long processing) {
            this.processing = processing;
        }

        public long getCancelled() {
            return cancelled;
        }

        public void setCancelled(long cancelled) {
            this.cancelled = cancelled;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public double getCompletedPercent() {
            return completedPercent;
        }

        public void setCompletedPercent(double completedPercent) {
            this.completedPercent = completedPercent;
        }

        public double getProcessingPercent() {
            return processingPercent;
        }

        public void setProcessingPercent(double processingPercent) {
            this.processingPercent = processingPercent;
        }

        public double getCancelledPercent() {
            return cancelledPercent;
        }

        public void setCancelledPercent(double cancelledPercent) {
            this.cancelledPercent = cancelledPercent;
        }
    }

    // ==========================================
    // 5. Time Summary Row DTO
    // ==========================================
    public static class TimeSummaryRowDTO {
        private String periodKey;
        private String periodName;
        private BigDecimal totalRevenue;
        private BigDecimal cash;
        private BigDecimal transfer;
        private long orderCount;
        private BigDecimal aov;
        private double growthRate;

        public TimeSummaryRowDTO() {
            super();
            this.totalRevenue = BigDecimal.ZERO;
            this.cash = BigDecimal.ZERO;
            this.transfer = BigDecimal.ZERO;
            this.orderCount = 0;
            this.aov = BigDecimal.ZERO;
            this.growthRate = 0.0;
        }

        public TimeSummaryRowDTO(String periodKey, String periodName, BigDecimal totalRevenue,
                                 BigDecimal cash, BigDecimal transfer, long orderCount,
                                 BigDecimal aov, double growthRate) {
            super();
            this.periodKey = periodKey;
            this.periodName = periodName;
            this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
            this.cash = cash != null ? cash : BigDecimal.ZERO;
            this.transfer = transfer != null ? transfer : BigDecimal.ZERO;
            this.orderCount = orderCount;
            this.aov = aov != null ? aov : BigDecimal.ZERO;
            this.growthRate = growthRate;
        }

        public String getPeriodKey() {
            return periodKey;
        }

        public void setPeriodKey(String periodKey) {
            this.periodKey = periodKey;
        }

        public String getPeriodName() {
            return periodName;
        }

        public void setPeriodName(String periodName) {
            this.periodName = periodName;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public BigDecimal getCash() {
            return cash;
        }

        public void setCash(BigDecimal cash) {
            this.cash = cash;
        }

        public BigDecimal getTransfer() {
            return transfer;
        }

        public void setTransfer(BigDecimal transfer) {
            this.transfer = transfer;
        }

        public long getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(long orderCount) {
            this.orderCount = orderCount;
        }

        public BigDecimal getAov() {
            return aov;
        }

        public void setAov(BigDecimal aov) {
            this.aov = aov;
        }

        public double getGrowthRate() {
            return growthRate;
        }

        public void setGrowthRate(double growthRate) {
            this.growthRate = growthRate;
        }
    }

    // ==========================================
    // 6. Product Stat DTO
    // ==========================================
    public static class ProductStatDTO {
        private Long id;
        private String code;
        private String name;
        private String brand;
        private String sizes;
        private long soldCount;
        private long returnedCount;
        private BigDecimal revenue;
        private long stock;
        private String image;

        public ProductStatDTO() {
            super();
            this.soldCount = 0;
            this.returnedCount = 0;
            this.revenue = BigDecimal.ZERO;
            this.stock = 0;
        }

        public ProductStatDTO(Long id, String code, String name, String brand, String sizes,
                              long soldCount, long returnedCount, BigDecimal revenue, long stock, String image) {
            super();
            this.id = id;
            this.code = code;
            this.name = name;
            this.brand = brand;
            this.sizes = sizes;
            this.soldCount = soldCount;
            this.returnedCount = returnedCount;
            this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
            this.stock = stock;
            this.image = image;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBrand() {
            return brand;
        }

        public void setBrand(String brand) {
            this.brand = brand;
        }

        public String getSizes() {
            return sizes;
        }

        public void setSizes(String sizes) {
            this.sizes = sizes;
        }

        public long getSoldCount() {
            return soldCount;
        }

        public void setSoldCount(long soldCount) {
            this.soldCount = soldCount;
        }

        public long getReturnedCount() {
            return returnedCount;
        }

        public void setReturnedCount(long returnedCount) {
            this.returnedCount = returnedCount;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }

        public long getStock() {
            return stock;
        }

        public void setStock(long stock) {
            this.stock = stock;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }
    }
}
