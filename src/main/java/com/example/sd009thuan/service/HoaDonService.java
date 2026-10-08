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

    public List<HoaDon> getAll() {
        return hoaDonRepository.findAll();
    }

    public Optional<HoaDon> getById(Long id) {
        return hoaDonRepository.findById(id);
    }

    public Optional<HoaDon> getByMaHoaDon(String maHoaDon) {
        return hoaDonRepository.findByMaHoaDon(maHoaDon);
    }

    public HoaDon create(HoaDon hoaDon) {

        // Tự sinh mã HD001, HD002... nếu chưa có
        if (hoaDon.getMaHoaDon() == null || hoaDon.getMaHoaDon().trim().isEmpty()) {
            long count = hoaDonRepository.count() + 1;

            String maHoaDon = String.format("HD%03d", count);

            while (hoaDonRepository.findByMaHoaDon(maHoaDon).isPresent()) {
                count++;
                maHoaDon = String.format("HD%03d", count);
            }

            hoaDon.setMaHoaDon(maHoaDon);
        }

        // Gán giá trị mặc định ban đầu
        if (hoaDon.getNgayTao() == null) {
            hoaDon.setNgayTao(Instant.now());
        }

        if (hoaDon.getTrangThai() == null) {
            hoaDon.setTrangThai(1); // 1: Chờ xác nhận
        }

        if (hoaDon.getTrangThaiThanhToan() == null) {
            hoaDon.setTrangThaiThanhToan(0); // 0: Chưa thanh toán
        }

        if (hoaDon.getPhiVanChuyen() == null) {
            hoaDon.setPhiVanChuyen(BigDecimal.ZERO);
        }

        if (hoaDon.getTongTien() == null) {
            hoaDon.setTongTien(BigDecimal.ZERO);
        }

        if (hoaDon.getTienSauGiamGia() == null) {
            hoaDon.setTienSauGiamGia(BigDecimal.ZERO);
        }

        return hoaDonRepository.save(hoaDon);
    }

    public HoaDon update(Long id, HoaDon hoaDonMoi) {

        HoaDon hoaDonCu = hoaDonRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy hóa đơn có ID: " + id
                        )
                );

        if (hoaDonMoi.getMaHoaDon() != null) {
            hoaDonCu.setMaHoaDon(hoaDonMoi.getMaHoaDon());
        }

        if (hoaDonMoi.getLoaiDon() != null) {
            hoaDonCu.setLoaiDon(hoaDonMoi.getLoaiDon());
        }

        if (hoaDonMoi.getPhiVanChuyen() != null) {
            hoaDonCu.setPhiVanChuyen(hoaDonMoi.getPhiVanChuyen());
        }

        if (hoaDonMoi.getTongTien() != null) {
            hoaDonCu.setTongTien(hoaDonMoi.getTongTien());
        }

        if (hoaDonMoi.getTienSauGiamGia() != null) {
            hoaDonCu.setTienSauGiamGia(hoaDonMoi.getTienSauGiamGia());
        }

        if (hoaDonMoi.getTenKhachHang() != null) {
            hoaDonCu.setTenKhachHang(hoaDonMoi.getTenKhachHang());
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

        if (hoaDonMoi.getTrangThai() != null) {
            hoaDonCu.setTrangThai(
                    hoaDonMoi.getTrangThai()
            );
        }

        if (hoaDonMoi.getTrangThaiThanhToan() != null) {
            hoaDonCu.setTrangThaiThanhToan(
                    hoaDonMoi.getTrangThaiThanhToan()
            );
        }

        if (hoaDonMoi.getGhiChu() != null) {
            hoaDonCu.setGhiChu(
                    hoaDonMoi.getGhiChu()
            );
        }

        hoaDonCu.setNgayCapNhat(Instant.now());

        return hoaDonRepository.save(hoaDonCu);
    }

    // Cập nhật nhanh trạng thái đơn hàng
    // dùng cho các nút chuyển bước / hủy đơn
    public HoaDon updateTrangThai(Long id, Integer trangThai) {

        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy hóa đơn có ID: " + id
                        )
                );

        hoaDon.setTrangThai(trangThai);
        hoaDon.setNgayCapNhat(Instant.now());

        return hoaDonRepository.save(hoaDon);
    }

    // Cập nhật trạng thái thanh toán
    // 0 = Chưa thanh toán
    // 1 = Đã thanh toán
    public HoaDon updateTrangThaiThanhToan(
            Long id,
            Integer trangThaiThanhToan
    ) {

        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy hóa đơn có ID: " + id
                        )
                );

        if (trangThaiThanhToan == null
            || trangThaiThanhToan < 0
            || trangThaiThanhToan > 1) {

            throw new RuntimeException(
                    "Trạng thái thanh toán không hợp lệ. " +
                    "Phải là 0 hoặc 1."
            );
        }

        hoaDon.setTrangThaiThanhToan(trangThaiThanhToan);
        hoaDon.setNgayCapNhat(Instant.now());

        return hoaDonRepository.save(hoaDon);
    }

    public void delete(Long id) {

        if (!hoaDonRepository.existsById(id)) {
            throw new RuntimeException(
                    "Không tìm thấy hóa đơn có ID: " + id
            );
        }

        hoaDonRepository.deleteById(id);
    }
}