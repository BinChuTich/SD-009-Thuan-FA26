package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.CaLamViecRequest;
import com.example.sd009thuan.entity.CaLamViec;
import com.example.sd009thuan.service.CaLamViecService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ca-lam-viec")
public class CaLamViecController {

    private final CaLamViecService service;

    public CaLamViecController(CaLamViecService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CaLamViec>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime gioBatDau,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime gioKetThuc
    ) {
        if (gioBatDau != null && gioKetThuc != null && !gioKetThuc.isAfter(gioBatDau)) {
            throw new IllegalArgumentException("Thời gian bắt đầu tìm kiếm phải nhỏ hơn Thời gian kết thúc!");
        }
        return ResponseEntity.ok(service.search(keyword, trangThai, gioBatDau, gioKetThuc));
    }

    @GetMapping("/check-ten")
    public ResponseEntity<Map<String, Object>> checkTen(
            @RequestParam String tenCa,
            @RequestParam(required = false) Long excludeId
    ) {
        return ResponseEntity.ok(service.checkTenCa(tenCa, excludeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaLamViec> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CaLamViecRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CaLamViecRequest req) {
        try {
            return ResponseEntity.ok(service.update(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.toggleStatus(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(Map.of("message", "Xóa ca làm việc thành công!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

