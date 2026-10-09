package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.ChiTietHoaDon;
import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.repository.ChiTietHoaDonRepository;
import com.example.sd009thuan.repository.HoaDonRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class HoaDonService {

    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;

    // Tiêm cả HoaDonRepository và ChiTietHoaDonRepository qua constructor
    public HoaDonService(
            HoaDonRepository hoaDonRepository,
            ChiTietHoaDonRepository chiTietHoaDonRepository
    ) {
        this.hoaDonRepository = hoaDonRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
    }

    // Lấy danh sách tất cả hóa đơn
    public List<HoaDon> getAll() {
        return hoaDonRepository.findAll();
    }

    // Lấy hóa đơn theo ID
    public Optional<HoaDon> getById(Long id) {
        return hoaDonRepository.findById(id);
    }
    // Lấy hóa đơn theo mã hóa đơn (hàm trả về Optional<HoaDon> đang bị thiếu)
    public Optional<HoaDon> getByMaHoaDon(String maHoaDon) {
        return hoaDonRepository.findByMaHoaDon(maHoaDon);
    }
    // Lấy danh sách sản phẩm chi tiết theo ID hóa đơn
    public List<ChiTietHoaDon> getChiTietByHoaDonId(Long idHoaDon) {
        return chiTietHoaDonRepository.findByIdHoaDon_Id(idHoaDon);
    }

    // Lấy danh sách sản phẩm chi tiết theo Mã hóa đơn (HD001, HD002, ...)
    public List<ChiTietHoaDon> getChiTietByMaHoaDon(String maHoaDon) {
        return chiTietHoaDonRepository.findByIdHoaDon_MaHoaDon(maHoaDon);
    }

    // Tạo hóa đơn mới
    public HoaDon create(HoaDon hoaDon) {

        // Tự sinh mã hóa đơn HD001, HD002... nếu chưa có
        if (hoaDon.getMaHoaDon() == null
            || hoaDon.getMaHoaDon().trim().isEmpty()) {

            long count = hoaDonRepository.count() + 1;
            String maHoaDon = String.format("HD%03d", count);

            while (hoaDonRepository.findByMaHoaDon(maHoaDon).isPresent()) {
                count++;
                maHoaDon = String.format("HD%03d", count);
            }

            hoaDon.setMaHoaDon(maHoaDon);
        }

        // Thiết lập ngày tạo mặc định
        if (hoaDon.getNgayTao() == null) {
            hoaDon.setNgayTao(Instant.now());
        }

        // Thiết lập trạng thái hóa đơn mặc định
        if (hoaDon.getTrangThai() == null) {
            hoaDon.setTrangThai(1); // 1: Chờ xác nhận
        }

        // Thiết lập trạng thái thanh toán mặc định
        if (hoaDon.getTrangThaiThanhToan() == null) {
            hoaDon.setTrangThaiThanhToan(0); // 0: Chưa thanh toán
        }

        // Thiết lập các giá trị tiền mặc định
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

    // Cập nhật thông tin hóa đơn
    public HoaDon update(Long id, HoaDon hoaDonMoi) {

        HoaDon hoaDonCu = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy hóa đơn có ID: " + id
                ));

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
            hoaDonCu.setNguoiTao(hoaDonMoi.getNguoiTao());
        }

        if (hoaDonMoi.getNguoiCapNhat() != null) {
            hoaDonCu.setNguoiCapNhat(hoaDonMoi.getNguoiCapNhat());
        }

        if (hoaDonMoi.getTrangThai() != null) {
            hoaDonCu.setTrangThai(hoaDonMoi.getTrangThai());
        }

        if (hoaDonMoi.getTrangThaiThanhToan() != null) {
            hoaDonCu.setTrangThaiThanhToan(
                    hoaDonMoi.getTrangThaiThanhToan()
            );
        }

        if (hoaDonMoi.getGhiChu() != null) {
            hoaDonCu.setGhiChu(hoaDonMoi.getGhiChu());
        }

        hoaDonCu.setNgayCapNhat(Instant.now());

        return hoaDonRepository.save(hoaDonCu);
    }

    // Cập nhật nhanh trạng thái hóa đơn theo ID
    public HoaDon updateTrangThai(Long id, Integer trangThai) {
        return updateTrangThai(id, trangThai, null);
    }

    // Cập nhật trạng thái hóa đơn và ghi chú theo ID
    public HoaDon updateTrangThai(
            Long id,
            Integer trangThai,
            String ghiChu
    ) {

        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy hóa đơn có ID: " + id
                ));

        hoaDon.setTrangThai(trangThai);

        if (ghiChu != null && !ghiChu.trim().isEmpty()) {
            hoaDon.setGhiChu(ghiChu.trim());
        }

        hoaDon.setNgayCapNhat(Instant.now());

        return hoaDonRepository.save(hoaDon);
    }

    // Cập nhật trạng thái hóa đơn theo mã hóa đơn
    public HoaDon updateTrangThaiByMa(
            String maHoaDon,
            Integer trangThai,
            String ghiChu
    ) {

        HoaDon hoaDon = hoaDonRepository.findByMaHoaDon(maHoaDon)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy hóa đơn có mã: " + maHoaDon
                ));

        hoaDon.setTrangThai(trangThai);

        if (ghiChu != null && !ghiChu.trim().isEmpty()) {
            hoaDon.setGhiChu(ghiChu.trim());
        }

        hoaDon.setNgayCapNhat(Instant.now());

        return hoaDonRepository.save(hoaDon);
    }

    // Cập nhật trạng thái thanh toán
    // 0: Chưa thanh toán
    // 1: Đã thanh toán
    public HoaDon updateTrangThaiThanhToan(
            Long id,
            Integer trangThaiThanhToan
    ) {

        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy hóa đơn có ID: " + id
                ));

        if (trangThaiThanhToan == null
            || trangThaiThanhToan < 0
            || trangThaiThanhToan > 1) {

            throw new RuntimeException(
                    "Trạng thái thanh toán không hợp lệ. "
                    + "Phải là 0 hoặc 1."
            );
        }

        hoaDon.setTrangThaiThanhToan(trangThaiThanhToan);
        hoaDon.setNgayCapNhat(Instant.now());

        return hoaDonRepository.save(hoaDon);
    }

    // Xóa hóa đơn theo ID
    public void delete(Long id) {

        if (!hoaDonRepository.existsById(id)) {
            throw new RuntimeException(
                    "Không tìm thấy hóa đơn có ID: " + id
            );
        }

        hoaDonRepository.deleteById(id);
    }
}