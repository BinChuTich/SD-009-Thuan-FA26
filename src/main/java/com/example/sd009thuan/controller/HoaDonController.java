package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.HoaDonPosDTO;
import com.example.sd009thuan.entity.ChiTietHoaDon;
import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.service.HoaDonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/hoa-don")
@CrossOrigin(origins = "*")
public class HoaDonController {

    private final HoaDonService hoaDonService;
    private final com.example.sd009thuan.repository.HoaDonRepository hoaDonRepository;

    public HoaDonController(HoaDonService hoaDonService, com.example.sd009thuan.repository.HoaDonRepository hoaDonRepository) {
        this.hoaDonService = hoaDonService;
        this.hoaDonRepository = hoaDonRepository;
    }

    // Lấy danh sách tất cả hóa đơn
    @GetMapping
    public ResponseEntity<List<HoaDon>> getAll() {
        return ResponseEntity.ok(hoaDonService.getAll());
    }

    // Lấy mã hóa đơn tiếp theo không trùng lặp
    @GetMapping("/next-code")
    public ResponseEntity<Map<String, String>> getNextCode() {
        return ResponseEntity.ok(Map.of("code", hoaDonService.generateNextMaHoaDon()));
    }

    @PostMapping("/{id}/send-payment-reminder")
    public ResponseEntity<?> sendPaymentReminder(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            String email = body != null && body.get("email") != null ? body.get("email").toString() : null;
            boolean sent = hoaDonService.sendUnpaidPaymentReminder(id, email);
            if (!sent) return ResponseEntity.internalServerError().body(Map.of("message", "Không thể gửi email nhắc thanh toán!"));
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã gửi email nhắc thanh toán thành công!"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Lấy hóa đơn theo ID
    @GetMapping("/{id}")
    public ResponseEntity<HoaDon> getById(@PathVariable Long id) {
        return hoaDonService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Lấy hóa đơn theo mã hóa đơn
    @GetMapping("/code/{maHoaDon}")
    public ResponseEntity<HoaDon> getByMaHoaDon(
            @PathVariable String maHoaDon
    ) {
        return hoaDonService.getByMaHoaDon(maHoaDon)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Lấy danh sách sản phẩm chi tiết của hóa đơn theo ID
    @GetMapping("/{id}/chi-tiet")
    public ResponseEntity<List<ChiTietHoaDon>> getChiTietByHoaDonId(@PathVariable Long id) {
        return ResponseEntity.ok(hoaDonService.getChiTietByHoaDonId(id));
    }

    // Lấy danh sách sản phẩm chi tiết của hóa đơn theo Mã hóa đơn
    @GetMapping("/code/{maHoaDon}/chi-tiet")
    public ResponseEntity<List<ChiTietHoaDon>> getChiTietByMaHoaDon(@PathVariable String maHoaDon) {
        return ResponseEntity.ok(hoaDonService.getChiTietByMaHoaDon(maHoaDon));
    }

    // Lấy lịch sử hóa đơn theo ID
    @GetMapping("/{id}/lich-su")
    public ResponseEntity<List<com.example.sd009thuan.entity.LichSuHoaDon>> getLichSuByHoaDonId(@PathVariable Long id) {
        return ResponseEntity.ok(hoaDonService.getLichSuByHoaDonId(id));
    }

    // Lấy lịch sử hóa đơn theo Mã hóa đơn
    @GetMapping("/code/{maHoaDon}/lich-su")
    public ResponseEntity<List<com.example.sd009thuan.entity.LichSuHoaDon>> getLichSuByMaHoaDon(@PathVariable String maHoaDon) {
        return ResponseEntity.ok(hoaDonService.getLichSuByMaHoaDon(maHoaDon));
    }

    // Lấy hình thức thanh toán theo ID hóa đơn
    @GetMapping("/{id}/thanh-toan")
    public ResponseEntity<List<com.example.sd009thuan.entity.HinhThucThanhToan>> getThanhToanByHoaDonId(@PathVariable Long id) {
        return ResponseEntity.ok(hoaDonService.getThanhToanByHoaDonId(id));
    }

    // Lấy hình thức thanh toán theo Mã hóa đơn
    @GetMapping("/code/{maHoaDon}/thanh-toan")
    public ResponseEntity<List<com.example.sd009thuan.entity.HinhThucThanhToan>> getThanhToanByMaHoaDon(@PathVariable String maHoaDon) {
        return ResponseEntity.ok(hoaDonService.getThanhToanByMaHoaDon(maHoaDon));
    }

    // Tạo hóa đơn mới
    @PostMapping
    public ResponseEntity<HoaDon> create(@RequestBody HoaDon hoaDon) {
        return ResponseEntity.ok(hoaDonService.create(hoaDon));
    }

    // Hủy tất cả hóa đơn chờ của ngày hôm trước khi sang ngày mới
    // Hủy đơn online VNPAY, MoMo hoặc VietQR quá hạn 5 phút chưa thanh toán.
    @PostMapping("/code/{maHoaDon}/cancel-expired-vnpay")
    public ResponseEntity<?> cancelExpiredVnPayByMa(@PathVariable String maHoaDon) {
        Optional<HoaDon> opt = hoaDonService.getByMaHoaDon(maHoaDon);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        HoaDon hd = opt.get();
        if (hoaDonService.isExpiredVnPayOrder(hd)) {
            hoaDonService.cancelSingleVnPayOrder(hd, "Hệ thống tự động hủy đơn do quá hạn thanh toán online (5 phút)");
            return ResponseEntity.ok(Map.of("cancelled", true, "order", hoaDonService.getByMaHoaDon(maHoaDon).orElse(hd)));
        }
        return ResponseEntity.ok(Map.of("cancelled", false, "order", hd));
    }

    @PostMapping("/cancel-expired-waiting")
    public ResponseEntity<Map<String, Object>> cancelExpiredWaiting() {
        List<HoaDon> cancelled = hoaDonService.cancelExpiredWaitingInvoices();
        List<String> codes = cancelled.stream().map(HoaDon::getMaHoaDon).toList();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", cancelled.size(),
                "cancelledCodes", codes,
                "message", "Đã hủy " + cancelled.size() + " hóa đơn chờ của ngày trước"
        ));
    }

    // Lấy danh sách hóa đơn chờ tại quầy
    @GetMapping("/waiting")
    public ResponseEntity<List<HoaDon>> getWaitingInvoices() {
        return ResponseEntity.ok(hoaDonService.getWaitingInvoices());
    }

    // Tạo hóa đơn chờ tại quầy mới
    @PostMapping("/waiting")
    public ResponseEntity<HoaDon> createWaitingInvoice(@RequestBody(required = false) Map<String, Object> body) {
        Long idNhanVien = null;
        String nguoiTao = null;
        Long idGiaoCa = null;
        if (body != null) {
            if (body.get("idNhanVien") != null) {
                try { idNhanVien = Long.valueOf(body.get("idNhanVien").toString()); } catch (Exception ignored) {}
            }
            if (body.get("nguoiTao") != null) {
                nguoiTao = body.get("nguoiTao").toString();
            }
            if (body.get("idGiaoCa") != null) {
                try { idGiaoCa = Long.valueOf(body.get("idGiaoCa").toString()); } catch (Exception ignored) {}
            }
        }
        return ResponseEntity.ok(hoaDonService.createWaitingInvoice(idNhanVien, nguoiTao, idGiaoCa));
    } /*
    }


    }

    */ // Đồng bộ danh sách sản phẩm trong giỏ hàng của hóa đơn chờ
    @PostMapping("/{id}/sync-cart")
    public ResponseEntity<List<ChiTietHoaDon>> syncCart(
            @PathVariable Long id,
            @RequestBody List<HoaDonPosDTO.ItemChiTietDTO> items
    ) {
        return ResponseEntity.ok(hoaDonService.syncCartToInvoice(id, items));
    }

    // Tiền kiểm tra tính hợp lệ toàn diện của đơn hàng POS (Đổi giá, Sửa voucher, Chuyển quyền NV, Đợt giảm giá)
    @PostMapping("/pos/kiem-tra-hop-le")
    public ResponseEntity<Map<String, Object>> kiemTraHopLeDonHangPos(@RequestBody HoaDonPosDTO dto) {
        return ResponseEntity.ok(hoaDonService.kiemTraHopLeDonHangPos(dto));
    }

    // Bàn giao hóa đơn chờ cho nhân viên khác tiếp quản (Case 3)
    @PostMapping("/pos/ban-giao")
    public ResponseEntity<Map<String, Object>> banGiaoHoaDonPos(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(hoaDonService.banGiaoHoaDonPos(body));
    }

    // Lấy danh sách nhân viên đang hoạt động để phục vụ bàn giao hóa đơn
    @GetMapping("/pos/danh-sach-nhan-vien-ban-giao")
    public ResponseEntity<List<Map<String, Object>>> getDanhSachNhanVienBanGiao() {
        return ResponseEntity.ok(hoaDonService.getDanhSachNhanVienKhaDung());
    }

    // Tạo đơn hàng bán tại quầy (POS) kèm danh sách sản phẩm chi tiết, trừ tồn kho và thanh toán
    @PostMapping("/pos")
    public ResponseEntity<HoaDon> createPos(@RequestBody HoaDonPosDTO dto) {
        return ResponseEntity.ok(hoaDonService.createHoaDonPos(dto));
    }

    // Cập nhật đơn vị vận chuyển theo Mã hóa đơn
    @PutMapping("/code/{maHoaDon}/don-vi-van-chuyen")
    public ResponseEntity<?> updateDonViVanChuyenByMa(
            @PathVariable String maHoaDon,
            @RequestBody java.util.Map<String, String> body
    ) {
        String carrier = body != null ? body.get("donViVanChuyen") : null;
        return ResponseEntity.ok(hoaDonService.updateDonViVanChuyenByMa(maHoaDon, carrier));
    }

    // Cập nhật đơn vị vận chuyển theo ID
    @PutMapping("/{id}/don-vi-van-chuyen")
    public ResponseEntity<?> updateDonViVanChuyen(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body
    ) {
        String carrier = body != null ? body.get("donViVanChuyen") : null;
        return ResponseEntity.ok(hoaDonService.updateDonViVanChuyen(id, carrier));
    }

    // Cập nhật thông tin hóa đơn theo ID
    @PutMapping("/{id}")
    public ResponseEntity<HoaDon> update(
            @PathVariable Long id,
            @RequestBody HoaDon hoaDon
    ) {
        try {
            return ResponseEntity.ok(hoaDonService.update(id, hoaDon));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Cập nhật thông tin hóa đơn theo mã hóa đơn
    @PutMapping("/code/{maHoaDon}")
    public ResponseEntity<?> updateByMa(
            @PathVariable String maHoaDon,
            @RequestBody HoaDon hoaDon
    ) {
        Optional<HoaDon> opt = hoaDonService.getByMaHoaDon(maHoaDon);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(hoaDonService.update(opt.get().getId(), hoaDon));
    }

    private static class OrderStatusUpdateParams {
        Integer trangThai;
        String ghiChu;
        String email;
        Long nhanVienId;
        String nguoiThucHien;
        boolean hoanTien;
        boolean guiEmail = true;
    }

    private OrderStatusUpdateParams parseStatusUpdateParams(
            Map<String, Object> body,
            Integer paramTrangThai,
            String paramEmail
    ) {
        OrderStatusUpdateParams params = new OrderStatusUpdateParams();
        params.trangThai = paramTrangThai;
        params.email = paramEmail;

        if (body != null) {
            if (body.get("trangThai") != null) {
                try {
                    params.trangThai = Integer.valueOf(body.get("trangThai").toString());
                } catch (NumberFormatException ignored) {
                    // Bỏ qua nếu trạng thái không phải số nguyên
                }
            }

            if (body.get("ghiChu") != null) {
                params.ghiChu = body.get("ghiChu").toString();
            }

            if (body.get("email") != null && !body.get("email").toString().trim().isEmpty()) {
                params.email = body.get("email").toString().trim();
            } else if (body.get("emailKhachHang") != null && !body.get("emailKhachHang").toString().trim().isEmpty()) {
                params.email = body.get("emailKhachHang").toString().trim();
            }
            if (body.get("idNhanVien") != null) {
                try { params.nhanVienId = Long.valueOf(body.get("idNhanVien").toString()); } catch (Exception ignored) {}
            } else if (body.get("nhanVienId") != null) {
                try { params.nhanVienId = Long.valueOf(body.get("nhanVienId").toString()); } catch (Exception ignored) {}
            }
            if (body.get("nguoiThucHien") != null && !body.get("nguoiThucHien").toString().trim().isEmpty()) {
                params.nguoiThucHien = body.get("nguoiThucHien").toString().trim();
            } else if (body.get("nguoiCapNhat") != null && !body.get("nguoiCapNhat").toString().trim().isEmpty()) {
                params.nguoiThucHien = body.get("nguoiCapNhat").toString().trim();
            }
            if (body.get("hoanTien") != null) {
                Object htObj = body.get("hoanTien");
                params.hoanTien = Boolean.TRUE.equals(htObj) || "true".equalsIgnoreCase(htObj.toString().trim());
            }
            if (body.get("guiEmail") != null) {
                Object geObj = body.get("guiEmail");
                params.guiEmail = !(Boolean.FALSE.equals(geObj) || "false".equalsIgnoreCase(geObj.toString().trim()));
            }
        }

        return params;
    }

    private boolean isOutOfStockException(Exception e) {
        return e.getMessage() != null && (e.getMessage().contains("không đủ số lượng") || e.getMessage().contains("hết hàng"));
    }

    private boolean isUnpaidPrepaymentException(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("chưa ghi nhận thanh toán") || message.contains("không thể xác nhận đơn");
    }

    private boolean isRefundValidationException(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("hoàn tiền trước đó") || message.contains("chưa thanh toán nên không thể thực hiện hoàn tiền");
    }

    // Cập nhật trạng thái hóa đơn theo ID
    @PutMapping("/{id}/trang-thai")
    public ResponseEntity<?> updateTrangThai(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(name = "trangThai", required = false)
            Integer paramTrangThai,
            @RequestParam(name = "email", required = false)
            String paramEmail
    ) {
        OrderStatusUpdateParams params = parseStatusUpdateParams(body, paramTrangThai, paramEmail);

        if (params.trangThai == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Trạng thái không được để trống"
                    ));
        }

        try {
            return ResponseEntity.ok(
                    hoaDonService.updateTrangThai(
                            id,
                            params.trangThai,
                            params.ghiChu,
                            params.email,
                            params.nhanVienId,
                            params.nguoiThucHien,
                            params.hoanTien,
                            params.guiEmail
                    )
            );
        } catch (Exception e) {
            if (isUnpaidPrepaymentException(e)) {
                return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(), "paymentRequired", true));
            }
            if (isRefundValidationException(e)) {
                return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
            }
            if (isOutOfStockException(e)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", e.getMessage() != null ? e.getMessage() : "Sản phẩm trong kho không đủ số lượng!",
                        "outOfStock", true
                ));
            }
            try {
                HoaDon hd = new HoaDon();
                hd.setTrangThai(params.trangThai);
                if (params.ghiChu != null) hd.setGhiChu(params.ghiChu);
                if (params.email != null) hd.setEmail(params.email);
                return ResponseEntity.ok(hoaDonService.update(id, hd));
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message",
                        ex.getMessage() != null ? ex.getMessage() : (e.getMessage() != null ? e.getMessage() : "Cập nhật trạng thái thất bại")
                ));
            }
        }
    }

    // Cập nhật trạng thái hóa đơn theo mã hóa đơn
    @PutMapping("/code/{maHoaDon}/trang-thai")
    public ResponseEntity<?> updateTrangThaiByMa(
            @PathVariable String maHoaDon,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(name = "trangThai", required = false)
            Integer paramTrangThai,
            @RequestParam(name = "email", required = false)
            String paramEmail
    ) {
        OrderStatusUpdateParams params = parseStatusUpdateParams(body, paramTrangThai, paramEmail);

        if (params.trangThai == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Trạng thái không được để trống"
                    ));
        }

        try {
            return ResponseEntity.ok(
                    hoaDonService.updateTrangThaiByMa(
                            maHoaDon,
                            params.trangThai,
                            params.ghiChu,
                            params.email,
                            params.nhanVienId,
                            params.nguoiThucHien,
                            params.hoanTien,
                            params.guiEmail
                    )
            );
        } catch (Exception e) {
            if (isUnpaidPrepaymentException(e)) {
                return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(), "paymentRequired", true));
            }
            if (isRefundValidationException(e)) {
                return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
            }
            if (isOutOfStockException(e)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", e.getMessage() != null ? e.getMessage() : "Sản phẩm trong kho không đủ số lượng!",
                        "outOfStock", true
                ));
            }
            try {
                java.util.Optional<HoaDon> opt = hoaDonService.getByMaHoaDon(maHoaDon);
                if (opt.isPresent()) {
                    HoaDon hd = new HoaDon();
                    hd.setTrangThai(params.trangThai);
                    if (params.ghiChu != null) hd.setGhiChu(params.ghiChu);
                    if (params.email != null) hd.setEmail(params.email);
                    return ResponseEntity.ok(hoaDonService.update(opt.get().getId(), hd));
                }
            } catch (Exception ignored) {}
            return ResponseEntity.badRequest().body(Map.of(
                    "message",
                    e.getMessage() != null ? e.getMessage() : "Cập nhật trạng thái thất bại"
            ));
        }
    }

    // Kiểm tra tồn kho các sản phẩm trong đơn hàng (phục vụ xác nhận đơn)
    @GetMapping("/{id}/check-stock")
    public ResponseEntity<?> checkStockForOrder(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(hoaDonService.checkStockForOrder(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Cập nhật trạng thái thanh toán theo ID
    @PutMapping("/{id}/thanh-toan")
    public ResponseEntity<?> updateTrangThaiThanhToan(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(name = "trangThaiThanhToan", required = false) Integer paramTrangThaiThanhToan
    ) {
        Integer status = paramTrangThaiThanhToan;
        String ghiChu = null;
        if (body != null) {
            if (body.get("trangThaiThanhToan") != null) {
                try {
                    status = Integer.valueOf(body.get("trangThaiThanhToan").toString());
                } catch (NumberFormatException ignored) {}
            }
            if (body.get("ghiChu") != null) ghiChu = body.get("ghiChu").toString();
            if (body.get("ghiChuThanhToan") != null) ghiChu = body.get("ghiChuThanhToan").toString();
            if (body.get("hinhThucThanhToan") != null) {
                String ht = body.get("hinhThucThanhToan").toString();
                ghiChu = (ghiChu != null && !ghiChu.isEmpty()) ? (ht + " - " + ghiChu) : ht;
            }
        }
        if (status == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trạng thái thanh toán không được để trống"));
        }
        try {
            return ResponseEntity.ok(hoaDonService.updateTrangThaiThanhToan(id, status, ghiChu));
        } catch (Exception e) {
            try {
                HoaDon hd = new HoaDon();
                hd.setTrangThaiThanhToan(status);
                if (ghiChu != null) hd.setGhiChu(ghiChu);
                return ResponseEntity.ok(hoaDonService.update(id, hd));
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message",
                        e.getMessage() != null ? e.getMessage() : "Cập nhật trạng thái thanh toán thất bại"
                ));
            }
        }
    }

    // Cập nhật trạng thái thanh toán theo Mã hóa đơn
    @PutMapping("/code/{maHoaDon}/thanh-toan")
    public ResponseEntity<?> updateTrangThaiThanhToanByMa(
            @PathVariable String maHoaDon,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(name = "trangThaiThanhToan", required = false) Integer paramTrangThaiThanhToan
    ) {
        Integer status = paramTrangThaiThanhToan;
        String ghiChu = null;
        if (body != null) {
            if (body.get("trangThaiThanhToan") != null) {
                try {
                    status = Integer.valueOf(body.get("trangThaiThanhToan").toString());
                } catch (NumberFormatException ignored) {}
            }
            if (body.get("ghiChu") != null) ghiChu = body.get("ghiChu").toString();
            if (body.get("ghiChuThanhToan") != null) ghiChu = body.get("ghiChuThanhToan").toString();
            if (body.get("hinhThucThanhToan") != null) {
                String ht = body.get("hinhThucThanhToan").toString();
                ghiChu = (ghiChu != null && !ghiChu.isEmpty()) ? (ht + " - " + ghiChu) : ht;
            }
        }
        if (status == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trạng thái thanh toán không được để trống"));
        }
        try {
            return ResponseEntity.ok(hoaDonService.updateTrangThaiThanhToanByMa(maHoaDon, status, ghiChu));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message",
                    e.getMessage() != null ? e.getMessage() : "Cập nhật trạng thái thanh toán thất bại"
            ));
        }
    }

    // Xóa hóa đơn theo ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!hoaDonService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        hoaDonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Đặt hàng online từ Website Client
    @PostMapping("/dat-hang-online")
    public ResponseEntity<?> datHangOnline(@RequestBody com.example.sd009thuan.dto.DatHangOnlineDTO dto) {
        try {
            HoaDon saved = hoaDonService.createHoaDonOnline(dto);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Đặt hàng online thất bại!"));
        }
    }

    @PutMapping("/code/{maHoaDon}/doi-phuong-thuc-thanh-toan")
    public ResponseEntity<?> doiPhuongThucThanhToan(
            @PathVariable String maHoaDon,
            @RequestBody Map<String, Object> body
    ) {
        try {
            String method = body.get("phuongThucThanhToan") != null
                    ? body.get("phuongThucThanhToan").toString() : null;
            return ResponseEntity.ok(hoaDonService.changeOnlinePaymentMethod(maHoaDon, method));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Khách hàng online sửa thông tin nhận hàng (1 lần duy nhất khi đơn đang Chờ xác nhận)
    @PutMapping("/code/{maHoaDon}/khach-sua-thong-tin")
    public ResponseEntity<?> khachSuaThongTinDonHangByCode(
            @PathVariable String maHoaDon,
            @RequestBody com.example.sd009thuan.dto.KhachSuaDonDTO dto
    ) {
        try {
            HoaDon updated = hoaDonService.khachSuaThongTinDonHang(maHoaDon, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Đã xảy ra lỗi khi cập nhật thông tin đơn hàng: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/khach-sua-thong-tin")
    public ResponseEntity<?> khachSuaThongTinDonHangById(
            @PathVariable Long id,
            @RequestBody com.example.sd009thuan.dto.KhachSuaDonDTO dto
    ) {
        Optional<HoaDon> opt = hoaDonService.getById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return khachSuaThongTinDonHangByCode(opt.get().getMaHoaDon(), dto);
    }

    // Nhân viên sửa thông tin nhận hàng 1 lần riêng khi đơn Online đang Chờ xác nhận.
    @PutMapping("/code/{maHoaDon}/nhan-vien-sua-thong-tin")
    public ResponseEntity<?> nhanVienSuaThongTinDonHangByCode(
            @PathVariable String maHoaDon,
            @RequestParam(required = false) String tenNhanVien,
            @RequestBody com.example.sd009thuan.dto.KhachSuaDonDTO dto
    ) {
        try {
            return ResponseEntity.ok(hoaDonService.nhanVienSuaThongTinDonHang(maHoaDon, dto, tenNhanVien));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Đã xảy ra lỗi khi nhân viên cập nhật thông tin đơn hàng: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/nhan-vien-sua-thong-tin")
    public ResponseEntity<?> nhanVienSuaThongTinDonHangById(
            @PathVariable Long id,
            @RequestParam(required = false) String tenNhanVien,
            @RequestBody com.example.sd009thuan.dto.KhachSuaDonDTO dto
    ) {
        Optional<HoaDon> opt = hoaDonService.getById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return nhanVienSuaThongTinDonHangByCode(opt.get().getMaHoaDon(), tenNhanVien, dto);
    }


    // Quản lý thông báo đơn hàng mới cho Admin
    private static final java.util.Set<String> readNotificationCodes = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @GetMapping("/thong-bao-moi")
    public ResponseEntity<java.util.Map<String, Object>> getNewOrderNotifications() {
        List<HoaDon> allRecent = hoaDonRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "ngayTao")
        );
        List<java.util.Map<String, Object>> notifList = new java.util.ArrayList<>();
        int unreadCount = 0;

        for (HoaDon hd : allRecent) {
            if (notifList.size() >= 25) break;
            boolean isOnline = Integer.valueOf(2).equals(hd.getLoaiDon())
                               || (hd.getGhiChu() != null && hd.getGhiChu().toLowerCase().contains("website"));
            if (isOnline) {
                boolean daDoc = readNotificationCodes.contains(hd.getMaHoaDon()) || (hd.getTrangThai() != null && hd.getTrangThai() > 1);
                if (!daDoc) {
                    unreadCount++;
                }
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", hd.getId());
                item.put("maHoaDon", hd.getMaHoaDon());
                item.put("tenKhachHang", hd.getTenKhachHang() != null ? hd.getTenKhachHang() : "Khách hàng online");
                item.put("soDienThoai", hd.getSoDienThoaiKhachHang() != null ? hd.getSoDienThoaiKhachHang() : "");
                item.put("tongTien", hd.getTienSauGiamGia() != null ? hd.getTienSauGiamGia() : (hd.getTongTien() != null ? hd.getTongTien() : java.math.BigDecimal.ZERO));
                item.put("ngayTao", hd.getNgayTao() != null ? hd.getNgayTao().toString() : "");
                item.put("trangThai", hd.getTrangThai() != null ? hd.getTrangThai() : 1);
                item.put("daDoc", daDoc);
                notifList.add(item);
            }
        }

        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("unreadCount", unreadCount);
        res.put("notifications", notifList);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/danh-dau-da-doc")
    public ResponseEntity<java.util.Map<String, Object>> markNotificationAsRead(
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        if (body != null && body.containsKey("maHoaDon")) {
            readNotificationCodes.add(body.get("maHoaDon"));
        } else if (body != null && Boolean.parseBoolean(String.valueOf(body.get("all")))) {
            List<HoaDon> all = hoaDonRepository.findAll();
            for (HoaDon hd : all) {
                if (hd.getMaHoaDon() != null) {
                    readNotificationCodes.add(hd.getMaHoaDon());
                }
            }
        }
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Đã đánh dấu thông báo là đã đọc"));
    }

}
