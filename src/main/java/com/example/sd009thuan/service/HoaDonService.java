package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.ChiTietHoaDon;
import com.example.sd009thuan.entity.HinhThucThanhToan;
import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.entity.LichSuHoaDon;
import com.example.sd009thuan.repository.ChiTietHoaDonRepository;
import com.example.sd009thuan.repository.HinhThucThanhToanRepository;
import com.example.sd009thuan.repository.HoaDonRepository;
import com.example.sd009thuan.repository.LichSuHoaDonRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class HoaDonService {

    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;
    private final HinhThucThanhToanRepository hinhThucThanhToanRepository;

    public HoaDonService(
            HoaDonRepository hoaDonRepository,
            ChiTietHoaDonRepository chiTietHoaDonRepository,
            LichSuHoaDonRepository lichSuHoaDonRepository,
            HinhThucThanhToanRepository hinhThucThanhToanRepository
    ) {
        this.hoaDonRepository = hoaDonRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
        this.lichSuHoaDonRepository = lichSuHoaDonRepository;
        this.hinhThucThanhToanRepository = hinhThucThanhToanRepository;
    }

    // Đồng bộ tính toán tổng tiền và tiền sau giảm giá từ danh sách sản phẩm chi tiết
    private void syncInvoiceTotals(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getId() == null) return;
        try {
            List<ChiTietHoaDon> items = chiTietHoaDonRepository.findByIdHoaDon_Id(hoaDon.getId());
            if (items != null && !items.isEmpty()) {
                BigDecimal sum = BigDecimal.ZERO;
                for (ChiTietHoaDon item : items) {
                    if (item.getThanhTien() != null && item.getThanhTien().compareTo(BigDecimal.ZERO) > 0) {
                        sum = sum.add(item.getThanhTien());
                    } else if (item.getDonGia() != null && item.getSoLuong() != null) {
                        sum = sum.add(item.getDonGia().multiply(BigDecimal.valueOf(item.getSoLuong())));
                    }
                }
                if (sum.compareTo(BigDecimal.ZERO) > 0) {
                    boolean changed = false;
                    if (hoaDon.getTongTien() == null || hoaDon.getTongTien().compareTo(sum) != 0) {
                        hoaDon.setTongTien(sum);
                        changed = true;
                    }
                    BigDecimal shipping = hoaDon.getPhiVanChuyen() != null ? hoaDon.getPhiVanChuyen() : BigDecimal.ZERO;
                    BigDecimal discount = BigDecimal.ZERO;
                    if (hoaDon.getIdPhieuGiamGia() != null) {
                        com.example.sd009thuan.entity.PhieuGiamGia pgg = hoaDon.getIdPhieuGiamGia();
                        if (pgg.getGiaTriGiamGia() != null) {
                            if (Integer.valueOf(2).equals(pgg.getLoaiPhieuGiamGia())) {
                                BigDecimal percentDiscount = sum.multiply(pgg.getGiaTriGiamGia()).divide(BigDecimal.valueOf(100));
                                if (pgg.getGiamToiDa() != null && percentDiscount.compareTo(pgg.getGiamToiDa()) > 0) {
                                    discount = pgg.getGiamToiDa();
                                } else {
                                    discount = percentDiscount;
                                }
                            } else {
                                discount = pgg.getGiaTriGiamGia();
                            }
                        }
                    }
                    BigDecimal finalAmount = sum.add(shipping).subtract(discount);
                    if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;
                    if (hoaDon.getTienSauGiamGia() == null || hoaDon.getTienSauGiamGia().compareTo(finalAmount) != 0) {
                        hoaDon.setTienSauGiamGia(finalAmount);
                        changed = true;
                    }
                    if (changed) {
                        hoaDonRepository.save(hoaDon);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    // Lấy danh sách tất cả hóa đơn (kèm theo các bảng khóa ngoại NhanVien, KhachHang, PhieuGiamGia)
    public List<HoaDon> getAll() {
        List<HoaDon> list;
        try {
            list = hoaDonRepository.findAllWithForeignKeys();
        } catch (Exception e) {
            list = hoaDonRepository.findAll();
        }
        for (HoaDon h : list) {
            syncInvoiceTotals(h);
        }
        return list;
    }

    // Lấy hóa đơn theo ID kèm các khóa ngoại
    public Optional<HoaDon> getById(Long id) {
        Optional<HoaDon> opt;
        try {
            opt = hoaDonRepository.findByIdWithForeignKeys(id);
        } catch (Exception e) {
            opt = hoaDonRepository.findById(id);
        }
        opt.ifPresent(this::syncInvoiceTotals);
        return opt;
    }

    // Lấy hóa đơn theo mã hóa đơn kèm các khóa ngoại
    public Optional<HoaDon> getByMaHoaDon(String maHoaDon) {
        Optional<HoaDon> opt;
        try {
            opt = hoaDonRepository.findByMaHoaDonWithForeignKeys(maHoaDon);
        } catch (Exception e) {
            opt = hoaDonRepository.findByMaHoaDon(maHoaDon);
        }
        opt.ifPresent(this::syncInvoiceTotals);
        return opt;
    }

    // Lấy danh sách sản phẩm chi tiết theo ID hóa đơn
    public List<ChiTietHoaDon> getChiTietByHoaDonId(Long idHoaDon) {
        return chiTietHoaDonRepository.findByIdHoaDon_Id(idHoaDon);
    }

    // Lấy danh sách sản phẩm chi tiết theo Mã hóa đơn (HD001, HD002, ...)
    public List<ChiTietHoaDon> getChiTietByMaHoaDon(String maHoaDon) {
        return chiTietHoaDonRepository.findByIdHoaDon_MaHoaDon(maHoaDon);
    }

    // Lấy lịch sử thao tác hóa đơn theo ID
    public List<LichSuHoaDon> getLichSuByHoaDonId(Long idHoaDon) {
        return lichSuHoaDonRepository.findByIdHoaDon_IdOrderByThoiGianDesc(idHoaDon);
    }

    // Lấy lịch sử thao tác hóa đơn theo Mã
    public List<LichSuHoaDon> getLichSuByMaHoaDon(String maHoaDon) {
        return lichSuHoaDonRepository.findByIdHoaDon_MaHoaDonOrderByThoiGianDesc(maHoaDon);
    }

    // Lấy thông tin thanh toán theo ID hóa đơn
    public List<HinhThucThanhToan> getThanhToanByHoaDonId(Long idHoaDon) {
        return hinhThucThanhToanRepository.findByIdHoaDon_Id(idHoaDon);
    }

    // Lấy thông tin thanh toán theo Mã hóa đơn
    public List<HinhThucThanhToan> getThanhToanByMaHoaDon(String maHoaDon) {
        return hinhThucThanhToanRepository.findByIdHoaDon_MaHoaDon(maHoaDon);
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

        HoaDon saved = hoaDonRepository.save(hoaDon);

        try {
            LichSuHoaDon log = new LichSuHoaDon();
            log.setIdHoaDon(saved);
            log.setTrangThai(trangThai);
            log.setThoiGian(Instant.now());
            log.setGhiChu(ghiChu != null && !ghiChu.trim().isEmpty() ? ghiChu.trim() : "Cập nhật trạng thái hóa đơn");
            log.setNguoiThucHien(saved.getNguoiCapNhat() != null ? saved.getNguoiCapNhat() : "Nhân viên");
            log.setVaiTroNguoiThucHien("Nhân viên");
            lichSuHoaDonRepository.save(log);
        } catch (Exception ignored) {}

        return saved;
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

        HoaDon saved = hoaDonRepository.save(hoaDon);

        try {
            LichSuHoaDon log = new LichSuHoaDon();
            log.setIdHoaDon(saved);
            log.setTrangThai(trangThai);
            log.setThoiGian(Instant.now());
            log.setGhiChu(ghiChu != null && !ghiChu.trim().isEmpty() ? ghiChu.trim() : "Cập nhật trạng thái hóa đơn");
            log.setNguoiThucHien(saved.getNguoiCapNhat() != null ? saved.getNguoiCapNhat() : "Nhân viên");
            log.setVaiTroNguoiThucHien("Nhân viên");
            lichSuHoaDonRepository.save(log);
        } catch (Exception ignored) {}

        return saved;
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