package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.ExcelImportResult;
import com.example.sd009thuan.dto.LichLamViecRequest;
import com.example.sd009thuan.dto.ThayTheNhanVienRequest;
import com.example.sd009thuan.entity.LichLamViec;
import com.example.sd009thuan.service.LichLamViecService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lich-lam-viec")
public class LichLamViecController {

    private final LichLamViecService service;

    public LichLamViecController(LichLamViecService service) {
        this.service = service;
    }

    @GetMapping("/tuan")
    public ResponseEntity<?> getWeeklySchedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Từ ngày không được lớn hơn Đến ngày!"));
        }
        return ResponseEntity.ok(service.getWeeklySchedule(fromDate, toDate));
    }

    @PostMapping
    public ResponseEntity<?> assignShift(@Valid @RequestBody LichLamViecRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.assignShift(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeShift(@PathVariable Long id) {
        try {
            service.removeShift(id);
            return ResponseEntity.ok(Map.of("message", "Xóa phân ca thành công!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/kiem-tra-dong-bo-nhan-vien/{nhanVienId}")
    public ResponseEntity<?> checkSync(@PathVariable Long nhanVienId) {
        return ResponseEntity.ok(service.checkSyncShiftsWhenDeactivatingEmployee(nhanVienId));
    }

    @PostMapping("/thay-the-nhan-vien")
    public ResponseEntity<?> replaceEmployee(@Valid @RequestBody ThayTheNhanVienRequest req) {
        try {
            service.replaceEmployeeInShifts(req);
            return ResponseEntity.ok(Map.of("message", "Thay thế nhân sự thành công!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExcelImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        ExcelImportResult result = service.importExcel(file);
        if (result.errorCount() > 0) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = service.exportTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Lich_Lam_Viec_Template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
