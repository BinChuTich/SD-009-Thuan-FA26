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

    // LẤY TẤT CẢ HÓA ĐƠN
    public List<HoaDon> getAll() {
        return hoaDonRepository.findAll();
    }

    // LẤY HÓA ĐƠN THEO ID
    public Optional<HoaDon> getById(Long id) {
        return hoaDonRepository.findById(id);
    }

    // LẤY HÓA ĐƠN THEO MÃ
    public Optional<HoaDon> getByMaHoaDon(String maHoaDon) {
        return hoaDonRepository.findByMaHoaDon(maHoaDon);
    }

    // TẠO HÓA ĐƠN
    public HoaDon create(HoaDon hoaDon) {

        // TỰ SINH MÃ HÓA ĐƠN: 1, 2, 3, 4...
        if (hoaDon.getMaHoaDon() == null
            || hoaDon.getMaHoaDon().trim().isEmpty()) {

            long soThuTu = hoaDonRepository.count() + 1;
            String maHoaDon = String.valueOf(soThuTu);

            // Đảm bảo không trùng mã
            while (hoaDonRepository.findByMaHoaDon(maHoaDon).isPresent()) {
                soThuTu++;
                maHoaDon = String.valueOf(soThuTu);
            }

            hoaDon.setMaHoaDon(maHoaDon);
        }

        // NGÀY TẠO
        if (hoaDon.getNgayTao() == null) {
            hoaDon.setNgayTao(Instant.now());
        }

        // TRẠNG THÁI ĐƠN HÀNG
        // 1 = Chờ xử lý
        if (hoaDon.getTrangThai() == null) {
            hoaDon.setTrangThai(1);
        }

        // TRẠNG THÁI THANH TOÁN
        // 0 = Chưa thanh toán
        // 1 = Đã thanh toán
        // 2 = Thanh toán một phần
        if (hoaDon.getTrangThaiThanhToan() == null) {
            hoaDon.setTrangThaiThanhToan(0);
        }

        // PHÍ VẬN CHUYỂN
        if (hoaDon.getPhiVanChuyen() == null) {
            hoaDon.setPhiVanChuyen(BigDecimal.ZERO);
        }

        // TỔNG TIỀN
        if (hoaDon.getTongTien() == null) {
            hoaDon.setTongTien(BigDecimal.ZERO);
        }

        // TIỀN SAU GIẢM GIÁ
        if (hoaDon.getTienSauGiamGia() == null) {
            hoaDon.setTienSauGiamGia(BigDecimal.ZERO);
        }

        return hoaDonRepository.save(hoaDon);
    }

    // CẬP NHẬT HÓA ĐƠN
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
            hoaDonCu.setPhiVanChuyen(
                    hoaDonMoi.getPhiVanChuyen()
            );
        }

        if (hoaDonMoi.getTongTien() != null) {
            hoaDonCu.setTongTien(
                    hoaDonMoi.getTongTien()
            );
        }

        if (hoaDonMoi.getTienSauGiamGia() != null) {
            hoaDonCu.setTienSauGiamGia(
                    hoaDonMoi.getTienSauGiamGia()
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

        // CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG
        if (hoaDonMoi.getTrangThai() != null) {
            if (hoaDonMoi.getTrangThai() < 1
                || hoaDonMoi.getTrangThai() > 6) {

                throw new RuntimeException(
                        "Trạng thái hóa đơn không hợp lệ. Phải từ 1 đến 6."
                );
            }

            hoaDonCu.setTrangThai(
                    hoaDonMoi.getTrangThai()
            );
        }

        // CẬP NHẬT TRẠNG THÁI THANH TOÁN
        if (hoaDonMoi.getTrangThaiThanhToan() != null) {

            if (hoaDonMoi.getTrangThaiThanhToan() < 0
                || hoaDonMoi.getTrangThaiThanhToan() > 2) {

                throw new RuntimeException(
                        "Trạng thái thanh toán không hợp lệ. Phải là 0, 1 hoặc 2."
                );
            }

            hoaDonCu.setTrangThaiThanhToan(
                    hoaDonMoi.getTrangThaiThanhToan()
            );
        }

        if (hoaDonMoi.getGhiChu() != null) {
            hoaDonCu.setGhiChu(
                    hoaDonMoi.getGhiChu()
            );
        }

        // CẬP NHẬT THỜI GIAN
        hoaDonCu.setNgayCapNhat(Instant.now());

        return hoaDonRepository.save(hoaDonCu);
    }

    // CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG
    public HoaDon updateTrangThai(Long id, Integer trangThai) {

        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy hóa đơn có ID: " + id
                        )
                );

        /*
         * 1 = Chờ xử lý
         * 2 = Đã xác nhận
         * 3 = Đang chuẩn bị
         * 4 = Đang giao
         * 5 = Hoàn thành
         * 6 = Đã hủy
         */

        if (trangThai == null
            || trangThai < 1
            || trangThai > 6) {

            throw new RuntimeException(
                    "Trạng thái hóa đơn không hợp lệ. Phải từ 1 đến 6."
            );
        }

        hoaDon.setTrangThai(trangThai);
        hoaDon.setNgayCapNhat(Instant.now());

        return hoaDonRepository.save(hoaDon);
    }

    // CẬP NHẬT TRẠNG THÁI THANH TOÁN
    public HoaDon updateTrangThaiThanhToan(
            Long id,
            Integer trangThaiThanhToan) {

        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy hóa đơn có ID: " + id
                        )
                );

        /*
         * 0 = Chưa thanh toán
         * 1 = Đã thanh toán
         * 2 = Thanh toán một phần
         */

        if (trangThaiThanhToan == null
            || trangThaiThanhToan < 0
            || trangThaiThanhToan > 2) {

            throw new RuntimeException(
                    "Trạng thái thanh toán không hợp lệ. Phải là 0, 1 hoặc 2."
            );
        }

        hoaDon.setTrangThaiThanhToan(
                trangThaiThanhToan
        );

        hoaDon.setNgayCapNhat(Instant.now());

        return hoaDonRepository.save(hoaDon);
    }

    // XÓA HÓA ĐƠN
    public void delete(Long id) {

        if (!hoaDonRepository.existsById(id)) {
            throw new RuntimeException(
                    "Không tìm thấy hóa đơn có ID: " + id
            );
        }

        hoaDonRepository.deleteById(id);
    }
}