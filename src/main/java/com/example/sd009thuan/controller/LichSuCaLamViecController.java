package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.DongMoCaRequest;
import com.example.sd009thuan.dto.ShiftCashSummaryDTO;
import com.example.sd009thuan.entity.LichSuCaLamViec;
import com.example.sd009thuan.service.LichSuCaLamViecService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/lich-su-ca")
public class LichSuCaLamViecController {

    private final LichSuCaLamViecService service;

    public LichSuCaLamViecController(LichSuCaLamViecService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Từ ngày không được lớn hơn Đến ngày!"));
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<LichSuCaLamViec> result = service.searchHistory(keyword, fromDate, toDate, trangThai, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/last-closed")
    public ResponseEntity<?> getLastClosedShift() {
        return ResponseEntity.ok(service.getLastClosedShift().orElse(null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/current/{id}")
    public ResponseEntity<?> getCurrentOpenShift(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getCurrentOpenShift(id).orElse(null));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<?> getShiftSummary(@PathVariable Long id) {
        try {
            ShiftCashSummaryDTO summary = service.getShiftSummary(id);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/mo-ca")
    public ResponseEntity<?> moCa(@Valid @RequestBody DongMoCaRequest req) {
        try {
            return ResponseEntity.ok(service.moCa(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/dong-ca")
    public ResponseEntity<?> dongCa(@Valid @RequestBody DongMoCaRequest req) {
        try {
            return ResponseEntity.ok(service.dongCa(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private LocalDate parseLocalDateSafe(String val) {
        if (val == null) return null;
        String s = val.trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s) || "undefined".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            if (s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
                return LocalDate.parse(s.substring(0, 10));
            }
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
