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
    private final HoaDonRepository hoaDonRepository;
    private final HinhThucThanhToanRepository hinhThucThanhToanRepository;

    public LichSuCaLamViecServiceImpl(
            LichSuCaLamViecRepository repository,
            NhanVienRepository nhanVienRepository,
            CaLamViecRepository caLamViecRepository,
            HoaDonRepository hoaDonRepository,
            HinhThucThanhToanRepository hinhThucThanhToanRepository) {
        this.repository = repository;
        this.nhanVienRepository = nhanVienRepository;
        this.caLamViecRepository = caLamViecRepository;
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

        return repository.searchHistory(kw, fromDT, toDT, trangThai, pageable);
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

        // Kiểm tra xem nhân viên đã có ca đang mở chưa
        Optional<LichSuCaLamViec> current = repository.findTopByNhanVienIdAndTrangThaiOrderByThoiGianMoCaDesc(nv.getId(), 1);
        if (current.isPresent()) {
            throw new IllegalArgumentException("Nhân viên đang có ca '" + current.get().getCaLamViec().getTenCa() + "' đang mở chưa đóng. Vui lòng đóng ca trước khi mở ca mới!");
        }

        LichSuCaLamViec ls = new LichSuCaLamViec();
        ls.setNhanVien(nv);
        ls.setCaLamViec(ca);
        ls.setThoiGianMoCa(LocalDateTime.now());
        ls.setTienMatDauCa(req.getTienMatDauCa());
        ls.setTienMatCuoiCa(null);
        ls.setTienChuyenKhoan(BigDecimal.ZERO);
        ls.setTongTienTrongCa(BigDecimal.ZERO);
        ls.setChenhLech(BigDecimal.ZERO);
        ls.setTrangThai(1); // 1 = Đang mở
        ls.setGhiChu(req.getGhiChu());
        ls.setNgayTao(LocalDateTime.now());

        return repository.save(ls);
    }

    @Override
    public LichSuCaLamViec dongCa(DongMoCaRequest req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("Thiếu ID ca làm việc cần đóng");
        }
        LichSuCaLamViec ls = repository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin ca cần đóng với ID: " + req.getId()));

        if (ls.getTrangThai() == 2) {
            throw new IllegalArgumentException("Ca làm việc này đã được đóng trước đó!");
        }

        if (req.getTienMatCuoiCa() == null || req.getTienMatCuoiCa().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số tiền mặt cuối ca phải là số hợp lệ lớn hơn hoặc bằng 0");
        }

        ShiftCashSummaryDTO summary = computeSummary(ls);
        ls.setThoiGianDongCa(LocalDateTime.now());
        ls.setTienMatCuoiCa(req.getTienMatCuoiCa());
        ls.setTienChuyenKhoan(summary.getTienChuyenKhoanHeThong());
        ls.setTongTienTrongCa(summary.getTienMatHeThong().add(summary.getTienChuyenKhoanHeThong()));

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

        ls.setTrangThai(2); // 2 = Đã đóng
        if (req.getGhiChu() != null) {
            ls.setGhiChu(req.getGhiChu());
        }

        return repository.save(ls);
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
        List<HoaDon> invoices = hoaDonRepository.findByNhanVienAndNgayTaoBetween(nvId, fromInstant, toInstant);
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
        dto.setTienMatDuKien(dauCa.add(cashSum));

        return dto;
    }
}
