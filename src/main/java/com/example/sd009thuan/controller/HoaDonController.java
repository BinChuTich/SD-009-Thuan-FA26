package com.example.sd009thuan.controller;

import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.service.HoaDonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hoa-don")
@CrossOrigin("*")
public class HoaDonController {

    private final HoaDonService hoaDonService;

    public HoaDonController(HoaDonService hoaDonService) {
        this.hoaDonService = hoaDonService;
    }
    // GET - Lấy hóa đơn theo Mã Hóa Đơn (VD: /api/hoa-don/code/HD001)
    @GetMapping("/code/{maHoaDon}")
    public ResponseEntity<HoaDon> getByMaHoaDon(@PathVariable String maHoaDon) {
        return hoaDonService.getByMaHoaDon(maHoaDon)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    // GET - Lấy tất cả hóa đơn
    @GetMapping
    public ResponseEntity<List<HoaDon>> getAll() {
        return ResponseEntity.ok(hoaDonService.getAll());
    }
    // GET - Lấy hóa đơn theo ID
    @GetMapping("/{id}")
    public ResponseEntity<HoaDon> getById(@PathVariable Long id) {

        return hoaDonService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    // POST - Thêm hóa đơn
    @PostMapping
    public ResponseEntity<HoaDon> create(
            @RequestBody HoaDon hoaDon
    ) {
        return ResponseEntity.ok(
                hoaDonService.create(hoaDon)
        );
    }
//    // PUT - Cập nhật hóa đơn
//    @PutMapping("/{id}")
//    public ResponseEntity<HoaDon> update(
//            @PathVariable Long id,
//            @RequestBody HoaDon hoaDon
//    ) {
//
//        try {
//            return ResponseEntity.ok(
//                    hoaDonService.update(id, hoaDon)
//            );
//
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
    // DELETE - Xóa hóa đơn
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        if (hoaDonService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        hoaDonService.delete(id);

        return ResponseEntity.noContent().build();
    }
}