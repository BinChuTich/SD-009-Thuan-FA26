package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.repository.HoaDonRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

        // =========================
        // TỰ SINH MÃ HÓA ĐƠN
        // HD001, HD002, HD003...
        // =========================
        if (hoaDon.getMaHoaDon() == null ||
            hoaDon.getMaHoaDon().trim().isEmpty()) {

            long soLuong = hoaDonRepository.count() + 1;

            String maHoaDon = String.format(
                    "HD%03d",
                    soLuong
            );

            // Nếu mã đã tồn tại thì tăng tiếp
            while (hoaDonRepository.findByMaHoaDon(maHoaDon).isPresent()) {
                soLuong++;

                maHoaDon = String.format(
                        "HD%03d",
                        soLuong
                );
            }

            hoaDon.setMaHoaDon(maHoaDon);
        }

        // =========================
        // NGÀY TẠO
        // =========================
        if (hoaDon.getNgayTao() == null) {
            hoaDon.setNgayTao(Instant.now());
        }

        // TRẠNG THÁI ĐƠ
        if (hoaDon.getTrangThai() == null) {
            hoaDon.setTrangThai(1);
        }

        // =========================
        // PHÍ SHIP
        // =========================
        if (hoaDon.getPhiShip() == null) {
            hoaDon.setPhiShip(BigDecimal.ZERO);
        }

        // =========================
        // TỔNG TIỀN
        // =========================
        if (hoaDon.getTongTien() == null) {
            hoaDon.setTongTien(BigDecimal.ZERO);
        }

        // =========================
        // TIỀN GIẢM GIÁ
        // =========================
        if (hoaDon.getTongTienGiamGia() == null) {
            hoaDon.setTongTienGiamGia(BigDecimal.ZERO);
        }

        return hoaDonRepository.save(hoaDon);
    }

    // =========================
    // PUT - Cập nhật hóa đơn
    // =========================
    public HoaDon update(Long id, HoaDon hoaDonMoi) {

        HoaDon hoaDonCu =
                hoaDonRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy hóa đơn có ID: " + id
                                )
                        );

        // =========================
        // CHỈ CẬP NHẬT NẾU CÓ DỮ LIỆU
        // =========================

        if (hoaDonMoi.getMaHoaDon() != null) {
            hoaDonCu.setMaHoaDon(
                    hoaDonMoi.getMaHoaDon()
            );
        }

        if (hoaDonMoi.getLoaiDon() != null) {
            hoaDonCu.setLoaiDon(
                    hoaDonMoi.getLoaiDon()
            );
        }

        if (hoaDonMoi.getPhiShip() != null) {
            hoaDonCu.setPhiShip(
                    hoaDonMoi.getPhiShip()
            );
        }

        if (hoaDonMoi.getTongTien() != null) {
            hoaDonCu.setTongTien(
                    hoaDonMoi.getTongTien()
            );
        }

        if (hoaDonMoi.getTongTienGiamGia() != null) {
            hoaDonCu.setTongTienGiamGia(
                    hoaDonMoi.getTongTienGiamGia()
            );
        }

        if (hoaDonMoi.getTenKhachHang() != null) {
            hoaDonCu.setTenKhachHang(
                    hoaDonMoi.getTenKhachHang()
            );
        }

        if (hoaDonMoi.getSoDienThoaiKhachHang() != null) {
            hoaDonCu.setSoDienThoaiKhachHang(
                    hoaDonMoi.getSoDienThoaiKhachHang()
            );
        }

        if (hoaDonMoi.getDiaChiNhanHang() != null) {
            hoaDonCu.setDiaChiNhanHang(
                    hoaDonMoi.getDiaChiNhanHang()
            );
        }

        if (hoaDonMoi.getNguoiTao() != null) {
            hoaDonCu.setNguoiTao(
                    hoaDonMoi.getNguoiTao()
            );
        }

        if (hoaDonMoi.getNguoiCapNhat() != null) {
            hoaDonCu.setNguoiCapNhat(
                    hoaDonMoi.getNguoiCapNhat()
            );
        }

        // =========================
        // TRẠNG THÁI ĐƠN
        // =========================
        if (hoaDonMoi.getTrangThai() != null) {
            hoaDonCu.setTrangThai(
                    hoaDonMoi.getTrangThai()
            );
        }


        if (hoaDonMoi.getGhiChu() != null) {
            hoaDonCu.setGhiChu(
                    hoaDonMoi.getGhiChu()
            );
        }

        // =========================
        // THỜI GIAN CẬP NHẬT
        // =========================
        hoaDonCu.setNgayCapNhat(
                Instant.now()
        );

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