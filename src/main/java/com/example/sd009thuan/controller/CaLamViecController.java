package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.CaLamViecRequest;
import com.example.sd009thuan.entity.CaLamViec;
import com.example.sd009thuan.service.CaLamViecService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/ca-lam-viec")
public class CaLamViecController {

    private final CaLamViecService service;
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d{1,2}):(\\d{1,2})(?::(\\d{1,2}))?");

    public CaLamViecController(CaLamViecService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CaLamViec>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(required = false) String gioBatDau,
            @RequestParam(required = false) String gioKetThuc
    ) {
        LocalTime start = parseLocalTimeSafe(gioBatDau);
        LocalTime end = parseLocalTimeSafe(gioKetThuc);
        if (start != null && end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException("Thời gian bắt đầu tìm kiếm phải nhỏ hơn Thời gian kết thúc!");
        }
        return ResponseEntity.ok(service.search(keyword, trangThai, start, end));
    }

    @GetMapping("/check-ten")
    public ResponseEntity<Map<String, Object>> checkTen(
            @RequestParam(required = false, defaultValue = "") String tenCa,
            @RequestParam(required = false) Long excludeId
    ) {
        return ResponseEntity.ok(service.checkTenCa(tenCa != null ? tenCa.trim() : "", excludeId));
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

    private LocalTime parseLocalTimeSafe(String val) {
        if (val == null) return null;
        String s = val.trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s) || "undefined".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            Matcher m = TIME_PATTERN.matcher(s);
            if (m.find()) {
                int hour = Integer.parseInt(m.group(1));
                int min = Integer.parseInt(m.group(2));
                int sec = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
                if (hour >= 0 && hour <= 23 && min >= 0 && min <= 59 && sec >= 0 && sec <= 59) {
                    return LocalTime.of(hour, min, sec);
                }
            }
            return LocalTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
