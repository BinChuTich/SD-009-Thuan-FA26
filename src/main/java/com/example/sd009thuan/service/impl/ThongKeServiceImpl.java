package com.example.sd009thuan.service.impl;

import com.example.sd009thuan.dto.PeriodRequestDTO;
import com.example.sd009thuan.dto.ThongKeOverviewDTO;
import com.example.sd009thuan.dto.ThongKeOverviewDTO.*;
import com.example.sd009thuan.dto.ThongKeSoSanhDTO;
import com.example.sd009thuan.dto.ThongKeSoSanhDTO.*;
import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.repository.*;
import com.example.sd009thuan.service.EmailService;
import com.example.sd009thuan.service.ThongKeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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

    private static final Logger log = LoggerFactory.getLogger(ThongKeServiceImpl.class);

    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final HinhThucThanhToanRepository hinhThucThanhToanRepository;
    private final SanPhamRepository sanPhamRepository;
    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final HinhAnhRepository hinhAnhRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final NhanVienRepository nhanVienRepository;
    private final EmailService emailService;

    @Value("${app.admin.email:${spring.mail.username:thuanminh2908200334.1@gmail.com}}")
    private String defaultAdminEmail;

    private static final String[] COLOR_PALETTE = {
            "#8C7355", "#C8B6A6", "#4A6572", "#E07A5F",
            "#16A34A", "#F59E0B", "#3B82F6", "#8B5CF6", "#EC4899", "#14B8A6"
    };

    private static final String DEFAULT_PRODUCT_IMAGE =
            "/images/products/default_tshirt.jpg";

    public ThongKeServiceImpl(
            HoaDonRepository hoaDonRepository,
            ChiTietHoaDonRepository chiTietHoaDonRepository,
            HinhThucThanhToanRepository hinhThucThanhToanRepository,
            SanPhamRepository sanPhamRepository,
            ChiTietSanPhamRepository chiTietSanPhamRepository,
            HinhAnhRepository hinhAnhRepository,
            ThuongHieuRepository thuongHieuRepository,
            NhanVienRepository nhanVienRepository,
            EmailService emailService) {
        this.hoaDonRepository = hoaDonRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
        this.hinhThucThanhToanRepository = hinhThucThanhToanRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
        this.hinhAnhRepository = hinhAnhRepository;
        this.thuongHieuRepository = thuongHieuRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.emailService = emailService;
    }

    // =========================================================================
    // PHẦN 1: THỐNG KÊ TỔNG QUAN (OVERVIEW)
    // =========================================================================

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
        kpi.setTotalOrders(allInvoices.size());
        kpi.setCompletedOrders(orderStatusDonut.getCompleted());
        kpi.setCancelledOrders(orderStatusDonut.getCancelled());
        kpi.setProcessingOrders(orderStatusDonut.getProcessing());

        // 6. Biểu đồ cột: Sản phẩm bán được theo mốc thời gian
        List<BarChartItemDTO> barChart = buildBarChart(fromDate, toDate, statType, filteredDetails, zone);

        // 7. Biểu đồ Donut: Thương hiệu (đảm bảo đồng bộ với bộ lọc)
        List<BrandDonutItemDTO> brandDonut = buildBrandDonut(brandId != null ? filteredDetails : validDetails);

        // 8. Bảng tổng hợp 4 mốc thời gian (Hôm nay, Tuần này, Tháng này, Năm nay) kèm % Tăng trưởng
        List<TimeSummaryRowDTO> timeSummary = buildTimeSummary(zone);

        // 9. Bảng Thống kê sản phẩm (STT, Mã, Tên, Thương hiệu, Size, Đã bán, Doanh thu, Tồn kho)
        List<ProductStatDTO> productStats = buildProductStats(filteredDetails, brandId, productKeyword);

        // 10. Danh sách hóa đơn trong kỳ thống kê (Đồng bộ theo bộ lọc)
        List<HoaDon> displayedInvoices = allInvoices;
        if (brandId != null || (productKeyword != null && !productKeyword.isBlank())) {
            Set<Long> matchedInvoiceIds = filteredDetails.stream()
                    .filter(d -> d.getIdHoaDon() != null)
                    .map(d -> d.getIdHoaDon().getId())
                    .collect(Collectors.toSet());
            displayedInvoices = allInvoices.stream()
                    .filter(h -> matchedInvoiceIds.contains(h.getId()))
                    .toList();
        }
        List<InvoiceStatDTO> invoiceStats = buildInvoiceStats(displayedInvoices, paymentsByInvoice);
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
            if (d.getIdChiTietSanPham() == null || d.getIdChiTietSanPham().getIdSanPham() == null) {
                return false;
            }
            SanPham sp = d.getIdChiTietSanPham().getIdSanPham();

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

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal cashRevenue = BigDecimal.ZERO;
        BigDecimal transferRevenue = BigDecimal.ZERO;
        long totalItemsSold = 0;

        Set<Long> involvedInvoiceIds = new HashSet<>();
        boolean isFiltered = (brandId != null || (productKeyword != null && !productKeyword.isBlank()));

        if (isFiltered) {
            // Khi lọc theo sản phẩm / thương hiệu: doanh thu tính theo các dòng chi tiết thỏa mãn bộ lọc
            for (ChiTietHoaDon d : filteredDetails) {
                BigDecimal tt = d.getThanhTien() != null ? d.getThanhTien() : BigDecimal.ZERO;
                totalRevenue = totalRevenue.add(tt);
                totalItemsSold += (d.getSoLuong() != null ? d.getSoLuong() : 0);
                if (d.getIdHoaDon() != null && d.getIdHoaDon().getId() != null) {
                    involvedInvoiceIds.add(d.getIdHoaDon().getId());
                }
            }

            // Tỷ lệ phân bổ tiền mặt / chuyển khoản tương ứng theo hóa đơn
            for (Long invId : involvedInvoiceIds) {
                List<HinhThucThanhToan> pts = paymentsByInvoice.get(invId);
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
                    cashRevenue = totalRevenue;
                }
            }
            BigDecimal sumPts = cashRevenue.add(transferRevenue);
            if (sumPts.compareTo(BigDecimal.ZERO) > 0 && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                cashRevenue = totalRevenue.multiply(cashRevenue).divide(sumPts, 0, RoundingMode.HALF_UP);
                transferRevenue = totalRevenue.subtract(cashRevenue);
            }
        } else {
            // Không lọc thương hiệu / sản phẩm: Tính chính xác từ bảng HoaDon & HinhThucThanhToan
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
            LocalDate curr = fromDate;
            int maxDays = 90;
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
                long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(fromDate, ld);
                long w = (daysDiff / 7) + 1;
                bucketKey = "Tuần " + w;
            }

            if (bucketKey != null && bucketData.containsKey(bucketKey)) {
                long[] val = bucketData.get(bucketKey);
                val[0] += (d.getSoLuong() != null ? d.getSoLuong() : 0);
                if (d.getThanhTien() != null) {
                    val[1] += d.getThanhTien().longValue();
                }
            }
        }

        List<BarChartItemDTO> list = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : bucketData.entrySet()) {
            list.add(new BarChartItemDTO(entry.getKey(), entry.getKey(), entry.getValue()[0], BigDecimal.valueOf(entry.getValue()[1])));
        }
        return list;
    }

    private List<BrandDonutItemDTO> buildBrandDonut(List<ChiTietHoaDon> validDetails) {
        Map<Long, BrandAccumulator> brandMap = new HashMap<>();

        for (ChiTietHoaDon d : validDetails) {
            if (d.getIdChiTietSanPham() == null || d.getIdChiTietSanPham().getIdSanPham() == null) continue;
            SanPham sp = d.getIdChiTietSanPham().getIdSanPham();
            ThuongHieu th = sp.getIdThuongHieu();

            Long bId = th != null ? th.getId() : 0L;
            String bName = (th != null && th.getTenThuongHieu() != null) ? th.getTenThuongHieu() : "Khác";

            BrandAccumulator acc = brandMap.computeIfAbsent(bId, k -> new BrandAccumulator(bId, bName));
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

        for (HoaDon hd : currInvoices) {
            BigDecimal amt = hd.getTienSauGiamGia() != null && hd.getTienSauGiamGia().compareTo(BigDecimal.ZERO) > 0
                    ? hd.getTienSauGiamGia() : (hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO);
            if (Integer.valueOf(1).equals(hd.getLoaiDon())) {
                cash = cash.add(amt);
            } else {
                transfer = transfer.add(amt);
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

        double growth = calculateGrowthRate(currRevenue, prevRevenue);

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

            // Lấy ảnh thumbnail sản phẩm đồng bộ chính xác
            List<HinhAnh> imgs = hinhAnhRepository.findByIdSanPham_Id(sp.getId());
            String imgUrl = null;
            if (imgs != null && !imgs.isEmpty()) {
                // Ưu tiên 1: Ảnh chung của sản phẩm (không phải biến thể hay màu riêng)
                for (HinhAnh img : imgs) {
                    String ten = img.getTenAnh();
                    if (ten != null && !ten.startsWith("COLOR_") && !ten.startsWith("CTSP_")
                            && img.getDuongDan() != null && !img.getDuongDan().isBlank()) {
                        imgUrl = img.getDuongDan().trim();
                        break;
                    }
                }
                // Ưu tiên 2: Ảnh đầu tiên bất kỳ
                if (imgUrl == null) {
                    for (HinhAnh img : imgs) {
                        if (img.getDuongDan() != null && !img.getDuongDan().isBlank()) {
                            imgUrl = img.getDuongDan().trim();
                            break;
                        }
                    }
                }
            }

            // Đồng bộ fallback theo bộ ảnh áo phông chuẩn trong hệ thống nếu chưa có ảnh
            if (imgUrl == null || imgUrl.isBlank() || imgUrl.contains("photo-1521572267360-ee0c2909d518")) {
                Long sid = sp.getId();
                String scode = sp.getMaSanPham() != null ? sp.getMaSanPham().toUpperCase() : "";
                if (Long.valueOf(1).equals(sid) || scode.contains("SP001") || scode.contains("SP01")) {
                    imgUrl = "/images/products/sp01_front.jpg";
                } else if (Long.valueOf(2).equals(sid) || scode.contains("SP002") || scode.contains("SP02")) {
                    imgUrl = "/images/products/sp02_front.jpg";
                } else if (Long.valueOf(3).equals(sid) || scode.contains("SP003") || scode.contains("SP03")) {
                    imgUrl = "/images/products/sp03_front.jpg";
                } else if (Long.valueOf(4).equals(sid) || scode.contains("SP004") || scode.contains("SP04")) {
                    imgUrl = "/images/products/sp04_front.jpg";
                } else {
                    imgUrl = DEFAULT_PRODUCT_IMAGE;
                }
            }

            stats.add(new ProductStatDTO(
                    sp.getId(),
                    sp.getMaSanPham() != null ? sp.getMaSanPham() : "SP" + sp.getId(),
                    sp.getTenSanPham(),
                    sp.getIdThuongHieu() != null ? sp.getIdThuongHieu().getTenThuongHieu() : "FF-Tshirt",
                    sizes,
                    saleData[0],
                    0L, // Số lượng trả
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

    // =========================================================================
    // PHẦN 2: SO SÁNH & TĂNG TRƯỞNG DOANH THU (COMPARISON)
    // =========================================================================

    private static class PeriodDefinition {
        String key;
        String name;
        LocalDate start;
        LocalDate end;

        PeriodDefinition(String key, String name, LocalDate start, LocalDate end) {
            this.key = key;
            this.name = name;
            this.start = start;
            this.end = end;
        }
    }

    @Override
    public ThongKeSoSanhDTO getComparison(
            String compareMode,
            Integer year,
            Integer startYear,
            Integer endYear,
            Long brandId,
            String productKeyword,
            String fromDateA,
            String toDateA,
            String fromDateB,
            String toDateB,
            String periodsJson) {

        if (compareMode == null || compareMode.isBlank()) {
            compareMode = "month";
        }
        int curYear = LocalDate.now().getYear();
        if (year == null || year <= 2000) year = curYear;
        if (startYear == null || startYear <= 2000) startYear = Math.max(2023, curYear - 3);
        if (endYear == null || endYear <= 2000) endYear = curYear;
        if (startYear > endYear) {
            int tmp = startYear;
            startYear = endYear;
            endYear = tmp;
        }

        String brandName = "Tất cả thương hiệu";
        if (brandId != null) {
            brandName = thuongHieuRepository.findById(brandId)
                    .map(ThuongHieu::getTenThuongHieu)
                    .orElse("Thương hiệu #" + brandId);
        }

        ZoneId zone = ZoneId.systemDefault();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // 1. Tạo danh sách các kỳ cần so sánh (hỗ trợ danh sách động từ Frontend qua periodsJson)
        List<PeriodDefinition> defs = new ArrayList<>();

        if (periodsJson != null && !periodsJson.isBlank() && !periodsJson.trim().equals("[]") && !periodsJson.trim().equals("null")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<PeriodRequestDTO> reqPeriods = mapper.readValue(periodsJson, new TypeReference<List<PeriodRequestDTO>>() {});
                if (reqPeriods != null && !reqPeriods.isEmpty()) {
                    int pIdx = 1;
                    for (PeriodRequestDTO p : reqPeriods) {
                        LocalDate s = parseLocalDateSafe(p.getStartDate());
                        LocalDate e = parseLocalDateSafe(p.getEndDate());
                        if (s != null && e != null) {
                            String k = (p.getKey() != null && !p.getKey().isBlank()) ? p.getKey() : ("P" + pIdx);
                            String n = (p.getName() != null && !p.getName().isBlank()) ? p.getName() : ("Kỳ " + pIdx);
                            defs.add(new PeriodDefinition(k, n, s, e));
                            pIdx++;
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Lỗi phân tích periodsJson, chuyển sang cơ chế mặc định: {}", ex.getMessage());
            }
        }

        if (defs.isEmpty()) {
            if ("month".equalsIgnoreCase(compareMode)) {
                for (int m = 1; m <= 12; m++) {
                    LocalDate s = LocalDate.of(year, m, 1);
                    LocalDate e = s.with(TemporalAdjusters.lastDayOfMonth());
                    String k = "T" + (m < 10 ? "0" + m : m);
                    String n = "Tháng " + m;
                    defs.add(new PeriodDefinition(k, n, s, e));
                }
            } else if ("quarter".equalsIgnoreCase(compareMode)) {
                defs.add(new PeriodDefinition("Q1", "Quý 1", LocalDate.of(year, 1, 1), LocalDate.of(year, 3, 31)));
                defs.add(new PeriodDefinition("Q2", "Quý 2", LocalDate.of(year, 4, 1), LocalDate.of(year, 6, 30)));
                defs.add(new PeriodDefinition("Q3", "Quý 3", LocalDate.of(year, 7, 1), LocalDate.of(year, 9, 30)));
                defs.add(new PeriodDefinition("Q4", "Quý 4", LocalDate.of(year, 10, 1), LocalDate.of(year, 12, 31)));
            } else if ("year".equalsIgnoreCase(compareMode)) {
                for (int y = startYear; y <= endYear; y++) {
                    defs.add(new PeriodDefinition(String.valueOf(y), "Năm " + y, LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31)));
                }
            } else {
                // Tùy chọn 2 kỳ
                LocalDate sA = parseLocalDateSafe(fromDateA);
                LocalDate eA = parseLocalDateSafe(toDateA);
                LocalDate sB = parseLocalDateSafe(fromDateB);
                LocalDate eB = parseLocalDateSafe(toDateB);
                if (sA == null) sA = LocalDate.now().withDayOfMonth(1);
                if (eA == null) eA = LocalDate.now();
                if (sB == null) sB = sA.minusMonths(1);
                if (eB == null) eB = eA.minusMonths(1);

                defs.add(new PeriodDefinition("KyA", "Kỳ A (Kỳ trước)", sA, eA));
                defs.add(new PeriodDefinition("KyB", "Kỳ B (Kỳ này)", sB, eB));
            }
        }

        // 2. Tính toán số liệu cho từng kỳ
        List<PeriodItemDTO> periodList = new ArrayList<>();
        BigDecimal prevRevenue = BigDecimal.ZERO;

        // Lưu thông tin bán hàng theo từng sản phẩm qua các kỳ: Map<ProductId, Map<PeriodKey, long[]{sold, revenue}>>
        Map<Long, Map<String, long[]>> productSalesByPeriod = new HashMap<>();

        for (PeriodDefinition def : defs) {
            Instant fromInstant = def.start.atStartOfDay(zone).toInstant();
            Instant toInstant = def.end.atTime(LocalTime.MAX).atZone(zone).toInstant();

            List<HoaDon> invoices = hoaDonRepository.findByNgayTaoBetween(fromInstant, toInstant).stream()
                    .filter(h -> (Integer.valueOf(5).equals(h.getTrangThai()) || Integer.valueOf(1).equals(h.getTrangThaiThanhToan()))
                            && !Integer.valueOf(6).equals(h.getTrangThai()))
                    .toList();

            List<Long> invIds = invoices.stream().map(HoaDon::getId).toList();
            List<ChiTietHoaDon> details = invIds.isEmpty() ? Collections.emptyList()
                    : chiTietHoaDonRepository.findByHoaDonIds(invIds);
            List<HinhThucThanhToan> payments = invIds.isEmpty() ? Collections.emptyList()
                    : hinhThucThanhToanRepository.findByHoaDonIds(invIds);

            Map<Long, List<HinhThucThanhToan>> payMap = payments.stream()
                    .filter(p -> p.getIdHoaDon() != null)
                    .collect(Collectors.groupingBy(p -> p.getIdHoaDon().getId()));

            List<ChiTietHoaDon> filteredDetails = filterDetails(details, brandId, productKeyword);
            KpiDTO kpi = calculateKpis(invoices, filteredDetails, payMap, brandId, productKeyword);

            BigDecimal currRevenue = kpi.getTotalRevenue();
            BigDecimal diff = currRevenue.subtract(prevRevenue);
            double growth = calculateGrowthRate(currRevenue, prevRevenue);

            String rangeText = def.start.format(df) + " - " + def.end.format(df);

            PeriodItemDTO item = new PeriodItemDTO(
                    def.key,
                    def.name,
                    rangeText,
                    def.start,
                    def.end,
                    currRevenue,
                    kpi.getCashRevenue(),
                    kpi.getTransferRevenue(),
                    kpi.getTotalOrders(),
                    kpi.getAov(),
                    kpi.getTotalItemsSold(),
                    diff,
                    growth,
                    false,
                    false
            );
            periodList.add(item);
            prevRevenue = currRevenue;

            // Thu thập số liệu sản phẩm của kỳ này
            for (ChiTietHoaDon d : filteredDetails) {
                if (d.getIdChiTietSanPham() == null || d.getIdChiTietSanPham().getIdSanPham() == null) continue;
                Long spId = d.getIdChiTietSanPham().getIdSanPham().getId();
                Map<String, long[]> pMap = productSalesByPeriod.computeIfAbsent(spId, k -> new HashMap<>());
                long[] pVal = pMap.computeIfAbsent(def.key, k -> new long[]{0, 0});
                pVal[0] += (d.getSoLuong() != null ? d.getSoLuong() : 0);
                if (d.getThanhTien() != null) {
                    pVal[1] += d.getThanhTien().longValue();
                }
            }
        }

        // Đánh dấu kỳ có doanh thu cao nhất & thấp nhất
        BigDecimal maxRev = BigDecimal.valueOf(-1);
        BigDecimal minRev = null;
        for (PeriodItemDTO p : periodList) {
            if (p.getTotalRevenue().compareTo(maxRev) > 0) {
                maxRev = p.getTotalRevenue();
            }
            if (p.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
                if (minRev == null || p.getTotalRevenue().compareTo(minRev) < 0) {
                    minRev = p.getTotalRevenue();
                }
            }
        }
        for (PeriodItemDTO p : periodList) {
            if (maxRev.compareTo(BigDecimal.ZERO) > 0 && p.getTotalRevenue().compareTo(maxRev) == 0) {
                p.setHighest(true);
            }
            if (minRev != null && p.getTotalRevenue().compareTo(minRev) == 0) {
                p.setLowest(true);
            }
        }

        // 3. Tính Tổng quan Summary KPI cho toàn bộ các kỳ
        BigDecimal sumTotalRevenue = BigDecimal.ZERO;
        BigDecimal sumCash = BigDecimal.ZERO;
        BigDecimal sumTransfer = BigDecimal.ZERO;
        long sumOrders = 0;
        long sumItems = 0;
        double sumGrowth = 0.0;
        int growthCount = 0;

        for (int i = 0; i < periodList.size(); i++) {
            PeriodItemDTO p = periodList.get(i);
            sumTotalRevenue = sumTotalRevenue.add(p.getTotalRevenue());
            sumCash = sumCash.add(p.getCashRevenue());
            sumTransfer = sumTransfer.add(p.getTransferRevenue());
            sumOrders += p.getOrderCount();
            sumItems += p.getItemsSold();
            if (i > 0) {
                sumGrowth += p.getGrowthRate();
                growthCount++;
            }
        }
        BigDecimal overallAov = sumOrders > 0
                ? sumTotalRevenue.divide(BigDecimal.valueOf(sumOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        double avgGrowth = growthCount > 0 ? Math.round((sumGrowth / growthCount) * 10.0) / 10.0 : 0.0;

        SummaryKpiDTO summaryKpi = new SummaryKpiDTO(
                sumTotalRevenue, sumCash, sumTransfer, sumOrders, overallAov, sumItems, avgGrowth
        );

        // 4. Lấy danh sách Top sản phẩm đối chiếu (Top 10)
        List<SanPham> allProducts = sanPhamRepository.findAll();
        Map<Long, SanPham> productEntityMap = allProducts.stream()
                .collect(Collectors.toMap(SanPham::getId, sp -> sp, (a, b) -> a));

        List<TopProductComparisonDTO> topProducts = new ArrayList<>();
        for (Map.Entry<Long, Map<String, long[]>> entry : productSalesByPeriod.entrySet()) {
            Long spId = entry.getKey();
            SanPham sp = productEntityMap.get(spId);
            if (sp == null) continue;

            Map<String, long[]> pSales = entry.getValue();
            long totalSold = 0;
            BigDecimal totalRev = BigDecimal.ZERO;
            Map<String, Long> periodSoldMap = new HashMap<>();
            Map<String, BigDecimal> periodRevenueMap = new HashMap<>();

            for (PeriodDefinition def : defs) {
                long[] s = pSales.getOrDefault(def.key, new long[]{0, 0});
                totalSold += s[0];
                totalRev = totalRev.add(BigDecimal.valueOf(s[1]));
                periodSoldMap.put(def.key, s[0]);
                periodRevenueMap.put(def.key, BigDecimal.valueOf(s[1]));
            }

            // Tính xu hướng: so sánh 2 kỳ gần nhất
            Double trendRate = 0.0;
            String trend = "flat";
            if (defs.size() >= 2) {
                String lastKey = defs.get(defs.size() - 1).key;
                String prevKey = defs.get(defs.size() - 2).key;
                long lastSold = periodSoldMap.getOrDefault(lastKey, 0L);
                long prevSold = periodSoldMap.getOrDefault(prevKey, 0L);
                trendRate = calculateGrowthRate(BigDecimal.valueOf(lastSold), BigDecimal.valueOf(prevSold));
                if (trendRate > 0) trend = "up";
                else if (trendRate < 0) trend = "down";
            }

            List<HinhAnh> imgs = hinhAnhRepository.findByIdSanPham_Id(sp.getId());
            String imgUrl = (imgs != null && !imgs.isEmpty() && imgs.get(0).getDuongDan() != null)
                    ? imgs.get(0).getDuongDan() : DEFAULT_PRODUCT_IMAGE;

            String brand = sp.getIdThuongHieu() != null ? sp.getIdThuongHieu().getTenThuongHieu() : "FF-Tshirt";

            topProducts.add(new TopProductComparisonDTO(
                    sp.getId(),
                    sp.getMaSanPham() != null ? sp.getMaSanPham() : "SP" + sp.getId(),
                    sp.getTenSanPham(),
                    brand,
                    imgUrl,
                    totalSold,
                    totalRev,
                    periodSoldMap,
                    periodRevenueMap,
                    trendRate,
                    trend
            ));
        }

        // Sắp xếp theo tổng số lượng bán giảm dần, lấy tối đa 10 sản phẩm
        topProducts.sort((a, b) -> Long.compare(b.getTotalSold(), a.getTotalSold()));
        if (topProducts.size() > 10) {
            topProducts = topProducts.subList(0, 10);
        }

        return new ThongKeSoSanhDTO(
                compareMode,
                year,
                startYear,
                endYear,
                brandName,
                brandId,
                productKeyword,
                summaryKpi,
                periodList,
                topProducts
        );
    }

    @Override
    public byte[] exportComparisonExcel(
            String compareMode,
            Integer year,
            Integer startYear,
            Integer endYear,
            Long brandId,
            String productKeyword,
            String fromDateA,
            String toDateA,
            String fromDateB,
            String toDateB,
            String periodsJson) {

        ThongKeSoSanhDTO data = getComparison(compareMode, year, startYear, endYear, brandId, productKeyword, fromDateA, toDateA, fromDateB, toDateB, periodsJson);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Sheet 1: Đối chiếu doanh thu
            Sheet sheet1 = workbook.createSheet("SoSanhDoanhThu");
            int r = 0;
            Row titleRow = sheet1.createRow(r++);
            titleRow.createCell(0).setCellValue("BÁO CÁO SO SÁNH & TĂNG TRƯỞNG DOANH THU");

            Row filterRow = sheet1.createRow(r++);
            String modeText = "Theo Tháng (" + data.getTargetYear() + ")";
            if ("quarter".equalsIgnoreCase(data.getCompareMode())) modeText = "Theo Quý (" + data.getTargetYear() + ")";
            else if ("year".equalsIgnoreCase(data.getCompareMode())) modeText = "Theo Năm (" + data.getStartYear() + " - " + data.getEndYear() + ")";
            else if ("custom".equalsIgnoreCase(data.getCompareMode())) modeText = "Tùy chọn 2 kỳ";
            filterRow.createCell(0).setCellValue("Chế độ: " + modeText + " | Thương hiệu: " + data.getBrandName());

            r++;
            Row pHeader = sheet1.createRow(r++);
            String[] colNames = {"KỲ SO SÁNH", "KHOẢNG THỜI GIAN", "TỔNG DOANH THU", "TIỀN MẶT", "CHUYỂN KHOẢN", "SỐ ĐƠN", "AOV", "SẢN PHẨM BÁN", "CHÊNH LỆCH (+/- đ)", "TĂNG TRƯỞNG (%)"};
            for (int i = 0; i < colNames.length; i++) {
                Cell c = pHeader.createCell(i);
                c.setCellValue(colNames[i]);
                c.setCellStyle(headerStyle);
            }

            for (PeriodItemDTO p : data.getPeriods()) {
                Row row = sheet1.createRow(r++);
                row.createCell(0).setCellValue(p.getPeriodName());
                row.createCell(1).setCellValue(p.getDateRangeText());
                row.createCell(2).setCellValue(p.getTotalRevenue().doubleValue());
                row.createCell(3).setCellValue(p.getCashRevenue().doubleValue());
                row.createCell(4).setCellValue(p.getTransferRevenue().doubleValue());
                row.createCell(5).setCellValue(p.getOrderCount());
                row.createCell(6).setCellValue(p.getAov().doubleValue());
                row.createCell(7).setCellValue(p.getItemsSold());
                row.createCell(8).setCellValue(p.getDiffAmount().doubleValue());
                row.createCell(9).setCellValue(p.getGrowthRate() + "%");
            }

            // Dòng tổng cộng
            r++;
            Row sumRow = sheet1.createRow(r++);
            sumRow.createCell(0).setCellValue("TỔNG CỘNG");
            sumRow.createCell(2).setCellValue(data.getSummary().getTotalRevenue().doubleValue());
            sumRow.createCell(3).setCellValue(data.getSummary().getCashRevenue().doubleValue());
            sumRow.createCell(4).setCellValue(data.getSummary().getTransferRevenue().doubleValue());
            sumRow.createCell(5).setCellValue(data.getSummary().getTotalOrders());
            sumRow.createCell(6).setCellValue(data.getSummary().getAov().doubleValue());
            sumRow.createCell(7).setCellValue(data.getSummary().getTotalItemsSold());
            sumRow.createCell(9).setCellValue(data.getSummary().getAvgGrowthRate() + "% (TB)");

            // Sheet 2: Top sản phẩm bán chạy
            Sheet sheet2 = workbook.createSheet("TopSanPhamBanChay");
            Row spHeader = sheet2.createRow(0);
            List<String> spCols = new ArrayList<>(List.of("STT", "Mã sản phẩm", "Tên sản phẩm", "Thương hiệu", "Tổng đã bán", "Tổng doanh thu (đ)", "Tăng trưởng gần nhất (%)"));
            for (PeriodItemDTO period : data.getPeriods()) {
                spCols.add(period.getPeriodName() + " (SL)");
            }
            for (int i = 0; i < spCols.size(); i++) {
                Cell c = spHeader.createCell(i);
                c.setCellValue(spCols.get(i));
                c.setCellStyle(headerStyle);
            }

            int pRow = 1;
            for (TopProductComparisonDTO sp : data.getTopProducts()) {
                Row row = sheet2.createRow(pRow);
                row.createCell(0).setCellValue(pRow);
                row.createCell(1).setCellValue(sp.getProductCode());
                row.createCell(2).setCellValue(sp.getProductName());
                row.createCell(3).setCellValue(sp.getBrandName());
                row.createCell(4).setCellValue(sp.getTotalSold());
                row.createCell(5).setCellValue(sp.getTotalRevenue().doubleValue());
                row.createCell(6).setCellValue(sp.getGrowthTrend() + "%");

                int colIdx = 7;
                for (PeriodItemDTO period : data.getPeriods()) {
                    long sold = sp.getPeriodSoldMap().getOrDefault(period.getPeriodKey(), 0L);
                    row.createCell(colIdx++).setCellValue(sold);
                }
                pRow++;
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất Excel so sánh doanh thu: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // PHẦN 3: TỰ ĐỘNG GỬI EMAIL TỔNG KẾT & SO SÁNH DOANH THU (@Scheduled)
    // =========================================================================

    /**
     * Tự động chạy vào lúc 23:59:59 hàng ngày để tổng hợp doanh thu trong ngày
     * và gửi email báo cáo trực tiếp về hộp thư của Admin mà không cần bấm nút thủ công.
     */
    @Scheduled(cron = "59 59 23 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void autoSendDailyReport() {
        LocalDate today = LocalDate.now();
        log.info("==> [Scheduled] Kích hoạt tự động lúc 23:59:59 hàng ngày ({}). Bắt đầu tổng hợp và gửi email báo cáo doanh thu về Admin...", today);
        try {
            sendReportEmail("day", today.getYear(), today.getMonthValue(), null);
        } catch (Exception e) {
            log.error("==> [Scheduled] Lỗi gửi email tổng kết doanh thu hàng ngày: {}", e.getMessage(), e);
        }
    }

    /**
     * Tự động chạy vào 23:59 các ngày từ 28-31 hàng tháng.
     * Kiểm tra nếu ngày mai là ngày 1 (tức hôm nay là đêm cuối tháng) -> Gửi mail tổng kết tháng!
     */
    @Scheduled(cron = "0 59 23 28-31 * ?", zone = "Asia/Ho_Chi_Minh")
    public void autoSendMonthlyReport() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        if (tomorrow.getDayOfMonth() == 1) {
            log.info("==> [Scheduled] Hôm nay ({}) là ngày cuối tháng. Bắt đầu gửi email báo cáo tổng kết tháng {}/{}...",
                    today, today.getMonthValue(), today.getYear());
            try {
                sendReportEmail("month", today.getYear(), today.getMonthValue(), null);
            } catch (Exception e) {
                log.error("==> [Scheduled] Lỗi gửi email tổng kết tháng: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Tự động chạy vào 23:59 ngày 31/12 hàng năm -> Gửi mail tổng kết năm!
     */
    @Scheduled(cron = "0 59 23 31 12 ?", zone = "Asia/Ho_Chi_Minh")
    public void autoSendYearlyReport() {
        LocalDate today = LocalDate.now();
        log.info("==> [Scheduled] Đêm giao thừa ({})! Bắt đầu gửi email tổng kết doanh thu năm {}...", today, today.getYear());
        try {
            sendReportEmail("year", today.getYear(), null, null);
        } catch (Exception e) {
            log.error("==> [Scheduled] Lỗi gửi email tổng kết năm: {}", e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> sendReportEmail(String reportType, Integer year, Integer month, String recipientEmail) {
        int targetYear = (year != null && year > 2000) ? year : LocalDate.now().getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : LocalDate.now().getMonthValue();
        if (reportType == null || reportType.isBlank()) {
            reportType = "month";
        }

        // Xác định email người nhận
        String recipient = resolveAdminEmail(recipientEmail);

        String subject;
        String htmlContent;

        if ("day".equalsIgnoreCase(reportType) || "daily".equalsIgnoreCase(reportType)) {
            LocalDate today = LocalDate.now();
            subject = String.format("[FF T-SHIRT] Báo cáo Doanh thu Ngày %s (Tự động 23:59:59)", today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            htmlContent = buildDailyReportHtml(today);
        } else if ("year".equalsIgnoreCase(reportType)) {
            subject = "[FF T-SHIRT] Báo cáo Tổng kết & Tăng trưởng Doanh thu Toàn diện Năm " + targetYear;
            htmlContent = buildYearlyReportHtml(targetYear);
        } else {
            subject = String.format("[FF T-SHIRT] Báo cáo Tổng kết & So sánh Doanh thu Tháng %02d/%d", targetMonth, targetYear);
            htmlContent = buildMonthlyReportHtml(targetYear, targetMonth);
        }

        boolean sent = emailService.sendRevenueReportEmail(recipient, subject, htmlContent);

        Map<String, Object> res = new HashMap<>();
        res.put("success", sent);
        res.put("recipient", recipient);
        res.put("reportType", reportType);
        res.put("subject", subject);
        res.put("message", "Đã gửi email báo cáo doanh thu thành công tới: " + recipient);
        return res;
    }

    private String resolveAdminEmail(String customEmail) {
        if (customEmail != null && !customEmail.trim().isEmpty() && customEmail.contains("@")) {
            return customEmail.trim();
        }

        try {
            List<NhanVien> list = nhanVienRepository.findAll();
            for (NhanVien nv : list) {
                if (nv.getIdVaiTro() != null && nv.getIdVaiTro().getTenVaiTro() != null) {
                    String role = nv.getIdVaiTro().getTenVaiTro().toLowerCase();
                    if ((role.contains("admin") || role.contains("quản lý") || role.contains("chủ"))
                            && nv.getEmail() != null && !nv.getEmail().isBlank()) {
                        return nv.getEmail().trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể truy vấn danh sách Admin từ DB: {}", e.getMessage());
        }

        return defaultAdminEmail != null && !defaultAdminEmail.isBlank() ? defaultAdminEmail : "thuanminh2908200334.1@gmail.com";
    }

    private String buildDailyReportHtml(LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        ThongKeOverviewDTO overview = getOverview(today, today, "day", null, null);
        ThongKeOverviewDTO overviewPrev = getOverview(yesterday, yesterday, "day", null, null);

        BigDecimal currRev = overview.getKpi().getTotalRevenue();
        BigDecimal prevRev = overviewPrev.getKpi().getTotalRevenue();
        BigDecimal diffRev = currRev.subtract(prevRev);
        double growthRate = calculateGrowthRate(currRev, prevRev);

        String growthSign = growthRate > 0 ? "+" : "";
        String growthBadgeStyle = growthRate >= 0 ? "color: #15803d; background: #dcfce7;" : "color: #b91c1c; background: #fee2e2;";
        String diffSign = diffRev.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";

        // Top 5 sản phẩm bán chạy nhất hôm nay
        StringBuilder topProductsHtml = new StringBuilder();
        int spCount = 0;
        for (ProductStatDTO sp : overview.getProducts()) {
            if (spCount >= 5) break;
            topProductsHtml.append(String.format("""
                <tr style="border-bottom: 1px solid #f0eae1;">
                    <td style="padding: 10px 12px; font-weight: 700; color: #4a3427;">#%d</td>
                    <td style="padding: 10px 12px;"><b>%s</b> <span style="font-size: 11px; color: #8c7355;">(%s)</span></td>
                    <td style="padding: 10px 12px; text-align: center; font-weight: 700;">%d</td>
                    <td style="padding: 10px 12px; text-align: right; font-weight: 700; color: #16a34a;">%s</td>
                </tr>
            """, spCount + 1, sp.getName(), sp.getCode(), sp.getSoldCount(), formatMoney(sp.getRevenue())));
            spCount++;
        }
        if (spCount == 0) {
            topProductsHtml.append("""
                <tr><td colspan="4" style="text-align: center; padding: 16px; color: #888;">Hôm nay chưa có đơn hàng hoàn thành phát sinh sản phẩm bán ra.</td></tr>
            """);
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return String.format("""
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 680px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.06); color: #333333; line-height: 1.6;">
                <div style="background: linear-gradient(135deg, #1e3a8a 0%%, #3b82f6 100%%); padding: 30px 24px; text-align: center; color: #ffffff;">
                    <h1 style="margin: 0 0 6px 0; font-size: 21px; font-weight: 700;">BÁO CÁO TỔNG KẾT DOANH THU HÀNG NGÀY</h1>
                    <p style="margin: 0; font-size: 14px; opacity: 0.95;">Ngày %s — Hệ thống quản lý kinh doanh thời trang FF T-Shirt</p>
                    <span style="display: inline-block; margin-top: 8px; font-size: 11.5px; background: rgba(255,255,255,0.2); padding: 3px 10px; border-radius: 12px;">Được gửi tự động lúc 23:59:59</span>
                </div>
                <div style="padding: 26px 24px;">
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 22px;">
                        <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #16a34a; border-radius: 10px; padding: 14px 18px;">
                            <div style="font-size: 12px; color: #76695b; font-weight: 600; text-transform: uppercase;">Doanh thu hôm nay</div>
                            <div style="font-size: 22px; font-weight: 800; color: #16a34a; margin-top: 4px;">%s</div>
                            <div style="margin-top: 6px; font-size: 13px; color: #57534e; line-height: 1.6;">
                                Tiền mặt: <b>%s</b>
                            </div>
                            <div style="margin-top: 4px; font-size: 13px; color: #57534e; line-height: 1.6;">
                                Chuyển khoản: <b>%s</b>
                            </div>
                        </div>
                        <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #3b82f6; border-radius: 10px; padding: 14px 18px;">
                            <div style="font-size: 12px; color: #76695b; font-weight: 600; text-transform: uppercase;">So với hôm qua (%s)</div>
                            <div style="font-size: 19px; font-weight: 800; margin-top: 4px; display: inline-block; padding: 2px 10px; border-radius: 6px; %s">%s%.1f%%</div>
                            <div style="font-size: 12px; color: #64748b; margin-top: 4px;">Chênh lệch: %s%s</div>
                        </div>
                    </div>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 22px; background-color: #faf8f5; border-radius: 8px;">
                        <tr>
                            <td style="padding: 12px 16px; text-align: center; border-right: 1px solid #ece6dc;">
                                <div style="font-size: 11.5px; color: #8c7355; font-weight: 600;">ĐƠN HOÀN THÀNH</div>
                                <div style="font-size: 18px; font-weight: 800; color: #2c3e50; margin-top: 4px;">%d đơn</div>
                            </td>
                            <td style="padding: 12px 16px; text-align: center; border-right: 1px solid #ece6dc;">
                                <div style="font-size: 11.5px; color: #8c7355; font-weight: 600;">GIÁ TRỊ TB (AOV)</div>
                                <div style="font-size: 18px; font-weight: 800; color: #2c3e50; margin-top: 4px;">%s</div>
                            </td>
                            <td style="padding: 12px 16px; text-align: center;">
                                <div style="font-size: 11.5px; color: #8c7355; font-weight: 600;">SẢN PHẨM ĐÃ BÁN</div>
                                <div style="font-size: 18px; font-weight: 800; color: #2c3e50; margin-top: 4px;">%d sp</div>
                            </td>
                        </tr>
                    </table>
                    <div style="font-size: 14px; font-weight: 700; color: #2c3e50; margin-bottom: 10px;">
                        <span>Top 5 sản phẩm bán chạy nhất trong ngày</span>
                    </div>
                    <table style="width: 100%%; border-collapse: collapse; font-size: 13px; margin-bottom: 20px;">
                        <thead>
                            <tr style="background-color: #f5efe6; text-align: left; color: #4a3427; font-size: 11.5px; text-transform: uppercase;">
                                <th style="padding: 8px 12px;">Top</th>
                                <th style="padding: 8px 12px;">Tên sản phẩm</th>
                                <th style="padding: 8px 12px; text-align: center;">Đã bán</th>
                                <th style="padding: 8px 12px; text-align: right;">Doanh thu</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
                <div style="background-color: #faf7f2; border-top: 1px solid #eee8df; padding: 16px 24px; text-align: center; font-size: 12px; color: #8c7355;">
                    <p style="margin: 0;">Báo cáo tự động được tạo và gửi từ Hệ thống Bán hàng FF T-Shirt.</p>
                    <p style="margin: 4px 0 0 0; color: #b5a496; font-size: 11px;">Thời gian chốt số liệu tự động: 23:59:59</p>
                </div>
            </div>
        """,
        today.format(df),
        formatMoney(currRev),
        formatMoney(overview.getKpi().getCashRevenue()),
        formatMoney(overview.getKpi().getTransferRevenue()),
        yesterday.format(df),
        growthBadgeStyle,
        growthSign,
        growthRate,
        diffSign,
        formatMoney(diffRev),
        overview.getKpi().getTotalOrders(),
        formatMoney(overview.getKpi().getAov()),
        overview.getKpi().getTotalItemsSold(),
        topProductsHtml.toString()
        );
    }

    private String buildMonthlyReportHtml(int year, int month) {
        LocalDate startMonth = LocalDate.of(year, month, 1);
        LocalDate endMonth = startMonth.with(TemporalAdjusters.lastDayOfMonth());

        ThongKeOverviewDTO overview = getOverview(startMonth, endMonth, "day", null, null);
        ThongKeSoSanhDTO comparison = getComparison("month", year, null, null, null, null, null, null, null, null);

        // Tìm tháng hiện tại và tháng trước trong bảng so sánh
        String curKey = "T" + (month < 10 ? "0" + month : month);
        int prevMonthNum = month > 1 ? month - 1 : 12;
        int prevYearNum = month > 1 ? year : year - 1;
        String prevKey = "T" + (prevMonthNum < 10 ? "0" + prevMonthNum : prevMonthNum);

        PeriodItemDTO curPeriod = null;
        for (PeriodItemDTO p : comparison.getPeriods()) {
            if (curKey.equals(p.getPeriodKey())) {
                curPeriod = p;
                break;
            }
        }
        if (curPeriod == null) {
            curPeriod = new PeriodItemDTO(curKey, "Tháng " + month, "", startMonth, endMonth,
                    overview.getKpi().getTotalRevenue(), overview.getKpi().getCashRevenue(), overview.getKpi().getTransferRevenue(),
                    overview.getKpi().getTotalOrders(), overview.getKpi().getAov(), overview.getKpi().getTotalItemsSold(),
                    BigDecimal.ZERO, 0.0, false, false);
        }

        // Build Top 5 products HTML
        StringBuilder topProductsHtml = new StringBuilder();
        int spCount = 0;
        for (ProductStatDTO sp : overview.getProducts()) {
            if (spCount >= 5) break;
            topProductsHtml.append(String.format("""
                <tr style="border-bottom: 1px solid #f0eae1;">
                    <td style="padding: 10px 12px; font-weight: 700; color: #4a3427;">#%d</td>
                    <td style="padding: 10px 12px;"><b>%s</b> <span style="font-size: 11px; color: #8c7355;">(%s)</span></td>
                    <td style="padding: 10px 12px; text-align: center; font-weight: 700;">%d</td>
                    <td style="padding: 10px 12px; text-align: right; font-weight: 700; color: #16a34a;">%s</td>
                </tr>
            """, spCount + 1, sp.getName(), sp.getCode(), sp.getSoldCount(), formatMoney(sp.getRevenue())));
            spCount++;
        }
        if (spCount == 0) {
            topProductsHtml.append("""
                <tr><td colspan="4" style="text-align: center; padding: 16px; color: #888;">Chưa có sản phẩm bán ra trong tháng này.</td></tr>
            """);
        }

        // Build All Months Comparison Table HTML
        StringBuilder monthsTableHtml = new StringBuilder();
        for (PeriodItemDTO p : comparison.getPeriods()) {
            boolean isCurrent = curKey.equals(p.getPeriodKey());
            String bg = isCurrent ? "#fef3c7" : (p.isHighest() ? "#ecfdf5" : (p.isLowest() ? "#fff7ed" : "#ffffff"));
            String badge = isCurrent ? "<span style='background:#f59e0b;color:#fff;font-size:10px;padding:2px 6px;border-radius:4px;'>Hiện tại</span>"
                    : (p.isHighest() ? "<span style='background:#10b981;color:#fff;font-size:10px;padding:2px 6px;border-radius:4px;'>Cao nhất</span>"
                    : (p.isLowest() ? "<span style='background:#ef4444;color:#fff;font-size:10px;padding:2px 6px;border-radius:4px;'>Thấp nhất</span>" : ""));

            String growthStyle = p.getGrowthRate() > 0 ? "color: #15803d; font-weight: 700;"
                    : (p.getGrowthRate() < 0 ? "color: #b91c1c; font-weight: 700;" : "color: #64748b;");
            String growthSign = p.getGrowthRate() > 0 ? "+" : "";

            monthsTableHtml.append(String.format("""
                <tr style="background-color: %s; border-bottom: 1px solid #f0eae1;">
                    <td style="padding: 9px 12px; font-weight: 700; color: #2c3e50;">%s %s</td>
                    <td style="padding: 9px 12px; text-align: right; font-weight: 700; color: #1e293b;">%s</td>
                    <td style="padding: 9px 12px; text-align: center;">%d</td>
                    <td style="padding: 9px 12px; text-align: right; color: #64748b;">%s</td>
                    <td style="padding: 9px 12px; text-align: center; %s">%s%.1f%%</td>
                </tr>
            """, bg, p.getPeriodName(), badge, formatMoney(p.getTotalRevenue()), p.getOrderCount(),
                    formatMoney(p.getAov()), growthStyle, growthSign, p.getGrowthRate()));
        }

        String growthSign = curPeriod.getGrowthRate() > 0 ? "+" : "";
        String growthBadgeClass = curPeriod.getGrowthRate() >= 0 ? "color: #15803d; background: #dcfce7;" : "color: #b91c1c; background: #fee2e2;";

        return """
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 680px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.06); color: #333333; line-height: 1.6;">
                <!-- Header Banner -->
                <div style="background: linear-gradient(135deg, #4a3427 0%%, #8c6d58 100%%); padding: 32px 28px; text-align: center; color: #ffffff;">
                    <h1 style="margin: 0 0 8px 0; font-size: 22px; font-weight: 700; letter-spacing: 0.5px;">BÁO CÁO TỔNG KẾT & SO SÁNH DOANH THU</h1>
                    <p style="margin: 0; font-size: 15px; opacity: 0.95;">Tháng %02d Năm %d — Hệ thống thời trang FF T-Shirt</p>
                </div>

                <div style="padding: 28px 26px;">
                    <!-- Thẻ KPI chính -->
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 24px;">
                        <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #16a34a; border-radius: 10px; padding: 14px 18px;">
                            <div style="font-size: 12.5px; color: #76695b; font-weight: 600; text-transform: uppercase;">Tổng doanh thu</div>
                            <div style="font-size: 22px; font-weight: 800; color: #16a34a; margin-top: 4px;">%s</div>
                            <div style="margin-top: 6px; font-size: 13px; color: #57534e; line-height: 1.6;">
                                Tiền mặt: <b>%s</b>
                            </div>
                            <div style="margin-top: 4px; font-size: 13px; color: #57534e; line-height: 1.6;">
                                Chuyển khoản: <b>%s</b>
                            </div>
                        </div>
                        <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #3b82f6; border-radius: 10px; padding: 14px 18px;">
                            <div style="font-size: 12.5px; color: #76695b; font-weight: 600; text-transform: uppercase;">Tăng trưởng so với tháng trước</div>
                            <div style="font-size: 20px; font-weight: 800; margin-top: 4px; display: inline-block; padding: 2px 10px; border-radius: 6px; %s">%s%.1f%%</div>
                            <div style="font-size: 12px; color: #64748b; margin-top: 4px;">Chênh lệch: %s</div>
                        </div>
                    </div>

                    <!-- 4 Chỉ số bổ sung -->
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 26px; background-color: #faf8f5; border-radius: 8px;">
                        <tr>
                            <td style="padding: 12px 16px; text-align: center; border-right: 1px solid #ece6dc;">
                                <div style="font-size: 11.5px; color: #888;">ĐƠN HOÀN THÀNH</div>
                                <div style="font-size: 18px; font-weight: 700; color: #2c3e50;">%d</div>
                            </td>
                            <td style="padding: 12px 16px; text-align: center; border-right: 1px solid #ece6dc;">
                                <div style="font-size: 11.5px; color: #888;">ĐƠN ĐÃ HỦY</div>
                                <div style="font-size: 18px; font-weight: 700; color: #ef4444;">%d</div>
                            </td>
                            <td style="padding: 12px 16px; text-align: center; border-right: 1px solid #ece6dc;">
                                <div style="font-size: 11.5px; color: #888;">AOV (TB ĐƠN)</div>
                                <div style="font-size: 18px; font-weight: 700; color: #2c3e50;">%s</div>
                            </td>
                            <td style="padding: 12px 16px; text-align: center;">
                                <div style="font-size: 11.5px; color: #888;">SẢN PHẨM BÁN</div>
                                <div style="font-size: 18px; font-weight: 700; color: #2c3e50;">%d</div>
                            </td>
                        </tr>
                    </table>

                    <!-- Top 5 sản phẩm bán chạy -->
                    <h3 style="margin: 0 0 12px 0; font-size: 16px; color: #4a3427; border-bottom: 2px solid #ddd3c5; padding-bottom: 6px;">
                        TOP 5 SẢN PHẨM BÁN CHẠY NHẤT THÁNG %02d
                    </h3>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 28px; font-size: 13.5px;">
                        <thead>
                            <tr style="background-color: #ddd3c5; color: #3b2f23;">
                                <th style="padding: 8px 12px; text-align: left; width: 45px;">STT</th>
                                <th style="padding: 8px 12px; text-align: left;">Sản phẩm</th>
                                <th style="padding: 8px 12px; text-align: center; width: 80px;">Đã bán</th>
                                <th style="padding: 8px 12px; text-align: right; width: 130px;">Doanh thu</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>

                    <!-- Bảng tổng hợp & So sánh các tháng trong năm -->
                    <h3 style="margin: 0 0 12px 0; font-size: 16px; color: #4a3427; border-bottom: 2px solid #ddd3c5; padding-bottom: 6px;">
                        BẢNG ĐỐI CHIẾU DOANH THU CÁC THÁNG TRONG NĂM %d
                    </h3>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 20px; font-size: 13px;">
                        <thead>
                            <tr style="background-color: #ddd3c5; color: #3b2f23;">
                                <th style="padding: 8px 12px; text-align: left;">Thời gian</th>
                                <th style="padding: 8px 12px; text-align: right;">Doanh thu</th>
                                <th style="padding: 8px 12px; text-align: center;">Số đơn</th>
                                <th style="padding: 8px 12px; text-align: right;">AOV</th>
                                <th style="padding: 8px 12px; text-align: center;">Tăng trưởng</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>

                <!-- Footer -->
                <div style="background-color: #f7f5ef; border-top: 1px solid #e8e4de; padding: 18px 24px; text-align: center; font-size: 12px; color: #777;">
                    <p style="margin: 0 0 4px 0;">Báo cáo được khởi tạo tự động từ Hệ thống Quản lý Bán hàng & Doanh thu <b>FF T-Shirt</b>.</p>
                    <p style="margin: 0; color: #999;">© %d FF T-Shirt. Mọi quyền được bảo lưu.</p>
                </div>
            </div>
        """.formatted(
                month, year,
                formatMoney(overview.getKpi().getTotalRevenue()),
                formatMoney(overview.getKpi().getCashRevenue()),
                formatMoney(overview.getKpi().getTransferRevenue()),
                growthBadgeClass, growthSign, curPeriod.getGrowthRate(),
                (curPeriod.getDiffAmount().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + formatMoney(curPeriod.getDiffAmount()),
                overview.getKpi().getTotalOrders(),
                overview.getOrderStatusDonut().getCancelled(),
                formatMoney(overview.getKpi().getAov()),
                overview.getKpi().getTotalItemsSold(),
                month,
                topProductsHtml.toString(),
                year,
                monthsTableHtml.toString(),
                year
        );
    }

    private String buildYearlyReportHtml(int year) {
        LocalDate startYear = LocalDate.of(year, 1, 1);
        LocalDate endYear = LocalDate.of(year, 12, 31);

        ThongKeOverviewDTO overview = getOverview(startYear, endYear, "month", null, null);
        ThongKeSoSanhDTO quarterComparison = getComparison("quarter", year, null, null, null, null, null, null, null, null);
        ThongKeSoSanhDTO yearComparison = getComparison("year", null, year - 3, year, null, null, null, null, null, null);

        // Build Quarters HTML
        StringBuilder quartersHtml = new StringBuilder();
        for (PeriodItemDTO q : quarterComparison.getPeriods()) {
            String growthSign = q.getGrowthRate() > 0 ? "+" : "";
            String growthStyle = q.getGrowthRate() > 0 ? "color: #15803d; font-weight: 700;"
                    : (q.getGrowthRate() < 0 ? "color: #b91c1c; font-weight: 700;" : "color: #64748b;");
            quartersHtml.append(String.format("""
                <tr style="border-bottom: 1px solid #f0eae1;">
                    <td style="padding: 10px 12px; font-weight: 700; color: #4a3427;">%s</td>
                    <td style="padding: 10px 12px; text-align: right; font-weight: 700; color: #16a34a;">%s</td>
                    <td style="padding: 10px 12px; text-align: center;">%d</td>
                    <td style="padding: 10px 12px; text-align: right; color: #64748b;">%s</td>
                    <td style="padding: 10px 12px; text-align: center; %s">%s%.1f%%</td>
                </tr>
            """, q.getPeriodName(), formatMoney(q.getTotalRevenue()), q.getOrderCount(),
                    formatMoney(q.getAov()), growthStyle, growthSign, q.getGrowthRate()));
        }

        // Build Prior Years Comparison HTML
        StringBuilder yearsHtml = new StringBuilder();
        for (PeriodItemDTO yItem : yearComparison.getPeriods()) {
            boolean isCurYear = String.valueOf(year).equals(yItem.getPeriodKey());
            String bg = isCurYear ? "#fef3c7" : "#ffffff";
            String badge = isCurYear ? "<span style='background:#f59e0b;color:#fff;font-size:10px;padding:2px 6px;border-radius:4px;'>Năm nay</span>" : "";
            String growthSign = yItem.getGrowthRate() > 0 ? "+" : "";
            String growthStyle = yItem.getGrowthRate() > 0 ? "color: #15803d; font-weight: 700;"
                    : (yItem.getGrowthRate() < 0 ? "color: #b91c1c; font-weight: 700;" : "color: #64748b;");

            yearsHtml.append(String.format("""
                <tr style="background-color: %s; border-bottom: 1px solid #f0eae1;">
                    <td style="padding: 10px 12px; font-weight: 700; color: #2c3e50;">%s %s</td>
                    <td style="padding: 10px 12px; text-align: right; font-weight: 700; color: #1e293b;">%s</td>
                    <td style="padding: 10px 12px; text-align: center;">%d</td>
                    <td style="padding: 10px 12px; text-align: right; color: #64748b;">%s</td>
                    <td style="padding: 10px 12px; text-align: center; %s">%s%.1f%%</td>
                </tr>
            """, bg, yItem.getPeriodName(), badge, formatMoney(yItem.getTotalRevenue()), yItem.getOrderCount(),
                    formatMoney(yItem.getAov()), growthStyle, growthSign, yItem.getGrowthRate()));
        }

        // Top 5 Products in Year
        StringBuilder topProductsHtml = new StringBuilder();
        int spCount = 0;
        for (ProductStatDTO sp : overview.getProducts()) {
            if (spCount >= 5) break;
            topProductsHtml.append(String.format("""
                <tr style="border-bottom: 1px solid #f0eae1;">
                    <td style="padding: 10px 12px; font-weight: 700; color: #4a3427;">#%d</td>
                    <td style="padding: 10px 12px;"><b>%s</b> <span style="font-size: 11px; color: #8c7355;">(%s)</span></td>
                    <td style="padding: 10px 12px; text-align: center; font-weight: 700;">%d</td>
                    <td style="padding: 10px 12px; text-align: right; font-weight: 700; color: #16a34a;">%s</td>
                </tr>
            """, spCount + 1, sp.getName(), sp.getCode(), sp.getSoldCount(), formatMoney(sp.getRevenue())));
            spCount++;
        }

        return """
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 680px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.06); color: #333333; line-height: 1.6;">
                <div style="background: linear-gradient(135deg, #2b1f17 0%%, #7c5c45 100%%); padding: 34px 28px; text-align: center; color: #ffffff;">
                    <h1 style="margin: 0 0 8px 0; font-size: 23px; font-weight: 800; letter-spacing: 0.5px;">BÁO CÁO TỔNG KẾT DOANH THU NĂM %d</h1>
                    <p style="margin: 0; font-size: 15px; opacity: 0.95;">Tổng kết kinh doanh & Tăng trưởng hàng năm — FF T-Shirt</p>
                </div>

                <div style="padding: 28px 26px;">
                    <!-- Thẻ KPI tổng quan năm -->
                    <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 5px solid #16a34a; border-radius: 10px; padding: 18px 22px; margin-bottom: 24px;">
                        <div style="font-size: 13px; color: #76695b; font-weight: 700; text-transform: uppercase;">TỔNG DOANH THU CẢ NĂM %d</div>
                        <div style="font-size: 28px; font-weight: 800; color: #16a34a; margin: 6px 0;">%s</div>
                        <div style="margin-top: 6px; font-size: 13px; color: #57534e; line-height: 1.6;">
                            Tiền mặt: <b>%s</b>
                        </div>
                        <div style="margin-top: 4px; font-size: 13px; color: #57534e; line-height: 1.6;">
                            Chuyển khoản: <b>%s</b>
                        </div>
                    </div>

                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 26px; background-color: #faf8f5; border-radius: 8px;">
                        <tr>
                            <td style="padding: 12px 16px; text-align: center; border-right: 1px solid #ece6dc;">
                                <div style="font-size: 11.5px; color: #888;">TỔNG ĐƠN HOÀN THÀNH</div>
                                <div style="font-size: 19px; font-weight: 700; color: #2c3e50;">%d</div>
                            </td>
                            <td style="padding: 12px 16px; text-align: center; border-right: 1px solid #ece6dc;">
                                <div style="font-size: 11.5px; color: #888;">AOV (TRUNG BÌNH ĐƠN)</div>
                                <div style="font-size: 19px; font-weight: 700; color: #2c3e50;">%s</div>
                            </td>
                            <td style="padding: 12px 16px; text-align: center;">
                                <div style="font-size: 11.5px; color: #888;">TỔNG SẢN PHẨM BÁN</div>
                                <div style="font-size: 19px; font-weight: 700; color: #2c3e50;">%d</div>
                            </td>
                        </tr>
                    </table>

                    <!-- Bảng 4 Quý -->
                    <h3 style="margin: 0 0 12px 0; font-size: 16px; color: #4a3427; border-bottom: 2px solid #ddd3c5; padding-bottom: 6px;">
                        TỔNG KẾT DOANH THU 4 QUÝ NĂM %d
                    </h3>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 28px; font-size: 13.5px;">
                        <thead>
                            <tr style="background-color: #ddd3c5; color: #3b2f23;">
                                <th style="padding: 8px 12px; text-align: left;">Quý</th>
                                <th style="padding: 8px 12px; text-align: right;">Doanh thu</th>
                                <th style="padding: 8px 12px; text-align: center;">Số đơn</th>
                                <th style="padding: 8px 12px; text-align: right;">AOV</th>
                                <th style="padding: 8px 12px; text-align: center;">Tăng trưởng</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>

                    <!-- Top 5 sản phẩm của năm -->
                    <h3 style="margin: 0 0 12px 0; font-size: 16px; color: #4a3427; border-bottom: 2px solid #ddd3c5; padding-bottom: 6px;">
                        TOP 5 SẢN PHẨM BÁN CHẠY NHẤT NĂM %d
                    </h3>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 28px; font-size: 13.5px;">
                        <thead>
                            <tr style="background-color: #ddd3c5; color: #3b2f23;">
                                <th style="padding: 8px 12px; text-align: left; width: 45px;">STT</th>
                                <th style="padding: 8px 12px; text-align: left;">Sản phẩm</th>
                                <th style="padding: 8px 12px; text-align: center; width: 80px;">Đã bán</th>
                                <th style="padding: 8px 12px; text-align: right; width: 130px;">Doanh thu</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>

                    <!-- Đối chiếu giữa các năm -->
                    <h3 style="margin: 0 0 12px 0; font-size: 16px; color: #4a3427; border-bottom: 2px solid #ddd3c5; padding-bottom: 6px;">
                        ĐỐI CHIẾU DOANH THU VỚI CÁC NĂM TRƯỚC
                    </h3>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 20px; font-size: 13.5px;">
                        <thead>
                            <tr style="background-color: #ddd3c5; color: #3b2f23;">
                                <th style="padding: 8px 12px; text-align: left;">Năm</th>
                                <th style="padding: 8px 12px; text-align: right;">Doanh thu</th>
                                <th style="padding: 8px 12px; text-align: center;">Số đơn</th>
                                <th style="padding: 8px 12px; text-align: right;">AOV</th>
                                <th style="padding: 8px 12px; text-align: center;">Tăng trưởng</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>

                <div style="background-color: #f7f5ef; border-top: 1px solid #e8e4de; padding: 18px 24px; text-align: center; font-size: 12px; color: #777;">
                    <p style="margin: 0 0 4px 0;">Báo cáo được khởi tạo tự động từ Hệ thống Quản lý Bán hàng & Doanh thu <b>FF T-Shirt</b>.</p>
                    <p style="margin: 0; color: #999;">© %d FF T-Shirt. Mọi quyền được bảo lưu.</p>
                </div>
            </div>
        """.formatted(
                year, year,
                formatMoney(overview.getKpi().getTotalRevenue()),
                formatMoney(overview.getKpi().getCashRevenue()),
                formatMoney(overview.getKpi().getTransferRevenue()),
                overview.getKpi().getTotalOrders(),
                formatMoney(overview.getKpi().getAov()),
                overview.getKpi().getTotalItemsSold(),
                year,
                quartersHtml.toString(),
                year,
                topProductsHtml.toString(),
                yearsHtml.toString(),
                year
        );
    }

    private static double calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (current == null) current = BigDecimal.ZERO;
        if (previous == null) previous = BigDecimal.ZERO;

        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        double diff = current.subtract(previous).doubleValue();
        double prevVal = previous.doubleValue();
        return Math.round((diff / prevVal) * 1000.0) / 10.0;
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 đ";
        return String.format("%,d đ", amount.longValue()).replace(',', '.');
    }

    private static LocalDate parseLocalDateSafe(String val) {
        if (val == null) return null;
        String s = val.trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s) || "undefined".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            if (s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
                return LocalDate.parse(s.substring(0, 10));
            }
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
