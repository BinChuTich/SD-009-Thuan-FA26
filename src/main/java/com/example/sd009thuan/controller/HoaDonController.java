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

    public HoaDonController(HoaDonService hoaDonService) {
        this.hoaDonService = hoaDonService;
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

    // Tạo đơn hàng bán tại quầy (POS) kèm danh sách sản phẩm chi tiết, trừ tồn kho và thanh toán
    @PostMapping("/pos")
    public ResponseEntity<HoaDon> createPos(@RequestBody HoaDonPosDTO dto) {
        return ResponseEntity.ok(hoaDonService.createHoaDonPos(dto));
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
        Integer trangThai = paramTrangThai;
        String ghiChu = null;
        String email = paramEmail;

        if (body != null) {
            if (body.get("trangThai") != null) {
                try {
                    trangThai = Integer.valueOf(
                            body.get("trangThai").toString()
                    );
                } catch (NumberFormatException ignored) {
                    // Bỏ qua nếu trạng thái không phải số nguyên
                }
            }

            if (body.get("ghiChu") != null) {
                ghiChu = body.get("ghiChu").toString();
            }

            if (body.get("email") != null && !body.get("email").toString().trim().isEmpty()) {
                email = body.get("email").toString().trim();
            } else if (body.get("emailKhachHang") != null && !body.get("emailKhachHang").toString().trim().isEmpty()) {
                email = body.get("emailKhachHang").toString().trim();
            }
        }

        if (trangThai == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Trạng thái không được để trống"
                    ));
        }

        try {
            return ResponseEntity.ok(
                    hoaDonService.updateTrangThai(id, trangThai, ghiChu, email)
            );
        } catch (Exception e) {
            try {
                HoaDon hd = new HoaDon();
                hd.setTrangThai(trangThai);
                if (ghiChu != null) hd.setGhiChu(ghiChu);
                if (email != null) hd.setEmail(email);
                return ResponseEntity.ok(hoaDonService.update(id, hd));
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message",
                        e.getMessage() != null ? e.getMessage() : "Cập nhật trạng thái thất bại"
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
        Integer trangThai = paramTrangThai;
        String ghiChu = null;
        String email = paramEmail;

        if (body != null) {
            if (body.get("trangThai") != null) {
                try {
                    trangThai = Integer.valueOf(
                            body.get("trangThai").toString()
                    );
                } catch (NumberFormatException ignored) {
                    // Bỏ qua nếu trạng thái không phải số nguyên
                }
            }

            if (body.get("ghiChu") != null) {
                ghiChu = body.get("ghiChu").toString();
            }

            if (body.get("email") != null && !body.get("email").toString().trim().isEmpty()) {
                email = body.get("email").toString().trim();
            } else if (body.get("emailKhachHang") != null && !body.get("emailKhachHang").toString().trim().isEmpty()) {
                email = body.get("emailKhachHang").toString().trim();
            }
        }

        if (trangThai == null) {
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
                            trangThai,
                            ghiChu,
                            email
                    )
            );
        } catch (Exception e) {
            try {
                java.util.Optional<HoaDon> opt = hoaDonService.getByMaHoaDon(maHoaDon);
                if (opt.isPresent()) {
                    HoaDon hd = new HoaDon();
                    hd.setTrangThai(trangThai);
                    if (ghiChu != null) hd.setGhiChu(ghiChu);
                    if (email != null) hd.setEmail(email);
                    return ResponseEntity.ok(hoaDonService.update(opt.get().getId(), hd));
                }
            } catch (Exception ignored) {}
            return ResponseEntity.badRequest().body(Map.of(
                    "message",
                    e.getMessage() != null ? e.getMessage() : "Cập nhật trạng thái thất bại"
                ));
        }
    }

    // Gửi email xác nhận đơn hàng theo ID
    @PostMapping("/{id}/send-email")
    public ResponseEntity<?> sendConfirmationEmail(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String email = (body != null && body.get("email") != null) ? body.get("email").toString().trim() : null;
        try {
            boolean sent = hoaDonService.sendConfirmationEmail(id, email);
            if (sent) {
                return ResponseEntity.ok(Map.of("message", "Đã gửi email xác nhận đơn hàng thành công!"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Không thể gửi email. Vui lòng kiểm tra lại địa chỉ email người nhận!"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Gửi email thất bại!"));
        }
    }

    // Gửi email xác nhận đơn hàng theo mã hóa đơn
    @PostMapping("/code/{maHoaDon}/send-email")
    public ResponseEntity<?> sendConfirmationEmailByCode(
            @PathVariable String maHoaDon,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Optional<HoaDon> opt = hoaDonService.getByMaHoaDon(maHoaDon);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String email = (body != null && body.get("email") != null) ? body.get("email").toString().trim() : null;
        try {
            boolean sent = hoaDonService.sendConfirmationEmail(opt.get().getId(), email);
            if (sent) {
                return ResponseEntity.ok(Map.of("message", "Đã gửi email xác nhận đơn hàng thành công!"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Không thể gửi email. Vui lòng kiểm tra lại địa chỉ email người nhận!"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Gửi email thất bại!"));
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

}
