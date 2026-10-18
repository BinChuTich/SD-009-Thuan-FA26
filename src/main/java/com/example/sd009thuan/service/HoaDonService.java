package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.HoaDonPosDTO;
import com.example.sd009thuan.entity.ChiTietHoaDon;
import com.example.sd009thuan.entity.ChiTietSanPham;
import com.example.sd009thuan.entity.HinhAnh;
import com.example.sd009thuan.entity.HinhThucThanhToan;
import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.entity.KhachHang;
import com.example.sd009thuan.entity.LichSuHoaDon;
import com.example.sd009thuan.entity.PhuongThucThanhToan;
import com.example.sd009thuan.repository.ChiTietHoaDonRepository;
import com.example.sd009thuan.repository.ChiTietSanPhamRepository;
import com.example.sd009thuan.repository.HinhAnhRepository;
import com.example.sd009thuan.repository.HinhThucThanhToanRepository;
import com.example.sd009thuan.repository.HoaDonRepository;
import com.example.sd009thuan.repository.KhachHangRepository;
import com.example.sd009thuan.repository.LichSuHoaDonRepository;
import com.example.sd009thuan.repository.NhanVienRepository;
import com.example.sd009thuan.repository.PhieuGiamGiaRepository;
import com.example.sd009thuan.repository.PhuongThucThanhToanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class HoaDonService {

    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;
    private final HinhThucThanhToanRepository hinhThucThanhToanRepository;
    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final KhachHangRepository khachHangRepository;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final NhanVienRepository nhanVienRepository;
    private final HinhAnhRepository hinhAnhRepository;

    public HoaDonService(
            HoaDonRepository hoaDonRepository,
            ChiTietHoaDonRepository chiTietHoaDonRepository,
            LichSuHoaDonRepository lichSuHoaDonRepository,
            HinhThucThanhToanRepository hinhThucThanhToanRepository,
            ChiTietSanPhamRepository chiTietSanPhamRepository,
            KhachHangRepository khachHangRepository,
            PhuongThucThanhToanRepository phuongThucThanhToanRepository,
            PhieuGiamGiaRepository phieuGiamGiaRepository,
            NhanVienRepository nhanVienRepository,
            HinhAnhRepository hinhAnhRepository
    ) {
        this.hoaDonRepository = hoaDonRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
        this.lichSuHoaDonRepository = lichSuHoaDonRepository;
        this.hinhThucThanhToanRepository = hinhThucThanhToanRepository;
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
        this.khachHangRepository = khachHangRepository;
        this.phuongThucThanhToanRepository = phuongThucThanhToanRepository;
        this.phieuGiamGiaRepository = phieuGiamGiaRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.hinhAnhRepository = hinhAnhRepository;
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
                            boolean isPercent = Integer.valueOf(1).equals(pgg.getLoaiPhieuGiamGia())
                                    || (pgg.getLoaiPhieuGiamGia() == null && pgg.getGiaTriGiamGia().compareTo(BigDecimal.valueOf(100)) <= 0);
                            if (isPercent) {
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

    // Kiểm tra xem hóa đơn có phải là hóa đơn chờ tại quầy hay không
    public boolean isWaitingInvoice(HoaDon h) {
        if (h == null) return false;
        boolean isStoreDraft = Integer.valueOf(1).equals(h.getLoaiDon())
                && Integer.valueOf(1).equals(h.getTrangThai())
                && (h.getTrangThaiThanhToan() == null || Integer.valueOf(0).equals(h.getTrangThaiThanhToan()));
        boolean isWaitingNote = h.getGhiChu() != null && h.getGhiChu().toLowerCase().contains("hóa đơn chờ");
        return isStoreDraft || isWaitingNote;
    }

    // Lấy danh sách tất cả hóa đơn (loại trừ các hóa đơn chờ tại quầy)
    public List<HoaDon> getAll() {
        List<HoaDon> list;
        try {
            list = hoaDonRepository.findAllWithForeignKeys();
        } catch (Exception e) {
            list = hoaDonRepository.findAll();
        }
        List<HoaDon> filtered = list.stream()
                .filter(h -> !isWaitingInvoice(h))
                .toList();
        for (HoaDon h : filtered) {
            syncInvoiceTotals(h);
        }
        return filtered;
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

    // Gắn đường dẫn ảnh đại diện cho các sản phẩm chi tiết của hóa đơn
    private void attachImagesToChiTiet(List<ChiTietHoaDon> list) {
        if (list == null || list.isEmpty()) return;
        for (ChiTietHoaDon ct : list) {
            if (ct.getAnhDaiDien() == null && ct.getIdChiTietSanPham() != null && ct.getIdChiTietSanPham().getIdSanPham() != null) {
                Long idSp = ct.getIdChiTietSanPham().getIdSanPham().getId();
                if (idSp != null) {
                    try {
                        List<HinhAnh> imgs = hinhAnhRepository.findByIdSanPham_IdAndTrangThai(idSp, 1);
                        if (imgs != null && !imgs.isEmpty()) {
                            ct.setAnhDaiDien(imgs.get(0).getDuongDan());
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    // Lấy danh sách sản phẩm chi tiết theo ID hóa đơn
    public List<ChiTietHoaDon> getChiTietByHoaDonId(Long idHoaDon) {
        if (idHoaDon == null) return List.of();
        List<ChiTietHoaDon> list;
        try {
            list = chiTietHoaDonRepository.findByIdHoaDon_Id(idHoaDon);
        } catch (Exception e) {
            list = chiTietHoaDonRepository.findByHoaDonIdSimple(idHoaDon);
        }
        attachImagesToChiTiet(list);
        return list;
    }

    // Lấy danh sách sản phẩm chi tiết theo Mã hóa đơn (HD001, HD002, ...)
    public List<ChiTietHoaDon> getChiTietByMaHoaDon(String maHoaDon) {
        if (maHoaDon == null || maHoaDon.trim().isEmpty()) return List.of();
        List<ChiTietHoaDon> list;
        try {
            list = chiTietHoaDonRepository.findByIdHoaDon_MaHoaDon(maHoaDon.trim());
        } catch (Exception e) {
            list = chiTietHoaDonRepository.findByHoaDonMaSimple(maHoaDon.trim());
        }
        attachImagesToChiTiet(list);
        return list;
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

    // Sinh mã hóa đơn tiếp theo đảm bảo không trùng lặp với bất kỳ hóa đơn nào trong danh sách
    public String generateNextMaHoaDon() {
        List<HoaDon> all = hoaDonRepository.findAll();
        long maxNum = 0;
        for (HoaDon h : all) {
            if (h.getMaHoaDon() != null) {
                String ma = h.getMaHoaDon().trim();
                if (ma.toUpperCase().startsWith("HD")) {
                    try {
                        long num = Long.parseLong(ma.substring(2).trim());
                        if (num > maxNum) {
                            maxNum = num;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        long nextNum = Math.max(maxNum + 1, all.size() + 1);
        String nextCode = String.format("HD%03d", nextNum);
        while (hoaDonRepository.findByMaHoaDon(nextCode).isPresent()) {
            nextNum++;
            nextCode = String.format("HD%03d", nextNum);
        }
        return nextCode;
    }

    // Lấy danh sách hóa đơn chờ tại quầy
    public List<HoaDon> getWaitingInvoices() {
        List<HoaDon> all;
        try {
            all = hoaDonRepository.findAllWithForeignKeys();
        } catch (Exception e) {
            all = hoaDonRepository.findAll();
        }
        return all.stream()
                .filter(this::isWaitingInvoice)
                .toList();
    }

    // Tạo hóa đơn chờ tại quầy mới
    @Transactional
    public HoaDon createWaitingInvoice() {
        HoaDon hd = new HoaDon();
        hd.setMaHoaDon(generateNextMaHoaDon());
        hd.setLoaiDon(1); // 1: Tại quầy
        hd.setTrangThai(1); // 1: Chờ xử lý / Chờ thanh toán
        hd.setTrangThaiThanhToan(0); // 0: Chưa thanh toán
        hd.setTenKhachHang("Khách lẻ");
        hd.setDiaChiNhanHang("Bán tại quầy");
        hd.setTongTien(BigDecimal.ZERO);
        hd.setPhiVanChuyen(BigDecimal.ZERO);
        hd.setTienSauGiamGia(BigDecimal.ZERO);
        hd.setNgayTao(Instant.now());
        hd.setNgayCapNhat(Instant.now());
        hd.setNguoiTao("Nhân viên bán hàng");
        hd.setGhiChu("Hóa đơn chờ tại quầy");
        nhanVienRepository.findTopByOrderByIdDesc().ifPresent(hd::setIdNhanVien);
        return hoaDonRepository.save(hd);
    }

    // Đồng bộ danh sách sản phẩm của hóa đơn chờ mà không trừ tồn kho
    @Transactional
    public List<ChiTietHoaDon> syncCartToInvoice(Long idHoaDon, List<HoaDonPosDTO.ItemChiTietDTO> items) {
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn ID: " + idHoaDon));

        // Xóa chi tiết cũ
        List<ChiTietHoaDon> oldDetails = chiTietHoaDonRepository.findByIdHoaDon_Id(idHoaDon);
        if (oldDetails != null && !oldDetails.isEmpty()) {
            chiTietHoaDonRepository.deleteAll(oldDetails);
        }

        BigDecimal calculatedTotal = BigDecimal.ZERO;
        if (items != null && !items.isEmpty()) {
            int itemIndex = 1;
            for (HoaDonPosDTO.ItemChiTietDTO item : items) {
                Long variantId = item.getResolvedVariantId();
                if (variantId == null) continue;
                Optional<ChiTietSanPham> ctspOpt = chiTietSanPhamRepository.findById(variantId);
                if (ctspOpt.isEmpty()) continue;
                ChiTietSanPham ctsp = ctspOpt.get();
                int qty = item.getSoLuong() != null ? Math.max(1, item.getSoLuong()) : 1;
                BigDecimal unitPrice = item.getResolvedPrice();
                if (unitPrice.compareTo(BigDecimal.ZERO) <= 0 && ctsp.getGiaBan() != null) {
                    unitPrice = ctsp.getGiaBan();
                }
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                calculatedTotal = calculatedTotal.add(lineTotal);

                ChiTietHoaDon cthd = new ChiTietHoaDon();
                cthd.setIdHoaDon(hoaDon);
                cthd.setIdChiTietSanPham(ctsp);
                cthd.setMaHoaDonChiTiet(String.format("CTHD%s-%02d", hoaDon.getMaHoaDon(), itemIndex++));
                cthd.setDonGia(unitPrice);
                cthd.setSoLuong(qty);
                cthd.setThanhTien(lineTotal);
                cthd.setTrangThai(1);
                chiTietHoaDonRepository.save(cthd);
            }
        }
        hoaDon.setTongTien(calculatedTotal);
        BigDecimal ship = hoaDon.getPhiVanChuyen() != null ? hoaDon.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal finalTotal = calculatedTotal.add(ship);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;
        hoaDon.setTienSauGiamGia(finalTotal);
        hoaDon.setNgayCapNhat(Instant.now());
        hoaDonRepository.save(hoaDon);

        List<ChiTietHoaDon> list = chiTietHoaDonRepository.findByIdHoaDon_Id(idHoaDon);
        attachImagesToChiTiet(list);
        return list;
    }

    // Tạo hóa đơn mới
    @Transactional
    public HoaDon create(HoaDon hoaDon) {

        // Tự sinh mã hóa đơn nếu chưa có hoặc đã tồn tại trong danh sách
        if (hoaDon.getMaHoaDon() == null
            || hoaDon.getMaHoaDon().trim().isEmpty()
            || hoaDonRepository.findByMaHoaDon(hoaDon.getMaHoaDon().trim()).isPresent()) {
            hoaDon.setMaHoaDon(generateNextMaHoaDon());
        }

        // Tên khách hàng: Nếu khách không để lại thông tin, note là "Khách lẻ"
        String rawKh = hoaDon.getTenKhachHang();
        if (rawKh == null || rawKh.trim().isEmpty() || rawKh.trim().equalsIgnoreCase("null") || rawKh.trim().equals("---") || rawKh.trim().equals("—")) {
            hoaDon.setTenKhachHang("Khách lẻ");
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

        if (hoaDon.getTienSauGiamGia() == null || (BigDecimal.ZERO.compareTo(hoaDon.getTienSauGiamGia()) == 0 && hoaDon.getTongTien().compareTo(BigDecimal.ZERO) > 0)) {
            hoaDon.setTienSauGiamGia(hoaDon.getTongTien());
        }

        return hoaDonRepository.save(hoaDon);
    }

    // Cập nhật thông tin hóa đơn
    @Transactional
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
            String ten = hoaDonMoi.getTenKhachHang().trim();
            boolean isKhachLe = ten.isEmpty() || ten.equalsIgnoreCase("null") || ten.equals("---") || ten.equals("—") || ten.equalsIgnoreCase("Khách lẻ");
            if (isKhachLe) {
                hoaDonCu.setTenKhachHang("Khách lẻ");
                hoaDonCu.setSoDienThoaiKhachHang(null);
                hoaDonCu.setIdKhachHang(null);
                if (hoaDonMoi.getDiaChiNhanHang() == null && Integer.valueOf(1).equals(hoaDonCu.getLoaiDon())) {
                    hoaDonCu.setDiaChiNhanHang("Bán tại quầy");
                }
            } else {
                hoaDonCu.setTenKhachHang(ten);
                if (hoaDonCu.getIdKhachHang() != null) {
                    try {
                        hoaDonCu.getIdKhachHang().setTenKhachHang(ten);
                        khachHangRepository.save(hoaDonCu.getIdKhachHang());
                    } catch (Exception ignored) {}
                }
            }
        }

        if (hoaDonMoi.getSoDienThoaiKhachHang() != null) {
            String sdt = hoaDonMoi.getSoDienThoaiKhachHang().trim();
            hoaDonCu.setSoDienThoaiKhachHang(sdt.isEmpty() ? null : sdt);
            if (!sdt.isEmpty() && hoaDonCu.getIdKhachHang() != null) {
                try {
                    hoaDonCu.getIdKhachHang().setSoDienThoai(sdt);
                    khachHangRepository.save(hoaDonCu.getIdKhachHang());
                } catch (Exception ignored) {}
            }
        }

        if (hoaDonMoi.getDiaChiNhanHang() != null) {
            String dc = hoaDonMoi.getDiaChiNhanHang().trim();
            hoaDonCu.setDiaChiNhanHang(dc.isEmpty() ? null : dc);
        }

        if (hoaDonMoi.getNguoiTao() != null) {
            hoaDonCu.setNguoiTao(hoaDonMoi.getNguoiTao());
        }

        if (hoaDonMoi.getNguoiCapNhat() != null) {
            hoaDonCu.setNguoiCapNhat(hoaDonMoi.getNguoiCapNhat());
        }

        if (hoaDonMoi.getTrangThai() != null) {
            if (Integer.valueOf(7).equals(hoaDonMoi.getTrangThai())) {
                hoaDonCu.setTrangThai(6);
                String gc = hoaDonMoi.getGhiChu() != null ? hoaDonMoi.getGhiChu() : hoaDonCu.getGhiChu();
                if (gc == null || gc.trim().isEmpty()) {
                    hoaDonCu.setGhiChu("Giao hàng thất bại");
                } else if (!gc.toLowerCase().contains("giao hàng thất bại")) {
                    hoaDonCu.setGhiChu("Giao hàng thất bại: " + gc.trim());
                } else {
                    hoaDonCu.setGhiChu(gc.trim());
                }
            } else {
                hoaDonCu.setTrangThai(hoaDonMoi.getTrangThai());
            }
        }

        if (hoaDonMoi.getTrangThaiThanhToan() != null) {
            hoaDonCu.setTrangThaiThanhToan(
                    hoaDonMoi.getTrangThaiThanhToan()
            );
        }

        if (hoaDonMoi.getGhiChu() != null && !Integer.valueOf(7).equals(hoaDonMoi.getTrangThai())) {
            hoaDonCu.setGhiChu(hoaDonMoi.getGhiChu().trim());
        }

        hoaDonCu.setNgayCapNhat(Instant.now());

        HoaDon saved = hoaDonRepository.save(hoaDonCu);

        try {
            LichSuHoaDon log = new LichSuHoaDon();
            log.setIdHoaDon(saved);
            log.setThoiGian(Instant.now());
            String desc = "Cập nhật thông tin đơn hàng / khách hàng";
            if (hoaDonMoi.getGhiChu() != null && !hoaDonMoi.getGhiChu().trim().isEmpty() && !hoaDonMoi.getGhiChu().trim().equalsIgnoreCase("Giao hàng thất bại")) {
                desc += ": " + hoaDonMoi.getGhiChu().trim();
            }
            if (desc.length() > 250) desc = desc.substring(0, 250);
            log.setHanhDong(desc);
            String nguoiTh = saved.getNguoiCapNhat() != null ? saved.getNguoiCapNhat() : "Nhân viên";
            if (nguoiTh.length() > 95) nguoiTh = nguoiTh.substring(0, 95);
            log.setNguoiThucHien(nguoiTh);
            log.setVaiTroNguoiThucHien("Nhân viên");
            lichSuHoaDonRepository.save(log);
        } catch (Exception ignored) {}

        return saved;
    }

    // Cập nhật nhanh trạng thái hóa đơn theo ID
    @Transactional
    public HoaDon updateTrangThai(Long id, Integer trangThai) {
        return updateTrangThai(id, trangThai, null);
    }

    private String getTenTrangThai(Integer trangThai) {
        if (trangThai == null) return "Chờ xác nhận";
        return switch (trangThai) {
            case 1 -> "Chờ xác nhận";
            case 2 -> "Đã xác nhận";
            case 3 -> "Chờ vận chuyển";
            case 4 -> "Đang vận chuyển";
            case 5 -> "Hoàn thành";
            case 6 -> "Đã hủy";
            case 7 -> "Giao hàng thất bại";
            default -> "Trạng thái " + trangThai;
        };
    }

    // Đếm số lần đã thực hiện "Giao lại đơn hàng" dựa trên lịch sử hóa đơn hoặc ghi chú
    public long countGiaoLaiDonHang(Long idHoaDon) {
        if (idHoaDon == null) return 0;
        long count = 0;
        try {
            List<LichSuHoaDon> logs = lichSuHoaDonRepository.findByIdHoaDon_IdOrderByThoiGianDesc(idHoaDon);
            if (logs != null && !logs.isEmpty()) {
                count = logs.stream().filter(l -> {
                    String act = l.getHanhDong() != null ? l.getHanhDong().toLowerCase() : "";
                    return act.contains("giao lại");
                }).count();
            }
        } catch (Exception ignored) {}

        // Fallback kiểm tra ghi chú của hóa đơn nếu log chưa có hoặc chưa lưu kịp
        try {
            Optional<HoaDon> hdOpt = hoaDonRepository.findById(idHoaDon);
            if (hdOpt.isPresent() && hdOpt.get().getGhiChu() != null) {
                String gc = hdOpt.get().getGhiChu().toLowerCase();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("lần\\s*(\\d+)").matcher(gc);
                if (m.find()) {
                    long val = Long.parseLong(m.group(1));
                    if (gc.contains("giao lại")) {
                        count = Math.max(count, val);
                    } else if (gc.contains("thất bại") && val > 1) {
                        count = Math.max(count, val - 1);
                    }
                }
            }
        } catch (Exception ignored) {}
        return Math.min(3, Math.max(0, count));
    }

    // Cập nhật trạng thái hóa đơn và ghi chú theo ID
    @Transactional
    public HoaDon updateTrangThai(
            Long id,
            Integer trangThai,
            String ghiChu
    ) {

        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy hóa đơn có ID: " + id
                ));

        long retryCount = countGiaoLaiDonHang(hoaDon.getId());
        String actionDesc = (ghiChu != null && !ghiChu.trim().isEmpty())
                ? ghiChu.trim()
                : ("Cập nhật trạng thái sang: " + getTenTrangThai(trangThai));

        if (Integer.valueOf(4).equals(trangThai)) {
            // Khi chọn Giao lại đơn hàng: tối đa 3 lần
            boolean isRetryAction = Integer.valueOf(6).equals(hoaDon.getTrangThai())
                    || (hoaDon.getGhiChu() != null && hoaDon.getGhiChu().toLowerCase().contains("giao hàng thất bại"))
                    || (ghiChu != null && ghiChu.toLowerCase().contains("giao lại"));
            if (isRetryAction) {
                if (retryCount >= 3) {
                    throw new RuntimeException("Đơn hàng đã được giao lại tối đa 3 lần. Vui lòng thực hiện hủy đơn!");
                }
                long nextRetry = Math.min(3, retryCount + 1);
                actionDesc = "Tiến hành giao lại đơn hàng (Lần " + nextRetry + "/3)";
                hoaDon.setTrangThai(4);
                hoaDon.setGhiChu(actionDesc);
            } else {
                hoaDon.setTrangThai(4);
                if (ghiChu != null && !ghiChu.trim().isEmpty()) {
                    hoaDon.setGhiChu(ghiChu.trim());
                }
            }
        } else if (Integer.valueOf(7).equals(trangThai)) {
            // Khi đánh dấu giao hàng thất bại
            // Nếu đã giao lại đủ 3 lần mà vẫn thất bại -> TỰ ĐỘNG HỦY ĐƠN HÀNG
            if (retryCount >= 3) {
                hoaDon.setTrangThai(6);
                String cancelMsg = "Đã giao lại tối đa 3 lần thất bại - Tự động hủy đơn hàng";
                if (ghiChu != null && !ghiChu.trim().isEmpty() && !ghiChu.toLowerCase().contains("tự động hủy")) {
                    cancelMsg += " (Lý do: " + ghiChu.trim() + ")";
                }
                hoaDon.setGhiChu(cancelMsg);
                actionDesc = cancelMsg;
            } else {
                hoaDon.setTrangThai(6);
                long currentFail = retryCount + 1;
                String failMsg = "Giao hàng thất bại (lần " + currentFail + "/3)";
                if (ghiChu != null && !ghiChu.trim().isEmpty()) {
                    String clean = ghiChu.trim();
                    if (!clean.toLowerCase().contains("giao hàng thất bại")) {
                        failMsg += ": " + clean;
                    }
                }
                hoaDon.setGhiChu(failMsg);
                actionDesc = failMsg;
            }
        } else {
            hoaDon.setTrangThai(trangThai);
            if (ghiChu != null && !ghiChu.trim().isEmpty()) {
                hoaDon.setGhiChu(ghiChu.trim());
            }
        }

        hoaDon.setNgayCapNhat(Instant.now());

        HoaDon saved = hoaDonRepository.save(hoaDon);

        try {
            LichSuHoaDon log = new LichSuHoaDon();
            log.setIdHoaDon(saved);
            log.setThoiGian(Instant.now());
            if (actionDesc.length() > 250) {
                actionDesc = actionDesc.substring(0, 250);
            }
            log.setHanhDong(actionDesc);
            String nguoiTh = saved.getNguoiCapNhat() != null ? saved.getNguoiCapNhat() : "Nhân viên";
            if (nguoiTh.length() > 95) nguoiTh = nguoiTh.substring(0, 95);
            log.setNguoiThucHien(nguoiTh);
            log.setVaiTroNguoiThucHien("Nhân viên");
            lichSuHoaDonRepository.save(log);
        } catch (Exception ignored) {}

        return saved;
    }

    // Cập nhật trạng thái hóa đơn theo mã hóa đơn
    @Transactional
    public HoaDon updateTrangThaiByMa(
            String maHoaDon,
            Integer trangThai,
            String ghiChu
    ) {

        HoaDon hoaDon = hoaDonRepository.findByMaHoaDon(maHoaDon)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy hóa đơn có mã: " + maHoaDon
                ));

        long retryCount = countGiaoLaiDonHang(hoaDon.getId());
        String actionDesc = (ghiChu != null && !ghiChu.trim().isEmpty())
                ? ghiChu.trim()
                : ("Cập nhật trạng thái sang: " + getTenTrangThai(trangThai));

        if (Integer.valueOf(4).equals(trangThai)) {
            boolean isRetryAction = Integer.valueOf(6).equals(hoaDon.getTrangThai())
                    || (hoaDon.getGhiChu() != null && hoaDon.getGhiChu().toLowerCase().contains("giao hàng thất bại"))
                    || (ghiChu != null && ghiChu.toLowerCase().contains("giao lại"));
            if (isRetryAction) {
                if (retryCount >= 3) {
                    throw new RuntimeException("Đơn hàng đã được giao lại tối đa 3 lần. Vui lòng thực hiện hủy đơn!");
                }
                long nextRetry = Math.min(3, retryCount + 1);
                actionDesc = "Tiến hành giao lại đơn hàng (Lần " + nextRetry + "/3)";
                hoaDon.setTrangThai(4);
                hoaDon.setGhiChu(actionDesc);
            } else {
                hoaDon.setTrangThai(4);
                if (ghiChu != null && !ghiChu.trim().isEmpty()) {
                    hoaDon.setGhiChu(ghiChu.trim());
                }
            }
        } else if (Integer.valueOf(7).equals(trangThai)) {
            if (retryCount >= 3) {
                hoaDon.setTrangThai(6);
                String cancelMsg = "Đã giao lại tối đa 3 lần thất bại - Tự động hủy đơn hàng";
                if (ghiChu != null && !ghiChu.trim().isEmpty() && !ghiChu.toLowerCase().contains("tự động hủy")) {
                    cancelMsg += " (Lý do: " + ghiChu.trim() + ")";
                }
                hoaDon.setGhiChu(cancelMsg);
                actionDesc = cancelMsg;
            } else {
                hoaDon.setTrangThai(6);
                long currentFail = retryCount + 1;
                String failMsg = "Giao hàng thất bại (lần " + currentFail + "/3)";
                if (ghiChu != null && !ghiChu.trim().isEmpty()) {
                    String clean = ghiChu.trim();
                    if (!clean.toLowerCase().contains("giao hàng thất bại")) {
                        failMsg += ": " + clean;
                    }
                }
                hoaDon.setGhiChu(failMsg);
                actionDesc = failMsg;
            }
        } else {
            hoaDon.setTrangThai(trangThai);
            if (ghiChu != null && !ghiChu.trim().isEmpty()) {
                hoaDon.setGhiChu(ghiChu.trim());
            }
        }

        hoaDon.setNgayCapNhat(Instant.now());

        HoaDon saved = hoaDonRepository.save(hoaDon);

        try {
            LichSuHoaDon log = new LichSuHoaDon();
            log.setIdHoaDon(saved);
            log.setThoiGian(Instant.now());
            if (actionDesc.length() > 250) {
                actionDesc = actionDesc.substring(0, 250);
            }
            log.setHanhDong(actionDesc);
            String nguoiTh = saved.getNguoiCapNhat() != null ? saved.getNguoiCapNhat() : "Nhân viên";
            if (nguoiTh.length() > 95) nguoiTh = nguoiTh.substring(0, 95);
            log.setNguoiThucHien(nguoiTh);
            log.setVaiTroNguoiThucHien("Nhân viên");
            lichSuHoaDonRepository.save(log);
        } catch (Exception ignored) {}

        return saved;
    }

    // Cập nhật trạng thái thanh toán
    // 0: Chưa thanh toán
    // 1: Đã thanh toán
    @Transactional
    public HoaDon updateTrangThaiThanhToan(
            Long id,
            Integer trangThaiThanhToan
    ) {
        return updateTrangThaiThanhToan(id, trangThaiThanhToan, null);
    }

    @Transactional
    public HoaDon updateTrangThaiThanhToan(
            Long id,
            Integer trangThaiThanhToan,
            String ghiChu
    ) {

        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy hóa đơn có ID: " + id
                ));

        if (trangThaiThanhToan == null
            || (trangThaiThanhToan != 0 && trangThaiThanhToan != 1)) {

            throw new RuntimeException(
                    "Trạng thái thanh toán không hợp lệ. "
                    + "Phải là 0 hoặc 1."
            );
        }

        hoaDon.setTrangThaiThanhToan(trangThaiThanhToan);
        if (ghiChu != null && !ghiChu.trim().isEmpty()) {
            hoaDon.setGhiChu(ghiChu.trim());
        }
        hoaDon.setNgayCapNhat(Instant.now());

        HoaDon saved = hoaDonRepository.save(hoaDon);

        try {
            LichSuHoaDon log = new LichSuHoaDon();
            log.setIdHoaDon(saved);
            log.setThoiGian(Instant.now());
            String desc = (trangThaiThanhToan == 1) ? "Xác nhận đã thanh toán hóa đơn" : "Chuyển trạng thái sang chưa thanh toán";
            if (ghiChu != null && !ghiChu.trim().isEmpty()) {
                desc += " - " + ghiChu.trim();
            }
            if (desc.length() > 250) desc = desc.substring(0, 250);
            log.setHanhDong(desc);
            String nguoiTh = saved.getNguoiCapNhat() != null ? saved.getNguoiCapNhat() : "Nhân viên";
            if (nguoiTh.length() > 95) nguoiTh = nguoiTh.substring(0, 95);
            log.setNguoiThucHien(nguoiTh);
            log.setVaiTroNguoiThucHien("Nhân viên");
            lichSuHoaDonRepository.save(log);
        } catch (Exception ignored) {}

        return saved;
    }

    @Transactional
    public HoaDon updateTrangThaiThanhToanByMa(
            String maHoaDon,
            Integer trangThaiThanhToan,
            String ghiChu
    ) {
        HoaDon hoaDon = hoaDonRepository.findByMaHoaDon(maHoaDon)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn có mã: " + maHoaDon));
        return updateTrangThaiThanhToan(hoaDon.getId(), trangThaiThanhToan, ghiChu);
    }

    // Xóa hóa đơn theo ID
    @Transactional
    public void delete(Long id) {

        if (!hoaDonRepository.existsById(id)) {
            throw new RuntimeException(
                    "Không tìm thấy hóa đơn có ID: " + id
            );
        }

        hoaDonRepository.deleteById(id);
    }

    // Tạo và xử lý đơn hàng POS tại quầy kèm lưu sản phẩm chi tiết, trừ tồn kho và ghi nhận thanh toán
    @Transactional
    public HoaDon createHoaDonPos(HoaDonPosDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu đơn hàng POS không hợp lệ");
        }

        HoaDon hoaDon;

        // 1. Mã hóa đơn: Nếu hóa đơn đã tồn tại trong CSDL (hóa đơn chờ), cập nhật lại hóa đơn đó
        if (dto.getMaHoaDon() != null && !dto.getMaHoaDon().trim().isEmpty()) {
            Optional<HoaDon> existingOpt = hoaDonRepository.findByMaHoaDon(dto.getMaHoaDon().trim());
            if (existingOpt.isPresent()) {
                hoaDon = existingOpt.get();
                try {
                    List<ChiTietHoaDon> oldDetails = chiTietHoaDonRepository.findByIdHoaDon_Id(hoaDon.getId());
                    if (oldDetails != null && !oldDetails.isEmpty()) {
                        chiTietHoaDonRepository.deleteAll(oldDetails);
                    }
                } catch (Exception ignored) {}
            } else {
                hoaDon = new HoaDon();
                hoaDon.setMaHoaDon(dto.getMaHoaDon().trim());
            }
        } else {
            hoaDon = new HoaDon();
            hoaDon.setMaHoaDon(generateNextMaHoaDon());
        }

        // 2. Loại đơn (1: Tại quầy, 2: Giao hàng)
        Integer loaiDon = dto.getLoaiDon() != null ? dto.getLoaiDon() : 1;
        hoaDon.setLoaiDon(loaiDon);

        // 3. Trạng thái đơn & trạng thái thanh toán
        Integer trangThai = dto.getTrangThai();
        if (trangThai == null) {
            trangThai = (loaiDon == 1) ? 5 : 1; // 5: Hoàn thành tại quầy, 1: Chờ xử lý nếu giao hàng
        }
        hoaDon.setTrangThai(trangThai);

        Integer trangThaiThanhToan = dto.getTrangThaiThanhToan();
        if (trangThaiThanhToan == null) {
            trangThaiThanhToan = (loaiDon == 1) ? 1 : 0; // 1: Đã thanh toán
        }
        hoaDon.setTrangThaiThanhToan(trangThaiThanhToan);

        // 4. Khách hàng: Nếu không để lại thông tin, tự động note là "Khách lẻ"
        if (dto.getIdKhachHang() != null) {
            khachHangRepository.findById(dto.getIdKhachHang()).ifPresent(hoaDon::setIdKhachHang);
        } else if (dto.getSoDienThoaiKhachHang() != null && !dto.getSoDienThoaiKhachHang().trim().isEmpty()) {
            try {
                khachHangRepository.findBySoDienThoai(dto.getSoDienThoaiKhachHang().trim()).ifPresent(hoaDon::setIdKhachHang);
            } catch (Exception ignored) {}
        }

        String rawCust = dto.getTenKhachHang();
        String finalCustomerName = (rawCust != null && !rawCust.trim().isEmpty() && !rawCust.trim().equalsIgnoreCase("null") && !rawCust.trim().equals("---") && !rawCust.trim().equals("—"))
                ? rawCust.trim() : "Khách lẻ";

        hoaDon.setTenKhachHang(finalCustomerName);
        if (finalCustomerName.equalsIgnoreCase("Khách lẻ")) {
            hoaDon.setIdKhachHang(null);
        }
        hoaDon.setSoDienThoaiKhachHang(dto.getSoDienThoaiKhachHang() != null ? dto.getSoDienThoaiKhachHang().trim() : "");
        hoaDon.setDiaChiNhanHang(dto.getDiaChiNhanHang() != null && !dto.getDiaChiNhanHang().trim().isEmpty()
                ? dto.getDiaChiNhanHang().trim() : (loaiDon == 1 ? "Bán tại quầy" : ""));

        // 5. Phiếu giảm giá
        if (dto.getIdPhieuGiamGia() != null) {
            phieuGiamGiaRepository.findById(dto.getIdPhieuGiamGia()).ifPresent(hoaDon::setIdPhieuGiamGia);
        }

        // 6. Nhân viên tạo hóa đơn
        if (dto.getIdNhanVien() != null) {
            nhanVienRepository.findById(dto.getIdNhanVien()).ifPresent(hoaDon::setIdNhanVien);
        } else {
            nhanVienRepository.findTopByOrderByIdDesc().ifPresent(hoaDon::setIdNhanVien);
        }

        BigDecimal phiShip = dto.getPhiShip() != null ? dto.getPhiShip() : BigDecimal.ZERO;
        hoaDon.setPhiVanChuyen(phiShip);

        BigDecimal tongTien = dto.getTongTien() != null ? dto.getTongTien() : BigDecimal.ZERO;
        BigDecimal giamGia = dto.getTongTienGiamGia() != null ? dto.getTongTienGiamGia() : BigDecimal.ZERO;
        BigDecimal finalAmount = tongTien.add(phiShip).subtract(giamGia);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        hoaDon.setTongTien(tongTien);
        hoaDon.setTienSauGiamGia(finalAmount);
        hoaDon.setGhiChu(dto.getGhiChu() != null && !dto.getGhiChu().trim().isEmpty()
                ? dto.getGhiChu().trim() : (loaiDon == 1 ? "Bán hàng tại quầy" : "Đơn giao hàng"));
        hoaDon.setNgayTao(Instant.now());
        hoaDon.setNgayCapNhat(Instant.now());
        hoaDon.setNguoiTao("Nhân viên bán hàng");
        hoaDon.setNguoiCapNhat("Nhân viên bán hàng");

        HoaDon saved = hoaDonRepository.save(hoaDon);

        // 7. Lưu danh sách chi tiết hóa đơn & trừ tồn kho
        if (dto.getChiTietList() != null && !dto.getChiTietList().isEmpty()) {
            int itemIndex = 1;
            BigDecimal calculatedTotal = BigDecimal.ZERO;

            for (HoaDonPosDTO.ItemChiTietDTO item : dto.getChiTietList()) {
                Long variantId = item.getResolvedVariantId();
                if (variantId == null) continue;

                Optional<ChiTietSanPham> ctspOpt = chiTietSanPhamRepository.findById(variantId);
                if (ctspOpt.isEmpty()) continue;

                ChiTietSanPham ctsp = ctspOpt.get();
                int qty = item.getSoLuong() != null ? Math.max(1, item.getSoLuong()) : 1;
                BigDecimal unitPrice = item.getResolvedPrice();
                if (unitPrice.compareTo(BigDecimal.ZERO) <= 0 && ctsp.getGiaBan() != null) {
                    unitPrice = ctsp.getGiaBan();
                }

                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                calculatedTotal = calculatedTotal.add(lineTotal);

                ChiTietHoaDon cthd = new ChiTietHoaDon();
                cthd.setIdHoaDon(saved);
                cthd.setIdChiTietSanPham(ctsp);
                cthd.setMaHoaDonChiTiet(String.format("CTHD%s-%02d", saved.getMaHoaDon(), itemIndex++));
                cthd.setDonGia(unitPrice);
                cthd.setSoLuong(qty);
                cthd.setThanhTien(lineTotal);
                cthd.setTrangThai(1);
                chiTietHoaDonRepository.save(cthd);

                // Trừ tồn kho biến thể sản phẩm
                if (ctsp.getSoLuong() != null) {
                    ctsp.setSoLuong(Math.max(0, ctsp.getSoLuong() - qty));
                    chiTietSanPhamRepository.save(ctsp);
                }
            }

            // Cập nhật lại tổng tiền chính xác từ các mục chi tiết nếu cần
            if (calculatedTotal.compareTo(BigDecimal.ZERO) > 0 && (saved.getTongTien() == null || saved.getTongTien().compareTo(BigDecimal.ZERO) == 0)) {
                saved.setTongTien(calculatedTotal);
                BigDecimal recalculatedFinal = calculatedTotal.add(saved.getPhiVanChuyen()).subtract(giamGia);
                if (recalculatedFinal.compareTo(BigDecimal.ZERO) < 0) recalculatedFinal = BigDecimal.ZERO;
                saved.setTienSauGiamGia(recalculatedFinal);
                saved = hoaDonRepository.save(saved);
            }
        }

        // 8. Lưu thông tin hình thức thanh toán
        String ptName = dto.getPhuongThucThanhToan() != null && !dto.getPhuongThucThanhToan().trim().isEmpty()
                ? dto.getPhuongThucThanhToan().trim() : "Tiền mặt";
        try {
            List<PhuongThucThanhToan> allPts = phuongThucThanhToanRepository.findAll();
            PhuongThucThanhToan matchedPt = null;
            for (PhuongThucThanhToan p : allPts) {
                if (p.getTenPhuongThuc() != null && p.getTenPhuongThuc().equalsIgnoreCase(ptName)) {
                    matchedPt = p;
                    break;
                }
            }
            if (matchedPt == null && !allPts.isEmpty()) {
                matchedPt = allPts.get(0);
            }

            if (matchedPt != null) {
                HinhThucThanhToan httt = new HinhThucThanhToan();
                httt.setIdHoaDon(saved);
                httt.setIdPhuongThuc(matchedPt);
                httt.setSoTien(saved.getTienSauGiamGia() != null ? saved.getTienSauGiamGia() : saved.getTongTien());
                httt.setMaGiaoDich("POS-" + System.currentTimeMillis());
                httt.setThoiGian(Instant.now());
                httt.setTrangThai(1);
                hinhThucThanhToanRepository.save(httt);
            }
        } catch (Exception ignored) {}

        // 9. Lưu lịch sử đơn hàng
        try {
            LichSuHoaDon log1 = new LichSuHoaDon();
            log1.setIdHoaDon(saved);
            log1.setTrangThai(1);
            log1.setThoiGian(saved.getNgayTao());
            log1.setGhiChu(loaiDon == 1 ? "Tạo hóa đơn tại quầy" : "Tạo đơn hàng giao");
            log1.setNguoiThucHien("Nhân viên bán hàng");
            log1.setVaiTroNguoiThucHien("Nhân viên");
            lichSuHoaDonRepository.save(log1);

            if (saved.getTrangThai() == 5) {
                LichSuHoaDon log2 = new LichSuHoaDon();
                log2.setIdHoaDon(saved);
                log2.setTrangThai(5);
                log2.setThoiGian(Instant.now());
                log2.setGhiChu("Hoàn thành đơn hàng tại quầy");
                log2.setNguoiThucHien("Nhân viên bán hàng");
                log2.setVaiTroNguoiThucHien("Nhân viên");
                lichSuHoaDonRepository.save(log2);
            }
        } catch (Exception ignored) {}

        return saved;
    }
}