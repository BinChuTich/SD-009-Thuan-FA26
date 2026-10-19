package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.DongMoCaRequest;
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

    @GetMapping("/current/{nhanVienId}")
    public ResponseEntity<?> getCurrentOpenShift(@PathVariable Long nhanVienId) {
        return ResponseEntity.ok(service.getCurrentOpenShift(nhanVienId).orElse(null));
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
}

