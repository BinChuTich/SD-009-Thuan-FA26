package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.thongke.*;
import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ThongKeService {

    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final SanPhamRepository sanPhamRepository;
    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final HinhThucThanhToanRepository hinhThucThanhToanRepository;
    private final HinhAnhRepository hinhAnhRepository;

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DAY_LABEL_FMT = DateTimeFormatter.ofPattern("dd/MM");

    private static final String[] PALETTE = {
            "#8C7355", "#C8B6A6", "#4A6572", "#E07A5F", "#6D597A", "#3D5A80", "#2A9D8F", "#E76F51"
    };

    public ThongKeService(
            HoaDonRepository hoaDonRepository,
            ChiTietHoaDonRepository chiTietHoaDonRepository,
            SanPhamRepository sanPhamRepository,
            ChiTietSanPhamRepository chiTietSanPhamRepository,
            ThuongHieuRepository thuongHieuRepository,
            HinhThucThanhToanRepository hinhThucThanhToanRepository,
            HinhAnhRepository hinhAnhRepository
    ) {
        this.hoaDonRepository = hoaDonRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
        this.thuongHieuRepository = thuongHieuRepository;
        this.hinhThucThanhToanRepository = hinhThucThanhToanRepository;
        this.hinhAnhRepository = hinhAnhRepository;
    }

    /**
     * Kiểm tra một hóa đơn có phải là hóa đơn chờ tại quầy (bản nháp chưa thanh toán) hay không.
     */
    private boolean isWaitingDraft(HoaDon h) {
        if (h == null) return false;
        boolean isStoreDraft = Integer.valueOf(1).equals(h.getLoaiDon())
                && Integer.valueOf(1).equals(h.getTrangThai())
                && (h.getTrangThaiThanhToan() == null || Integer.valueOf(0).equals(h.getTrangThaiThanhToan()));
        String note = h.getGhiChu() != null ? h.getGhiChu().toLowerCase() : "";
        boolean isWaitingNote = note.contains("hóa đơn chờ") || note.contains("hoa don cho");
        return isStoreDraft || isWaitingNote;
    }

    /**
     * Xác định hóa đơn có được ghi nhận doanh thu hay không (Hoàn thành hoặc Đã thanh toán và không bị hủy).
     */
    private boolean isCompletedOrder(HoaDon h) {
        if (h == null) return false;
        if (Integer.valueOf(6).equals(h.getTrangThai())) return false; // Hủy
        return Integer.valueOf(5).equals(h.getTrangThai()) || Integer.valueOf(1).equals(h.getTrangThaiThanhToan());
    }

    /**
     * Chuyển đổi chuỗi ngày YYYY-MM-DD sang Instant đầu ngày hoặc cuối ngày.
     */
    private Instant parseStartOfDay(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDate.of(2020, 1, 1).atStartOfDay(VN_ZONE).toInstant();
        }
        try {
            return LocalDate.parse(dateStr.trim()).atStartOfDay(VN_ZONE).toInstant();
        } catch (Exception e) {
            return LocalDate.of(2020, 1, 1).atStartOfDay(VN_ZONE).toInstant();
        }
    }

    private Instant parseEndOfDay(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDate.now(VN_ZONE).atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();
        }
        try {
            return LocalDate.parse(dateStr.trim()).atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();
        } catch (Exception e) {
            return LocalDate.now(VN_ZONE).atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();
        }
    }

    private String getStatusLabel(Integer status) {
        if (status == null) return "Chưa cập nhật";
        return switch (status) {
            case 1 -> "Chờ xác nhận";
            case 2 -> "Đã xác nhận";
            case 3 -> "Chờ vận chuyển";
            case 4 -> "Vận chuyển";
            case 5 -> "Đã hoàn thành";
            case 6 -> "Đã hủy";
            case 7 -> "Giao hàng thất bại";
            default -> "Khác";
        };
    }

    /**
     * 1. Thống kê Tổng quan (KPIs, Bar chart, Donut chart, Time summary)
     */
    public ThongKeTongQuanResponse getTongQuan(ThongKeFilterRequest req) {
        Instant start = parseStartOfDay(req != null ? req.getFromDate() : null);
        Instant end = parseEndOfDay(req != null ? req.getToDate() : null);

        // Lấy tất cả hóa đơn trong khoảng thời gian
        List<HoaDon> invoicesInRange = hoaDonRepository.findByNgayTaoBetweenWithForeignKeys(start, end);

        // Lọc bỏ hóa đơn chờ bản nháp
        List<HoaDon> validInvoices = invoicesInRange.stream()
                .filter(h -> !isWaitingDraft(h))
                .toList();

        if (validInvoices.isEmpty()) {
            return buildEmptyTongQuanResponse(start, end, req != null ? req.getStatType() : "day");
        }

        List<Long> validInvoiceIds = validInvoices.stream().map(HoaDon::getId).toList();
        List<ChiTietHoaDon> allDetails = chiTietHoaDonRepository.findByIdHoaDon_IdIn(validInvoiceIds);
        List<HinhThucThanhToan> allPayments = hinhThucThanhToanRepository.findByIdHoaDon_IdIn(validInvoiceIds);

        // Map chi tiết hóa đơn theo id hóa đơn
        Map<Long, List<ChiTietHoaDon>> detailsByInvoice = allDetails.stream()
                .filter(c -> c.getIdHoaDon() != null)
                .collect(Collectors.groupingBy(c -> c.getIdHoaDon().getId()));

        // Lọc theo Thương hiệu hoặc Từ khóa nếu có
        Long brandFilter = req != null ? req.getIdThuongHieu() : null;
        String kw = req != null && req.getKeyword() != null ? req.getKeyword().trim().toLowerCase() : "";

        List<HoaDon> filteredInvoices = validInvoices.stream().filter(h -> {
            List<ChiTietHoaDon> items = detailsByInvoice.getOrDefault(h.getId(), Collections.emptyList());
            if (brandFilter != null) {
                boolean hasBrand = items.stream().anyMatch(item ->
                        item.getIdChiTietSanPham() != null
                                && item.getIdChiTietSanPham().getIdSanPham() != null
                                && item.getIdChiTietSanPham().getIdSanPham().getIdThuongHieu() != null
                                && Objects.equals(item.getIdChiTietSanPham().getIdSanPham().getIdThuongHieu().getId(), brandFilter)
                );
                if (!hasBrand) return false;
            }
            if (!kw.isEmpty()) {
                boolean matchCode = h.getMaHoaDon() != null && h.getMaHoaDon().toLowerCase().contains(kw);
                boolean matchCust = (h.getTenKhachHang() != null && h.getTenKhachHang().toLowerCase().contains(kw))
                        || (h.getIdKhachHang() != null && h.getIdKhachHang().getTenKhachHang() != null && h.getIdKhachHang().getTenKhachHang().toLowerCase().contains(kw));
                boolean matchItem = items.stream().anyMatch(item -> {
                    if (item.getIdChiTietSanPham() == null || item.getIdChiTietSanPham().getIdSanPham() == null) return false;
                    SanPham sp = item.getIdChiTietSanPham().getIdSanPham();
                    return (sp.getMaSanPham() != null && sp.getMaSanPham().toLowerCase().contains(kw))
                            || (sp.getTenSanPham() != null && sp.getTenSanPham().toLowerCase().contains(kw));
                });
                if (!matchCode && !matchCust && !matchItem) return false;
            }
            return true;
        }).toList();

        // 1. Chỉ số tổng quan KPIs
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal cashRevenue = BigDecimal.ZERO;
        BigDecimal transferRevenue = BigDecimal.ZERO;
        long totalOrders = filteredInvoices.size();
        long completedOrdersCount = 0;
        long totalItemsSold = 0;

        // Map thanh toán theo id hóa đơn
        Map<Long, List<HinhThucThanhToan>> paymentsByInvoice = allPayments.stream()
                .filter(p -> p.getIdHoaDon() != null)
                .collect(Collectors.groupingBy(p -> p.getIdHoaDon().getId()));

        for (HoaDon h : filteredInvoices) {
            if (isCompletedOrder(h)) {
                completedOrdersCount++;
                BigDecimal amt = h.getTienSauGiamGia() != null && h.getTienSauGiamGia().compareTo(BigDecimal.ZERO) > 0
                        ? h.getTienSauGiamGia()
                        : (h.getTongTien() != null ? h.getTongTien() : BigDecimal.ZERO);
                totalRevenue = totalRevenue.add(amt);

                // Phương thức thanh toán
                List<HinhThucThanhToan> pays = paymentsByInvoice.getOrDefault(h.getId(), Collections.emptyList());
                if (!pays.isEmpty()) {
                    for (HinhThucThanhToan p : pays) {
                        BigDecimal pAmt = p.getSoTien() != null ? p.getSoTien() : BigDecimal.ZERO;
                        String pName = p.getIdPhuongThuc() != null && p.getIdPhuongThuc().getTenPhuongThuc() != null
                                ? p.getIdPhuongThuc().getTenPhuongThuc().toLowerCase() : "";
                        if (pName.contains("tiền mặt") || pName.contains("cash")) {
                            cashRevenue = cashRevenue.add(pAmt);
                        } else {
                            transferRevenue = transferRevenue.add(pAmt);
                        }
                    }
                } else {
                    // Chưa có record chi tiết thanh toán: gán theo loại đơn
                    if (Integer.valueOf(1).equals(h.getLoaiDon())) {
                        cashRevenue = cashRevenue.add(amt);
                    } else {
                        transferRevenue = transferRevenue.add(amt);
                    }
                }

                // Sản phẩm đã bán
                List<ChiTietHoaDon> items = detailsByInvoice.getOrDefault(h.getId(), Collections.emptyList());
                for (ChiTietHoaDon it : items) {
                    if (brandFilter != null) {
                        if (it.getIdChiTietSanPham() != null && it.getIdChiTietSanPham().getIdSanPham() != null
                                && it.getIdChiTietSanPham().getIdSanPham().getIdThuongHieu() != null
                                && !Objects.equals(it.getIdChiTietSanPham().getIdSanPham().getIdThuongHieu().getId(), brandFilter)) {
                            continue;
                        }
                    }
                    totalItemsSold += (it.getSoLuong() != null ? it.getSoLuong() : 0);
                }
            }
        }

        BigDecimal aov = completedOrdersCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(completedOrdersCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 2. Biểu đồ bán theo ngày (Bar Chart)
        List<ThongKeTongQuanResponse.DailySalesItem> dailySales = buildDailySales(filteredInvoices, detailsByInvoice, brandFilter, start, end);

        // 3. Biểu đồ thương hiệu (Donut Chart)
        List<ThongKeTongQuanResponse.BrandStatsItem> brandStats = buildBrandStats(filteredInvoices, detailsByInvoice);

        // 4. Biểu đồ trạng thái đơn hàng (Donut Chart)
        long completedCount = filteredInvoices.stream().filter(this::isCompletedOrder).count();
        long cancelledCount = filteredInvoices.stream().filter(h -> Integer.valueOf(6).equals(h.getTrangThai())).count();
        long processingCount = filteredInvoices.size() - completedCount - cancelledCount;
        if (processingCount < 0) processingCount = 0;

        ThongKeTongQuanResponse.OrderStatusStats orderStatusStats = ThongKeTongQuanResponse.OrderStatusStats.builder()
                .completed(completedCount)
                .cancelled(cancelledCount)
                .processing(processingCount)
                .total((long) filteredInvoices.size())
                .build();

        // 5. Bảng so sánh 4 mốc thời gian (Hôm nay, Tuần này, Tháng này, Năm nay)
        List<ThongKeTongQuanResponse.TimeSummaryRow> timeSummary = buildTimeSummary();

        return ThongKeTongQuanResponse.builder()
                .totalRevenue(totalRevenue)
                .cashRevenue(cashRevenue)
                .transferRevenue(transferRevenue)
                .totalOrders(totalOrders)
                .aov(aov)
                .totalItemsSold(totalItemsSold)
                .dailySales(dailySales)
                .brandStats(brandStats)
                .orderStatusStats(orderStatusStats)
                .timeSummary(timeSummary)
                .build();
    }

    private ThongKeTongQuanResponse buildEmptyTongQuanResponse(Instant start, Instant end, String statType) {
        return ThongKeTongQuanResponse.builder()
                .totalRevenue(BigDecimal.ZERO)
                .cashRevenue(BigDecimal.ZERO)
                .transferRevenue(BigDecimal.ZERO)
                .totalOrders(0L)
                .aov(BigDecimal.ZERO)
                .totalItemsSold(0L)
                .dailySales(Collections.emptyList())
                .brandStats(Collections.emptyList())
                .orderStatusStats(ThongKeTongQuanResponse.OrderStatusStats.builder()
                        .completed(0L).processing(0L).cancelled(0L).total(0L).build())
                .timeSummary(buildTimeSummary())
                .build();
    }

    private List<ThongKeTongQuanResponse.DailySalesItem> buildDailySales(
            List<HoaDon> invoices,
            Map<Long, List<ChiTietHoaDon>> detailsByInvoice,
            Long brandFilter,
            Instant start,
            Instant end
    ) {
        LocalDate startDate = start.atZone(VN_ZONE).toLocalDate();
        LocalDate endDate = end.atZone(VN_ZONE).toLocalDate();

        // Nếu khoảng ngày vượt quá 31 ngày, nhóm theo 7 ngày gần nhất hoặc ngày có phát sinh
        Map<LocalDate, Long> quantityByDate = new TreeMap<>();
        Map<LocalDate, BigDecimal> revenueByDate = new TreeMap<>();

        // Khởi tạo các mốc ngày (nếu khoảng ngày <= 14 ngày)
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween <= 14) {
            LocalDate cur = startDate;
            while (!cur.isAfter(endDate)) {
                quantityByDate.put(cur, 0L);
                revenueByDate.put(cur, BigDecimal.ZERO);
                cur = cur.plusDays(1);
            }
        }

        for (HoaDon h : invoices) {
            if (!isCompletedOrder(h) || h.getNgayTao() == null) continue;
            LocalDate d = h.getNgayTao().atZone(VN_ZONE).toLocalDate();
            BigDecimal amt = h.getTienSauGiamGia() != null && h.getTienSauGiamGia().compareTo(BigDecimal.ZERO) > 0
                    ? h.getTienSauGiamGia()
                    : (h.getTongTien() != null ? h.getTongTien() : BigDecimal.ZERO);

            revenueByDate.merge(d, amt, BigDecimal::add);

            List<ChiTietHoaDon> items = detailsByInvoice.getOrDefault(h.getId(), Collections.emptyList());
            long count = 0;
            for (ChiTietHoaDon it : items) {
                if (brandFilter != null) {
                    if (it.getIdChiTietSanPham() != null && it.getIdChiTietSanPham().getIdSanPham() != null
                            && it.getIdChiTietSanPham().getIdSanPham().getIdThuongHieu() != null
                            && !Objects.equals(it.getIdChiTietSanPham().getIdSanPham().getIdThuongHieu().getId(), brandFilter)) {
                        continue;
                    }
                }
                count += (it.getSoLuong() != null ? it.getSoLuong() : 0);
            }
            quantityByDate.merge(d, count, Long::sum);
        }

        List<ThongKeTongQuanResponse.DailySalesItem> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> e : quantityByDate.entrySet()) {
            LocalDate d = e.getKey();
            result.add(ThongKeTongQuanResponse.DailySalesItem.builder()
                    .date(d.format(DATE_FMT))
                    .label(d.format(DAY_LABEL_FMT))
                    .value(e.getValue())
                    .revenue(revenueByDate.getOrDefault(d, BigDecimal.ZERO))
                    .build());
        }

        if (result.isEmpty()) {
            // Đảm bảo có ít nhất mốc ngày hôm nay
            LocalDate today = LocalDate.now(VN_ZONE);
            result.add(ThongKeTongQuanResponse.DailySalesItem.builder()
                    .date(today.format(DATE_FMT))
                    .label(today.format(DAY_LABEL_FMT))
                    .value(0L)
                    .revenue(BigDecimal.ZERO)
                    .build());
        }

        return result;
    }

    private List<ThongKeTongQuanResponse.BrandStatsItem> buildBrandStats(
            List<HoaDon> invoices,
            Map<Long, List<ChiTietHoaDon>> detailsByInvoice
    ) {
        Map<String, BigDecimal> revenueByBrand = new HashMap<>();
        Map<String, Long> quantityByBrand = new HashMap<>();

        for (HoaDon h : invoices) {
            if (!isCompletedOrder(h)) continue;
            List<ChiTietHoaDon> items = detailsByInvoice.getOrDefault(h.getId(), Collections.emptyList());
            for (ChiTietHoaDon it : items) {
                String bName = "Khác";
                if (it.getIdChiTietSanPham() != null && it.getIdChiTietSanPham().getIdSanPham() != null
                        && it.getIdChiTietSanPham().getIdSanPham().getIdThuongHieu() != null
                        && it.getIdChiTietSanPham().getIdSanPham().getIdThuongHieu().getTenThuongHieu() != null) {
                    bName = it.getIdChiTietSanPham().getIdSanPham().getIdThuongHieu().getTenThuongHieu().trim();
                }

                BigDecimal lineTotal = it.getThanhTien() != null && it.getThanhTien().compareTo(BigDecimal.ZERO) > 0
                        ? it.getThanhTien()
                        : (it.getDonGia() != null && it.getSoLuong() != null ? it.getDonGia().multiply(BigDecimal.valueOf(it.getSoLuong())) : BigDecimal.ZERO);

                long qty = it.getSoLuong() != null ? it.getSoLuong() : 0;

                revenueByBrand.merge(bName, lineTotal, BigDecimal::add);
                quantityByBrand.merge(bName, qty, Long::sum);
            }
        }

        BigDecimal grandTotal = revenueByBrand.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ThongKeTongQuanResponse.BrandStatsItem> list = new ArrayList<>();
        int colorIdx = 0;

        for (Map.Entry<String, BigDecimal> entry : revenueByBrand.entrySet()) {
            String name = entry.getKey();
            BigDecimal rev = entry.getValue();
            Long qty = quantityByBrand.getOrDefault(name, 0L);
            double pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? rev.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            list.add(ThongKeTongQuanResponse.BrandStatsItem.builder()
                    .name(name)
                    .revenue(rev)
                    .quantity(qty)
                    .percentage(pct)
                    .color(PALETTE[colorIdx % PALETTE.length])
                    .build());
            colorIdx++;
        }

        // Sắp xếp giảm dần theo doanh thu
        list.sort((a, b) -> b.getRevenue().compareTo(a.getRevenue()));

        if (list.isEmpty()) {
            list.add(ThongKeTongQuanResponse.BrandStatsItem.builder()
                    .name("FF-Tshirt")
                    .revenue(BigDecimal.ZERO)
                    .quantity(0L)
                    .percentage(100.0)
                    .color(PALETTE[0])
                    .build());
        }

        return list;
    }

    private List<ThongKeTongQuanResponse.TimeSummaryRow> buildTimeSummary() {
        LocalDate today = LocalDate.now(VN_ZONE);

        // 1. Hôm nay vs Hôm qua
        Instant todayStart = today.atStartOfDay(VN_ZONE).toInstant();
        Instant todayEnd = today.atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();
        Instant yestStart = today.minusDays(1).atStartOfDay(VN_ZONE).toInstant();
        Instant yestEnd = today.minusDays(1).atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();

        // 2. Tuần này vs Tuần trước
        LocalDate monThisWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant weekStart = monThisWeek.atStartOfDay(VN_ZONE).toInstant();
        Instant lastWeekStart = monThisWeek.minusWeeks(1).atStartOfDay(VN_ZONE).toInstant();
        Instant lastWeekEnd = monThisWeek.minusDays(1).atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();

        // 3. Tháng này vs Tháng trước
        LocalDate firstDayMonth = today.withDayOfMonth(1);
        Instant monthStart = firstDayMonth.atStartOfDay(VN_ZONE).toInstant();
        LocalDate firstDayLastMonth = firstDayMonth.minusMonths(1);
        Instant lastMonthStart = firstDayLastMonth.atStartOfDay(VN_ZONE).toInstant();
        Instant lastMonthEnd = firstDayMonth.minusDays(1).atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();

        // 4. Năm nay vs Năm trước
        LocalDate firstDayYear = today.withDayOfYear(1);
        Instant yearStart = firstDayYear.atStartOfDay(VN_ZONE).toInstant();
        LocalDate firstDayLastYear = firstDayYear.minusYears(1);
        Instant lastYearStart = firstDayLastYear.atStartOfDay(VN_ZONE).toInstant();
        Instant lastYearEnd = firstDayYear.minusDays(1).atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();

        List<ThongKeTongQuanResponse.TimeSummaryRow> rows = new ArrayList<>();
        rows.add(computePeriodSummary("today", "Hôm nay", todayStart, todayEnd, yestStart, yestEnd));
        rows.add(computePeriodSummary("thisWeek", "Tuần này", weekStart, todayEnd, lastWeekStart, lastWeekEnd));
        rows.add(computePeriodSummary("thisMonth", "Tháng này", monthStart, todayEnd, lastMonthStart, lastMonthEnd));
        rows.add(computePeriodSummary("thisYear", "Năm nay", yearStart, todayEnd, lastYearStart, lastYearEnd));

        return rows;
    }

    private ThongKeTongQuanResponse.TimeSummaryRow computePeriodSummary(
            String key,
            String name,
            Instant start,
            Instant end,
            Instant prevStart,
            Instant prevEnd
    ) {
        List<HoaDon> curInvoices = hoaDonRepository.findByNgayTaoBetweenWithForeignKeys(start, end)
                .stream().filter(h -> !isWaitingDraft(h) && isCompletedOrder(h)).toList();

        List<HoaDon> prevInvoices = hoaDonRepository.findByNgayTaoBetweenWithForeignKeys(prevStart, prevEnd)
                .stream().filter(h -> !isWaitingDraft(h) && isCompletedOrder(h)).toList();

        BigDecimal curRevenue = BigDecimal.ZERO;
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal transfer = BigDecimal.ZERO;

        for (HoaDon h : curInvoices) {
            BigDecimal amt = h.getTienSauGiamGia() != null && h.getTienSauGiamGia().compareTo(BigDecimal.ZERO) > 0
                    ? h.getTienSauGiamGia() : (h.getTongTien() != null ? h.getTongTien() : BigDecimal.ZERO);
            curRevenue = curRevenue.add(amt);
            if (Integer.valueOf(1).equals(h.getLoaiDon())) {
                cash = cash.add(amt);
            } else {
                transfer = transfer.add(amt);
            }
        }

        BigDecimal prevRevenue = BigDecimal.ZERO;
        for (HoaDon h : prevInvoices) {
            BigDecimal amt = h.getTienSauGiamGia() != null && h.getTienSauGiamGia().compareTo(BigDecimal.ZERO) > 0
                    ? h.getTienSauGiamGia() : (h.getTongTien() != null ? h.getTongTien() : BigDecimal.ZERO);
            prevRevenue = prevRevenue.add(amt);
        }

        long orderCount = curInvoices.size();
        BigDecimal aov = orderCount > 0 ? curRevenue.divide(BigDecimal.valueOf(orderCount), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        double growth = 0.0;
        if (prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growth = curRevenue.subtract(prevRevenue)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(prevRevenue, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        } else if (curRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growth = 100.0;
        }

        return ThongKeTongQuanResponse.TimeSummaryRow.builder()
                .periodKey(key)
                .periodName(name)
                .totalRevenue(curRevenue)
                .cash(cash)
                .transfer(transfer)
                .orderCount(orderCount)
                .aov(aov)
                .growthRate(growth)
                .build();
    }

    /**
     * 2. Thống kê theo Sản phẩm kết hợp với Hóa đơn
     */
    public List<ThongKeSanPhamDTO> getThongKeSanPham(ThongKeFilterRequest req) {
        Instant start = parseStartOfDay(req != null ? req.getFromDate() : null);
        Instant end = parseEndOfDay(req != null ? req.getToDate() : null);

        // Lấy tất cả hóa đơn trong khoảng lọc
        List<HoaDon> invoicesInRange = hoaDonRepository.findByNgayTaoBetweenWithForeignKeys(start, end)
                .stream().filter(h -> !isWaitingDraft(h)).toList();

        Map<Long, HoaDon> invoiceMap = invoicesInRange.stream().collect(Collectors.toMap(HoaDon::getId, h -> h, (a, b) -> a));

        List<Long> invoiceIds = new ArrayList<>(invoiceMap.keySet());
        List<ChiTietHoaDon> allDetails = invoiceIds.isEmpty() ? Collections.emptyList() : chiTietHoaDonRepository.findByIdHoaDon_IdIn(invoiceIds);

        // Lấy toàn bộ sản phẩm
        List<SanPham> allProducts = sanPhamRepository.findAll();

        // Lấy tất cả biến thể và ảnh
        List<ChiTietSanPham> allVariants = chiTietSanPhamRepository.findAll();
        Map<Long, List<ChiTietSanPham>> variantsByProduct = allVariants.stream()
                .filter(v -> v.getIdSanPham() != null)
                .collect(Collectors.groupingBy(v -> v.getIdSanPham().getId()));

        List<HinhAnh> allImages = hinhAnhRepository.findAll();
        Map<Long, String> imageByProduct = new HashMap<>();
        for (HinhAnh img : allImages) {
            if (img.getIdSanPham() != null && !imageByProduct.containsKey(img.getIdSanPham().getId())) {
                imageByProduct.put(img.getIdSanPham().getId(), img.getDuongDan());
            }
        }

        // Nhóm chi tiết hóa đơn theo id sản phẩm
        Map<Long, List<ChiTietHoaDon>> detailsByProductId = new HashMap<>();
        for (ChiTietHoaDon c : allDetails) {
            if (c.getIdChiTietSanPham() != null && c.getIdChiTietSanPham().getIdSanPham() != null) {
                Long spId = c.getIdChiTietSanPham().getIdSanPham().getId();
                detailsByProductId.computeIfAbsent(spId, k -> new ArrayList<>()).add(c);
            }
        }

        Long brandFilter = req != null ? req.getIdThuongHieu() : null;
        String kw = req != null && req.getKeyword() != null ? req.getKeyword().trim().toLowerCase() : "";

        List<ThongKeSanPhamDTO> result = new ArrayList<>();

        for (SanPham sp : allProducts) {
            // Bộ lọc thương hiệu
            if (brandFilter != null) {
                if (sp.getIdThuongHieu() == null || !Objects.equals(sp.getIdThuongHieu().getId(), brandFilter)) {
                    continue;
                }
            }

            // Bộ lọc từ khóa
            if (!kw.isEmpty()) {
                boolean matchCode = sp.getMaSanPham() != null && sp.getMaSanPham().toLowerCase().contains(kw);
                boolean matchName = sp.getTenSanPham() != null && sp.getTenSanPham().toLowerCase().contains(kw);
                if (!matchCode && !matchName) continue;
            }

            List<ChiTietSanPham> variants = variantsByProduct.getOrDefault(sp.getId(), Collections.emptyList());

            // Tồn kho
            long stock = variants.stream()
                    .mapToLong(v -> v.getSoLuong() != null ? v.getSoLuong() : 0)
                    .sum();

            // Kích cỡ
            String sizes = variants.stream()
                    .filter(v -> v.getIdKichCo() != null && v.getIdKichCo().getTenKichCo() != null)
                    .map(v -> v.getIdKichCo().getTenKichCo())
                    .distinct()
                    .collect(Collectors.joining(", "));
            if (sizes.isEmpty()) sizes = "Free Size";

            // Doanh số và danh sách hóa đơn chứa sản phẩm này
            List<ChiTietHoaDon> productDetails = detailsByProductId.getOrDefault(sp.getId(), Collections.emptyList());

            long soldCount = 0;
            long returnedCount = 0;
            BigDecimal revenue = BigDecimal.ZERO;
            List<ThongKeHoaDonItemDTO> invoicesForProduct = new ArrayList<>();

            for (ChiTietHoaDon it : productDetails) {
                HoaDon h = invoiceMap.get(it.getIdHoaDon() != null ? it.getIdHoaDon().getId() : null);
                if (h == null) continue;

                int qty = it.getSoLuong() != null ? it.getSoLuong() : 0;
                BigDecimal unitPrice = it.getDonGia() != null ? it.getDonGia() : BigDecimal.ZERO;
                BigDecimal lineTotal = it.getThanhTien() != null && it.getThanhTien().compareTo(BigDecimal.ZERO) > 0
                        ? it.getThanhTien() : unitPrice.multiply(BigDecimal.valueOf(qty));

                if (isCompletedOrder(h)) {
                    soldCount += qty;
                    revenue = revenue.add(lineTotal);
                } else if (Integer.valueOf(6).equals(h.getTrangThai())) {
                    returnedCount += qty;
                }

                // Thêm vào danh sách hóa đơn đã mua sản phẩm này
                String custName = h.getTenKhachHang() != null ? h.getTenKhachHang() : (h.getIdKhachHang() != null ? h.getIdKhachHang().getTenKhachHang() : "Khách lẻ");
                String phone = h.getSoDienThoaiKhachHang() != null ? h.getSoDienThoaiKhachHang() : (h.getIdKhachHang() != null ? h.getIdKhachHang().getSoDienThoai() : "");
                String dateStr = h.getNgayTao() != null ? h.getNgayTao().atZone(VN_ZONE).format(DATETIME_FMT) : "";

                invoicesForProduct.add(ThongKeHoaDonItemDTO.builder()
                        .idHoaDon(h.getId())
                        .maHoaDon(h.getMaHoaDon())
                        .ngayTao(dateStr)
                        .tenKhachHang(custName)
                        .soDienThoai(phone)
                        .loaiDon(h.getLoaiDon())
                        .soLuong(qty)
                        .donGia(unitPrice)
                        .thanhTien(lineTotal)
                        .trangThaiHoaDon(h.getTrangThai())
                        .tenTrangThai(getStatusLabel(h.getTrangThai()))
                        .build());
            }

            // Sắp xếp các hóa đơn mua sản phẩm mới nhất lên đầu
            invoicesForProduct.sort((a, b) -> Long.compare(b.getIdHoaDon(), a.getIdHoaDon()));

            String brandName = sp.getIdThuongHieu() != null ? sp.getIdThuongHieu().getTenThuongHieu() : "FF-Tshirt";
            String image = imageByProduct.getOrDefault(sp.getId(), null);

            result.add(ThongKeSanPhamDTO.builder()
                    .id(sp.getId())
                    .code(sp.getMaSanPham())
                    .name(sp.getTenSanPham())
                    .brand(brandName)
                    .sizes(sizes)
                    .soldCount(soldCount)
                    .returnedCount(returnedCount)
                    .revenue(revenue)
                    .stock(stock)
                    .image(image)
                    .invoiceCount(invoicesForProduct.size())
                    .invoices(invoicesForProduct)
                    .build());
        }

        return result;
    }

    /**
     * 3. Thống kê theo Hóa đơn kết hợp với Sản phẩm
     */
    public List<ThongKeHoaDonDTO> getThongKeHoaDon(ThongKeFilterRequest req) {
        Instant start = parseStartOfDay(req != null ? req.getFromDate() : null);
        Instant end = parseEndOfDay(req != null ? req.getToDate() : null);

        List<HoaDon> invoicesInRange = hoaDonRepository.findByNgayTaoBetweenWithForeignKeys(start, end)
                .stream().filter(h -> !isWaitingDraft(h)).toList();

        if (invoicesInRange.isEmpty()) return Collections.emptyList();

        List<Long> invoiceIds = invoicesInRange.stream().map(HoaDon::getId).toList();
        List<ChiTietHoaDon> allDetails = chiTietHoaDonRepository.findByIdHoaDon_IdIn(invoiceIds);
        List<HinhThucThanhToan> allPayments = hinhThucThanhToanRepository.findByIdHoaDon_IdIn(invoiceIds);

        Map<Long, List<ChiTietHoaDon>> detailsByInvoice = allDetails.stream()
                .filter(c -> c.getIdHoaDon() != null)
                .collect(Collectors.groupingBy(c -> c.getIdHoaDon().getId()));

        Map<Long, List<HinhThucThanhToan>> paymentsByInvoice = allPayments.stream()
                .filter(p -> p.getIdHoaDon() != null)
                .collect(Collectors.groupingBy(p -> p.getIdHoaDon().getId()));

        // Ảnh đại diện sản phẩm
        List<HinhAnh> allImages = hinhAnhRepository.findAll();
        Map<Long, String> imageByProduct = new HashMap<>();
        for (HinhAnh img : allImages) {
            if (img.getIdSanPham() != null && !imageByProduct.containsKey(img.getIdSanPham().getId())) {
                imageByProduct.put(img.getIdSanPham().getId(), img.getDuongDan());
            }
        }

        Long brandFilter = req != null ? req.getIdThuongHieu() : null;
        String kw = req != null && req.getKeyword() != null ? req.getKeyword().trim().toLowerCase() : "";
        Integer statusFilter = req != null ? req.getTrangThaiHoaDon() : null;

        List<ThongKeHoaDonDTO> result = new ArrayList<>();

        for (HoaDon h : invoicesInRange) {
            if (statusFilter != null && !statusFilter.equals(h.getTrangThai())) {
                continue;
            }

            List<ChiTietHoaDon> items = detailsByInvoice.getOrDefault(h.getId(), Collections.emptyList());

            if (brandFilter != null) {
                boolean hasBrand = items.stream().anyMatch(item ->
                        item.getIdChiTietSanPham() != null
                                && item.getIdChiTietSanPham().getIdSanPham() != null
                                && item.getIdChiTietSanPham().getIdSanPham().getIdThuongHieu() != null
                                && Objects.equals(item.getIdChiTietSanPham().getIdSanPham().getIdThuongHieu().getId(), brandFilter)
                );
                if (!hasBrand) continue;
            }

            if (!kw.isEmpty()) {
                boolean matchCode = h.getMaHoaDon() != null && h.getMaHoaDon().toLowerCase().contains(kw);
                boolean matchCust = (h.getTenKhachHang() != null && h.getTenKhachHang().toLowerCase().contains(kw))
                        || (h.getIdKhachHang() != null && h.getIdKhachHang().getTenKhachHang() != null && h.getIdKhachHang().getTenKhachHang().toLowerCase().contains(kw));
                boolean matchItem = items.stream().anyMatch(item -> {
                    if (item.getIdChiTietSanPham() == null || item.getIdChiTietSanPham().getIdSanPham() == null) return false;
                    SanPham sp = item.getIdChiTietSanPham().getIdSanPham();
                    return (sp.getMaSanPham() != null && sp.getMaSanPham().toLowerCase().contains(kw))
                            || (sp.getTenSanPham() != null && sp.getTenSanPham().toLowerCase().contains(kw));
                });
                if (!matchCode && !matchCust && !matchItem) continue;
            }

            // Phương thức thanh toán
            List<HinhThucThanhToan> pays = paymentsByInvoice.getOrDefault(h.getId(), Collections.emptyList());
            String pMethod = "Tiền mặt";
            if (!pays.isEmpty()) {
                List<String> names = pays.stream()
                        .filter(p -> p.getIdPhuongThuc() != null && p.getIdPhuongThuc().getTenPhuongThuc() != null)
                        .map(p -> p.getIdPhuongThuc().getTenPhuongThuc().trim())
                        .distinct()
                        .toList();
                if (!names.isEmpty()) {
                    pMethod = String.join(" + ", names);
                }
            } else if (Integer.valueOf(2).equals(h.getLoaiDon())) {
                pMethod = "Chuyển khoản (Online)";
            }

            // Danh sách sản phẩm trong hóa đơn
            List<ThongKeSanPhamItemDTO> sanPhams = new ArrayList<>();
            int totalItemsInInvoice = 0;

            for (ChiTietHoaDon it : items) {
                ChiTietSanPham ct = it.getIdChiTietSanPham();
                SanPham sp = ct != null ? ct.getIdSanPham() : null;

                String spMa = sp != null ? sp.getMaSanPham() : "";
                String spTen = sp != null ? sp.getTenSanPham() : "Sản phẩm";
                String brand = (sp != null && sp.getIdThuongHieu() != null) ? sp.getIdThuongHieu().getTenThuongHieu() : "";
                String mau = (ct != null && ct.getIdMauSac() != null) ? ct.getIdMauSac().getTenMauSac() : "";
                String size = (ct != null && ct.getIdKichCo() != null) ? ct.getIdKichCo().getTenKichCo() : "";
                int qty = it.getSoLuong() != null ? it.getSoLuong() : 0;
                totalItemsInInvoice += qty;
                BigDecimal unitPrice = it.getDonGia() != null ? it.getDonGia() : BigDecimal.ZERO;
                BigDecimal lineTotal = it.getThanhTien() != null && it.getThanhTien().compareTo(BigDecimal.ZERO) > 0
                        ? it.getThanhTien() : unitPrice.multiply(BigDecimal.valueOf(qty));
                String img = sp != null ? imageByProduct.getOrDefault(sp.getId(), null) : null;

                sanPhams.add(ThongKeSanPhamItemDTO.builder()
                        .idChiTietHoaDon(it.getId())
                        .idSanPham(sp != null ? sp.getId() : null)
                        .idChiTietSanPham(ct != null ? ct.getId() : null)
                        .maSanPham(spMa)
                        .tenSanPham(spTen)
                        .tenThuongHieu(brand)
                        .tenMauSac(mau)
                        .tenKichCo(size)
                        .soLuong(qty)
                        .donGia(unitPrice)
                        .thanhTien(lineTotal)
                        .anhDaiDien(img)
                        .build());
            }

            String custName = h.getTenKhachHang() != null ? h.getTenKhachHang() : (h.getIdKhachHang() != null ? h.getIdKhachHang().getTenKhachHang() : "Khách lẻ");
            String phone = h.getSoDienThoaiKhachHang() != null ? h.getSoDienThoaiKhachHang() : (h.getIdKhachHang() != null ? h.getIdKhachHang().getSoDienThoai() : "");
            String dateStr = h.getNgayTao() != null ? h.getNgayTao().atZone(VN_ZONE).format(DATETIME_FMT) : "";
            String loaiDonStr = Integer.valueOf(1).equals(h.getLoaiDon()) ? "Tại quầy" : "Online";
            String ttThanhToanStr = Integer.valueOf(1).equals(h.getTrangThaiThanhToan()) ? "Đã thanh toán" : "Chưa thanh toán";

            result.add(ThongKeHoaDonDTO.builder()
                    .id(h.getId())
                    .maHoaDon(h.getMaHoaDon())
                    .ngayTao(dateStr)
                    .tenKhachHang(custName)
                    .soDienThoai(phone)
                    .loaiDon(h.getLoaiDon())
                    .tenLoaiDon(loaiDonStr)
                    .phuongThucThanhToan(pMethod)
                    .tongTien(h.getTongTien() != null ? h.getTongTien() : BigDecimal.ZERO)
                    .tienSauGiamGia(h.getTienSauGiamGia() != null ? h.getTienSauGiamGia() : (h.getTongTien() != null ? h.getTongTien() : BigDecimal.ZERO))
                    .phiVanChuyen(h.getPhiVanChuyen() != null ? h.getPhiVanChuyen() : BigDecimal.ZERO)
                    .trangThai(h.getTrangThai())
                    .tenTrangThai(getStatusLabel(h.getTrangThai()))
                    .trangThaiThanhToan(h.getTrangThaiThanhToan())
                    .tenTrangThaiThanhToan(ttThanhToanStr)
                    .tongSoLuongSanPham(totalItemsInInvoice)
                    .sanPhams(sanPhams)
                    .build());
        }

        // Sắp xếp hóa đơn mới nhất lên trước
        result.sort((a, b) -> Long.compare(b.getId(), a.getId()));

        return result;
    }
}

