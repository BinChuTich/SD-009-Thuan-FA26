package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.repository.HoaDonRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class HoaDonService {

    private final HoaDonRepository hoaDonRepository;

    public HoaDonService(HoaDonRepository hoaDonRepository) {
        this.hoaDonRepository = hoaDonRepository;
    }

    // =========================
    // GET - Theo mã hóa đơn
    // =========================
    public Optional<HoaDon> getByMaHoaDon(String maHoaDon) {
        return hoaDonRepository.findByMaHoaDon(maHoaDon);
    }

    // =========================
    // GET - Tất cả hóa đơn
    // =========================
    public List<HoaDon> getAll() {
        return hoaDonRepository.findAll();
    }

    // =========================
    // GET - Theo ID
    // =========================
    public Optional<HoaDon> getById(Long id) {
        return hoaDonRepository.findById(id);
    }

    // =========================
    // POST - Thêm hóa đơn
    // =========================
    public HoaDon create(HoaDon hoaDon) {

        // Nếu chưa nhập mã hóa đơn thì tự sinh
        if (hoaDon.getMaHoaDon() == null ||
            hoaDon.getMaHoaDon().trim().isEmpty()) {

            String maHoaDon = "HD" +
                              System.currentTimeMillis();

            hoaDon.setMaHoaDon(maHoaDon);
        }

        // Nếu chưa có ngày tạo thì tự lấy thời gian hiện tại
        if (hoaDon.getNgayTao() == null) {
            hoaDon.setNgayTao(Instant.now());
        }

        // Nếu chưa có trạng thái thì mặc định = 1
        if (hoaDon.getTrangThai() == null) {
            hoaDon.setTrangThai(1);
        }

        // Các số tiền nếu null thì cho = 0
        if (hoaDon.getPhiShip() == null) {
            hoaDon.setPhiShip(java.math.BigDecimal.ZERO);
        }

        if (hoaDon.getTongTien() == null) {
            hoaDon.setTongTien(java.math.BigDecimal.ZERO);
        }

        if (hoaDon.getTongTienGiamGia() == null) {
            hoaDon.setTongTienGiamGia(java.math.BigDecimal.ZERO);
        }

        return hoaDonRepository.save(hoaDon);
    }

    // =========================
    // PUT - Cập nhật hóa đơn
    // =========================
    public HoaDon update(Long id, HoaDon hoaDonMoi) {

        HoaDon hoaDonCu = hoaDonRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy hóa đơn có ID: " + id)
                );

        hoaDonCu.setMaHoaDon(hoaDonMoi.getMaHoaDon());
        hoaDonCu.setLoaiDon(hoaDonMoi.getLoaiDon());
        hoaDonCu.setPhiShip(hoaDonMoi.getPhiShip());
        hoaDonCu.setTongTien(hoaDonMoi.getTongTien());
        hoaDonCu.setTongTienGiamGia(hoaDonMoi.getTongTienGiamGia());
        hoaDonCu.setTenKhachHang(hoaDonMoi.getTenKhachHang());
        hoaDonCu.setSoDienThoaiKhachHang(
                hoaDonMoi.getSoDienThoaiKhachHang()
        );
        hoaDonCu.setDiaChiNhanHang(
                hoaDonMoi.getDiaChiNhanHang()
        );
        hoaDonCu.setNguoiTao(hoaDonMoi.getNguoiTao());
        hoaDonCu.setNgayCapNhat(Instant.now());
        hoaDonCu.setNguoiCapNhat(hoaDonMoi.getNguoiCapNhat());
        hoaDonCu.setTrangThai(hoaDonMoi.getTrangThai());
        hoaDonCu.setGhiChu(hoaDonMoi.getGhiChu());

        return hoaDonRepository.save(hoaDonCu);
    }

    // =========================
    // DELETE - Xóa hóa đơn
    // =========================
    public void delete(Long id) {

        if (!hoaDonRepository.existsById(id)) {
            throw new RuntimeException(
                    "Không tìm thấy hóa đơn có ID: " + id
            );
        }

        hoaDonRepository.deleteById(id);
    }
}