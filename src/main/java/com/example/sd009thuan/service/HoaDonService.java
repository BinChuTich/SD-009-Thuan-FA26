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
            hoaDonCu.setTenKhachHang(isKhachLe ? "Khách lẻ" : ten);
            if (isKhachLe) {
                hoaDonCu.setIdKhachHang(null);
            }
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
    @Transactional
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

        HoaDon hoaDon = new HoaDon();

        // 1. Mã hóa đơn: Đảm bảo không trùng với bất kỳ hóa đơn nào trong danh sách
        if (dto.getMaHoaDon() != null && !dto.getMaHoaDon().trim().isEmpty() && hoaDonRepository.findByMaHoaDon(dto.getMaHoaDon().trim()).isEmpty()) {
            hoaDon.setMaHoaDon(dto.getMaHoaDon().trim());
        } else {
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