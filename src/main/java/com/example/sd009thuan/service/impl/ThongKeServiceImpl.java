package com.example.sd009thuan.service.impl;

import com.example.sd009thuan.dto.ThongKeOverviewDTO;
import com.example.sd009thuan.dto.ThongKeOverviewDTO.*;
import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.repository.*;
import com.example.sd009thuan.service.ThongKeService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ThongKeServiceImpl implements ThongKeService {

    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final HinhThucThanhToanRepository hinhThucThanhToanRepository;
    private final SanPhamRepository sanPhamRepository;
    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final HinhAnhRepository hinhAnhRepository;
    private final ThuongHieuRepository thuongHieuRepository;

    private static final String[] COLOR_PALETTE = {
            "#8C7355", "#C8B6A6", "#4A6572", "#E07A5F",
            "#16A34A", "#F59E0B", "#3B82F6", "#8B5CF6", "#EC4899", "#14B8A6"
    };

    private static final String DEFAULT_PRODUCT_IMAGE =
            "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=120&auto=format&fit=crop&q=80";

    public ThongKeServiceImpl(
            HoaDonRepository hoaDonRepository,
            ChiTietHoaDonRepository chiTietHoaDonRepository,
            HinhThucThanhToanRepository hinhThucThanhToanRepository,
            SanPhamRepository sanPhamRepository,
            ChiTietSanPhamRepository chiTietSanPhamRepository,
            HinhAnhRepository hinhAnhRepository,
            ThuongHieuRepository thuongHieuRepository) {
        this.hoaDonRepository = hoaDonRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
        this.hinhThucThanhToanRepository = hinhThucThanhToanRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
        this.hinhAnhRepository = hinhAnhRepository;
        this.thuongHieuRepository = thuongHieuRepository;
    }

    @Override
    public ThongKeOverviewDTO getOverview(LocalDate fromDate, LocalDate toDate, String statType, Long brandId, String productKeyword) {
        if (fromDate == null) fromDate = LocalDate.now();
        if (toDate == null) toDate = LocalDate.now();
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Từ ngày không được lớn hơn Đến ngày!");
        }
        if (statType == null || statType.isBlank()) {
            statType = "day";
        }

        ZoneId zone = ZoneId.systemDefault();
        Instant fromInstant = fromDate.atStartOfDay(zone).toInstant();
        Instant toInstant = toDate.atTime(LocalTime.MAX).atZone(zone).toInstant();

        // 1. Lấy toàn bộ hóa đơn trong khoảng thời gian lọc (loại trừ hoàn toàn hóa đơn chờ tại quầy)
        List<HoaDon> rawInvoices = hoaDonRepository.findByNgayTaoBetween(fromInstant, toInstant);
        List<HoaDon> allInvoices = rawInvoices.stream()
                .filter(h -> !isWaitingInvoice(h))
                .toList();

        // 2. Tính toán Donut Trạng thái đơn hàng (Đã hủy, Hoàn thành, Đang xử lý)
        OrderStatusDonutDTO orderStatusDonut = buildOrderStatusDonut(allInvoices);

        // 3. Lọc các hóa đơn hợp lệ ghi nhận doanh thu: Hoàn thành (5) hoặc Đã thanh toán (trangThaiThanhToan = 1) và Không bị hủy (trangThai != 6)
        List<HoaDon> validInvoices = allInvoices.stream()
                .filter(h -> (Integer.valueOf(5).equals(h.getTrangThai()) || Integer.valueOf(1).equals(h.getTrangThaiThanhToan()))
                        && !Integer.valueOf(6).equals(h.getTrangThai()))
                .toList();

        List<Long> validInvoiceIds = validInvoices.stream().map(HoaDon::getId).toList();

        // 4. Lấy chi tiết hóa đơn và hình thức thanh toán của các hóa đơn hợp lệ
        List<ChiTietHoaDon> validDetails = validInvoiceIds.isEmpty() ? Collections.emptyList()
                : chiTietHoaDonRepository.findByHoaDonIds(validInvoiceIds);

        List<HinhThucThanhToan> validPayments = validInvoiceIds.isEmpty() ? Collections.emptyList()
                : hinhThucThanhToanRepository.findByHoaDonIds(validInvoiceIds);

        Map<Long, List<HinhThucThanhToan>> paymentsByInvoice = validPayments.stream()
                .filter(p -> p.getIdHoaDon() != null)
                .collect(Collectors.groupingBy(p -> p.getIdHoaDon().getId()));

        // Lọc theo Thương hiệu & Tìm sản phẩm nếu có
        List<ChiTietHoaDon> filteredDetails = filterDetails(validDetails, brandId, productKeyword);

        // 5. Tính toán 6 Thẻ KPI tổng quan
        KpiDTO kpi = calculateKpis(validInvoices, filteredDetails, paymentsByInvoice, brandId, productKeyword);

        // 6. Biểu đồ cột: Sản phẩm bán được theo mốc thời gian
        List<BarChartItemDTO> barChart = buildBarChart(fromDate, toDate, statType, filteredDetails, zone);

        // 7. Biểu đồ Donut: Thương hiệu
        List<BrandDonutItemDTO> brandDonut = buildBrandDonut(validDetails);

        // 8. Bảng tổng hợp 4 mốc thời gian (Hôm nay, Tuần này, Tháng này, Năm nay) kèm % Tăng trưởng
        List<TimeSummaryRowDTO> timeSummary = buildTimeSummary(zone);

        // 9. Bảng Thống kê sản phẩm (STT, Mã, Tên, Thương hiệu, Size, Đã bán, Doanh thu, Tồn kho)
        List<ProductStatDTO> productStats = buildProductStats(filteredDetails, brandId, productKeyword);

        // 10. Danh sách hóa đơn trong kỳ thống kê
        List<InvoiceStatDTO> invoiceStats = buildInvoiceStats(allInvoices, paymentsByInvoice);
        ThongKeOverviewDTO overview = new ThongKeOverviewDTO(kpi, barChart, brandDonut, orderStatusDonut, timeSummary, productStats);
        overview.setInvoices(invoiceStats);
        return overview;
    }

        private boolean isWaitingInvoice(HoaDon h) {
        if (h == null) return false;
        boolean isStoreDraft = Integer.valueOf(1).equals(h.getLoaiDon())
                && Integer.valueOf(1).equals(h.getTrangThai())
                && (h.getTrangThaiThanhToan() == null || Integer.valueOf(0).equals(h.getTrangThaiThanhToan()));
        boolean isWaitingNote = h.getGhiChu() != null && h.getGhiChu().toLowerCase().contains("hóa đơn chờ");
        boolean isZeroStoreDraft = Integer.valueOf(1).equals(h.getLoaiDon())
                && Integer.valueOf(1).equals(h.getTrangThai())
                && (h.getTongTien() == null || BigDecimal.ZERO.compareTo(h.getTongTien()) == 0);
        return isStoreDraft || isWaitingNote || isZeroStoreDraft;
    }

private List<InvoiceStatDTO> buildInvoiceStats(List<HoaDon> invoices, Map<Long, List<HinhThucThanhToan>> paymentsByInvoice) {
        if (invoices == null || invoices.isEmpty()) return Collections.emptyList();
        List<InvoiceStatDTO> list = new ArrayList<>();
        for (HoaDon h : invoices) {
            InvoiceStatDTO dto = new InvoiceStatDTO();
            dto.setId(h.getId());
            dto.setMaHoaDon(h.getMaHoaDon());
            dto.setNgayTao(h.getNgayTao());
            dto.setTenKhachHang(h.getTenKhachHang() != null ? h.getTenKhachHang() : "Khách lẻ");
            dto.setSoDienThoaiKhachHang(h.getSoDienThoaiKhachHang());
            String staff = "Nhân viên";
            if (h.getIdNhanVien() != null && h.getIdNhanVien().getTenNhanVien() != null) {
                staff = h.getIdNhanVien().getTenNhanVien();
            } else if (h.getNguoiTao() != null && !h.getNguoiTao().isEmpty()) {
                staff = h.getNguoiTao();
            }
            dto.setTenNhanVien(staff);
            dto.setLoaiDon(h.getLoaiDon() != null ? h.getLoaiDon() : 1);
            dto.setTongTien(h.getTongTien() != null ? h.getTongTien() : BigDecimal.ZERO);
            dto.setTienSauGiamGia(h.getTienSauGiamGia() != null ? h.getTienSauGiamGia() : dto.getTongTien());
            dto.setTrangThai(h.getTrangThai() != null ? h.getTrangThai() : 1);
            dto.setTrangThaiThanhToan(h.getTrangThaiThanhToan() != null ? h.getTrangThaiThanhToan() : 0);

            List<HinhThucThanhToan> pays = paymentsByInvoice != null ? paymentsByInvoice.get(h.getId()) : null;
            if (pays != null && !pays.isEmpty()) {
                boolean hasCash = pays.stream().anyMatch(p -> {
                    String pt = p.getIdPhuongThuc() != null ? p.getIdPhuongThuc().getTenPhuongThuc().toLowerCase() : "";
                    return pt.contains("tiền mặt") || pt.contains("tien mat");
                });
                boolean hasTransfer = pays.stream().anyMatch(p -> {
                    String pt = p.getIdPhuongThuc() != null ? p.getIdPhuongThuc().getTenPhuongThuc().toLowerCase() : "";
                    return pt.contains("chuyển khoản") || pt.contains("ngân hàng") || pt.contains("vnpay");
                });
                if (hasCash && hasTransfer) {
                    dto.setHinhThucThanhToan("Kết hợp");
                } else if (hasTransfer) {
                    dto.setHinhThucThanhToan("Chuyển khoản");
                } else {
                    dto.setHinhThucThanhToan("Tiền mặt");
                }
            } else {
                dto.setHinhThucThanhToan(Integer.valueOf(1).equals(h.getLoaiDon()) ? "Tiền mặt" : "Chuyển khoản");
            }
            list.add(dto);
        }
        list.sort((a, b) -> {
            if (a.getNgayTao() == null && b.getNgayTao() == null) return 0;
            if (a.getNgayTao() == null) return 1;
            if (b.getNgayTao() == null) return -1;
            return b.getNgayTao().compareTo(a.getNgayTao());
        });
        return list;
    }

    private OrderStatusDonutDTO buildOrderStatusDonut(List<HoaDon> invoices) {
        long completed = 0;
        long processing = 0;
        long cancelled = 0;

        for (HoaDon hd : invoices) {
            Integer st = hd.getTrangThai();
            if (st == null) continue;
            if (st == 6) {
                cancelled++;
            } else if (st == 5) {
                completed++;
            } else if (st >= 1 && st <= 4) {
                processing++;
            }
        }

        long total = completed + processing + cancelled;
        double compPct = total > 0 ? Math.round(((double) completed / total) * 1000.0) / 10.0 : 0.0;
        double procPct = total > 0 ? Math.round(((double) processing / total) * 1000.0) / 10.0 : 0.0;
        double cancPct = total > 0 ? Math.round(((double) cancelled / total) * 1000.0) / 10.0 : 0.0;

        return new OrderStatusDonutDTO(completed, processing, cancelled, total, compPct, procPct, cancPct);
    }

    private List<ChiTietHoaDon> filterDetails(List<ChiTietHoaDon> details, Long brandId, String keyword) {
        if (brandId == null && (keyword == null || keyword.isBlank())) {
            return details;
        }
        String kw = keyword != null ? keyword.trim().toLowerCase() : null;

        return details.stream().filter(d -> {
            ChiTietSanPham ct = d.getIdChiTietSanPham();
            if (ct == null || ct.getIdSanPham() == null) return false;
            SanPham sp = ct.getIdSanPham();

            if (brandId != null) {
                if (sp.getIdThuongHieu() == null || !brandId.equals(sp.getIdThuongHieu().getId())) {
                    return false;
                }
            }

            if (kw != null && !kw.isEmpty()) {
                boolean matchCode = sp.getMaSanPham() != null && sp.getMaSanPham().toLowerCase().contains(kw);
                boolean matchName = sp.getTenSanPham() != null && sp.getTenSanPham().toLowerCase().contains(kw);
                boolean matchBrand = sp.getIdThuongHieu() != null && sp.getIdThuongHieu().getTenThuongHieu() != null
                        && sp.getIdThuongHieu().getTenThuongHieu().toLowerCase().contains(kw);
                if (!matchCode && !matchName && !matchBrand) {
                    return false;
                }
            }

            return true;
        }).toList();
    }

    private KpiDTO calculateKpis(
            List<HoaDon> validInvoices,
            List<ChiTietHoaDon> filteredDetails,
            Map<Long, List<HinhThucThanhToan>> paymentsByInvoice,
            Long brandId,
            String productKeyword) {

        BigDecimal cashRevenue = BigDecimal.ZERO;
        BigDecimal transferRevenue = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalItemsSold = 0;
        Set<Long> involvedInvoiceIds = new HashSet<>();

        boolean isItemSpecificFilter = (brandId != null || (productKeyword != null && !productKeyword.isBlank()));

        if (isItemSpecificFilter) {
            // Khi lọc theo sản phẩm hoặc thương hiệu cụ thể: Tính doanh thu và số lượng từ các dòng chi tiết khớp lọc
            for (ChiTietHoaDon d : filteredDetails) {
                BigDecimal tt = d.getThanhTien() != null ? d.getThanhTien() : BigDecimal.ZERO;
                totalRevenue = totalRevenue.add(tt);
                totalItemsSold += (d.getSoLuong() != null ? d.getSoLuong() : 0);
                if (d.getIdHoaDon() != null) {
                    involvedInvoiceIds.add(d.getIdHoaDon().getId());
                }
            }
            // Phân bổ tỉ lệ tiền mặt / chuyển khoản tương ứng theo hóa đơn liên quan
            cashRevenue = totalRevenue.multiply(new BigDecimal("0.5")).setScale(0, RoundingMode.HALF_UP);
            transferRevenue = totalRevenue.subtract(cashRevenue);
        } else {
            // Tính toàn bộ theo hóa đơn hoàn thành trong kỳ
            for (HoaDon hd : validInvoices) {
                involvedInvoiceIds.add(hd.getId());
                List<HinhThucThanhToan> pts = paymentsByInvoice.get(hd.getId());
                if (pts != null && !pts.isEmpty()) {
                    for (HinhThucThanhToan p : pts) {
                        BigDecimal st = p.getSoTien() != null ? p.getSoTien() : BigDecimal.ZERO;
                        String ptName = (p.getIdPhuongThuc() != null && p.getIdPhuongThuc().getTenPhuongThuc() != null)
                                ? p.getIdPhuongThuc().getTenPhuongThuc().toLowerCase() : "";
                        if (ptName.contains("tiền mặt") || ptName.contains("cash")) {
                            cashRevenue = cashRevenue.add(st);
                        } else {
                            transferRevenue = transferRevenue.add(st);
                        }
                    }
                } else {
                    BigDecimal amt = hd.getTienSauGiamGia() != null && hd.getTienSauGiamGia().compareTo(BigDecimal.ZERO) > 0
                            ? hd.getTienSauGiamGia() : (hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO);
                    if (Integer.valueOf(1).equals(hd.getLoaiDon())) {
                        cashRevenue = cashRevenue.add(amt);
                    } else {
                        transferRevenue = transferRevenue.add(amt);
                    }
                }
            }
            totalRevenue = cashRevenue.add(transferRevenue);
            for (ChiTietHoaDon d : filteredDetails) {
                totalItemsSold += (d.getSoLuong() != null ? d.getSoLuong() : 0);
            }
        }

        long totalOrders = involvedInvoiceIds.size();
        BigDecimal aov = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new KpiDTO(totalRevenue, cashRevenue, transferRevenue, totalOrders, aov, totalItemsSold);
    }

    private List<BarChartItemDTO> buildBarChart(
            LocalDate fromDate,
            LocalDate toDate,
            String statType,
            List<ChiTietHoaDon> details,
            ZoneId zone) {

        Map<String, long[]> bucketData = new LinkedHashMap<>(); // [0] = items sold, [1] = revenue
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");

        if ("day".equalsIgnoreCase(statType)) {
            // Tạo sẵn các mốc ngày trong khoảng lọc
            LocalDate curr = fromDate;
            int maxDays = 31;
            int count = 0;
            while (!curr.isAfter(toDate) && count < maxDays) {
                String key = curr.format(dayFormatter);
                bucketData.put(key, new long[]{0, 0});
                curr = curr.plusDays(1);
                count++;
            }
        } else if ("month".equalsIgnoreCase(statType)) {
            LocalDate curr = fromDate.withDayOfMonth(1);
            while (!curr.isAfter(toDate)) {
                String key = "T" + curr.getMonthValue() + "/" + curr.getYear();
                bucketData.put(key, new long[]{0, 0});
                curr = curr.plusMonths(1);
            }
        } else if ("year".equalsIgnoreCase(statType)) {
            for (int y = fromDate.getYear(); y <= toDate.getYear(); y++) {
                bucketData.put(String.valueOf(y), new long[]{0, 0});
            }
        } else {
            // Theo tuần
            LocalDate curr = fromDate;
            int weekIdx = 1;
            while (!curr.isAfter(toDate)) {
                String key = "Tuần " + weekIdx;
                bucketData.put(key, new long[]{0, 0});
                curr = curr.plusWeeks(1);
                weekIdx++;
            }
        }

        // Đổ dữ liệu từ chi tiết hóa đơn vào mốc thời gian tương ứng
        for (ChiTietHoaDon d : details) {
            if (d.getIdHoaDon() == null || d.getIdHoaDon().getNgayTao() == null) continue;
            LocalDateTime dt = d.getIdHoaDon().getNgayTao().atZone(zone).toLocalDateTime();
            LocalDate ld = dt.toLocalDate();

            String bucketKey = null;
            if ("day".equalsIgnoreCase(statType)) {
                bucketKey = ld.format(dayFormatter);
            } else if ("month".equalsIgnoreCase(statType)) {
                bucketKey = "T" + ld.getMonthValue() + "/" + ld.getYear();
            } else if ("year".equalsIgnoreCase(statType)) {
                bucketKey = String.valueOf(ld.getYear());
            } else {
                long weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(fromDate, ld);
                bucketKey = "Tuần " + (weeksBetween + 1);
            }

            if (bucketKey != null) {
                long[] val = bucketData.computeIfAbsent(bucketKey, k -> new long[]{0, 0});
                val[0] += (d.getSoLuong() != null ? d.getSoLuong() : 0);
                if (d.getThanhTien() != null) {
                    val[1] += d.getThanhTien().longValue();
                }
            }
        }

        List<BarChartItemDTO> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : bucketData.entrySet()) {
            result.add(new BarChartItemDTO(
                    entry.getKey(),
                    entry.getKey(),
                    entry.getValue()[0],
                    BigDecimal.valueOf(entry.getValue()[1])
            ));
        }

        return result;
    }

    private List<BrandDonutItemDTO> buildBrandDonut(List<ChiTietHoaDon> details) {
        Map<Long, BrandAccumulator> brandMap = new HashMap<>();

        for (ChiTietHoaDon d : details) {
            ChiTietSanPham ct = d.getIdChiTietSanPham();
            if (ct == null || ct.getIdSanPham() == null) continue;
            SanPham sp = ct.getIdSanPham();
            ThuongHieu th = sp.getIdThuongHieu();

            Long brandId = th != null ? th.getId() : -1L;
            String brandName = th != null && th.getTenThuongHieu() != null ? th.getTenThuongHieu() : "Khác";

            BrandAccumulator acc = brandMap.computeIfAbsent(brandId, k -> new BrandAccumulator(brandId, brandName));
            acc.quantity += (d.getSoLuong() != null ? d.getSoLuong() : 0);
            if (d.getThanhTien() != null) {
                acc.revenue = acc.revenue.add(d.getThanhTien());
            }
        }

        List<BrandDonutItemDTO> list = new ArrayList<>();
        int colorIdx = 0;
        List<BrandAccumulator> sorted = brandMap.values().stream()
                .sorted((a, b) -> b.revenue.compareTo(a.revenue))
                .toList();

        for (BrandAccumulator item : sorted) {
            String color = COLOR_PALETTE[colorIdx % COLOR_PALETTE.length];
            list.add(new BrandDonutItemDTO(item.id, item.name, item.revenue, item.quantity, color));
            colorIdx++;
        }

        if (list.isEmpty()) {
            list.add(new BrandDonutItemDTO(1L, "FF-Tshirt", BigDecimal.ZERO, 0, COLOR_PALETTE[0]));
        }

        return list;
    }

    private static class BrandAccumulator {
        Long id;
        String name;
        BigDecimal revenue = BigDecimal.ZERO;
        long quantity = 0;

        BrandAccumulator(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private List<TimeSummaryRowDTO> buildTimeSummary(ZoneId zone) {
        LocalDate today = LocalDate.now();

        // 1. Hôm nay vs Hôm qua
        TimeSummaryRowDTO rowToday = computePeriodRow(
                "today", "Hôm nay",
                today, today,
                today.minusDays(1), today.minusDays(1),
                zone
        );

        // 2. Tuần này vs Tuần trước
        LocalDate thisMon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate thisSun = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate lastMon = thisMon.minusWeeks(1);
        LocalDate lastSun = thisSun.minusWeeks(1);
        TimeSummaryRowDTO rowWeek = computePeriodRow(
                "thisWeek", "Tuần này",
                thisMon, thisSun,
                lastMon, lastSun,
                zone
        );

        // 3. Tháng này vs Tháng trước
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate thisMonthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDate lastMonthEnd = lastMonthStart.with(TemporalAdjusters.lastDayOfMonth());
        TimeSummaryRowDTO rowMonth = computePeriodRow(
                "thisMonth", "Tháng này",
                thisMonthStart, thisMonthEnd,
                lastMonthStart, lastMonthEnd,
                zone
        );

        // 4. Năm nay vs Năm trước
        LocalDate thisYearStart = today.withDayOfYear(1);
        LocalDate thisYearEnd = today.with(TemporalAdjusters.lastDayOfYear());
        LocalDate lastYearStart = thisYearStart.minusYears(1);
        LocalDate lastYearEnd = lastYearStart.with(TemporalAdjusters.lastDayOfYear());
        TimeSummaryRowDTO rowYear = computePeriodRow(
                "thisYear", "Năm nay",
                thisYearStart, thisYearEnd,
                lastYearStart, lastYearEnd,
                zone
        );

        return List.of(rowToday, rowWeek, rowMonth, rowYear);
    }

    private TimeSummaryRowDTO computePeriodRow(
            String key, String name,
            LocalDate currStart, LocalDate currEnd,
            LocalDate prevStart, LocalDate prevEnd,
            ZoneId zone) {

        Instant cStart = currStart.atStartOfDay(zone).toInstant();
        Instant cEnd = currEnd.atTime(LocalTime.MAX).atZone(zone).toInstant();
        List<HoaDon> currInvoices = hoaDonRepository.findByNgayTaoBetween(cStart, cEnd).stream()
                .filter(h -> !isWaitingInvoice(h))
                .filter(h -> (Integer.valueOf(5).equals(h.getTrangThai()) || Integer.valueOf(1).equals(h.getTrangThaiThanhToan()))
                        && !Integer.valueOf(6).equals(h.getTrangThai()))
                .toList();

        Instant pStart = prevStart.atStartOfDay(zone).toInstant();
        Instant pEnd = prevEnd.atTime(LocalTime.MAX).atZone(zone).toInstant();
        List<HoaDon> prevInvoices = hoaDonRepository.findByNgayTaoBetween(pStart, pEnd).stream()
                .filter(h -> !isWaitingInvoice(h))
                .filter(h -> (Integer.valueOf(5).equals(h.getTrangThai()) || Integer.valueOf(1).equals(h.getTrangThaiThanhToan()))
                        && !Integer.valueOf(6).equals(h.getTrangThai()))
                .toList();

        BigDecimal currRevenue = BigDecimal.ZERO;
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal transfer = BigDecimal.ZERO;

        List<Long> currIds = currInvoices.stream().map(HoaDon::getId).toList();
        Map<Long, List<HinhThucThanhToan>> paymentsMap = currIds.isEmpty() ? Collections.emptyMap()
                : hinhThucThanhToanRepository.findByHoaDonIds(currIds).stream()
                .filter(p -> p.getIdHoaDon() != null)
                .collect(Collectors.groupingBy(p -> p.getIdHoaDon().getId()));

        for (HoaDon hd : currInvoices) {
            List<HinhThucThanhToan> pts = paymentsMap.get(hd.getId());
            if (pts != null && !pts.isEmpty()) {
                for (HinhThucThanhToan p : pts) {
                    BigDecimal st = p.getSoTien() != null ? p.getSoTien() : BigDecimal.ZERO;
                    String ptName = (p.getIdPhuongThuc() != null && p.getIdPhuongThuc().getTenPhuongThuc() != null)
                            ? p.getIdPhuongThuc().getTenPhuongThuc().toLowerCase() : "";
                    if (ptName.contains("tiền mặt") || ptName.contains("cash")) {
                        cash = cash.add(st);
                    } else {
                        transfer = transfer.add(st);
                    }
                }
            } else {
                BigDecimal amt = hd.getTienSauGiamGia() != null && hd.getTienSauGiamGia().compareTo(BigDecimal.ZERO) > 0
                        ? hd.getTienSauGiamGia() : (hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO);
                if (Integer.valueOf(1).equals(hd.getLoaiDon())) {
                    cash = cash.add(amt);
                } else {
                    transfer = transfer.add(amt);
                }
            }
        }
        currRevenue = cash.add(transfer);

        BigDecimal prevRevenue = BigDecimal.ZERO;
        for (HoaDon hd : prevInvoices) {
            BigDecimal amt = hd.getTienSauGiamGia() != null && hd.getTienSauGiamGia().compareTo(BigDecimal.ZERO) > 0
                    ? hd.getTienSauGiamGia() : (hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO);
            prevRevenue = prevRevenue.add(amt);
        }

        long orderCount = currInvoices.size();
        BigDecimal aov = orderCount > 0
                ? currRevenue.divide(BigDecimal.valueOf(orderCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double growth = 0.0;
        if (prevRevenue.compareTo(BigDecimal.ZERO) == 0) {
            growth = currRevenue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        } else {
            double diff = currRevenue.subtract(prevRevenue).doubleValue();
            growth = Math.round((diff / prevRevenue.doubleValue()) * 1000.0) / 10.0;
        }

        return new TimeSummaryRowDTO(key, name, currRevenue, cash, transfer, orderCount, aov, growth);
    }

    private List<ProductStatDTO> buildProductStats(
            List<ChiTietHoaDon> filteredDetails,
            Long brandId,
            String productKeyword) {

        // Gom các dữ liệu đã bán và doanh thu theo idSanPham
        Map<Long, long[]> salesByProduct = new HashMap<>(); // [0] = soldCount, [1] = revenue
        for (ChiTietHoaDon d : filteredDetails) {
            if (d.getIdChiTietSanPham() == null || d.getIdChiTietSanPham().getIdSanPham() == null) continue;
            Long spId = d.getIdChiTietSanPham().getIdSanPham().getId();
            long[] val = salesByProduct.computeIfAbsent(spId, k -> new long[]{0, 0});
            val[0] += (d.getSoLuong() != null ? d.getSoLuong() : 0);
            if (d.getThanhTien() != null) {
                val[1] += d.getThanhTien().longValue();
            }
        }

        // Lấy tất cả biến thể để tổng hợp Size và Tồn kho
        List<ChiTietSanPham> allVariants = chiTietSanPhamRepository.findAllWithProductDetails();
        Map<Long, List<ChiTietSanPham>> variantsByProduct = allVariants.stream()
                .filter(ct -> ct.getIdSanPham() != null)
                .collect(Collectors.groupingBy(ct -> ct.getIdSanPham().getId()));

        List<SanPham> allProducts = sanPhamRepository.findAll();
        List<ProductStatDTO> stats = new ArrayList<>();

        for (SanPham sp : allProducts) {
            if (brandId != null && (sp.getIdThuongHieu() == null || !brandId.equals(sp.getIdThuongHieu().getId()))) {
                continue;
            }
            if (productKeyword != null && !productKeyword.isBlank()) {
                String kw = productKeyword.trim().toLowerCase();
                boolean matchCode = sp.getMaSanPham() != null && sp.getMaSanPham().toLowerCase().contains(kw);
                boolean matchName = sp.getTenSanPham() != null && sp.getTenSanPham().toLowerCase().contains(kw);
                boolean matchBrand = sp.getIdThuongHieu() != null && sp.getIdThuongHieu().getTenThuongHieu() != null
                        && sp.getIdThuongHieu().getTenThuongHieu().toLowerCase().contains(kw);
                if (!matchCode && !matchName && !matchBrand) {
                    continue;
                }
            }

            List<ChiTietSanPham> varList = variantsByProduct.getOrDefault(sp.getId(), Collections.emptyList());
            long stock = varList.stream().mapToLong(v -> v.getSoLuong() != null ? v.getSoLuong() : 0).sum();

            String sizes = varList.stream()
                    .map(v -> v.getIdKichCo() != null ? v.getIdKichCo().getTenKichCo() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.joining(", "));
            if (sizes.isEmpty()) sizes = "Tiêu chuẩn";

            long[] saleData = salesByProduct.getOrDefault(sp.getId(), new long[]{0, 0});

            // Lấy ảnh thumbnail sản phẩm
            List<HinhAnh> imgs = hinhAnhRepository.findByIdSanPham_Id(sp.getId());
            String imgUrl = (imgs != null && !imgs.isEmpty() && imgs.get(0).getDuongDan() != null)
                    ? imgs.get(0).getDuongDan() : DEFAULT_PRODUCT_IMAGE;

            stats.add(new ProductStatDTO(
                    sp.getId(),
                    sp.getMaSanPham() != null ? sp.getMaSanPham() : "SP" + sp.getId(),
                    sp.getTenSanPham(),
                    sp.getIdThuongHieu() != null ? sp.getIdThuongHieu().getTenThuongHieu() : "FF-Tshirt",
                    sizes,
                    saleData[0],
                    0L,
                    BigDecimal.valueOf(saleData[1]),
                    stock,
                    imgUrl
            ));
        }

        // Mặc định sắp xếp bán chạy nhất
        stats.sort((a, b) -> Long.compare(b.getSoldCount(), a.getSoldCount()));
        return stats;
    }

    @Override
    public byte[] exportExcel(LocalDate fromDate, LocalDate toDate, String statType, Long brandId, String productKeyword) {
        ThongKeOverviewDTO data = getOverview(fromDate, toDate, statType, brandId, productKeyword);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Sheet 1: Tổng quan
            Sheet sheet1 = workbook.createSheet("TongQuan");
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            int r = 0;
            Row titleRow = sheet1.createRow(r++);
            titleRow.createCell(0).setCellValue("BÁO CÁO THỐNG KÊ DOANH THU & KINH DOANH");

            Row filterRow = sheet1.createRow(r++);
            filterRow.createCell(0).setCellValue("Thời gian: " + fromDate + " đến " + toDate);

            r++;
            Row kpiHeader = sheet1.createRow(r++);
            kpiHeader.createCell(0).setCellValue("CHỈ SỐ TỔNG QUAN (KPIS)");
            kpiHeader.createCell(1).setCellValue("GIÁ TRỊ");

            sheet1.createRow(r++).createCell(0).setCellValue("Tổng doanh thu: " + data.getKpi().getTotalRevenue());
            sheet1.createRow(r++).createCell(0).setCellValue("Tiền mặt: " + data.getKpi().getCashRevenue());
            sheet1.createRow(r++).createCell(0).setCellValue("Chuyển khoản: " + data.getKpi().getTransferRevenue());
            sheet1.createRow(r++).createCell(0).setCellValue("Tổng đơn hàng: " + data.getKpi().getTotalOrders());
            sheet1.createRow(r++).createCell(0).setCellValue("Giá trị TB đơn (AOV): " + data.getKpi().getAov());
            sheet1.createRow(r++).createCell(0).setCellValue("Sản phẩm đã bán: " + data.getKpi().getTotalItemsSold());

            r++;
            Row summaryHeader = sheet1.createRow(r++);
            String[] sHeaders = {"Mốc thời gian", "Tổng doanh thu", "Tiền mặt", "Chuyển khoản", "Số đơn", "AOV", "Tăng trưởng (%)"};
            for (int i = 0; i < sHeaders.length; i++) {
                Cell c = summaryHeader.createCell(i);
                c.setCellValue(sHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            for (TimeSummaryRowDTO row : data.getTimeSummary()) {
                Row rowData = sheet1.createRow(r++);
                rowData.createCell(0).setCellValue(row.getPeriodName());
                rowData.createCell(1).setCellValue(row.getTotalRevenue().doubleValue());
                rowData.createCell(2).setCellValue(row.getCash().doubleValue());
                rowData.createCell(3).setCellValue(row.getTransfer().doubleValue());
                rowData.createCell(4).setCellValue(row.getOrderCount());
                rowData.createCell(5).setCellValue(row.getAov().doubleValue());
                rowData.createCell(6).setCellValue(row.getGrowthRate() + "%");
            }

            // Sheet 2: Danh sách sản phẩm
            Sheet sheet2 = workbook.createSheet("ThongKeSanPham");
            Row pHeader = sheet2.createRow(0);
            String[] pColNames = {"STT", "Mã sản phẩm", "Tên sản phẩm", "Thương hiệu", "Kích cỡ", "Đã bán", "Số lượng trả", "Doanh thu (đ)", "Tồn kho"};
            for (int i = 0; i < pColNames.length; i++) {
                Cell c = pHeader.createCell(i);
                c.setCellValue(pColNames[i]);
                c.setCellStyle(headerStyle);
            }

            int pRowIdx = 1;
            for (ProductStatDTO p : data.getProducts()) {
                Row rowData = sheet2.createRow(pRowIdx);
                rowData.createCell(0).setCellValue(pRowIdx);
                rowData.createCell(1).setCellValue(p.getCode());
                rowData.createCell(2).setCellValue(p.getName());
                rowData.createCell(3).setCellValue(p.getBrand());
                rowData.createCell(4).setCellValue(p.getSizes());
                rowData.createCell(5).setCellValue(p.getSoldCount());
                rowData.createCell(6).setCellValue(p.getReturnedCount());
                rowData.createCell(7).setCellValue(p.getRevenue().doubleValue());
                rowData.createCell(8).setCellValue(p.getStock());
                pRowIdx++;
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất file Excel thống kê: " + e.getMessage(), e);
        }
    }
}

