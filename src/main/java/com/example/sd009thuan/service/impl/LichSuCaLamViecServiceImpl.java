package com.example.sd009thuan.service.impl;

import com.example.sd009thuan.dto.DongMoCaRequest;
import com.example.sd009thuan.dto.ShiftCashSummaryDTO;
import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.repository.*;
import com.example.sd009thuan.service.LichSuCaLamViecService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LichSuCaLamViecServiceImpl implements LichSuCaLamViecService {

    private final LichSuCaLamViecRepository repository;
    private final NhanVienRepository nhanVienRepository;
    private final CaLamViecRepository caLamViecRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final HoaDonRepository hoaDonRepository;
    private final HinhThucThanhToanRepository hinhThucThanhToanRepository;

    public LichSuCaLamViecServiceImpl(
            LichSuCaLamViecRepository repository,
            NhanVienRepository nhanVienRepository,
            CaLamViecRepository caLamViecRepository,
            LichLamViecRepository lichLamViecRepository,
            HoaDonRepository hoaDonRepository,
            HinhThucThanhToanRepository hinhThucThanhToanRepository) {
        this.repository = repository;
        this.nhanVienRepository = nhanVienRepository;
        this.caLamViecRepository = caLamViecRepository;
        this.lichLamViecRepository = lichLamViecRepository;
        this.hoaDonRepository = hoaDonRepository;
        this.hinhThucThanhToanRepository = hinhThucThanhToanRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LichSuCaLamViec> searchHistory(String keyword, LocalDate fromDate, LocalDate toDate, Integer trangThai, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Từ ngày không được lớn hơn Đến ngày!");
        }

        LocalDateTime fromDT = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDT = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        Integer dbTrangThai = trangThai;
        if (trangThai != null && trangThai == 2) {
            dbTrangThai = 0;
        }

        return repository.searchHistory(kw, fromDT, toDT, dbTrangThai, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LichSuCaLamViec> getCurrentOpenShift(Long nhanVienId) {
        return repository.findTopByNhanVienIdAndTrangThaiOrderByThoiGianMoCaDesc(nhanVienId, 1);
    }

    @Override
    public LichSuCaLamViec moCa(DongMoCaRequest req) {
        if (req.getNhanVienId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn nhân viên mở ca");
        }
        if (req.getCaLamViecId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn ca làm việc");
        }
        if (req.getTienMatDauCa() == null || req.getTienMatDauCa().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số tiền mặt đầu ca phải là số hợp lệ lớn hơn hoặc bằng 0");
        }

        NhanVien nv = nhanVienRepository.findById(req.getNhanVienId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên"));

        CaLamViec ca = caLamViecRepository.findById(req.getCaLamViecId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca làm việc"));

        // 1. Kiểm tra khung giờ làm việc của ca (chỉ được mở trong khoảng từ trước 30p đến khi kết thúc ca)
        LocalTime now = LocalTime.now();
        LocalTime start = ca.getGioBatDau();
        LocalTime end = ca.getGioKetThuc();
        if (start != null && end != null) {
            if (!isTimeInShift(now, start, end)) {
                LocalTime earliest = start.minusMinutes(30);
                if (start.isBefore(end) && now.isBefore(earliest)) {
                    throw new IllegalArgumentException(String.format(
                            "Hiện tại (%s) chưa đến giờ làm việc của ca '%s' (%s - %s). Bạn chỉ có thể mở ca sớm tối đa 30 phút (từ %s)!",
                            formatTime(now), ca.getTenCa(), formatTime(start), formatTime(end), formatTime(earliest)
                    ));
                } else if (start.isBefore(end) && now.isAfter(end.plusMinutes(30))) {
                    throw new IllegalArgumentException(String.format(
                            "Ca làm việc '%s' (%s - %s) đã kết thúc (%s). Vui lòng chọn ca làm việc tương ứng với thời điểm hiện tại!",
                            ca.getTenCa(), formatTime(start), formatTime(end), formatTime(end)
                    ));
                } else {
                    throw new IllegalArgumentException(String.format(
                            "Thời điểm hiện tại (%s) không nằm trong khung giờ làm việc của ca '%s' (%s - %s)!",
                            formatTime(now), ca.getTenCa(), formatTime(start), formatTime(end)
                    ));
                }
            }
        }

        // 2. Kiểm tra phân công ca của nhân viên trong ngày (nếu không phải Quản trị viên)
        if (!isEmployeeAdmin(nv)) {
            boolean hasSchedule = lichLamViecRepository
                    .findByNhanVienIdAndCaLamViecIdAndNgayLamViec(nv.getId(), ca.getId(), LocalDate.now())
                    .filter(l -> l.getTrangThai() != null && l.getTrangThai() == 1)
                    .isPresent();

            if (!hasSchedule) {
                throw new IllegalArgumentException(String.format(
                        "Nhân viên '%s' không có lịch phân công làm việc vào ca '%s' hôm nay (%s)! Vui lòng kiểm tra lại xếp lịch.",
                        nv.getTenNhanVien(), ca.getTenCa(), LocalDate.now()
                ));
            }
        }

        // 3. Kiểm tra xem nhân viên đã có ca đang mở chưa
        Optional<LichSuCaLamViec> current = repository.findTopByNhanVienIdAndTrangThaiOrderByThoiGianMoCaDesc(nv.getId(), 1);
        if (current.isPresent()) {
            String tenCa = current.get().getCaLamViec() != null ? current.get().getCaLamViec().getTenCa() : "hiện tại";
            throw new IllegalArgumentException("Nhân viên đang có ca '" + tenCa + "' đang mở chưa đóng. Vui lòng đóng ca trước khi mở ca mới!");
        }

        LichSuCaLamViec ls = new LichSuCaLamViec();
        ls.setNhanVien(nv);
        ls.setCaLamViec(ca);

        // Nối vào lịch làm việc trong ngày (nếu có hoặc tạo mới để liên kết ca)
        LichLamViec llv = lichLamViecRepository
                .findByNhanVienIdAndCaLamViecIdAndNgayLamViec(nv.getId(), ca.getId(), LocalDate.now())
                .map(existing -> {
                    if (existing.getTrangThai() == null || existing.getTrangThai() != 1) {
                        existing.setTrangThai(1);
                        return lichLamViecRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    LichLamViec newLlv = new LichLamViec();
                    newLlv.setNhanVien(nv);
                    newLlv.setCaLamViec(ca);
                    newLlv.setNgayLamViec(LocalDate.now());
                    newLlv.setTrangThai(1);
                    return lichLamViecRepository.save(newLlv);
                });
        ls.setLichLamViec(llv);
        ls.setMaGiaoCa("GC" + System.currentTimeMillis());
        ls.setThoiGianMoCa(LocalDateTime.now());
        ls.setTienMatDauCa(req.getTienMatDauCa());
        ls.setTienMatCuoiCa(BigDecimal.ZERO);
        ls.setTienChuyenKhoan(BigDecimal.ZERO);
        ls.setTongTienTrongCa(BigDecimal.ZERO);
        ls.setTienChiTrongCa(BigDecimal.ZERO);
        ls.setChenhLech(BigDecimal.ZERO);
        ls.setTrangThai(1); // 1 = Đang mở (trong ca)
        ls.setGhiChu(req.getGhiChu());

        return repository.save(ls);
    }

    @Override
    public LichSuCaLamViec dongCa(DongMoCaRequest req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("Thiếu ID ca làm việc cần đóng");
        }
        LichSuCaLamViec ls = repository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin ca cần đóng với ID: " + req.getId()));

        if (ls.getTrangThai() != null && ls.getTrangThai() == 0) {
            throw new IllegalArgumentException("Ca làm việc này đã được đóng trước đó!");
        }

        if (req.getTienMatCuoiCa() == null || req.getTienMatCuoiCa().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số tiền mặt cuối ca phải là số hợp lệ lớn hơn hoặc bằng 0");
        }

        BigDecimal tienChi = req.getTienChiTrongCa() != null && req.getTienChiTrongCa().compareTo(BigDecimal.ZERO) >= 0
                ? req.getTienChiTrongCa() : BigDecimal.ZERO;
        ls.setTienChiTrongCa(tienChi);

        if (req.getNhanVienNhanId() != null) {
            nhanVienRepository.findById(req.getNhanVienNhanId()).ifPresent(ls::setNhanVienNhan);
        }

        ShiftCashSummaryDTO summary = computeSummary(ls);
        ls.setThoiGianDongCa(LocalDateTime.now());
        ls.setTienMatCuoiCa(req.getTienMatCuoiCa());
        ls.setTienChuyenKhoan(summary.getTienChuyenKhoanHeThong());
        ls.setTongTienTrongCa(summary.getTienMatHeThong());

        BigDecimal tienMatDuKien = summary.getTienMatDuKien();
        BigDecimal cuoiCa = req.getTienMatCuoiCa();

        // Chênh lệch = Thực tế cuối ca - Dự kiến (Đầu ca + Tiền mặt hệ thống bán trong ca)
        BigDecimal chenhLech = cuoiCa.subtract(tienMatDuKien);
        if (req.getChenhLech() != null) {
            chenhLech = req.getChenhLech();
        }
        ls.setChenhLech(chenhLech);

        // Bắt buộc nhập lý do nếu phát sinh chênh lệch (khác 0)
        if (chenhLech.compareTo(BigDecimal.ZERO) != 0) {
            if (req.getLyDoChenhLech() == null || req.getLyDoChenhLech().trim().isEmpty()) {
                throw new IllegalArgumentException(String.format(
                        "Phát hiện chênh lệch tiền mặt %s đ so với dự kiến. Bắt buộc phải nhập lý do giải trình để xác nhận đóng ca!",
                        chenhLech.toPlainString()
                ));
            }
            ls.setLyDoChenhLech(req.getLyDoChenhLech().trim());
        } else {
            ls.setLyDoChenhLech(req.getLyDoChenhLech());
        }

        // Lưu lý do chênh lệch vào ghi_chu nếu có (vì DB bảng giao_ca không có cột ly_do_chenh_lech)
        String combinedGhiChu = "";
        if (req.getLyDoChenhLech() != null && !req.getLyDoChenhLech().trim().isEmpty()) {
            combinedGhiChu = "Lý do chênh lệch: " + req.getLyDoChenhLech().trim();
        }
        if (req.getGhiChu() != null && !req.getGhiChu().trim().isEmpty()) {
            combinedGhiChu += (combinedGhiChu.isEmpty() ? "" : " | ") + req.getGhiChu().trim();
        }
        if (!combinedGhiChu.isEmpty()) {
            ls.setGhiChu(combinedGhiChu);
        } else if (req.getGhiChu() != null) {
            ls.setGhiChu(req.getGhiChu().trim());
        }

        ls.setTrangThai(0); // 0 = Đã đóng (Đã kết thúc / giao ca)

        return repository.save(ls);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LichSuCaLamViec> getById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LichSuCaLamViec> getLastClosedShift() {
        return repository.findTopByTrangThaiOrderByThoiGianDongCaDesc(0);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftCashSummaryDTO getShiftSummary(Long id) {
        LichSuCaLamViec ls = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin ca làm việc với ID: " + id));
        return computeSummary(ls);
    }

    private ShiftCashSummaryDTO computeSummary(LichSuCaLamViec ls) {
        ShiftCashSummaryDTO dto = new ShiftCashSummaryDTO();
        dto.setId(ls.getId());
        if (ls.getNhanVien() != null) {
            dto.setNhanVienId(ls.getNhanVien().getId());
            dto.setTenNhanVien(ls.getNhanVien().getTenNhanVien());
        }
        if (ls.getCaLamViec() != null) {
            dto.setCaLamViecId(ls.getCaLamViec().getId());
            dto.setTenCa(ls.getCaLamViec().getTenCa());
        }
        BigDecimal dauCa = ls.getTienMatDauCa() != null ? ls.getTienMatDauCa() : BigDecimal.ZERO;
        dto.setTienMatDauCa(dauCa);
        BigDecimal tienChi = ls.getTienChiTrongCa() != null ? ls.getTienChiTrongCa() : BigDecimal.ZERO;
        dto.setTienChiTrongCa(tienChi);

        ZoneId zone = ZoneId.systemDefault();
        Instant fromInstant = ls.getThoiGianMoCa().atZone(zone).toInstant();
        LocalDateTime endLdt = ls.getThoiGianDongCa() != null ? ls.getThoiGianDongCa() : LocalDateTime.now();
        Instant toInstant = endLdt.atZone(zone).toInstant();

        Long nvId = ls.getNhanVien() != null ? ls.getNhanVien().getId() : null;
        if (nvId == null) {
            dto.setTienMatDuKien(dauCa);
            return dto;
        }

        // 1. Lấy tất cả hóa đơn hoàn thành/đã thanh toán của nhân viên trong ca
        List<HoaDon> invoices = (ls.getId() != null) ? hoaDonRepository.findByIdGiaoCa_Id(ls.getId()) : null;
        if (invoices == null || invoices.isEmpty()) {
            invoices = hoaDonRepository.findByNhanVienAndNgayTaoBetween(nvId, fromInstant, toInstant);
        }
        List<HoaDon> validInvoices = invoices.stream()
                .filter(h -> (Integer.valueOf(5).equals(h.getTrangThai()) || Integer.valueOf(1).equals(h.getTrangThaiThanhToan()))
                        && !Integer.valueOf(6).equals(h.getTrangThai()))
                .toList();
        dto.setSoHoaDon(validInvoices.size());

        // 2. Lấy hình thức thanh toán trong khoảng thời gian này của nhân viên
        List<HinhThucThanhToan> payments = hinhThucThanhToanRepository.findByNhanVienAndThoiGianBetween(nvId, fromInstant, toInstant);

        BigDecimal cashSum = BigDecimal.ZERO;
        BigDecimal transferSum = BigDecimal.ZERO;

        for (HinhThucThanhToan p : payments) {
            if (p.getSoTien() == null || p.getSoTien().compareTo(BigDecimal.ZERO) <= 0) continue;
            String ptName = (p.getIdPhuongThuc() != null && p.getIdPhuongThuc().getTenPhuongThuc() != null)
                    ? p.getIdPhuongThuc().getTenPhuongThuc().toLowerCase() : "";
            if (ptName.contains("tiền mặt") || ptName.contains("tien mat")) {
                cashSum = cashSum.add(p.getSoTien());
            } else {
                transferSum = transferSum.add(p.getSoTien());
            }
        }

        // Nếu không có bản ghi HinhThucThanhToan nhưng có hóa đơn hoàn thành tại quầy, tính vào tiền mặt
        if (payments.isEmpty() && !validInvoices.isEmpty()) {
            for (HoaDon h : validInvoices) {
                BigDecimal amount = h.getTienSauGiamGia() != null && h.getTienSauGiamGia().compareTo(BigDecimal.ZERO) > 0
                        ? h.getTienSauGiamGia() : (h.getTongTien() != null ? h.getTongTien() : BigDecimal.ZERO);
                cashSum = cashSum.add(amount);
            }
        }

        dto.setTienMatHeThong(cashSum);
        dto.setTienChuyenKhoanHeThong(transferSum);
        dto.setTongTienHeThong(cashSum.add(transferSum));
        dto.setTienMatDuKien(dauCa.add(cashSum).subtract(tienChi));

        java.util.List<ShiftCashSummaryDTO.ShiftInvoiceDTO> shiftInvoices = new java.util.ArrayList<>();
        for (HoaDon h : validInvoices) {
            ShiftCashSummaryDTO.ShiftInvoiceDTO item = new ShiftCashSummaryDTO.ShiftInvoiceDTO();
            item.setId(h.getId());
            item.setMaHoaDon(h.getMaHoaDon());
            item.setTenKhachHang(h.getTenKhachHang() != null ? h.getTenKhachHang() : "Khách lẻ");
            item.setSoDienThoaiKhachHang(h.getSoDienThoaiKhachHang());
            BigDecimal amount = h.getTienSauGiamGia() != null && h.getTienSauGiamGia().compareTo(BigDecimal.ZERO) > 0
                    ? h.getTienSauGiamGia() : (h.getTongTien() != null ? h.getTongTien() : BigDecimal.ZERO);
            item.setTongTien(amount);
            item.setNgayTao(h.getNgayTao());
            item.setLoaiDon(h.getLoaiDon());
            item.setTrangThai(h.getTrangThai());
            item.setTrangThaiThanhToan(h.getTrangThaiThanhToan());

            java.util.List<HinhThucThanhToan> matchingPts = payments.stream()
                    .filter(p -> p.getIdHoaDon() != null && h.getId().equals(p.getIdHoaDon().getId()))
                    .toList();
            if (!matchingPts.isEmpty()) {
                String ptNames = matchingPts.stream()
                        .map(p -> p.getIdPhuongThuc() != null ? p.getIdPhuongThuc().getTenPhuongThuc() : "Thanh toán")
                        .distinct()
                        .collect(java.util.stream.Collectors.joining(", "));
                item.setHinhThucThanhToan(ptNames);
            } else {
                item.setHinhThucThanhToan(Integer.valueOf(1).equals(h.getLoaiDon()) ? "Tiền mặt" : "Chuyển khoản");
            }
            shiftInvoices.add(item);
        }
        dto.setDanhSachHoaDon(shiftInvoices);

        return dto;
    }

    private boolean isTimeInShift(LocalTime now, LocalTime start, LocalTime end) {
        if (start == null || end == null) return true;
        LocalTime allowedStart = start.minusMinutes(30);
        LocalTime allowedEnd = end.plusMinutes(30);

        if (start.isBefore(end)) {
            return !now.isBefore(allowedStart) && !now.isAfter(allowedEnd);
        } else {
            // Ca qua đêm (ví dụ: 22:00 - 06:00)
            return !now.isBefore(allowedStart) || !now.isAfter(allowedEnd);
        }
    }

    private String formatTime(LocalTime time) {
        if (time == null) return "--:--";
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    private boolean isEmployeeAdmin(NhanVien x) {
        if (x == null || x.getIdVaiTro() == null) return false;
        String maVaiTro = x.getIdVaiTro().getMaVaiTro();
        String tenVaiTro = x.getIdVaiTro().getTenVaiTro();
        return (maVaiTro != null && "ADMIN".equalsIgnoreCase(maVaiTro.trim()))
                || (tenVaiTro != null && (tenVaiTro.toLowerCase().contains("quản trị") || tenVaiTro.toLowerCase().contains("admin")));
    }
}
