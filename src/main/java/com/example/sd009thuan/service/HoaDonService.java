package com.example.sd009thuan.service;



import com.example.sd009thuan.dto.DatHangOnlineDTO;
import com.example.sd009thuan.dto.KhachSuaDonDTO;



import com.example.sd009thuan.dto.HoaDonPosDTO;



import com.example.sd009thuan.entity.ChiTietHoaDon;



import com.example.sd009thuan.entity.ChiTietSanPham;



import com.example.sd009thuan.entity.HinhAnh;



import com.example.sd009thuan.entity.HinhThucThanhToan;



import com.example.sd009thuan.entity.HoaDon;



import com.example.sd009thuan.entity.KhachHang;



import com.example.sd009thuan.entity.LichSuHoaDon;



import com.example.sd009thuan.entity.NhanVien;



import com.example.sd009thuan.entity.PhuongThucThanhToan;



import com.example.sd009thuan.repository.ChiTietHoaDonRepository;



import com.example.sd009thuan.repository.ChiTietSanPhamRepository;



import com.example.sd009thuan.repository.HinhAnhRepository;



import com.example.sd009thuan.repository.HinhThucThanhToanRepository;



import com.example.sd009thuan.repository.HoaDonRepository;



import com.example.sd009thuan.repository.KhachHangRepository;



import com.example.sd009thuan.repository.LichSuHoaDonRepository;



import com.example.sd009thuan.repository.NhanVienRepository;



import com.example.sd009thuan.entity.PhieuGiamGia;



import java.util.ArrayList;



import com.example.sd009thuan.repository.PhieuGiamGiaRepository;



import com.example.sd009thuan.repository.PhuongThucThanhToanRepository;



import org.springframework.stereotype.Service;



import org.springframework.transaction.annotation.Transactional;



import java.math.BigDecimal;



import java.time.Instant;

import java.time.temporal.ChronoUnit;



import java.time.LocalDate;



import java.time.ZoneId;



import org.springframework.scheduling.annotation.Scheduled;



import org.springframework.context.event.EventListener;



import org.springframework.boot.context.event.ApplicationReadyEvent;



import java.util.List;



import java.util.Map;



import java.util.HashMap;



import java.util.LinkedHashMap;



import java.util.Set;



import java.util.HashSet;



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



    @org.springframework.beans.factory.annotation.Autowired(required = false)



    private EmailService emailService;



    @org.springframework.beans.factory.annotation.Autowired(required = false)



    private com.example.sd009thuan.repository.LichSuCaLamViecRepository lichSuCaLamViecRepository;



    @org.springframework.beans.factory.annotation.Autowired(required = false)



    private com.example.sd009thuan.service.DotGiamGiaService dotGiamGiaService;



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



        // Chuẩn hóa loại đơn: Bất kể đơn giao hàng nhưng phát sinh tại quầy thì luôn là Tại quầy (1)



        if (Integer.valueOf(2).equals(hoaDon.getLoaiDon())) {



            boolean isStoreCreated = false; if (false) isStoreCreated = hoaDon.getIdNhanVien() != null



                                                                        || (hoaDon.getNguoiTao() != null && !hoaDon.getNguoiTao().isBlank())



                                                                        || (hoaDon.getGhiChu() != null && (hoaDon.getGhiChu().toLowerCase().contains("pos") || hoaDon.getGhiChu().toLowerCase().contains("quầy")));



            if (isStoreCreated) {



                hoaDon.setLoaiDon(1);



                try {



                    hoaDonRepository.save(hoaDon);



                } catch (Exception ignored) {}



            }



        }



        try {



            String noteLower = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu().toLowerCase() : "";

            if (noteLower.contains("website") || noteLower.contains("online")) {

                if (!Integer.valueOf(2).equals(hoaDon.getLoaiDon())) {

                    hoaDon.setLoaiDon(2);

                    try { hoaDonRepository.save(hoaDon); } catch (Exception ignored) {}

                }

            }

            if (false && hoaDon.getIdKhachHang() == null && hoaDon.getSoDienThoaiKhachHang() != null && !hoaDon.getSoDienThoaiKhachHang().trim().isEmpty()) {

                khachHangRepository.findBySoDienThoai(hoaDon.getSoDienThoaiKhachHang().trim()).ifPresent(kh -> {

                    hoaDon.setIdKhachHang(kh);

                    try { hoaDonRepository.save(hoaDon); } catch (Exception ignored) {}

                });

            }

            if (hoaDon.getEmail() == null || hoaDon.getEmail().trim().isEmpty()) {

                String e = hoaDon.getEmailKhachHang();

                if (e != null && !e.trim().isEmpty()) {

                    hoaDon.setEmail(e.trim());

                }

            }

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



                               && Integer.valueOf(1).equals(h.getTrangThai());



        boolean isWaitingNote = h.getGhiChu() != null && h.getGhiChu().toLowerCase().contains("hóa đơn chờ");



        return isStoreDraft || isWaitingNote;



    }



    // Lấy danh sách tất cả hóa đơn (loại trừ các hóa đơn chờ tại quầy)



    public List<HoaDon> getAll() {



        try {



            cancelExpiredWaitingInvoices();



        } catch (Exception ignored) {}



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



        if (opt.isPresent()) {

            HoaDon hd = opt.get();

            if (isExpiredVnPayOrder(hd)) {

                cancelSingleVnPayOrder(hd, "Hệ thống tự động hủy đơn do quá hạn thanh toán VNPAY (5 phút)");

            }

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



        if (opt.isPresent()) {

            HoaDon hd = opt.get();

            if (isExpiredVnPayOrder(hd)) {

                cancelSingleVnPayOrder(hd, "Hệ thống tự động hủy đơn do quá hạn thanh toán VNPAY (5 phút)");

            }

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



        List<LichSuHoaDon> list = lichSuHoaDonRepository.findByIdHoaDon_IdOrderByThoiGianDesc(idHoaDon);



        enrichLichSuNguoiThucHien(list);



        return list;



    }



    // Lấy lịch sử thao tác hóa đơn theo Mã



    public List<LichSuHoaDon> getLichSuByMaHoaDon(String maHoaDon) {



        List<LichSuHoaDon> list = lichSuHoaDonRepository.findByIdHoaDon_MaHoaDonOrderByThoiGianDesc(maHoaDon);



        enrichLichSuNguoiThucHien(list);



        return list;



    }



    private void enrichLichSuNguoiThucHien(List<LichSuHoaDon> list) {



        if (list == null) return;



        for (LichSuHoaDon log : list) {



            String actor = log.getNguoiThucHien();



            if (actor == null || actor.trim().isEmpty() || "Nhân viên bán hàng".equals(actor) || "Nhân viên".equals(actor)) {



                if (log.getIdHoaDon() != null) {



                    HoaDon hd = log.getIdHoaDon();



                    String realName = (hd.getIdNhanVien() != null && hd.getIdNhanVien().getTenNhanVien() != null && !hd.getIdNhanVien().getTenNhanVien().trim().isEmpty())



                            ? hd.getIdNhanVien().getTenNhanVien()



                            : (hd.getNguoiTao() != null && !hd.getNguoiTao().trim().isEmpty() && !hd.getNguoiTao().equals("Nhân viên bán hàng") ? hd.getNguoiTao() : null);



                    if (realName != null) {



                        log.setNguoiThucHien(realName);



                    }



                }



            }



        }



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



    @Transactional



    public List<HoaDon> cancelExpiredWaitingInvoices() {



        ZoneId zoneVN = ZoneId.of("Asia/Ho_Chi_Minh");



        LocalDate today = LocalDate.now(zoneVN);



        Instant startOfToday = today.atStartOfDay(zoneVN).toInstant();



        List<HoaDon> allInvoices = hoaDonRepository.findAll();



        List<HoaDon> expiredWaitingInvoices = allInvoices.stream()



                .filter(h -> (isWaitingInvoice(h) || (Integer.valueOf(1).equals(h.getLoaiDon()) && Integer.valueOf(1).equals(h.getTrangThai()) && (h.getTrangThaiThanhToan() == null || Integer.valueOf(0).equals(h.getTrangThaiThanhToan())))))



                .filter(h -> h.getNgayTao() != null && h.getNgayTao().isBefore(startOfToday))



                .toList();



        if (expiredWaitingInvoices.isEmpty()) {



            return List.of();



        }



        for (HoaDon hd : expiredWaitingInvoices) {



            hd.setTrangThai(6); // 6: Đã hủy



            hd.setNgayCapNhat(Instant.now());



            hd.setNguoiCapNhat("Hệ thống");



            // Hoàn lại số lượng tồn kho cho các sản phẩm trong hóa đơn chờ bị hủy



            try {



                List<ChiTietHoaDon> details = chiTietHoaDonRepository.findByIdHoaDon_Id(hd.getId());



                if (details != null && !details.isEmpty()) {



                    for (ChiTietHoaDon cthd : details) {



                        if (cthd.getIdChiTietSanPham() != null && cthd.getIdChiTietSanPham().getId() != null) {



                            Long vid = cthd.getIdChiTietSanPham().getId();



                            int qty = cthd.getSoLuong() != null ? cthd.getSoLuong() : 0;



                            if (qty > 0) {



                                Optional<ChiTietSanPham> ctspOpt = chiTietSanPhamRepository.findById(vid);



                                if (ctspOpt.isPresent()) {



                                    ChiTietSanPham ctsp = ctspOpt.get();



                                    int curStock = ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0;



                                    ctsp.setSoLuong(curStock + qty);



                                    chiTietSanPhamRepository.save(ctsp);



                                }



                            }



                        }



                    }



                }



            } catch (Exception ex) {



                System.err.println("Lỗi hoàn tồn kho khi hủy hóa đơn chờ cũ: " + ex.getMessage());



            }



            String currentNote = hd.getGhiChu() != null ? hd.getGhiChu().trim() : "";



            String cancelReason = "Tự động hủy hóa đơn chờ sang ngày mới (" + today + ")";



            if (!currentNote.isEmpty() && !currentNote.contains("Tự động hủy")) {



                hd.setGhiChu(currentNote + " - " + cancelReason);



            } else if (currentNote.isEmpty()) {



                hd.setGhiChu(cancelReason);



            }



            hoaDonRepository.save(hd);



            // Ghi nhật ký vào LichSuHoaDon



            try {



                LichSuHoaDon log = new LichSuHoaDon();



                log.setIdHoaDon(hd);



                log.setTrangThai(6);



                log.setThoiGian(Instant.now());



                log.setHanhDong(cancelReason);



                log.setNguoiThucHien("Hệ thống");



                log.setVaiTroNguoiThucHien("Hệ thống");



                lichSuHoaDonRepository.save(log);



            } catch (Exception ex) {



                System.err.println("Lỗi ghi log lịch sử hủy hóa đơn chờ: " + ex.getMessage());



            }



        }



        System.out.println("==> Đã tự động hủy " + expiredWaitingInvoices.size() + " hóa đơn chờ của ngày trước sang ngày mới " + today);



        return expiredWaitingInvoices;



    }



    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Ho_Chi_Minh")



    /**

     * Tự động quét và hủy các đơn hàng online thanh toán qua VNPAY nhưng chưa thanh toán sau 5 phút.

     * Hoàn lại số lượng tồn kho sản phẩm, ghi nhận lịch sử đơn hàng và cập nhật trạng thái là Đã hủy (6).

     */

    @Scheduled(fixedDelay = 15000)

    @Transactional

    public List<HoaDon> autoCancelExpiredVnPayOrders() {

        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);

        List<HoaDon> allInvoices = hoaDonRepository.findAll();

        List<HoaDon> expiredOrders = allInvoices.stream()

                .filter(h -> (Integer.valueOf(2).equals(h.getLoaiDon()) || (h.getLoaiDon() != null && h.getLoaiDon() != 1)))

                .filter(h -> Integer.valueOf(1).equals(h.getTrangThai()))

                .filter(h -> h.getTrangThaiThanhToan() == null || Integer.valueOf(0).equals(h.getTrangThaiThanhToan()))

                .filter(this::isVnPayOrder)

                .filter(h -> h.getNgayTao() != null && h.getNgayTao().isBefore(fiveMinutesAgo))

                .toList();



        if (expiredOrders.isEmpty()) {

            return List.of();

        }



        for (HoaDon hd : expiredOrders) {

            cancelSingleVnPayOrder(hd, "Hệ thống tự động hủy đơn do quá hạn thanh toán VNPAY (5 phút)");

        }



        return expiredOrders;

    }



    public boolean isVnPayOrder(HoaDon h) {

        if (h == null) return false;

        String note = (h.getGhiChu() != null ? h.getGhiChu() : "").toUpperCase();

        if (note.contains("VNPAY")) {

            return true;

        }

        try {

            List<HinhThucThanhToan> htttList = hinhThucThanhToanRepository.findByIdHoaDon_Id(h.getId());

            if (htttList != null) {

                for (HinhThucThanhToan httt : htttList) {

                    if (httt.getMaGiaoDich() != null && httt.getMaGiaoDich().toUpperCase().contains("VNPAY")) {

                        return true;

                    }

                    if (httt.getIdPhuongThuc() != null && httt.getIdPhuongThuc().getTenPhuongThuc() != null) {

                        if (httt.getIdPhuongThuc().getTenPhuongThuc().toUpperCase().contains("VNPAY")) {

                            return true;

                        }

                    }

                }

            }

        } catch (Exception ignored) {}

        return false;

    }



    public boolean isExpiredVnPayOrder(HoaDon h) {

        if (h == null) return false;

        if (!Integer.valueOf(1).equals(h.getTrangThai())) {

            return false;

        }

        if (h.getTrangThaiThanhToan() != null && h.getTrangThaiThanhToan() == 1) {

            return false;

        }

        if (!isVnPayOrder(h)) {

            return false;

        }

        if (h.getNgayTao() == null) {

            return false;

        }

        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);

        return h.getNgayTao().isBefore(fiveMinutesAgo);

    }



    @Transactional

    public void cancelSingleVnPayOrder(HoaDon hd, String cancelReason) {

        if (hd == null || Integer.valueOf(6).equals(hd.getTrangThai())) {

            return;

        }

        hd.setTrangThai(6); // 6: Đã hủy

        hd.setNgayCapNhat(Instant.now());

        hd.setNguoiCapNhat("Hệ thống");



        // Hoàn lại tồn kho

        try {

            List<ChiTietHoaDon> details = chiTietHoaDonRepository.findByIdHoaDon_Id(hd.getId());

            if (details != null && !details.isEmpty()) {

                for (ChiTietHoaDon cthd : details) {

                    if (cthd.getIdChiTietSanPham() != null && cthd.getIdChiTietSanPham().getId() != null) {

                        Long vid = cthd.getIdChiTietSanPham().getId();

                        int qty = cthd.getSoLuong() != null ? cthd.getSoLuong() : 0;

                        if (qty > 0) {

                            Optional<ChiTietSanPham> ctspOpt = chiTietSanPhamRepository.findById(vid);

                            if (ctspOpt.isPresent()) {

                                ChiTietSanPham ctsp = ctspOpt.get();

                                int curStock = ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0;

                                ctsp.setSoLuong(curStock + qty);

                                chiTietSanPhamRepository.save(ctsp);

                            }

                        }

                    }

                }

            }

        } catch (Exception ex) {

            System.err.println("Lỗi hoàn tồn kho khi hủy đơn VNPAY quá hạn: " + ex.getMessage());

        }



        String currentNote = hd.getGhiChu() != null ? hd.getGhiChu().trim() : "";

        if (!currentNote.isEmpty() && !currentNote.contains("Tự động hủy")) {

            hd.setGhiChu(currentNote + " - " + cancelReason);

        } else if (currentNote.isEmpty()) {

            hd.setGhiChu(cancelReason);

        }

        hoaDonRepository.save(hd);



        // Ghi nhật ký vào LichSuHoaDon

        try {

            LichSuHoaDon log = new LichSuHoaDon();

            log.setIdHoaDon(hd);

            log.setTrangThai(6);

            log.setThoiGian(Instant.now());

            log.setHanhDong(cancelReason);

            log.setGhiChu(cancelReason);

            log.setNguoiThucHien("Hệ thống");

            log.setVaiTroNguoiThucHien("Hệ thống");

            lichSuHoaDonRepository.save(log);

        } catch (Exception ex) {

            System.err.println("Lỗi ghi log lịch sử hủy đơn VNPAY: " + ex.getMessage());

        }

        System.out.println("==> [VNPAY AUTO CANCEL] Đã tự động hủy đơn hàng #" + hd.getMaHoaDon() + " do quá hạn 5 phút chưa thanh toán");

    }



    public void scheduleCancelExpiredWaitingInvoices() {



        cancelExpiredWaitingInvoices();



    }



    @EventListener(ApplicationReadyEvent.class)



    public void onApplicationReady() {

        try {

            autoCancelExpiredVnPayOrders();

        } catch (Exception ignored) {}



        try {



            cancelExpiredWaitingInvoices();



        } catch (Exception e) {



            System.err.println("Lỗi kiểm tra hóa đơn chờ ngày cũ khi khởi động: " + e.getMessage());



        }



    }



    // Lấy danh sách hóa đơn chờ tại quầy



    public List<HoaDon> getWaitingInvoices() {



        try {



            cancelExpiredWaitingInvoices();



        } catch (Exception e) {



            System.err.println("Lỗi tự động hủy hóa đơn chờ cũ trong getWaitingInvoices: " + e.getMessage());



        }



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



        return createWaitingInvoice(null, null, null);



    }



    @Transactional



    public HoaDon createWaitingInvoice(Long idNhanVien, String nguoiTao, Long idGiaoCa) {



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



        if (idNhanVien != null) {



            nhanVienRepository.findById(idNhanVien).ifPresent(nv -> {



                hd.setIdNhanVien(nv);



                if (nv.getTenNhanVien() != null && !nv.getTenNhanVien().isEmpty() && (nguoiTao == null || nguoiTao.trim().isEmpty())) {



                    hd.setNguoiTao(nv.getTenNhanVien());



                }



            });



        }



        if (hd.getIdNhanVien() == null) {



            nhanVienRepository.findTopByOrderByIdDesc().ifPresent(nv -> {



                hd.setIdNhanVien(nv);



                if (nv.getTenNhanVien() != null && !nv.getTenNhanVien().isEmpty()) {



                    hd.setNguoiTao(nv.getTenNhanVien());



                }



            });



        }



        if (idGiaoCa != null && lichSuCaLamViecRepository != null) {



            lichSuCaLamViecRepository.findById(idGiaoCa).ifPresent(hd::setIdGiaoCa);



        } else if (hd.getIdNhanVien() != null && lichSuCaLamViecRepository != null) {



            lichSuCaLamViecRepository.findTopByNhanVienIdAndTrangThaiOrderByThoiGianMoCaDesc(hd.getIdNhanVien().getId(), 1)



                    .ifPresent(hd::setIdGiaoCa);



        }



        HoaDon saved = hoaDonRepository.save(hd);



        try {



            LichSuHoaDon log1 = new LichSuHoaDon();



            log1.setIdHoaDon(saved);



            log1.setTrangThai(1);



            log1.setThoiGian(saved.getNgayTao());



            log1.setGhiChu("Tạo hóa đơn tại quầy");



            log1.setNguoiThucHien(saved.getNguoiTao());



            log1.setVaiTroNguoiThucHien("Nhân viên");



            lichSuHoaDonRepository.save(log1);



        } catch (Exception ignored) {}



        return saved;



    }



    // Đồng bộ danh sách sản phẩm của hóa đơn chờ và trừ / hoàn tồn kho tương ứng



    @Transactional



    public List<ChiTietHoaDon> syncCartToInvoice(Long idHoaDon, List<HoaDonPosDTO.ItemChiTietDTO> items) {



        List<ChiTietHoaDon> oldDetails = chiTietHoaDonRepository.findByIdHoaDon_Id(idHoaDon);



        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon)



                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn ID: " + idHoaDon));



        // Lấy chi tiết cũ để tính chênh lệch tồn kho



        Map<Long, Integer> oldQtyMap = new HashMap<>();



        if (oldDetails != null) {



            for (ChiTietHoaDon c : oldDetails) {



                if (c.getIdChiTietSanPham() != null && c.getIdChiTietSanPham().getId() != null) {



                    Long vid = c.getIdChiTietSanPham().getId();



                    int q = c.getSoLuong() != null ? c.getSoLuong() : 0;



                    oldQtyMap.put(vid, oldQtyMap.getOrDefault(vid, 0) + q);



                }



            }



        }



        Map<Long, Integer> newQtyMap = new HashMap<>();



        if (items != null) {



            for (HoaDonPosDTO.ItemChiTietDTO item : items) {



                Long vid = item.getResolvedVariantId();



                if (vid != null) {



                    int q = item.getSoLuong() != null ? Math.max(1, item.getSoLuong()) : 1;



                    newQtyMap.put(vid, newQtyMap.getOrDefault(vid, 0) + q);



                }



            }



        }



        // 1. Kiểm tra tồn kho khả dụng trước khi điều chỉnh (diff = newQty - oldQty)



        Set<Long> allVariantIds = new HashSet<>(oldQtyMap.keySet());



        allVariantIds.addAll(newQtyMap.keySet());



        for (Long vid : allVariantIds) {



            int oldQ = oldQtyMap.getOrDefault(vid, 0);



            int newQ = newQtyMap.getOrDefault(vid, 0);



            int diff = newQ - oldQ;



            if (diff > 0) {



                Optional<ChiTietSanPham> ctspOpt = chiTietSanPhamRepository.findById(vid);



                if (ctspOpt.isPresent()) {



                    ChiTietSanPham ctsp = ctspOpt.get();



                    int curStock = ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0;



                    if (diff > curStock) {



                        String spName = ctsp.getIdSanPham() != null ? ctsp.getIdSanPham().getTenSanPham() : "Sản phẩm";



                        throw new IllegalArgumentException("Sản phẩm '" + spName + "' không đủ số lượng trong kho (chỉ còn tối đa " + (curStock + oldQ) + ")!");



                    }



                }



            }



        }



        // 2. Cập nhật tồn kho dựa trên chênh lệch



        for (Long vid : allVariantIds) {



            int oldQ = oldQtyMap.getOrDefault(vid, 0);



            int newQ = newQtyMap.getOrDefault(vid, 0);



            int diff = newQ - oldQ;



            if (diff != 0) {



                Optional<ChiTietSanPham> ctspOpt = chiTietSanPhamRepository.findById(vid);



                if (ctspOpt.isPresent()) {



                    ChiTietSanPham ctsp = ctspOpt.get();



                    int curStock = ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0;



                    ctsp.setSoLuong(Math.max(0, curStock - diff));



                    chiTietSanPhamRepository.save(ctsp);



                }



            }



        }



        // 3. Xóa chi tiết cũ sau khi đã tính toán điều chỉnh tồn kho



        if (oldDetails != null && !oldDetails.isEmpty()) {



            if (hoaDon.getChiTietHoaDons() != null) {



                hoaDon.getChiTietHoaDons().clear();



            }



            chiTietHoaDonRepository.deleteAll(oldDetails);



            chiTietHoaDonRepository.flush();



        }



        // 4. Tạo chi tiết hóa đơn mới (Gộp các item có cùng variantId VÀ cùng đơn giá; giữ nguyên các dòng khác đơn giá)



        BigDecimal calculatedTotal = BigDecimal.ZERO;



        if (items != null && !items.isEmpty()) {



            Map<String, Integer> mergedQtyMap = new LinkedHashMap<>();

            Map<String, Long> keyToVariantIdMap = new HashMap<>();

            Map<String, BigDecimal> priceMap = new HashMap<>();



            for (HoaDonPosDTO.ItemChiTietDTO it : items) {



                Long vid = it.getResolvedVariantId();



                if (vid == null) continue;



                int q = it.getSoLuong() != null ? Math.max(1, it.getSoLuong()) : 1;

                BigDecimal resolvedP = (it.getResolvedPrice() != null && it.getResolvedPrice().compareTo(BigDecimal.ZERO) > 0)
                        ? it.getResolvedPrice()
                        : BigDecimal.ZERO;

                String mergeKey = vid + "_" + resolvedP.stripTrailingZeros().toPlainString();



                mergedQtyMap.put(mergeKey, mergedQtyMap.getOrDefault(mergeKey, 0) + q);

                keyToVariantIdMap.put(mergeKey, vid);



                if (resolvedP.compareTo(BigDecimal.ZERO) > 0) {



                    priceMap.put(mergeKey, resolvedP);



                }



            }



            int itemIndex = 1;



            for (Map.Entry<String, Integer> entry : mergedQtyMap.entrySet()) {



                String mergeKey = entry.getKey();

                Long variantId = keyToVariantIdMap.get(mergeKey);



                int qty = entry.getValue();

                if (variantId == null) continue;



                Optional<ChiTietSanPham> ctspOpt = chiTietSanPhamRepository.findById(variantId);



                if (ctspOpt.isEmpty()) continue;



                ChiTietSanPham ctsp = ctspOpt.get();



                BigDecimal unitPrice = priceMap.get(mergeKey);



                if ((unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) && ctsp.getGiaBan() != null) {



                    unitPrice = ctsp.getGiaBan();



                }



                if (unitPrice == null) unitPrice = BigDecimal.ZERO;



                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));



                calculatedTotal = calculatedTotal.add(lineTotal);



                ChiTietHoaDon cthd = new ChiTietHoaDon();



                cthd.setIdHoaDon(hoaDon);



                cthd.setIdChiTietSanPham(ctsp);



                cthd.setMaHoaDonChiTiet(String.format("CTHD%s-%02d-%04d", hoaDon.getMaHoaDon(), itemIndex++, (int)(System.currentTimeMillis() % 10000)));



                cthd.setDonGia(unitPrice);



                cthd.setSoLuong(qty);



                cthd.setThanhTien(lineTotal);



                cthd.setTrangThai(1);



                if (hoaDon.getChiTietHoaDons() != null) {



                    hoaDon.getChiTietHoaDons().add(cthd);



                }



                chiTietHoaDonRepository.save(cthd);



            }



            chiTietHoaDonRepository.flush();



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



            if (hoaDon.getNgayTao() == null) {



                hoaDon.setNgayTao(Instant.now());



            }



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
            Integer oldStatusInUpdate = hoaDonCu.getTrangThai();
            if (Integer.valueOf(2).equals(hoaDonMoi.getTrangThai()) && (oldStatusInUpdate == null || Integer.valueOf(1).equals(oldStatusInUpdate))) {
                checkStockAndDeductForConfirmation(hoaDonCu);
            }
            if ((Integer.valueOf(6).equals(hoaDonMoi.getTrangThai()) || Integer.valueOf(7).equals(hoaDonMoi.getTrangThai()))
                && oldStatusInUpdate != null && oldStatusInUpdate >= 2 && oldStatusInUpdate != 6) {
                refundStockForCancellation(hoaDonCu);
            }

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



            if (Integer.valueOf(1).equals(hoaDonMoi.getTrangThaiThanhToan()) && hoaDonMoi.getTrangThai() == null) {



                boolean isStoreOrder = Integer.valueOf(1).equals(hoaDonCu.getLoaiDon())



                                       && (hoaDonCu.getPhiVanChuyen() == null || hoaDonCu.getPhiVanChuyen().compareTo(BigDecimal.ZERO) == 0)



                                       && (hoaDonCu.getDiaChiNhanHang() == null || hoaDonCu.getDiaChiNhanHang().trim().isEmpty() || hoaDonCu.getDiaChiNhanHang().equalsIgnoreCase("Bán tại quầy"));



                if (isStoreOrder && (hoaDonCu.getTrangThai() == null || hoaDonCu.getTrangThai() == 1)) {



                    hoaDonCu.setTrangThai(5);



                }



            }



        }



        if (hoaDonMoi.getGhiChu() != null && !Integer.valueOf(7).equals(hoaDonMoi.getTrangThai())) {



            hoaDonCu.setGhiChu(hoaDonMoi.getGhiChu().trim());



        }



        hoaDonCu.setNgayCapNhat(Instant.now());



        HoaDon saved = hoaDonRepository.save(hoaDonCu);



        // Nếu chuyển sang trạng thái 2 (Đã xác nhận), gửi email xác nhận đơn hàng cho khách nếu có email



        if (Integer.valueOf(2).equals(hoaDonMoi.getTrangThai())) {



            String recipient = (hoaDonMoi.getEmail() != null && !hoaDonMoi.getEmail().trim().isEmpty())



                    ? hoaDonMoi.getEmail().trim()



                    : (saved.getEmail() != null ? saved.getEmail().trim() : null);



            if (recipient != null && recipient.contains("@") && emailService != null) {



                try {



                    List<ChiTietHoaDon> items = getChiTietByHoaDonId(saved.getId());



                    emailService.sendOrderConfirmationEmail(saved, items, recipient);



                } catch (Exception ex) {



                    System.err.println("==> Lỗi kích hoạt gửi email xác nhận: " + ex.getMessage());



                }



            }



        }



        try {



            LichSuHoaDon log = new LichSuHoaDon();



            log.setIdHoaDon(saved);



            log.setTrangThai(saved.getTrangThai());



            log.setThoiGian(Instant.now());



            String desc = "Cập nhật thông tin đơn hàng / khách hàng";



            if (hoaDonMoi.getGhiChu() != null && !hoaDonMoi.getGhiChu().trim().isEmpty() && !hoaDonMoi.getGhiChu().trim().equalsIgnoreCase("Giao hàng thất bại")) {



                desc += ": " + hoaDonMoi.getGhiChu().trim();



            }



            if (desc.length() > 250) desc = desc.substring(0, 250);



            log.setHanhDong(desc);



            String nguoiTh = (saved.getNguoiCapNhat() != null && !saved.getNguoiCapNhat().equals("Nhân viên bán hàng") && !saved.getNguoiCapNhat().equals("Nhân viên"))



                    ? saved.getNguoiCapNhat()



                    : ((saved.getIdNhanVien() != null && saved.getIdNhanVien().getTenNhanVien() != null)



                    ? saved.getIdNhanVien().getTenNhanVien()



                    : (saved.getNguoiTao() != null && !saved.getNguoiTao().isEmpty() && !saved.getNguoiTao().equals("Nhân viên bán hàng") ? saved.getNguoiTao() : "Nhân viên"));



            if (nguoiTh.length() > 95) nguoiTh = nguoiTh.substring(0, 95);



            log.setNguoiThucHien(nguoiTh);



            log.setVaiTroNguoiThucHien("Nhân viên");



            lichSuHoaDonRepository.save(log);



        } catch (Exception ignored) {}



        return saved;



    }



    // Cập nhật đơn vị vận chuyển

    @Transactional

    public HoaDon updateDonViVanChuyen(Long id, String carrier) {

        HoaDon hoaDon = hoaDonRepository.findById(id)

                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn có ID: " + id));

        hoaDon.setDonViVanChuyen(carrier);

        String currentNote = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : "";

        if (carrier != null && !carrier.trim().isEmpty()) {

            String tag = "[ĐVVC: " + carrier.trim() + "]";

            if (currentNote.contains("[ĐVVC:")) {

                currentNote = currentNote.replaceAll("\\[ĐVVC:[^\\]]*\\]", tag);

            } else {

                currentNote = currentNote.isEmpty() ? tag : (currentNote + " " + tag);

            }

        }

        hoaDon.setGhiChu(currentNote);

        hoaDon.setNgayCapNhat(Instant.now());

        HoaDon saved = hoaDonRepository.saveAndFlush(hoaDon);

        try {

            LichSuHoaDon log = new LichSuHoaDon();

            log.setIdHoaDon(saved);

            log.setTrangThai(saved.getTrangThai());

            log.setThoiGian(Instant.now());

            log.setHanhDong("Cập nhật đơn vị vận chuyển: " + (carrier != null ? carrier : ""));

            log.setNguoiThucHien("Nhân viên");

            log.setVaiTroNguoiThucHien("Nhân viên");

            lichSuHoaDonRepository.save(log);

        } catch (Exception ignored) {}

        return saved;

    }



    @Transactional

    public HoaDon updateDonViVanChuyen(Integer id, String carrier) {

        return updateDonViVanChuyen(id != null ? id.longValue() : null, carrier);

    }



    @Transactional

    public HoaDon updateDonViVanChuyenByMa(String maHoaDon, String carrier) {

        HoaDon hoaDon = hoaDonRepository.findByMaHoaDon(maHoaDon)

                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn có mã: " + maHoaDon));

        return updateDonViVanChuyen(hoaDon.getId(), carrier);

    }



    // Cập nhật nhanh trạng thái hóa đơn theo ID



    @Transactional



    public HoaDon updateTrangThai(Long id, Integer trangThai) {



        return updateTrangThai(id, trangThai, null, null);



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



        return updateTrangThai(id, trangThai, ghiChu, null);



    }



    // Cập nhật trạng thái hóa đơn kèm nhân viên và người thực hiện

    @Transactional

    public HoaDon updateTrangThai(Long id, Integer trangThai, String ghiChu, String email) {

        return updateTrangThai(id, trangThai, ghiChu, email, null, null);

    }

    // Cập nhật trạng thái hóa đơn, ghi chú và email khách hàng theo ID



    @Transactional



    public HoaDon updateTrangThai(



            Long id,



            Integer trangThai,



            String ghiChu,



            String email,

            Long nhanVienId,

            String nguoiThucHien



    ) {



        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy hóa đơn có ID: " + id
                ));
        Integer oldTrangThai = hoaDon.getTrangThai();



        NhanVien actorStaff = null;

        if (nhanVienId != null) {

            actorStaff = nhanVienRepository.findById(nhanVienId).orElse(null);

        }

        if (actorStaff == null && nguoiThucHien != null && !nguoiThucHien.trim().isEmpty()) {

            String trimmed = nguoiThucHien.trim();

            actorStaff = nhanVienRepository.findByTenTaiKhoanIgnoreCase(trimmed)

                    .or(() -> nhanVienRepository.findByMaNhanVienIgnoreCase(trimmed))

                    .orElse(null);

            if (actorStaff == null) {

                try {

                    List<NhanVien> all = nhanVienRepository.findAll();

                    for (NhanVien nv : all) {

                        if (nv.getTenNhanVien() != null && nv.getTenNhanVien().equalsIgnoreCase(trimmed)) {

                            actorStaff = nv;

                            break;

                        }

                    }

                } catch (Exception ignored) {}

            }

        }

        if (actorStaff != null) {

            if (hoaDon.getIdNhanVien() == null || Integer.valueOf(2).equals(trangThai)) {

                hoaDon.setIdNhanVien(actorStaff);

            }

            hoaDon.setNguoiCapNhat(actorStaff.getTenNhanVien() != null ? actorStaff.getTenNhanVien() : actorStaff.getTenTaiKhoan());

        } else if (nguoiThucHien != null && !nguoiThucHien.trim().isEmpty()) {

            hoaDon.setNguoiCapNhat(nguoiThucHien.trim());

        }

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
            if (Integer.valueOf(2).equals(trangThai) && (oldTrangThai == null || Integer.valueOf(1).equals(oldTrangThai))) {
                checkStockAndDeductForConfirmation(hoaDon);
            }
            hoaDon.setTrangThai(trangThai);



            if (ghiChu != null && !ghiChu.trim().isEmpty()) {



                hoaDon.setGhiChu(ghiChu.trim());



            }



        }



        // Nếu trạng thái là HOÀN THÀNH (5) -> Tự động đánh dấu đã thanh toán (đặc biệt cho đơn COD)



        if (Integer.valueOf(5).equals(trangThai)) {



            hoaDon.setTrangThaiThanhToan(1);



        }



        // Nếu trạng thái là ĐÃ HỦY (6) và trước đó đã xác nhận (trạng thái >= 2 và != 6) -> Hoàn lại tồn kho cho các biến thể
        if (Integer.valueOf(6).equals(trangThai) && oldTrangThai != null && oldTrangThai >= 2 && oldTrangThai != 6) {



            try {



                String noteLower = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu().toLowerCase() : "";

                if (noteLower.contains("website") || noteLower.contains("online")) {

                    if (!Integer.valueOf(2).equals(hoaDon.getLoaiDon())) {

                        hoaDon.setLoaiDon(2);

                        try { hoaDonRepository.save(hoaDon); } catch (Exception ignored) {}

                    }

                }

                if (false && hoaDon.getIdKhachHang() == null && hoaDon.getSoDienThoaiKhachHang() != null && !hoaDon.getSoDienThoaiKhachHang().trim().isEmpty()) {

                    khachHangRepository.findBySoDienThoai(hoaDon.getSoDienThoaiKhachHang().trim()).ifPresent(kh -> {

                        hoaDon.setIdKhachHang(kh);

                        try { hoaDonRepository.save(hoaDon); } catch (Exception ignored) {}

                    });

                }

                if (hoaDon.getEmail() == null || hoaDon.getEmail().trim().isEmpty()) {

                    String e = hoaDon.getEmailKhachHang();

                    if (e != null && !e.trim().isEmpty()) {

                        hoaDon.setEmail(e.trim());

                    }

                }

                List<ChiTietHoaDon> items = chiTietHoaDonRepository.findByIdHoaDon_Id(hoaDon.getId());



                if (items != null) {



                    for (ChiTietHoaDon cthd : items) {



                        if (cthd.getIdChiTietSanPham() != null && cthd.getSoLuong() != null) {



                            ChiTietSanPham ct = cthd.getIdChiTietSanPham();



                            int cur = ct.getSoLuong() != null ? ct.getSoLuong() : 0;



                            ct.setSoLuong(cur + cthd.getSoLuong());



                            chiTietSanPhamRepository.save(ct);



                        }



                    }



                }



            } catch (Exception ignored) {}



        }



        hoaDon.setNgayCapNhat(Instant.now());



        HoaDon saved = hoaDonRepository.save(hoaDon);



        // Nếu chuyển sang trạng thái 2 (Đã xác nhận), gửi email xác nhận đơn hàng cho khách nếu có email



        if (Integer.valueOf(2).equals(trangThai)) {



            String recipientEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : null;



            if (recipientEmail == null && saved.getIdKhachHang() != null && saved.getIdKhachHang().getEmail() != null) {



                recipientEmail = saved.getIdKhachHang().getEmail().trim();



            }



            if (recipientEmail != null && !recipientEmail.isEmpty() && recipientEmail.contains("@")) {



                // Cập nhật lại email cho khách hàng nếu khách hàng đã liên kết



                if (saved.getIdKhachHang() != null) {



                    KhachHang kh = saved.getIdKhachHang();



                    if (kh.getEmail() == null || kh.getEmail().trim().isEmpty() || !kh.getEmail().trim().equalsIgnoreCase(recipientEmail)) {



                        kh.setEmail(recipientEmail);



                        try {



                            khachHangRepository.save(kh);



                        } catch (Exception ignored) {}



                    }



                }



                if (emailService != null) {



                    try {



                        List<ChiTietHoaDon> items = getChiTietByHoaDonId(saved.getId());



                        emailService.sendOrderConfirmationEmail(saved, items, recipientEmail);



                    } catch (Exception ex) {



                        System.err.println("==> Lỗi kích hoạt gửi email xác nhận: " + ex.getMessage());



                    }



                }



                actionDesc += " (Gửi mail: " + recipientEmail + ")";



            }



        }



        try {



            LichSuHoaDon log = new LichSuHoaDon();



            log.setIdHoaDon(saved);



            log.setTrangThai(saved.getTrangThai());



            log.setThoiGian(Instant.now());



            if (actionDesc.length() > 250) {



                actionDesc = actionDesc.substring(0, 250);



            }



            log.setHanhDong(actionDesc);

            log.setGhiChu(actionDesc);

            String actorName = null;

            String actorRole = "Nhân viên";

            if (actorStaff != null) {

                actorName = actorStaff.getTenNhanVien() != null ? actorStaff.getTenNhanVien() : actorStaff.getTenTaiKhoan();

                if (actorStaff.getIdVaiTro() != null && actorStaff.getIdVaiTro().getTenVaiTro() != null) {

                    actorRole = actorStaff.getIdVaiTro().getTenVaiTro();

                }

            } else if (nguoiThucHien != null && !nguoiThucHien.trim().isEmpty() && !nguoiThucHien.equals("Nhân viên bán hàng")) {

                actorName = nguoiThucHien.trim();

            }

            if (actorName != null && !actorName.trim().isEmpty()) {

                if (actorName.length() > 95) actorName = actorName.substring(0, 95);

                log.setNguoiThucHien(actorName);

                log.setVaiTroNguoiThucHien(actorRole);

                lichSuHoaDonRepository.save(log);

                return saved;

            }



            String nguoiTh = (saved.getNguoiCapNhat() != null && !saved.getNguoiCapNhat().equals("Nhân viên bán hàng") && !saved.getNguoiCapNhat().equals("Nhân viên"))



                    ? saved.getNguoiCapNhat()



                    : ((saved.getIdNhanVien() != null && saved.getIdNhanVien().getTenNhanVien() != null)



                    ? saved.getIdNhanVien().getTenNhanVien()



                    : (saved.getNguoiTao() != null && !saved.getNguoiTao().isEmpty() && !saved.getNguoiTao().equals("Nhân viên bán hàng") ? saved.getNguoiTao() : "Nhân viên"));



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



        return updateTrangThaiByMa(maHoaDon, trangThai, ghiChu, null);



    }



    // Cập nhật trạng thái hóa đơn theo mã hóa đơn kèm email

    @Transactional

    public HoaDon updateTrangThaiByMa(String maHoaDon, Integer trangThai, String ghiChu, String email) {

        return updateTrangThaiByMa(maHoaDon, trangThai, ghiChu, email, null, null);

    }

    // Cập nhật trạng thái hóa đơn theo mã hóa đơn đầy đủ tham số

    @Transactional

    public HoaDon updateTrangThaiByMa(

            String maHoaDon,

            Integer trangThai,

            String ghiChu,

            String email,

            Long nhanVienId,

            String nguoiThucHien

    ) {

        HoaDon hoaDon = hoaDonRepository.findByMaHoaDon(maHoaDon)

                .orElseThrow(() -> new RuntimeException(

                        "Không tìm thấy hóa đơn có mã: " + maHoaDon

                ));

        return updateTrangThai(hoaDon.getId(), trangThai, ghiChu, email, nhanVienId, nguoiThucHien);

    }

    /*

    }



    @Transactional



    public HoaDon updateTrangThaiByMa(



            String maHoaDon,



            Integer trangThai,



            String ghiChu,



            String email,

            Long nhanVienId,

            String nguoiThucHien



    ) {



        HoaDon hoaDon = hoaDonRepository.findByMaHoaDon(maHoaDon)



                .orElseThrow(() -> new RuntimeException(



                        "Không tìm thấy hóa đơn có mã: " + maHoaDon



                ));



        return null;

    }

    */

    private void _dummyUnused() {



    }



    // Gửi email xác nhận đơn hàng theo yêu cầu



    public boolean sendConfirmationEmail(Long id, String emailOverride) {



        Optional<HoaDon> opt = getById(id);



        if (opt.isEmpty()) return false;



        HoaDon hd = opt.get();



        String recipient = (emailOverride != null && !emailOverride.trim().isEmpty())



                ? emailOverride.trim()



                : (hd.getEmail() != null ? hd.getEmail().trim() : null);



        if (recipient == null || !recipient.contains("@")) return false;



        if (emailService != null) {



            List<ChiTietHoaDon> items = getChiTietByHoaDonId(hd.getId());



            emailService.sendOrderConfirmationEmail(hd, items, recipient);



            return true;



        }



        return false;



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



        if (Integer.valueOf(1).equals(trangThaiThanhToan)) {



            boolean isStoreOrder = Integer.valueOf(1).equals(hoaDon.getLoaiDon())



                                   && (hoaDon.getPhiVanChuyen() == null || hoaDon.getPhiVanChuyen().compareTo(BigDecimal.ZERO) == 0)



                                   && (hoaDon.getDiaChiNhanHang() == null || hoaDon.getDiaChiNhanHang().trim().isEmpty() || hoaDon.getDiaChiNhanHang().equalsIgnoreCase("Bán tại quầy"));



            if (isStoreOrder && (hoaDon.getTrangThai() == null || hoaDon.getTrangThai() == 1)) {



                hoaDon.setTrangThai(5);



            }



        }



        if (ghiChu != null && !ghiChu.trim().isEmpty()) {



            hoaDon.setGhiChu(ghiChu.trim());



        }



        hoaDon.setNgayCapNhat(Instant.now());



        HoaDon saved = hoaDonRepository.saveAndFlush(hoaDon);



        try {



            LichSuHoaDon log = new LichSuHoaDon();



            log.setIdHoaDon(saved);



            log.setTrangThai(saved.getTrangThai());



            log.setThoiGian(Instant.now());



            String desc = (trangThaiThanhToan == 1) ? "Xác nhận đã thanh toán hóa đơn" : "Chuyển trạng thái sang chưa thanh toán";



            if (ghiChu != null && !ghiChu.trim().isEmpty()) {



                desc += " - " + ghiChu.trim();



            }



            if (desc.length() > 250) desc = desc.substring(0, 250);



            log.setHanhDong(desc);



            String nguoiTh = (saved.getNguoiCapNhat() != null && !saved.getNguoiCapNhat().equals("Nhân viên bán hàng") && !saved.getNguoiCapNhat().equals("Nhân viên"))



                    ? saved.getNguoiCapNhat()



                    : ((saved.getIdNhanVien() != null && saved.getIdNhanVien().getTenNhanVien() != null)



                    ? saved.getIdNhanVien().getTenNhanVien()



                    : (saved.getNguoiTao() != null && !saved.getNguoiTao().isEmpty() && !saved.getNguoiTao().equals("Nhân viên bán hàng") ? saved.getNguoiTao() : "Nhân viên"));



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



    // Kiểm tra hóa đơn tồn tại theo ID



    public boolean existsById(Long id) {



        return id != null && hoaDonRepository.existsById(id);



    }



    // Xóa hóa đơn theo ID kèm các dữ liệu chi tiết, lịch sử, hình thức thanh toán liên quan



    @Transactional



    public void delete(Long id) {



        if (id == null) return;



        Optional<HoaDon> opt = hoaDonRepository.findById(id);



        if (opt.isEmpty()) {



            throw new RuntimeException("Không tìm thấy hóa đơn có ID: " + id);



        }



        HoaDon hoaDon = opt.get();



        // 1. Hoàn lại tồn kho cho hóa đơn chờ/chưa hoàn thành và xóa chi tiết hóa đơn



        try {



            List<ChiTietHoaDon> chiTiets = chiTietHoaDonRepository.findByHoaDonIdSimple(id);



            if (chiTiets != null && !chiTiets.isEmpty()) {



                boolean shouldRestoreStock = isWaitingInvoice(hoaDon)



                                             || Integer.valueOf(1).equals(hoaDon.getTrangThai())



                                             || hoaDon.getTrangThaiThanhToan() == null



                                             || Integer.valueOf(0).equals(hoaDon.getTrangThaiThanhToan());



                if (shouldRestoreStock) {



                    for (ChiTietHoaDon c : chiTiets) {



                        if (c.getIdChiTietSanPham() != null && c.getIdChiTietSanPham().getId() != null) {



                            Long vid = c.getIdChiTietSanPham().getId();



                            int qty = c.getSoLuong() != null ? c.getSoLuong() : 0;



                            if (qty > 0) {



                                Optional<ChiTietSanPham> ctspOpt = chiTietSanPhamRepository.findById(vid);



                                if (ctspOpt.isPresent()) {



                                    ChiTietSanPham ctsp = ctspOpt.get();



                                    int curStock = ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0;



                                    ctsp.setSoLuong(curStock + qty);



                                    chiTietSanPhamRepository.save(ctsp);



                                }



                            }



                        }



                    }



                }



            }



            if (chiTiets != null && !chiTiets.isEmpty()) {



                chiTietHoaDonRepository.deleteAll(chiTiets);



                chiTietHoaDonRepository.flush();



            }



        } catch (Exception e) {



            System.err.println("Lỗi khi xóa ChiTietHoaDon: " + e.getMessage());



        }



        // 2. Xóa lịch sử hóa đơn



        try {



            List<LichSuHoaDon> lichSus = lichSuHoaDonRepository.findByIdHoaDon_IdOrderByThoiGianDesc(id);



            if (lichSus != null && !lichSus.isEmpty()) {



                lichSuHoaDonRepository.deleteAll(lichSus);



                lichSuHoaDonRepository.flush();



            }



        } catch (Exception e) {



            System.err.println("Lỗi khi xóa LichSuHoaDon: " + e.getMessage());



        }



        // 3. Xóa hình thức thanh toán



        try {



            List<HinhThucThanhToan> hinhThucs = hinhThucThanhToanRepository.findByIdHoaDon_Id(id);



            if (hinhThucs != null && !hinhThucs.isEmpty()) {



                hinhThucThanhToanRepository.deleteAll(hinhThucs);



                hinhThucThanhToanRepository.flush();



            }



        } catch (Exception e) {



            System.err.println("Lỗi khi xóa HinhThucThanhToan: " + e.getMessage());



        }



        // 4. Xóa tham chiếu bộ nhớ nếu có



        if (hoaDon.getChiTietHoaDons() != null) hoaDon.getChiTietHoaDons().clear();



        if (hoaDon.getLichSuHoaDons() != null) hoaDon.getLichSuHoaDons().clear();



        if (hoaDon.getHinhThucThanhToan() != null) hoaDon.getHinhThucThanhToan().clear();



        // 5. Xóa hóa đơn



        hoaDonRepository.delete(hoaDon);



        hoaDonRepository.flush();



    }



    // Tạo và xử lý đơn hàng POS tại quầy kèm lưu sản phẩm chi tiết, trừ tồn kho và ghi nhận thanh toán



    @Transactional



    // =========================================================================



    // 4 CASE KHÓ NGHIỆP VỤ POS: ĐỔI GIÁ, SỬA VOUCHER, CHUYỂN QUYỀN NV, ĐỢT GIẢM GIÁ



    // =========================================================================



    /**



     * Tính đơn giá bán thực tế của biến thể tại thời điểm hiện tại



     * (bao gồm giá niêm yết và khuyến mãi DotGiamGia đang hiệu lực)



     */



    public BigDecimal calculateCurrentVariantPrice(ChiTietSanPham ctsp, Map<Long, Map<String, Object>> activeDiscountMap) {



        if (ctsp == null || ctsp.getGiaBan() == null) {



            return BigDecimal.ZERO;



        }



        BigDecimal basePrice = ctsp.getGiaBan();



        if (activeDiscountMap == null || !activeDiscountMap.containsKey(ctsp.getId())) {



            return basePrice;



        }



        Map<String, Object> discountInfo = activeDiscountMap.get(ctsp.getId());



        if (discountInfo == null) {



            return basePrice;



        }



        Integer loaiGiam = 1;



        if (discountInfo.get("loaiGiamGia") instanceof Number) {



            loaiGiam = ((Number) discountInfo.get("loaiGiamGia")).intValue();



        }



        BigDecimal discountVal = BigDecimal.ZERO;



        if (loaiGiam == 2) { // Giảm tiền cố định



            Object valObj = discountInfo.get("giaTriGiam");



            if (valObj instanceof BigDecimal) discountVal = (BigDecimal) valObj;



            else if (valObj instanceof Number) discountVal = BigDecimal.valueOf(((Number) valObj).doubleValue());



        } else { // Giảm theo %



            Object ptObj = discountInfo.get("phanTramGiam");



            BigDecimal pt = BigDecimal.ZERO;



            if (ptObj instanceof BigDecimal) pt = (BigDecimal) ptObj;



            else if (ptObj instanceof Number) pt = BigDecimal.valueOf(((Number) ptObj).doubleValue());



            if (pt.compareTo(BigDecimal.ZERO) > 0) {



                discountVal = basePrice.multiply(pt).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);



                Object maxObj = discountInfo.get("giamToiDa");



                BigDecimal maxDiscount = null;



                if (maxObj instanceof BigDecimal) maxDiscount = (BigDecimal) maxObj;



                else if (maxObj instanceof Number) maxDiscount = BigDecimal.valueOf(((Number) maxObj).doubleValue());



                if (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) > 0 && discountVal.compareTo(maxDiscount) > 0) {



                    discountVal = maxDiscount;



                }



            }



        }



        BigDecimal finalPrice = basePrice.subtract(discountVal);



        return finalPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalPrice;



    }



    /**



     * Kiểm tra tính hợp lệ và tính số tiền giảm chuẩn xác của phiếu giảm giá



     */



    public Map<String, Object> validateAndCalculateVoucher(Long idPhieuGiamGia, Long idKhachHang, BigDecimal tongTienHangThucTe) {



        Map<String, Object> result = new HashMap<>();



        result.put("hopLe", true);



        result.put("thayDoi", false);



        result.put("giamGia", BigDecimal.ZERO);



        result.put("lyDo", "");



        if (idPhieuGiamGia == null) {



            return result;



        }



        Optional<PhieuGiamGia> pggOpt = phieuGiamGiaRepository.findById(idPhieuGiamGia);



        if (pggOpt.isEmpty()) {



            result.put("hopLe", false);



            result.put("lyDo", "Phiếu giảm giá không tồn tại trong hệ thống.");



            return result;



        }



        PhieuGiamGia pgg = pggOpt.get();



        Instant now = Instant.now();



        // 1. Kiểm tra trạng thái kích hoạt



        if (pgg.getTrangThai() == null || pgg.getTrangThai() != 1) {



            result.put("hopLe", false);



            result.put("lyDo", "Phiếu giảm giá '" + pgg.getMaPhieuGiamGia() + "' đã bị khóa hoặc tạm ngưng áp dụng.");



            return result;



        }



        // 2. Kiểm tra ngày bắt đầu / kết thúc



        if (pgg.getNgayBatDau() != null && now.isBefore(pgg.getNgayBatDau())) {



            result.put("hopLe", false);



            result.put("lyDo", "Phiếu giảm giá '" + pgg.getMaPhieuGiamGia() + "' chưa đến thời gian có hiệu lực.");



            return result;



        }



        if (pgg.getNgayKetThuc() != null && now.isAfter(pgg.getNgayKetThuc())) {



            result.put("hopLe", false);



            result.put("lyDo", "Phiếu giảm giá '" + pgg.getMaPhieuGiamGia() + "' đã hết hạn sử dụng.");



            return result;



        }



        // 3. Kiểm tra số lượng



        if (pgg.getSoLuongSuDung() == null || pgg.getSoLuongSuDung() <= 0) {



            result.put("hopLe", false);



            result.put("lyDo", "Phiếu giảm giá '" + pgg.getMaPhieuGiamGia() + "' đã hết lượt sử dụng.");



            return result;



        }



        // 4. Kiểm tra giá trị đơn hàng tối thiểu



        if (pgg.getHoaDonToiThieu() != null && tongTienHangThucTe.compareTo(pgg.getHoaDonToiThieu()) < 0) {



            result.put("hopLe", false);



            result.put("lyDo", "Đơn hàng (" + tongTienHangThucTe + "đ) chưa đạt giá trị tối thiểu (" + pgg.getHoaDonToiThieu() + "đ) để dùng voucher '" + pgg.getMaPhieuGiamGia() + "'.");



            return result;



        }



        // 5. Tính số tiền giảm giá chuẩn xác



        BigDecimal discount = BigDecimal.ZERO;



        if (pgg.getLoaiPhieuGiamGia() != null && pgg.getLoaiPhieuGiamGia() == 1) { // Giảm %



            BigDecimal pt = pgg.getGiaTriGiamGia() != null ? pgg.getGiaTriGiamGia() : BigDecimal.ZERO;



            discount = tongTienHangThucTe.multiply(pt).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);



            if (pgg.getGiamToiDa() != null && pgg.getGiamToiDa().compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(pgg.getGiamToiDa()) > 0) {



                discount = pgg.getGiamToiDa();



            }



        } else { // Tiền cố định



            discount = pgg.getGiaTriGiamGia() != null ? pgg.getGiaTriGiamGia() : BigDecimal.ZERO;



            if (discount.compareTo(tongTienHangThucTe) > 0) {



                discount = tongTienHangThucTe;



            }



        }



        result.put("giamGia", discount);



        return result;



    }



    /**



     * Kiểm tra trạng thái và quyền hạn tài khoản nhân viên POS



     */



    public Map<String, Object> validateNhanVienPos(Long idNhanVien) {



        Map<String, Object> res = new HashMap<>();



        res.put("hopLe", true);



        res.put("lyDo", "");



        if (idNhanVien == null) {



            return res;



        }



        Optional<NhanVien> nvOpt = nhanVienRepository.findById(idNhanVien);



        if (nvOpt.isEmpty()) {



            res.put("hopLe", false);



            res.put("lyDo", "Không tìm thấy thông tin tài khoản nhân viên (ID: " + idNhanVien + ").");



            return res;



        }



        NhanVien nv = nvOpt.get();



        if (nv.getTrangThai() == null || nv.getTrangThai() == 0) {



            res.put("hopLe", false);



            res.put("lyDo", "Tài khoản nhân viên '" + nv.getTenNhanVien() + "' (" + nv.getMaNhanVien() + ") đã bị khóa hoặc ngừng hoạt động!");



            return res;



        }



        if (nv.getIdVaiTro() != null) {



            String roleName = nv.getIdVaiTro().getTenVaiTro() != null ? nv.getIdVaiTro().getTenVaiTro().toUpperCase() : "";



            if (roleName.contains("BLOCK") || roleName.contains("KHÓA") || roleName.contains("NGHỈ")) {



                res.put("hopLe", false);



                res.put("lyDo", "Nhân viên '" + nv.getTenNhanVien() + "' không có quyền thực hiện bán hàng tại quầy!");



                return res;



            }



        }



        return res;



    }



    /**



     * Tiền kiểm tra hợp lệ toàn diện đơn hàng POS trước khi thanh toán



     */



    public Map<String, Object> kiemTraHopLeDonHangPos(HoaDonPosDTO dto) {



        Map<String, Object> res = new HashMap<>();



        boolean hopLe = true;



        boolean requiresConfirmation = false;



        boolean loiNghiemTrong = false;



        List<String> messages = new ArrayList<>();



        // 1. Kiểm tra quyền nhân viên (Case 3)



        Map<String, Object> nvCheck = validateNhanVienPos(dto.getIdNhanVien());



        boolean nhanVienHopLe = Boolean.TRUE.equals(nvCheck.get("hopLe"));



        String lyDoNhanVien = nvCheck.get("lyDo") != null ? nvCheck.get("lyDo").toString() : "";



        if (!nhanVienHopLe) {



            hopLe = false;



            loiNghiemTrong = true;



            requiresConfirmation = true;



            messages.add(lyDoNhanVien);



        }



        res.put("nhanVienHopLe", nhanVienHopLe);



        res.put("lyDoNhanVien", lyDoNhanVien);



        // 2. Ngăn thanh toán trùng lặp (Idempotency)



        boolean daThanhToan = false;



        if (dto.getMaHoaDon() != null && !dto.getMaHoaDon().trim().isEmpty()) {



            Optional<HoaDon> hdExOpt = hoaDonRepository.findByMaHoaDon(dto.getMaHoaDon().trim());



            if (hdExOpt.isPresent()) {



                HoaDon hdEx = hdExOpt.get();



                if (Integer.valueOf(5).equals(hdEx.getTrangThai()) || Integer.valueOf(0).equals(hdEx.getTrangThai()) || Integer.valueOf(6).equals(hdEx.getTrangThai())) {



                    daThanhToan = true;



                    hopLe = false;



                    loiNghiemTrong = true;



                    messages.add("Hóa đơn " + dto.getMaHoaDon() + " đã được xử lý hoàn tất hoặc đã bị hủy trước đó!");



                }



            }



        }



        res.put("daThanhToan", daThanhToan);



        // 3. Kiểm tra Giá sản phẩm & Đợt giảm giá (Case 1 & Case 4)



        boolean thayDoiGia = false;



        List<Map<String, Object>> danhSachThayDoiGia = new ArrayList<>();



        BigDecimal tongTienHangThucTe = BigDecimal.ZERO;



        BigDecimal tongTienTrongGio = BigDecimal.ZERO;



        Map<Long, Map<String, Object>> activeDiscountMap = (dotGiamGiaService != null)



                ? dotGiamGiaService.getActiveVariantMap(null)



                : new HashMap<>();



        if (dto.getChiTietList() != null) {



            for (HoaDonPosDTO.ItemChiTietDTO item : dto.getChiTietList()) {



                Long variantId = item.getResolvedVariantId();



                if (variantId == null) continue;



                Optional<ChiTietSanPham> ctspOpt = chiTietSanPhamRepository.findById(variantId);



                if (ctspOpt.isEmpty()) continue;



                ChiTietSanPham ctsp = ctspOpt.get();



                int qty = item.getSoLuong() != null ? Math.max(1, item.getSoLuong()) : 1;



                BigDecimal cartUnitPrice = item.getResolvedPrice();



                BigDecimal realUnitPrice = calculateCurrentVariantPrice(ctsp, activeDiscountMap);



                tongTienTrongGio = tongTienTrongGio.add(cartUnitPrice.multiply(BigDecimal.valueOf(qty)));



                tongTienHangThucTe = tongTienHangThucTe.add(realUnitPrice.multiply(BigDecimal.valueOf(qty)));



                if (cartUnitPrice.compareTo(realUnitPrice) != 0) {



                    thayDoiGia = true;



                    requiresConfirmation = true;



                    Map<String, Object> diffItem = new HashMap<>();



                    diffItem.put("idChiTietSanPham", variantId);



                    diffItem.put("maChiTietSanPham", ctsp.getMaChiTietSanPham());



                    String spName = ctsp.getIdSanPham() != null ? ctsp.getIdSanPham().getTenSanPham() : "Sản phẩm";



                    String color = ctsp.getIdMauSac() != null ? ctsp.getIdMauSac().getTenMauSac() : "";



                    String size = ctsp.getIdKichCo() != null ? ctsp.getIdKichCo().getTenKichCo() : "";



                    diffItem.put("tenSanPham", spName);



                    diffItem.put("phanLoai", (color + " " + size).trim());



                    diffItem.put("soLuong", qty);



                    diffItem.put("giaTrongGio", cartUnitPrice);



                    diffItem.put("giaCu", cartUnitPrice);



                    diffItem.put("giaHienTai", realUnitPrice);



                    diffItem.put("giaMoi", realUnitPrice);



                    diffItem.put("chenhLechDonGia", realUnitPrice.subtract(cartUnitPrice));



                    diffItem.put("chenhLech", realUnitPrice.subtract(cartUnitPrice));



                    diffItem.put("chenhLechThanhTien", realUnitPrice.subtract(cartUnitPrice).multiply(BigDecimal.valueOf(qty)));



                    // Xác định nguyên nhân thay đổi



                    String lyDo = "Giá bán niêm yết của sản phẩm đã được thay đổi trong hệ thống.";



                    boolean hadDiscountInCart = cartUnitPrice.compareTo(ctsp.getGiaBan() != null ? ctsp.getGiaBan() : BigDecimal.ZERO) < 0;



                    boolean hasDiscountNow = activeDiscountMap.containsKey(variantId);



                    if (hadDiscountInCart && !hasDiscountNow) {



                        lyDo = "Chương trình khuyến mãi của sản phẩm đã kết thúc (áp dụng lại giá gốc).";



                    } else if (!hadDiscountInCart && hasDiscountNow) {



                        Map<String, Object> dInfo = activeDiscountMap.get(variantId);



                        String tenDot = dInfo.get("tenDotGiamGia") != null ? dInfo.get("tenDotGiamGia").toString() : "Khuyến mãi mới";



                        lyDo = "Sản phẩm được kích hoạt chương trình khuyến mãi mới: " + tenDot;



                    } else if (hadDiscountInCart && hasDiscountNow) {



                        lyDo = "Mức giảm giá của chương trình khuyến mãi đã được điều chỉnh.";



                    }



                    diffItem.put("lyDo", lyDo);



                    danhSachThayDoiGia.add(diffItem);



                }



            }



        }



        if (thayDoiGia) {



            messages.add("Đơn giá của " + danhSachThayDoiGia.size() + " sản phẩm trong giỏ đã thay đổi so với thời điểm thêm vào giỏ.");



        }



        res.put("thayDoiGia", thayDoiGia);



        res.put("danhSachThayDoiGia", danhSachThayDoiGia);



        res.put("tongTienCu", tongTienTrongGio);



        res.put("tongTienMoi", tongTienHangThucTe);



        res.put("chenhLechTongTien", tongTienHangThucTe.subtract(tongTienTrongGio));



        if (tongTienHangThucTe.compareTo(BigDecimal.ZERO) == 0 && dto.getTongTien() != null && dto.getTongTien().compareTo(BigDecimal.ZERO) > 0) {



            tongTienHangThucTe = dto.getTongTien();



        }



        // 4. Kiểm tra Voucher (Case 2)



        boolean thayDoiVoucher = false;



        boolean voucherHopLe = true;



        String lyDoVoucher = "";



        BigDecimal voucherGiamGiaCu = dto.getTongTienGiamGia() != null ? dto.getTongTienGiamGia() : BigDecimal.ZERO;



        BigDecimal voucherGiamGiaMoi = BigDecimal.ZERO;



        if (dto.getIdPhieuGiamGia() != null) {



            Map<String, Object> vCheck = validateAndCalculateVoucher(dto.getIdPhieuGiamGia(), dto.getIdKhachHang(), tongTienHangThucTe);



            voucherHopLe = Boolean.TRUE.equals(vCheck.get("hopLe"));



            lyDoVoucher = vCheck.get("lyDo") != null ? vCheck.get("lyDo").toString() : "";



            voucherGiamGiaMoi = (BigDecimal) vCheck.getOrDefault("giamGia", BigDecimal.ZERO);



            if (!voucherHopLe) {



                thayDoiVoucher = true;



                requiresConfirmation = true;



                messages.add(lyDoVoucher);



            } else if (voucherGiamGiaCu.compareTo(voucherGiamGiaMoi) != 0) {



                thayDoiVoucher = true;



                requiresConfirmation = true;



                lyDoVoucher = "Số tiền giảm giá voucher được tính lại theo giá mới: từ " + voucherGiamGiaCu + "đ thành " + voucherGiamGiaMoi + "đ.";



                messages.add(lyDoVoucher);



            }



        }



        res.put("voucherHopLe", voucherHopLe);



        res.put("thayDoiVoucher", thayDoiVoucher);



        res.put("lyDoVoucher", lyDoVoucher);



        res.put("voucherLyDo", lyDoVoucher);



        res.put("soTienGiamMoi", voucherGiamGiaMoi);



        res.put("voucherGiamGiaCu", voucherGiamGiaCu);



        res.put("voucherGiamGiaMoi", voucherGiamGiaMoi);



        // Tính tổng tiền thanh toán chuẩn



        BigDecimal phiShip = dto.getPhiShip() != null ? dto.getPhiShip() : BigDecimal.ZERO;



        BigDecimal finalPay = tongTienHangThucTe.add(phiShip).subtract(voucherGiamGiaMoi);



        if (finalPay.compareTo(BigDecimal.ZERO) < 0) finalPay = BigDecimal.ZERO;



        res.put("tongTienHangChuan", tongTienHangThucTe);



        res.put("giamGiaChuan", voucherGiamGiaMoi);



        res.put("phiShipChuan", phiShip);



        res.put("khachCanTraChuan", finalPay);



        res.put("hopLe", hopLe && !thayDoiGia && !thayDoiVoucher);



        res.put("requiresConfirmation", requiresConfirmation);



        res.put("loiNghiemTrong", loiNghiemTrong);



        res.put("thongDiep", String.join(" | ", messages));



        return res;



    }



    /**



     * Bàn giao hóa đơn chờ cho nhân viên khác tiếp quản (Case 3)



     */



    @Transactional



    public Map<String, Object> banGiaoHoaDonPos(Map<String, Object> body) {



        Map<String, Object> res = new HashMap<>();



        String maHoaDon = body.get("maHoaDon") != null ? body.get("maHoaDon").toString().trim() : null;



        Long idNhanVienMoi = null;



        if (body.get("idNhanVienMoi") instanceof Number) {



            idNhanVienMoi = ((Number) body.get("idNhanVienMoi")).longValue();



        } else if (body.get("idNhanVienMoi") != null) {



            try { idNhanVienMoi = Long.parseLong(body.get("idNhanVienMoi").toString().trim()); } catch (Exception ignored) {}



        }



        String ghiChu = body.get("ghiChu") != null ? body.get("ghiChu").toString().trim() : "Bàn giao hóa đơn tại quầy";



        if (maHoaDon == null || maHoaDon.isEmpty()) {



            throw new IllegalArgumentException("Vui lòng cung cấp mã hóa đơn cần bàn giao!");



        }



        if (idNhanVienMoi == null) {



            throw new IllegalArgumentException("Vui lòng chọn nhân viên tiếp nhận bàn giao!");



        }



        HoaDon hoaDon = hoaDonRepository.findByMaHoaDon(maHoaDon)



                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn " + maHoaDon + " trong hệ thống!"));



        // Ngăn bàn giao hóa đơn đã hoàn thành hoặc đã bị hủy



        if (Integer.valueOf(5).equals(hoaDon.getTrangThai()) || Integer.valueOf(0).equals(hoaDon.getTrangThai()) || Integer.valueOf(6).equals(hoaDon.getTrangThai())) {



            throw new IllegalStateException("Hóa đơn " + maHoaDon + " đã hoàn tất hoặc đã bị hủy, không thể bàn giao!");



        }



        final Long finalIdNv = idNhanVienMoi;



        NhanVien nhanVienMoi = nhanVienRepository.findById(finalIdNv)



                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên tiếp nhận (ID: " + finalIdNv + ")!"));



        if (nhanVienMoi.getTrangThai() == null || nhanVienMoi.getTrangThai() == 0) {



            throw new IllegalStateException("Nhân viên tiếp nhận '" + nhanVienMoi.getTenNhanVien() + "' đã bị khóa hoặc ngừng hoạt động!");



        }



        NhanVien nhanVienCu = hoaDon.getIdNhanVien();



        String tenNhanVienCu = nhanVienCu != null ? nhanVienCu.getTenNhanVien() : (hoaDon.getNguoiTao() != null ? hoaDon.getNguoiTao() : "Nhân viên cũ");



        hoaDon.setIdNhanVien(nhanVienMoi);



        hoaDon.setNguoiCapNhat(nhanVienMoi.getTenNhanVien());



        hoaDon.setNgayCapNhat(Instant.now());



        if (lichSuCaLamViecRepository != null) {



            lichSuCaLamViecRepository.findTopByNhanVienIdAndTrangThaiOrderByThoiGianMoCaDesc(nhanVienMoi.getId(), 1)



                    .ifPresent(hoaDon::setIdGiaoCa);



        }



        hoaDonRepository.save(hoaDon);



        // Ghi log lịch sử hóa đơn



        LichSuHoaDon log = new LichSuHoaDon();



        log.setIdHoaDon(hoaDon);



        log.setTrangThai(hoaDon.getTrangThai() != null ? hoaDon.getTrangThai() : 1);



        log.setThoiGian(Instant.now());



        log.setHanhDong("Bàn giao hóa đơn");



        log.setNguoiThucHien(nhanVienMoi.getTenNhanVien());



        log.setVaiTroNguoiThucHien(nhanVienMoi.getIdVaiTro() != null ? nhanVienMoi.getIdVaiTro().getTenVaiTro() : "Nhân viên");



        log.setGhiChu("Bàn giao từ: " + tenNhanVienCu + " sang: " + nhanVienMoi.getTenNhanVien() + ". Lý do: " + ghiChu);



        lichSuHoaDonRepository.save(log);



        res.put("success", true);



        res.put("message", "Đã bàn giao hóa đơn " + maHoaDon + " cho nhân viên " + nhanVienMoi.getTenNhanVien() + " thành công!");



        res.put("maHoaDon", maHoaDon);



        res.put("nhanVienMoiId", nhanVienMoi.getId());



        res.put("tenNhanVienMoi", nhanVienMoi.getTenNhanVien());



        return res;



    }



    /**



     * Lấy danh sách nhân viên đang hoạt động để phục vụ bàn giao hóa đơn



     */



    public List<Map<String, Object>> getDanhSachNhanVienKhaDung() {



        List<NhanVien> list = nhanVienRepository.findByTrangThai(1);



        List<Map<String, Object>> res = new ArrayList<>();



        for (NhanVien nv : list) {



            Map<String, Object> m = new HashMap<>();



            m.put("id", nv.getId());



            m.put("maNhanVien", nv.getMaNhanVien());



            m.put("tenNhanVien", nv.getTenNhanVien());



            m.put("soDienThoai", nv.getSoDienThoai());



            m.put("tenVaiTro", nv.getIdVaiTro() != null ? nv.getIdVaiTro().getTenVaiTro() : "Nhân viên");



            res.add(m);



        }



        return res;



    }



    public HoaDon createHoaDonPos(HoaDonPosDTO dto) {



        if (dto == null) {



            throw new IllegalArgumentException("Dữ liệu đơn hàng POS không hợp lệ");



        }



        // 0. Kiểm tra tài khoản và quyền nhân viên thực hiện (Case 3)



        Map<String, Object> nvCheck = validateNhanVienPos(dto.getIdNhanVien());



        if (!Boolean.TRUE.equals(nvCheck.get("hopLe"))) {



            throw new org.springframework.web.server.ResponseStatusException(



                    org.springframework.http.HttpStatus.FORBIDDEN,



                    nvCheck.get("lyDo") != null ? nvCheck.get("lyDo").toString() : "Tài khoản nhân viên không hợp lệ!");



        }



        HoaDon hoaDon;



        Map<Long, Integer> alreadyDeductedMap = new HashMap<>();



        // 1. Mã hóa đơn: Nếu hóa đơn đã tồn tại trong CSDL (hóa đơn chờ), cập nhật lại hóa đơn đó



        if (dto.getMaHoaDon() != null && !dto.getMaHoaDon().trim().isEmpty()) {



            Optional<HoaDon> existingOpt = hoaDonRepository.findByMaHoaDon(dto.getMaHoaDon().trim());



            if (existingOpt.isPresent()) {



                hoaDon = existingOpt.get();



                // Ngăn thanh toán trùng lặp (Case 3 Idempotency): Chỉ chặn khi hóa đơn đã HOÀN THÀNH (5) hoặc ĐÃ HỦY (0, 6)



                if (Integer.valueOf(5).equals(hoaDon.getTrangThai()) || Integer.valueOf(0).equals(hoaDon.getTrangThai()) || Integer.valueOf(6).equals(hoaDon.getTrangThai())) {



                    throw new org.springframework.web.server.ResponseStatusException(



                            org.springframework.http.HttpStatus.CONFLICT,



                            "Hóa đơn " + dto.getMaHoaDon() + " đã được xử lý hoàn tất hoặc đã bị hủy trước đó!");



                }



                try {



                    List<ChiTietHoaDon> oldDetails = chiTietHoaDonRepository.findByIdHoaDon_Id(hoaDon.getId());



                    if (oldDetails != null) {



                        for (ChiTietHoaDon c : oldDetails) {



                            if (c.getIdChiTietSanPham() != null && c.getIdChiTietSanPham().getId() != null) {



                                Long vid = c.getIdChiTietSanPham().getId();



                                int q = c.getSoLuong() != null ? c.getSoLuong() : 0;



                                alreadyDeductedMap.put(vid, alreadyDeductedMap.getOrDefault(vid, 0) + q);



                            }



                        }



                    }



                    if (oldDetails != null && !oldDetails.isEmpty()) {



                        if (hoaDon.getChiTietHoaDons() != null) {



                            hoaDon.getChiTietHoaDons().clear();



                        }



                        chiTietHoaDonRepository.deleteAll(oldDetails);



                        chiTietHoaDonRepository.flush();



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



        // 2. Loại đơn: Mọi hoạt động phát sinh từ quầy (POS) bất kể có giao hàng hay không thì luôn là Tại quầy (1)



        Integer loaiDon = 1;



        hoaDon.setLoaiDon(loaiDon);



        boolean isDelivery = (dto.getPhiShip() != null && dto.getPhiShip().compareTo(java.math.BigDecimal.ZERO) > 0)



                             || (dto.getDiaChiNhanHang() != null && !dto.getDiaChiNhanHang().trim().isEmpty() && !dto.getDiaChiNhanHang().trim().equalsIgnoreCase("Bán tại quầy"));



        // 3. Trạng thái đơn & trạng thái thanh toán



        Integer trangThai = dto.getTrangThai();



        if (trangThai == null) {



            trangThai = (!isDelivery) ? 5 : 2; // 5: Hoàn thành tại quầy, 2: Đã xác nhận nếu giao hàng xử lý tại quầy



        } else if (isDelivery && trangThai == 1) {



            // Đơn giao hàng xử lý tại quầy không còn trạng thái chờ xác nhận mà chuyển sang đã xác nhận



            trangThai = 2;



        }



        hoaDon.setTrangThai(trangThai);



        Integer trangThaiThanhToan = dto.getTrangThaiThanhToan();



        if (trangThaiThanhToan == null) {



            trangThaiThanhToan = (!isDelivery) ? 1 : 0; // 1: Đã thanh toán



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



        // 5. Phiếu giảm giá (Giữ nguyên mã/mức giảm đã chốt trên POS nếu chưa bấm áp dụng mã mới)
        if (dto.getIdPhieuGiamGia() != null) {
            if (Boolean.TRUE.equals(dto.getChapNhanVoucherMoi()) || dto.getTongTienGiamGia() == null) {
                Map<String, Object> vCheck = validateAndCalculateVoucher(dto.getIdPhieuGiamGia(), dto.getIdKhachHang(), dto.getTongTien() != null ? dto.getTongTien() : BigDecimal.ZERO);
                if (!Boolean.TRUE.equals(vCheck.get("hopLe"))) {
                    hoaDon.setIdPhieuGiamGia(null);
                    dto.setIdPhieuGiamGia(null);
                    dto.setTongTienGiamGia(BigDecimal.ZERO);
                } else {
                    phieuGiamGiaRepository.findById(dto.getIdPhieuGiamGia()).ifPresent(hoaDon::setIdPhieuGiamGia);
                    BigDecimal correctDiscount = (BigDecimal) vCheck.getOrDefault("giamGia", BigDecimal.ZERO);
                    dto.setTongTienGiamGia(correctDiscount);
                }
            } else {
                phieuGiamGiaRepository.findById(dto.getIdPhieuGiamGia()).ifPresent(hoaDon::setIdPhieuGiamGia);
                BigDecimal lockedDiscount = dto.getTongTienGiamGia();
                if (lockedDiscount.compareTo(BigDecimal.ZERO) < 0) {
                    lockedDiscount = BigDecimal.ZERO;
                }
                BigDecimal orderTotal = dto.getTongTien() != null ? dto.getTongTien() : BigDecimal.ZERO;
                if (orderTotal.compareTo(BigDecimal.ZERO) > 0 && lockedDiscount.compareTo(orderTotal) > 0) {
                    lockedDiscount = orderTotal;
                }
                dto.setTongTienGiamGia(lockedDiscount);
            }
        } else {
            hoaDon.setIdPhieuGiamGia(null);
        }



        // 6. Nhân viên tạo hóa đơn



        if (dto.getIdNhanVien() != null) {



            nhanVienRepository.findById(dto.getIdNhanVien()).ifPresent(hoaDon::setIdNhanVien);



        } else {



            nhanVienRepository.findTopByOrderByIdDesc().ifPresent(hoaDon::setIdNhanVien);



        }



        // Gán ca làm việc (giao_ca) nếu có



        if (dto.getIdGiaoCa() != null && lichSuCaLamViecRepository != null) {



            lichSuCaLamViecRepository.findById(dto.getIdGiaoCa()).ifPresent(hoaDon::setIdGiaoCa);



        } else if (hoaDon.getIdNhanVien() != null && lichSuCaLamViecRepository != null) {



            lichSuCaLamViecRepository.findTopByNhanVienIdAndTrangThaiOrderByThoiGianMoCaDesc(hoaDon.getIdNhanVien().getId(), 1)



                    .ifPresent(hoaDon::setIdGiaoCa);



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



        if (hoaDon.getNgayTao() == null) {



            hoaDon.setNgayTao(Instant.now());



        }



        hoaDon.setNgayCapNhat(Instant.now());



        String staffCreator = (dto.getNguoiTao() != null && !dto.getNguoiTao().trim().isEmpty())



                ? dto.getNguoiTao().trim()



                : (hoaDon.getNguoiTao() != null && !hoaDon.getNguoiTao().trim().isEmpty() && !hoaDon.getNguoiTao().equals("Nhân viên bán hàng"))



                ? hoaDon.getNguoiTao().trim()



                : ((hoaDon.getIdNhanVien() != null && hoaDon.getIdNhanVien().getTenNhanVien() != null)



                ? hoaDon.getIdNhanVien().getTenNhanVien() : "Nhân viên bán hàng");



        hoaDon.setNguoiTao(staffCreator);



        hoaDon.setNguoiCapNhat(staffCreator);



        HoaDon saved = hoaDonRepository.save(hoaDon);



        // 7. Lưu danh sách chi tiết hóa đơn & trừ tồn kho (chỉ trừ phần chênh lệch chưa trừ)



        Set<Long> processedVariants = new HashSet<>();



        if (dto.getChiTietList() != null && !dto.getChiTietList().isEmpty()) {



            int itemIndex = 1;



            BigDecimal calculatedTotal = BigDecimal.ZERO;

            // Tính tổng số lượng yêu cầu cho từng biến thể (hỗ trợ trường hợp cùng 1 biến thể có nhiều dòng khác đơn giá)
            Map<Long, Integer> totalRequestedByVariant = new HashMap<>();
            for (HoaDonPosDTO.ItemChiTietDTO reqItem : dto.getChiTietList()) {
                Long vid = reqItem.getResolvedVariantId();
                if (vid == null) continue;
                int rq = reqItem.getSoLuong() != null ? Math.max(1, reqItem.getSoLuong()) : 1;
                totalRequestedByVariant.put(vid, totalRequestedByVariant.getOrDefault(vid, 0) + rq);
            }



            for (HoaDonPosDTO.ItemChiTietDTO item : dto.getChiTietList()) {



                Long variantId = item.getResolvedVariantId();



                if (variantId == null) continue;



                Optional<ChiTietSanPham> ctspOpt = chiTietSanPhamRepository.findById(variantId);



                if (ctspOpt.isEmpty()) continue;



                ChiTietSanPham ctsp = ctspOpt.get();

                int qty = item.getSoLuong() != null ? Math.max(1, item.getSoLuong()) : 1;

                if (!processedVariants.contains(variantId)) {
                    int previouslyDeducted = alreadyDeductedMap.getOrDefault(variantId, 0);
                    int currentAvailable = ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0;
                    int totalAvailableForCheckout = currentAvailable + previouslyDeducted;
                    int totalVariantQty = totalRequestedByVariant.getOrDefault(variantId, qty);
                    int diff = totalVariantQty - previouslyDeducted;

                    if (totalAvailableForCheckout <= 0) {
                        String spName = ctsp.getIdSanPham() != null ? ctsp.getIdSanPham().getTenSanPham() : "Sản phẩm";
                        throw new IllegalArgumentException("Sản phẩm '" + spName + "' hiện đã hết hàng trong kho!");
                    }

                    if (diff > 0 && currentAvailable < diff) {
                        String spName = ctsp.getIdSanPham() != null ? ctsp.getIdSanPham().getTenSanPham() : "Sản phẩm";
                        throw new IllegalArgumentException("Sản phẩm '" + spName + "' không đủ số lượng trong kho (chỉ còn thêm tối đa " + currentAvailable + ")!");
                    }

                    if (ctsp.getSoLuong() != null) {
                        if (diff != 0) {
                            ctsp.setSoLuong(Math.max(0, ctsp.getSoLuong() - diff));
                        }
                        chiTietSanPhamRepository.save(ctsp);
                    }
                    processedVariants.add(variantId);
                }



                // Case 1 & Case 4: Kiểm tra đơn giá sản phẩm và đợt giảm giá



                Map<Long, Map<String, Object>> activeDiscountMap = (dotGiamGiaService != null)



                        ? dotGiamGiaService.getActiveVariantMap(null)



                        : new HashMap<>();



                BigDecimal currentRealPrice = calculateCurrentVariantPrice(ctsp, activeDiscountMap);



                BigDecimal cartUnitPrice = item.getResolvedPrice();



                BigDecimal unitPrice;



                if (cartUnitPrice != null && currentRealPrice != null && cartUnitPrice.compareTo(currentRealPrice) != 0) {



                    if (Boolean.FALSE.equals(dto.getChapNhanGiaMoi())) {



                        String spName = ctsp.getIdSanPham() != null ? ctsp.getIdSanPham().getTenSanPham() : "Sản phẩm";



                        throw new org.springframework.web.server.ResponseStatusException(



                                org.springframework.http.HttpStatus.CONFLICT,



                                "Đơn giá sản phẩm '" + spName + "' hoặc khuyến mãi đã thay đổi từ " + cartUnitPrice + "đ thành " + currentRealPrice + "đ! Vui lòng xác nhận cập nhật giá mới trước khi thanh toán.");



                    }



                    unitPrice = Boolean.TRUE.equals(dto.getChapNhanGiaMoi()) ? currentRealPrice : cartUnitPrice;



                } else {



                    unitPrice = (cartUnitPrice != null && cartUnitPrice.compareTo(BigDecimal.ZERO) > 0)



                            ? cartUnitPrice



                            : ((currentRealPrice != null && currentRealPrice.compareTo(BigDecimal.ZERO) > 0)



                            ? currentRealPrice



                            : (ctsp.getGiaBan() != null ? ctsp.getGiaBan() : BigDecimal.ZERO));



                }



                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));



                calculatedTotal = calculatedTotal.add(lineTotal);



                ChiTietHoaDon cthd = new ChiTietHoaDon();



                cthd.setIdHoaDon(saved);



                cthd.setIdChiTietSanPham(ctsp);



                cthd.setMaHoaDonChiTiet(String.format("CTHD%s-%02d-%04d", saved.getMaHoaDon(), itemIndex++, (int)(System.currentTimeMillis() % 10000)));



                cthd.setDonGia(unitPrice);



                cthd.setSoLuong(qty);



                cthd.setThanhTien(lineTotal);



                cthd.setTrangThai(1);



                if (saved.getChiTietHoaDons() != null) {



                    saved.getChiTietHoaDons().add(cthd);



                }



                chiTietHoaDonRepository.save(cthd);



            }



            // Hoàn lại kho cho các sản phẩm đã có trong hóa đơn chờ nhưng bị xóa trước khi bấm thanh toán



            for (Map.Entry<Long, Integer> entry : alreadyDeductedMap.entrySet()) {



                Long vid = entry.getKey();



                int prevQ = entry.getValue();



                if (!processedVariants.contains(vid) && prevQ > 0) {



                    Optional<ChiTietSanPham> leftoverOpt = chiTietSanPhamRepository.findById(vid);



                    if (leftoverOpt.isPresent()) {



                        ChiTietSanPham leftoverCtsp = leftoverOpt.get();



                        if (leftoverCtsp.getSoLuong() != null) {



                            leftoverCtsp.setSoLuong(leftoverCtsp.getSoLuong() + prevQ);



                            chiTietSanPhamRepository.save(leftoverCtsp);



                        }



                    }



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
            List<HinhThucThanhToan> oldHttt = hinhThucThanhToanRepository.findByIdHoaDon_Id(saved.getId());
            if (oldHttt != null && !oldHttt.isEmpty()) {
                if (saved.getHinhThucThanhToan() != null) {
                    saved.getHinhThucThanhToan().clear();
                }
                hinhThucThanhToanRepository.deleteAll(oldHttt);
                hinhThucThanhToanRepository.flush();
            }

            List<PhuongThucThanhToan> allPts = phuongThucThanhToanRepository.findAll();
            PhuongThucThanhToan ptTienMat = null;
            PhuongThucThanhToan ptChuyenKhoan = null;

            for (PhuongThucThanhToan p : allPts) {
                if (p.getTenPhuongThuc() != null) {
                    String t = p.getTenPhuongThuc().toLowerCase();
                    if (ptTienMat == null && (t.contains("tiền mặt") || t.contains("tien mat") || t.contains("cash"))) {
                        ptTienMat = p;
                    }
                    if (ptChuyenKhoan == null && (t.contains("chuyển khoản") || t.contains("ngân hàng") || t.contains("chuyen khoan") || t.contains("qr"))) {
                        ptChuyenKhoan = p;
                    }
                }
            }

            if (ptTienMat == null && !allPts.isEmpty()) ptTienMat = allPts.get(0);
            if (ptChuyenKhoan == null && allPts.size() > 1) {
                ptChuyenKhoan = allPts.get(1);
            } else if (ptChuyenKhoan == null && !allPts.isEmpty()) {
                ptChuyenKhoan = allPts.get(0);
            }

            BigDecimal tienMatVal = dto.getTienMat() != null ? dto.getTienMat() : BigDecimal.ZERO;
            BigDecimal tienCkVal = dto.getTienChuyenKhoan() != null ? dto.getTienChuyenKhoan() : BigDecimal.ZERO;
            BigDecimal tongThanhToanDon = saved.getTienSauGiamGia() != null ? saved.getTienSauGiamGia()
                    : (saved.getTongTien() != null ? saved.getTongTien() : BigDecimal.ZERO);

            String ptLower = ptName.toLowerCase();
            boolean isCombined = ptLower.contains("kết hợp") || ptLower.contains("ket_hop") || ptLower.contains("ket hop")
                    || (tienMatVal.compareTo(BigDecimal.ZERO) > 0 && tienCkVal.compareTo(BigDecimal.ZERO) > 0);

            boolean isTransfer = !isCombined && (ptLower.contains("chuyển khoản") || ptLower.contains("chuyen khoan")
                    || ptLower.contains("chuyen_khoan") || ptLower.contains("vietqr")
                    || ptLower.contains("qr") || ptLower.contains("vnpay")
                    || ptLower.contains("ngân hàng") || ptLower.contains("ngan hang")
                    || ptLower.contains("bank") || ptLower.contains("momo")
                    || (tienCkVal.compareTo(BigDecimal.ZERO) > 0 && tienMatVal.compareTo(BigDecimal.ZERO) == 0));

            if (isCombined) {
                if (ptTienMat != null && tienMatVal.compareTo(BigDecimal.ZERO) > 0) {
                    HinhThucThanhToan htttCash = new HinhThucThanhToan();
                    htttCash.setIdHoaDon(saved);
                    htttCash.setIdPhuongThuc(ptTienMat);
                    htttCash.setSoTien(tienMatVal);
                    htttCash.setMaGiaoDich("CASH-" + System.currentTimeMillis());
                    htttCash.setThoiGian(Instant.now());
                    htttCash.setTrangThai(1);
                    hinhThucThanhToanRepository.save(htttCash);
                }

                if (ptChuyenKhoan != null && tienCkVal.compareTo(BigDecimal.ZERO) > 0) {
                    HinhThucThanhToan htttCk = new HinhThucThanhToan();
                    htttCk.setIdHoaDon(saved);
                    htttCk.setIdPhuongThuc(ptChuyenKhoan);
                    htttCk.setSoTien(tienCkVal);
                    htttCk.setMaGiaoDich("TRANS-" + System.currentTimeMillis());
                    htttCk.setThoiGian(Instant.now());
                    htttCk.setTrangThai(1);
                    hinhThucThanhToanRepository.save(htttCk);
                }
            } else if (isTransfer) {
                PhuongThucThanhToan chosenPt = ptChuyenKhoan != null ? ptChuyenKhoan : ptTienMat;
                BigDecimal amount = tienCkVal.compareTo(BigDecimal.ZERO) > 0 ? tienCkVal : tongThanhToanDon;
                HinhThucThanhToan httt = new HinhThucThanhToan();
                httt.setIdHoaDon(saved);
                httt.setIdPhuongThuc(chosenPt);
                httt.setSoTien(amount);
                httt.setMaGiaoDich("TRANS-" + System.currentTimeMillis());
                httt.setThoiGian(Instant.now());
                httt.setTrangThai(1);
                hinhThucThanhToanRepository.save(httt);
            } else {
                PhuongThucThanhToan chosenPt = ptTienMat;
                BigDecimal amount = tienMatVal.compareTo(BigDecimal.ZERO) > 0 ? tienMatVal : tongThanhToanDon;
                if (amount.compareTo(tongThanhToanDon) > 0 && tongThanhToanDon.compareTo(BigDecimal.ZERO) > 0) {
                    amount = tongThanhToanDon;
                }
                HinhThucThanhToan httt = new HinhThucThanhToan();
                httt.setIdHoaDon(saved);
                httt.setIdPhuongThuc(chosenPt);
                httt.setSoTien(amount);
                httt.setMaGiaoDich("CASH-" + System.currentTimeMillis());
                httt.setThoiGian(Instant.now());
                httt.setTrangThai(1);
                hinhThucThanhToanRepository.save(httt);
            }
        } catch (Exception ignored) {}



        // 9. Lưu lịch sử đơn hàng



        try {



            String staffName = (dto.getNguoiTao() != null && !dto.getNguoiTao().trim().isEmpty())



                    ? dto.getNguoiTao().trim()



                    : ((saved.getIdNhanVien() != null && saved.getIdNhanVien().getTenNhanVien() != null)



                    ? saved.getIdNhanVien().getTenNhanVien()



                    : (saved.getNguoiTao() != null && !saved.getNguoiTao().isEmpty() ? saved.getNguoiTao() : "Nhân viên bán hàng"));



            boolean alreadyLoggedCreated = false;



            try {



                List<LichSuHoaDon> existingLogs = lichSuHoaDonRepository.findByIdHoaDon_IdOrderByThoiGianAsc(saved.getId());



                if (existingLogs != null && !existingLogs.isEmpty()) {



                    for (LichSuHoaDon ex : existingLogs) {



                        if (ex.getGhiChu() != null && ex.getGhiChu().contains("Tạo hóa đơn")) {



                            alreadyLoggedCreated = true;



                            if (staffName != null && !staffName.equals("Nhân viên bán hàng") && "Nhân viên bán hàng".equals(ex.getNguoiThucHien())) {



                                ex.setNguoiThucHien(staffName);



                                lichSuHoaDonRepository.save(ex);



                            }



                            break;



                        }



                    }



                }



            } catch (Exception ignored) {}



            if (!alreadyLoggedCreated) {



                LichSuHoaDon log1 = new LichSuHoaDon();



                log1.setIdHoaDon(saved);



                log1.setTrangThai(1);



                log1.setThoiGian(saved.getNgayTao() != null ? saved.getNgayTao() : Instant.now());



                log1.setGhiChu(!isDelivery ? "Tạo hóa đơn tại quầy" : "Tạo đơn giao hàng tại quầy");



                log1.setNguoiThucHien(staffName);



                log1.setVaiTroNguoiThucHien("Nhân viên");



                lichSuHoaDonRepository.save(log1);



            }



            if (Integer.valueOf(1).equals(saved.getTrangThaiThanhToan())) {



                LichSuHoaDon logPay = new LichSuHoaDon();



                logPay.setIdHoaDon(saved);



                logPay.setTrangThai(saved.getTrangThai());



                logPay.setThoiGian(Instant.now());



                String pMethod = dto.getPhuongThucThanhToan() != null && !dto.getPhuongThucThanhToan().isEmpty()



                        ? dto.getPhuongThucThanhToan() : "Tiền mặt";



                BigDecimal paidAmount = saved.getTienSauGiamGia() != null ? saved.getTienSauGiamGia() : saved.getTongTien();



                logPay.setGhiChu(String.format("Thanh toán thành công qua %s (%,d đ)", pMethod, paidAmount != null ? paidAmount.longValue() : 0));



                logPay.setNguoiThucHien(staffName);



                logPay.setVaiTroNguoiThucHien("Nhân viên");



                lichSuHoaDonRepository.save(logPay);



            }



            if (Integer.valueOf(2).equals(saved.getTrangThai())) {



                LichSuHoaDon log2 = new LichSuHoaDon();



                log2.setIdHoaDon(saved);



                log2.setTrangThai(2);



                log2.setThoiGian(Instant.now());



                log2.setGhiChu("Đã xác nhận đơn hàng giao tại quầy");



                log2.setNguoiThucHien(staffName);



                log2.setVaiTroNguoiThucHien("Nhân viên");



                lichSuHoaDonRepository.save(log2);



            }



            if (Integer.valueOf(5).equals(saved.getTrangThai())) {



                LichSuHoaDon log2 = new LichSuHoaDon();



                log2.setIdHoaDon(saved);



                log2.setTrangThai(5);



                log2.setThoiGian(Instant.now());



                log2.setGhiChu("Hoàn thành đơn hàng tại quầy");



                log2.setNguoiThucHien(staffName);



                log2.setVaiTroNguoiThucHien("Nhân viên");



                lichSuHoaDonRepository.save(log2);



            }



        } catch (Exception ignored) {}



        if (!isDelivery) {



            saved.setTrangThai(5);



            saved.setTrangThaiThanhToan(1);



        }



        saved = hoaDonRepository.saveAndFlush(saved);




        // Trừ 1 lượt sử dụng của phiếu giảm giá khi bán hàng tại quầy thành công
        if (saved.getIdPhieuGiamGia() != null) {
            try {
                PhieuGiamGia pgg = saved.getIdPhieuGiamGia();
                int currentQty = pgg.getSoLuongSuDung() != null ? pgg.getSoLuongSuDung() : 0;
                int newQty = Math.max(0, currentQty - 1);
                pgg.setSoLuongSuDung(newQty);
                if (newQty <= 0) {
                    pgg.setTrangThai(0); // Hết lượt sử dụng -> ngừng hoạt động
                }
                phieuGiamGiaRepository.saveAndFlush(pgg);
            } catch (Exception e) {
                System.err.println("Lỗi khi cập nhật số lượng phiếu giảm giá POS: " + e.getMessage());
            }
        }
        return saved;



    }



    private HoaDon oldLogHistoryDummy(HoaDon saved, int loaiDon) {



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



    // Tạo đơn hàng online từ Website FF T-Shirt



    @Transactional



    public HoaDon createHoaDonOnline(DatHangOnlineDTO dto) {



        if (dto == null) {



            throw new IllegalArgumentException("Dữ liệu đặt hàng không hợp lệ");



        }



        HoaDon hoaDon = new HoaDon();



        hoaDon.setMaHoaDon(generateNextMaHoaDon());



        hoaDon.setLoaiDon(2); // 2 = Bán online



        hoaDon.setTrangThai(1); // 1 = Chờ xác nhận / Chờ xử lý



        String pt = dto.getPhuongThucThanhToan() != null && !dto.getPhuongThucThanhToan().trim().isEmpty()



                ? dto.getPhuongThucThanhToan().trim() : "COD";



        hoaDon.setTrangThaiThanhToan(0); // 0 = Chưa thanh toán



        // Gán khách hàng nếu có



        if (dto.getIdKhachHang() != null) {



            khachHangRepository.findById(dto.getIdKhachHang()).ifPresent(hoaDon::setIdKhachHang);



        }



        if (false && hoaDon.getIdKhachHang() == null && dto.getSoDienThoai() != null && !dto.getSoDienThoai().trim().isEmpty()) {



            khachHangRepository.findBySoDienThoai(dto.getSoDienThoai().trim()).ifPresent(hoaDon::setIdKhachHang);

        }

        if (false && hoaDon.getIdKhachHang() == null && dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {

            khachHangRepository.findByEmail(dto.getEmail().trim()).ifPresent(hoaDon::setIdKhachHang);

        }

        if (false && hoaDon.getIdKhachHang() == null && dto.getSoDienThoai() != null && !dto.getSoDienThoai().trim().isEmpty()) {

            try {

                KhachHang newKh = new KhachHang();

                newKh.setMaKhachHang("KH" + (System.currentTimeMillis() % 1000000));

                newKh.setTenKhachHang(dto.getTenKhachHang() != null && !dto.getTenKhachHang().trim().isEmpty() ? dto.getTenKhachHang().trim() : "Khách online");

                newKh.setSoDienThoai(dto.getSoDienThoai().trim());

                if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {

                    newKh.setEmail(dto.getEmail().trim());

                }

                newKh.setTrangThai(1);

                newKh.setNgayTao(Instant.now());

                newKh.setNguoiTao("Website Online");

                KhachHang savedKh = khachHangRepository.save(newKh);

                hoaDon.setIdKhachHang(savedKh);

            } catch (Exception ignored) {}

        }

        if (hoaDon.getIdKhachHang() != null && dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {

            KhachHang kh = hoaDon.getIdKhachHang();

            if (kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {

                kh.setEmail(dto.getEmail().trim());

                try {

                    khachHangRepository.save(kh);

                } catch (Exception ignored) {}

            }



        }



        hoaDon.setTenKhachHang(dto.getTenKhachHang() != null ? dto.getTenKhachHang().trim() : "Khách hàng online");



        hoaDon.setSoDienThoaiKhachHang(dto.getSoDienThoai() != null ? dto.getSoDienThoai().trim() : "");



        hoaDon.setEmail(dto.getEmail() != null && !dto.getEmail().trim().isEmpty() ? dto.getEmail().trim() : null);



        hoaDon.setDiaChiNhanHang(dto.getDiaChiNhanHang() != null ? dto.getDiaChiNhanHang().trim() : "");



        BigDecimal phiShip = dto.getPhiVanChuyen() != null ? dto.getPhiVanChuyen() : BigDecimal.ZERO;



        BigDecimal tongTien = dto.getTongTien() != null ? dto.getTongTien() : BigDecimal.ZERO;



        BigDecimal giamGia = dto.getGiamGia() != null ? dto.getGiamGia() : BigDecimal.ZERO;



        hoaDon.setPhiVanChuyen(phiShip);



        hoaDon.setTongTien(tongTien);



        if (dto.getIdPhieuGiamGia() != null) {

            phieuGiamGiaRepository.findById(dto.getIdPhieuGiamGia()).ifPresent(hoaDon::setIdPhieuGiamGia);

        }



        if (dto.getDonViVanChuyen() != null && !dto.getDonViVanChuyen().trim().isEmpty()) {

            hoaDon.setDonViVanChuyen(dto.getDonViVanChuyen().trim());

        }



        BigDecimal tienSauGiam = tongTien.subtract(giamGia).add(phiShip);



        if (tienSauGiam.compareTo(BigDecimal.ZERO) < 0) tienSauGiam = BigDecimal.ZERO;



        hoaDon.setTienSauGiamGia(tienSauGiam);



        String note = (dto.getGhiChu() != null && !dto.getGhiChu().trim().isEmpty())



                ? dto.getGhiChu().trim() + " [Phương thức: " + pt + "]"



                : "Đơn đặt hàng online qua website [Phương thức: " + pt + "]";



        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {

            note += " [EMAIL: " + dto.getEmail().trim() + "]";

        }

        hoaDon.setGhiChu(note);



        // Tự động phân công nhân viên nhận đơn theo ca trực hoặc nhân viên bán hàng hoạt động



        NhanVien assignedStaff = null;



        try {



            if (lichSuCaLamViecRepository != null) {



                Optional<com.example.sd009thuan.entity.LichSuCaLamViec> openShift = lichSuCaLamViecRepository.findTopByThoiGianDongCaIsNullOrderByThoiGianMoCaDesc();



                if (openShift.isPresent() && openShift.get().getNhanVien() != null) {



                    assignedStaff = openShift.get().getNhanVien();



                }



            }



        } catch (Exception ignored) {}



        if (assignedStaff == null) {



            try {



                List<NhanVien> activeStaffs = nhanVienRepository.findAll();



                for (NhanVien nv : activeStaffs) {



                    if (Integer.valueOf(1).equals(nv.getTrangThai())) {



                        assignedStaff = nv;



                        break;



                    }



                }



            } catch (Exception ignored) {}



        }



        if (assignedStaff != null) {



            hoaDon.setIdNhanVien(null);



        }



        hoaDon.setNgayTao(Instant.now());



        hoaDon.setNgayCapNhat(Instant.now());



        hoaDon.setNguoiTao("Khách hàng online");



        hoaDon.setNguoiCapNhat("Khách hàng online");



        HoaDon saved = hoaDonRepository.save(hoaDon);



        // Lưu chi tiết & trừ tồn kho



        if (dto.getChiTiet() != null && !dto.getChiTiet().isEmpty()) {



            int itemIndex = 1;



            BigDecimal calculatedTotal = BigDecimal.ZERO;



            for (DatHangOnlineDTO.ItemChiTiet item : dto.getChiTiet()) {



                if (item.getIdChiTietSanPham() == null) continue;



                Optional<ChiTietSanPham> ctspOpt = chiTietSanPhamRepository.findById(item.getIdChiTietSanPham());



                if (ctspOpt.isEmpty()) continue;



                ChiTietSanPham ctsp = ctspOpt.get();



                // Kiểm tra trạng thái kinh doanh của biến thể và sản phẩm cha

                if (ctsp.getTrangThai() != null && ctsp.getTrangThai() == 0) {

                    String spName = ctsp.getIdSanPham() != null ? ctsp.getIdSanPham().getTenSanPham() : "Sản phẩm";

                    throw new IllegalArgumentException("Sản phẩm '" + spName + "' (biến thể này) đã ngừng kinh doanh, không thể đặt hàng!");

                }

                if (ctsp.getIdSanPham() != null && ctsp.getIdSanPham().getTrangThai() != null && ctsp.getIdSanPham().getTrangThai() == 0) {

                    String spName = ctsp.getIdSanPham().getTenSanPham();

                    throw new IllegalArgumentException("Sản phẩm '" + spName + "' đã ngừng kinh doanh, không thể đặt hàng!");

                }



                int currentAvailable = ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0;



                int qty = item.getSoLuong() != null ? Math.max(1, item.getSoLuong()) : 1;



                if (currentAvailable <= 0) {



                    String spName = ctsp.getIdSanPham() != null ? ctsp.getIdSanPham().getTenSanPham() : "Sản phẩm";



                    throw new IllegalArgumentException("Sản phẩm '" + spName + "' hiện đã hết hàng trong kho!");



                }



                if (qty > currentAvailable) {



                    String spName = ctsp.getIdSanPham() != null ? ctsp.getIdSanPham().getTenSanPham() : "Sản phẩm";



                    throw new IllegalArgumentException("Sản phẩm '" + spName + "' không đủ số lượng trong kho (chỉ còn " + currentAvailable + ")!");



                }



                BigDecimal unitPrice = item.getDonGia() != null && item.getDonGia().compareTo(BigDecimal.ZERO) > 0



                        ? item.getDonGia() : (ctsp.getGiaBan() != null ? ctsp.getGiaBan() : BigDecimal.ZERO);



                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));



                calculatedTotal = calculatedTotal.add(lineTotal);



                ChiTietHoaDon cthd = new ChiTietHoaDon();



                cthd.setIdHoaDon(saved);



                cthd.setIdChiTietSanPham(ctsp);



                cthd.setMaHoaDonChiTiet(String.format("CTHD%s-%02d-%04d", saved.getMaHoaDon(), itemIndex++, (int)(System.currentTimeMillis() % 10000)));



                cthd.setDonGia(unitPrice);



                cthd.setSoLuong(qty);



                cthd.setThanhTien(lineTotal);



                cthd.setTrangThai(1);



                if (saved.getChiTietHoaDons() != null) {



                    saved.getChiTietHoaDons().add(cthd);



                }



                chiTietHoaDonRepository.save(cthd);

                // THEO YÊU CẦU: Không trừ tồn kho khi tạo đơn hàng online (trạng thái Chờ xác nhận).
                // Số lượng tồn kho sẽ chỉ được trừ khi nhân viên duyệt xác nhận đơn hàng tại màn Admin (chuyển sang trạng thái 2).



            }



            if (calculatedTotal.compareTo(BigDecimal.ZERO) > 0 && (saved.getTongTien() == null || saved.getTongTien().compareTo(BigDecimal.ZERO) == 0)) {



                saved.setTongTien(calculatedTotal);



                BigDecimal finalAmount = calculatedTotal.add(phiShip).subtract(giamGia);



                if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;



                saved.setTienSauGiamGia(finalAmount);



                saved = hoaDonRepository.save(saved);



            }



        }



        // Lưu lịch sử hóa đơn



        try {



            LichSuHoaDon log1 = new LichSuHoaDon();



            log1.setIdHoaDon(saved);



            log1.setTrangThai(1);



            log1.setThoiGian(saved.getNgayTao());



            log1.setGhiChu("Khách hàng đặt đơn hàng online (Hình thức: " + pt + ")");



            log1.setNguoiThucHien("Khách hàng online");



            log1.setVaiTroNguoiThucHien("Khách hàng");



            lichSuHoaDonRepository.save(log1);



            if (false && assignedStaff != null) {



                LichSuHoaDon assignLog = new LichSuHoaDon();



                assignLog.setIdHoaDon(saved);



                assignLog.setTrangThai(1);



                assignLog.setThoiGian(saved.getNgayTao());



                assignLog.setGhiChu("Hệ thống tự động phân công nhân viên " + assignedStaff.getTenNhanVien() + " (" + assignedStaff.getMaNhanVien() + ") tiếp nhận đơn hàng");



                assignLog.setNguoiThucHien("Hệ thống");



                assignLog.setVaiTroNguoiThucHien("Hệ thống");



                lichSuHoaDonRepository.save(assignLog);



            }



        } catch (Exception ignored) {}



        // Lưu hình thức thanh toán ban đầu nếu COD hoặc MANUAL



        if (!pt.equalsIgnoreCase("VNPAY")) {



            try {



                List<PhuongThucThanhToan> allPts = phuongThucThanhToanRepository.findAll();



                PhuongThucThanhToan matchedPt = null;



                for (PhuongThucThanhToan p : allPts) {
                    if (p.getTenPhuongThuc() != null) {
                        String t = p.getTenPhuongThuc().toLowerCase();
                        if ((pt.equalsIgnoreCase("VIETQR") || pt.equalsIgnoreCase("CHUYEN_KHOAN") || pt.equalsIgnoreCase("Chuyển khoản"))
                                && (t.contains("chuyển khoản") || t.contains("ngân hàng") || t.contains("qr"))) {
                            matchedPt = p;
                            break;
                        } else if (p.getTenPhuongThuc().equalsIgnoreCase(pt)) {
                            matchedPt = p;
                            break;
                        }
                    }
                }

                if (matchedPt == null && !allPts.isEmpty()) {
                    if (pt.equalsIgnoreCase("VIETQR") || pt.equalsIgnoreCase("CHUYEN_KHOAN") || pt.equalsIgnoreCase("Chuyển khoản")) {
                        matchedPt = allPts.size() > 1 ? allPts.get(1) : allPts.get(0);
                    } else {
                        matchedPt = allPts.get(0);
                    }
                }



                HinhThucThanhToan httt = new HinhThucThanhToan();



                httt.setIdHoaDon(saved);



                httt.setIdPhuongThuc(matchedPt);



                httt.setSoTien(saved.getTienSauGiamGia() != null ? saved.getTienSauGiamGia() : BigDecimal.ZERO);



                httt.setMaGiaoDich(pt + "-" + System.currentTimeMillis());



                httt.setThoiGian(Instant.now());



                httt.setTrangThai(0);



                hinhThucThanhToanRepository.save(httt);



            } catch (Exception ignored) {}



        }




        // Gửi email xác nhận đặt hàng online ngay lập tức cho khách hàng
        try {
            String recipientEmail = null;
            if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
                recipientEmail = dto.getEmail().trim();
            } else if (saved.getEmailKhachHang() != null && !saved.getEmailKhachHang().trim().isEmpty()) {
                recipientEmail = saved.getEmailKhachHang().trim();
            } else if (saved.getEmail() != null && !saved.getEmail().trim().isEmpty()) {
                recipientEmail = saved.getEmail().trim();
            }

            if (recipientEmail != null && recipientEmail.contains("@") && emailService != null) {
                saved.setEmail(recipientEmail);
                List<ChiTietHoaDon> items = chiTietHoaDonRepository.findByIdHoaDon_Id(saved.getId());
                if (items == null || items.isEmpty()) {
                    items = saved.getChiTietHoaDons() != null ? new java.util.ArrayList<>(saved.getChiTietHoaDons()) : java.util.Collections.emptyList();
                }
                System.out.println("==> [createHoaDonOnline] Đang gửi email xác nhận đặt hàng online " + saved.getMaHoaDon() + " tới: " + recipientEmail);
                emailService.sendOrderConfirmationEmail(saved, items, recipientEmail);
            } else {
                System.out.println("==> [createHoaDonOnline] Bỏ qua gửi email: Không có địa chỉ email hợp lệ cho đơn hàng " + saved.getMaHoaDon());
            }
        } catch (Exception e) {
            System.err.println("==> [createHoaDonOnline] Lỗi gửi email xác nhận đặt hàng online: " + e.getMessage());
        }




        // Trừ 1 lượt sử dụng của phiếu giảm giá khi đặt hàng online thành công
        if (saved.getIdPhieuGiamGia() != null) {
            try {
                PhieuGiamGia pgg = saved.getIdPhieuGiamGia();
                int currentQty = pgg.getSoLuongSuDung() != null ? pgg.getSoLuongSuDung() : 0;
                int newQty = Math.max(0, currentQty - 1);
                pgg.setSoLuongSuDung(newQty);
                if (newQty <= 0) {
                    pgg.setTrangThai(0);
                }
                phieuGiamGiaRepository.saveAndFlush(pgg);
            } catch (Exception e) {
                System.err.println("Lỗi khi cập nhật số lượng phiếu giảm giá Online: " + e.getMessage());
            }
        }
        return saved;



    }




    public Map<String, Object> checkStockForOrder(Long idHoaDon) {
        Map<String, Object> result = new HashMap<>();
        List<ChiTietHoaDon> items = chiTietHoaDonRepository.findByIdHoaDon_Id(idHoaDon);
        boolean duHang = true;
        List<Map<String, Object>> itemList = new ArrayList<>();

        if (items != null) {
            for (ChiTietHoaDon cthd : items) {
                if (cthd.getIdChiTietSanPham() == null) continue;
                ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(cthd.getIdChiTietSanPham().getId()).orElse(null);
                if (ctsp == null) continue;

                int available = ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0;
                int needed = cthd.getSoLuong() != null ? cthd.getSoLuong() : 1;
                boolean hetHang = (available < needed) || (available <= 0);
                if (hetHang) {
                    duHang = false;
                }

                Map<String, Object> itMap = new HashMap<>();
                itMap.put("chiTietId", cthd.getId());
                itMap.put("variantId", ctsp.getId());
                itMap.put("tenSanPham", ctsp.getIdSanPham() != null ? ctsp.getIdSanPham().getTenSanPham() : "");
                itMap.put("maSanPham", ctsp.getIdSanPham() != null ? ctsp.getIdSanPham().getMaSanPham() : "");
                itMap.put("tenMauSac", ctsp.getIdMauSac() != null ? ctsp.getIdMauSac().getTenMauSac() : "");
                itMap.put("tenKichCo", ctsp.getIdKichCo() != null ? ctsp.getIdKichCo().getTenKichCo() : "");
                itMap.put("soLuongDat", needed);
                itMap.put("soLuongTon", available);
                itMap.put("hetHang", hetHang);
                itemList.add(itMap);
            }
        }

        result.put("duHang", duHang);
        result.put("items", itemList);
        return result;
    }

    @Transactional
    public void checkStockAndDeductForConfirmation(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getId() == null) return;
        List<ChiTietHoaDon> items = chiTietHoaDonRepository.findByIdHoaDon_Id(hoaDon.getId());
        if (items == null || items.isEmpty()) return;

        // 1. Kiểm tra tồn kho của tất cả các sản phẩm trong đơn xem có đủ không
        for (ChiTietHoaDon cthd : items) {
            if (cthd.getIdChiTietSanPham() == null || cthd.getIdChiTietSanPham().getId() == null) continue;
            ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(cthd.getIdChiTietSanPham().getId()).orElse(null);
            if (ctsp == null) continue;

            int available = ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0;
            int needed = cthd.getSoLuong() != null ? cthd.getSoLuong() : 1;

            if (available < needed || available <= 0) {
                String spName = (ctsp.getIdSanPham() != null && ctsp.getIdSanPham().getTenSanPham() != null)
                        ? ctsp.getIdSanPham().getTenSanPham() : "Sản phẩm";
                String color = (ctsp.getIdMauSac() != null && ctsp.getIdMauSac().getTenMauSac() != null)
                        ? ctsp.getIdMauSac().getTenMauSac() : "";
                String size = (ctsp.getIdKichCo() != null && ctsp.getIdKichCo().getTenKichCo() != null)
                        ? ctsp.getIdKichCo().getTenKichCo() : "";
                String variantDesc = (!color.isEmpty() || !size.isEmpty()) ? " (" + color + " - " + size + ")" : "";

                throw new IllegalStateException("Sản phẩm '" + spName + "'" + variantDesc
                                                + " hiện không đủ số lượng trong kho (chỉ còn " + available + ", đơn cần " + needed + ")! Không thể xác nhận đơn hàng.");
            }
        }

        // 2. Tất cả sản phẩm đều đủ hàng -> Trừ số lượng tồn kho tại màn admin
        for (ChiTietHoaDon cthd : items) {
            if (cthd.getIdChiTietSanPham() == null || cthd.getIdChiTietSanPham().getId() == null) continue;
            ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(cthd.getIdChiTietSanPham().getId()).orElse(null);
            if (ctsp == null) continue;

            int needed = cthd.getSoLuong() != null ? cthd.getSoLuong() : 1;
            int currentStock = ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0;
            ctsp.setSoLuong(Math.max(0, currentStock - needed));
            chiTietSanPhamRepository.save(ctsp);
        }
    }

    @Transactional
    public void refundStockForCancellation(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getId() == null) return;
        List<ChiTietHoaDon> items = chiTietHoaDonRepository.findByIdHoaDon_Id(hoaDon.getId());
        if (items == null || items.isEmpty()) return;
        for (ChiTietHoaDon cthd : items) {
            if (cthd.getIdChiTietSanPham() == null || cthd.getIdChiTietSanPham().getId() == null || cthd.getSoLuong() == null) continue;
            ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(cthd.getIdChiTietSanPham().getId()).orElse(null);
            if (ctsp == null) continue;
            int cur = ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0;
            ctsp.setSoLuong(cur + cthd.getSoLuong());
            chiTietSanPhamRepository.save(ctsp);
        }
    }

    @Transactional
    public HoaDon khachSuaThongTinDonHang(String maHoaDon, KhachSuaDonDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật không hợp lệ!");
        }

        HoaDon hoaDon = hoaDonRepository.findByMaHoaDon(maHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với mã: " + maHoaDon));

        // 1. Kiểm tra loại đơn: Phải là đơn Online
        boolean isOnline = Integer.valueOf(2).equals(hoaDon.getLoaiDon()) 
                || (hoaDon.getGhiChu() != null && hoaDon.getGhiChu().toLowerCase().contains("online"));
        if (!isOnline) {
            throw new IllegalStateException("Chỉ áp dụng sửa thông tin cho đơn hàng đặt online!");
        }

        // 2. Kiểm tra trạng thái: Phải ở trạng thái Chờ xác nhận (1)
        if (!Integer.valueOf(1).equals(hoaDon.getTrangThai())) {
            throw new IllegalStateException("Đơn hàng đã được nhân viên tiếp nhận hoặc đã chuyển trạng thái, không thể chỉnh sửa nữa!");
        }

        // 3. Kiểm tra số lần sửa: Chỉ được sửa 1 lần duy nhất
        if (hoaDon.getSoLanSuaThongTin() != null && hoaDon.getSoLanSuaThongTin() >= 1) {
            throw new IllegalStateException("Đơn hàng chỉ được phép chỉnh sửa thông tin nhận hàng 1 lần duy nhất!");
        }

        // Lưu lại thông tin cũ để ghi log lịch sử
        String tenCu = hoaDon.getTenKhachHangHienThi();
        String sdtCu = hoaDon.getSoDienThoaiHienThi();
        String diaChiCu = hoaDon.getDiaChiNhanHang();
        BigDecimal phiShipCu = hoaDon.getPhiVanChuyen() != null ? hoaDon.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal phiShipMoi = dto.getPhiVanChuyenMoi() != null ? dto.getPhiVanChuyenMoi() : phiShipCu;

        // 4. Kiểm tra phương thức thanh toán
        boolean daThanhToan = Integer.valueOf(1).equals(hoaDon.getTrangThaiThanhToan());
        
        if (daThanhToan) {
            // Đơn đã chuyển khoản / VNPAY: Phí ship mới không được cao hơn phí ship ban đầu
            if (phiShipMoi.compareTo(phiShipCu) > 0) {
                java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
                throw new IllegalArgumentException("Đơn hàng đã thanh toán trước. Phí ship địa chỉ mới (" 
                        + nf.format(phiShipMoi) + " đ) cao hơn phí ship đã thanh toán (" 
                        + nf.format(phiShipCu) + " đ). Quý khách không thể đổi sang địa chỉ này!");
            }
        } else {
            // Đơn COD (chưa thanh toán): Cập nhật phí ship mới và tính lại tổng tiền thu COD
            BigDecimal tienHangSauVoucher = hoaDon.getTienSauGiamGia() != null ? hoaDon.getTienSauGiamGia().subtract(phiShipCu) : hoaDon.getTongTien();
            if (tienHangSauVoucher == null || tienHangSauVoucher.compareTo(BigDecimal.ZERO) < 0) {
                tienHangSauVoucher = BigDecimal.ZERO;
            }
            hoaDon.setPhiVanChuyen(phiShipMoi);
            hoaDon.setTienSauGiamGia(tienHangSauVoucher.add(phiShipMoi));
        }

        // 5. Cập nhật thông tin nhận hàng
        if (dto.getTenNguoiNhan() != null && !dto.getTenNguoiNhan().trim().isEmpty()) {
            hoaDon.setTenKhachHang(dto.getTenNguoiNhan().trim());
        }
        if (dto.getSoDienThoaiNhan() != null && !dto.getSoDienThoaiNhan().trim().isEmpty()) {
            hoaDon.setSoDienThoaiKhachHang(dto.getSoDienThoaiNhan().trim());
        }
        if (dto.getDiaChiNhanHang() != null && !dto.getDiaChiNhanHang().trim().isEmpty()) {
            hoaDon.setDiaChiNhanHang(dto.getDiaChiNhanHang().trim());
        }

        // 6. Đánh dấu đã sửa thông tin 1 lần
        hoaDon.setSoLanSuaThongTin(1);
        String note = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : "";
        if (!note.contains("[DA_SUA_THONG_TIN]")) {
            note = "[DA_SUA_THONG_TIN] " + note;
        }
        if (dto.getGhiChu() != null && !dto.getGhiChu().trim().isEmpty()) {
            note += " [Ghi chú mới: " + dto.getGhiChu().trim() + "]";
        }
        hoaDon.setGhiChu(note);
        hoaDon.setNgayCapNhat(Instant.now());
        hoaDon.setNguoiCapNhat("Khách hàng online");

        HoaDon saved = hoaDonRepository.save(hoaDon);

        // 7. Ghi lịch sử đơn hàng (Audit Log)
        try {
            LichSuHoaDon log = new LichSuHoaDon();
            log.setIdHoaDon(saved);
            log.setTrangThai(saved.getTrangThai()); // 1
            log.setThoiGian(Instant.now());
            log.setNguoiThucHien("Khách hàng online");
            log.setVaiTroNguoiThucHien("Khách hàng");
            
            StringBuilder sb = new StringBuilder();
            sb.append("Khách hàng cập nhật thông tin nhận đơn (1 lần duy nhất): ");
            if (dto.getTenNguoiNhan() != null && !dto.getTenNguoiNhan().equals(tenCu)) {
                sb.append("Tên: '").append(tenCu).append("' -> '").append(dto.getTenNguoiNhan()).append("'; ");
            }
            if (dto.getSoDienThoaiNhan() != null && !dto.getSoDienThoaiNhan().equals(sdtCu)) {
                sb.append("SĐT: '").append(sdtCu).append("' -> '").append(dto.getSoDienThoaiNhan()).append("'; ");
            }
            sb.append("Địa chỉ: '").append(diaChiCu != null ? diaChiCu : "").append("' -> '").append(saved.getDiaChiNhanHang()).append("'. ");
            if (!daThanhToan && phiShipCu.compareTo(phiShipMoi) != 0) {
                sb.append("Phí ship: ").append(phiShipCu).append(" đ -> ").append(phiShipMoi).append(" đ (Cập nhật tiền COD).");
            } else if (daThanhToan) {
                sb.append("Đơn đã thanh toán trước (Phí ship ban đầu: ").append(phiShipCu).append(" đ, Phí ship địa chỉ mới: ").append(phiShipMoi).append(" đ).");
            }
            log.setGhiChu(sb.toString());
            lichSuHoaDonRepository.save(log);
        } catch (Exception e) {
            System.err.println("Lỗi lưu lịch sử hóa đơn khi khách sửa thông tin: " + e.getMessage());
        }

        return saved;
    }
}
