package com.example.sd009thuan.service.impl;

import com.example.sd009thuan.dto.DongMoCaRequest;
import com.example.sd009thuan.entity.CaLamViec;
import com.example.sd009thuan.entity.LichSuCaLamViec;
import com.example.sd009thuan.entity.NhanVien;
import com.example.sd009thuan.repository.CaLamViecRepository;
import com.example.sd009thuan.repository.LichSuCaLamViecRepository;
import com.example.sd009thuan.repository.NhanVienRepository;
import com.example.sd009thuan.service.LichSuCaLamViecService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Service
@Transactional
public class LichSuCaLamViecServiceImpl implements LichSuCaLamViecService {

    private final LichSuCaLamViecRepository repository;
    private final NhanVienRepository nhanVienRepository;
    private final CaLamViecRepository caLamViecRepository;

    public LichSuCaLamViecServiceImpl(
            LichSuCaLamViecRepository repository,
            NhanVienRepository nhanVienRepository,
            CaLamViecRepository caLamViecRepository) {
        this.repository = repository;
        this.nhanVienRepository = nhanVienRepository;
        this.caLamViecRepository = caLamViecRepository;
    }

    @Override
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
        ls.setTongTienTrongCa(req.getTienMatDauCa());
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

        ls.setThoiGianDongCa(LocalDateTime.now());
        ls.setTienMatCuoiCa(req.getTienMatCuoiCa());

        if (req.getTienChuyenKhoan() != null && req.getTienChuyenKhoan().compareTo(BigDecimal.ZERO) >= 0) {
            ls.setTienChuyenKhoan(req.getTienChuyenKhoan());
        }

        BigDecimal dauCa = ls.getTienMatDauCa() != null ? ls.getTienMatDauCa() : BigDecimal.ZERO;
        BigDecimal cuoiCa = req.getTienMatCuoiCa();

        // Tính chênh lệch nếu phía client truyền hoặc tính toán
        BigDecimal chenhLech = req.getChenhLech();
        if (chenhLech == null) {
            // Mặc định chênh lệch so với tiền mặt đầu ca + doanh thu tiền mặt nếu có
            chenhLech = cuoiCa.subtract(dauCa);
        }
        ls.setChenhLech(chenhLech);

        // Bắt buộc nhập lý do nếu phát sinh chênh lệch (khác 0)
        if (chenhLech.compareTo(BigDecimal.ZERO) != 0) {
            if (req.getLyDoChenhLech() == null || req.getLyDoChenhLech().trim().isEmpty()) {
                throw new IllegalArgumentException(String.format(
                        "Phát hiện chênh lệch tiền mặt %s đ khi kết thúc ca. Bắt buộc phải nhập lý do giải trình để xác nhận đóng ca!",
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
}

