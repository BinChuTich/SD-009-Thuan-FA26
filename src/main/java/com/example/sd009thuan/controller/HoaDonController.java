package com.example.sd009thuan.controller;

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

    @GetMapping
    public ResponseEntity<List<HoaDon>> getAll() {
        return ResponseEntity.ok(hoaDonService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HoaDon> getById(@PathVariable Long id) {
        return hoaDonService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{maHoaDon}")
    public ResponseEntity<HoaDon> getByMaHoaDon(@PathVariable String maHoaDon) {
        return hoaDonService.getByMaHoaDon(maHoaDon)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<HoaDon> create(@RequestBody HoaDon hoaDon) {
        return ResponseEntity.ok(hoaDonService.create(hoaDon));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HoaDon> update(@PathVariable Long id, @RequestBody HoaDon hoaDon) {
        try {
            return ResponseEntity.ok(hoaDonService.update(id, hoaDon));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/trang-thai")
    public ResponseEntity<HoaDon> updateTrangThai(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer trangThai = body.get("trangThai");
        if (trangThai == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(hoaDonService.updateTrangThai(id, trangThai));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (hoaDonService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        hoaDonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}