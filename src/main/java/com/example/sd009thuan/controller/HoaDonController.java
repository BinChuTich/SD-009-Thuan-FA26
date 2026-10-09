package com.example.sd009thuan.controller;

import com.example.sd009thuan.entity.ChiTietHoaDon;
import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.service.HoaDonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hoa-don")
@CrossOrigin("*")
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

    // Tạo hóa đơn mới
    @PostMapping
    public ResponseEntity<HoaDon> create(@RequestBody HoaDon hoaDon) {
        return ResponseEntity.ok(hoaDonService.create(hoaDon));
    }

    // Cập nhật thông tin hóa đơn
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

    // Cập nhật trạng thái hóa đơn theo ID
    @PutMapping("/{id}/trang-thai")
    public ResponseEntity<?> updateTrangThai(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(name = "trangThai", required = false)
            Integer paramTrangThai
    ) {
        Integer trangThai = paramTrangThai;
        String ghiChu = null;

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
                    hoaDonService.updateTrangThai(id, trangThai, ghiChu)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Cập nhật trạng thái hóa đơn theo mã hóa đơn
    @PutMapping("/code/{maHoaDon}/trang-thai")
    public ResponseEntity<?> updateTrangThaiByMa(
            @PathVariable String maHoaDon,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(name = "trangThai", required = false)
            Integer paramTrangThai
    ) {
        Integer trangThai = paramTrangThai;
        String ghiChu = null;

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
                            ghiChu
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Xóa hóa đơn theo ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (hoaDonService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        hoaDonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}