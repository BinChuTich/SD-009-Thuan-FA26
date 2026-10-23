package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.ThongKeOverviewDTO;
import com.example.sd009thuan.service.ThongKeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/thong-ke")
public class ThongKeController {

    private final ThongKeService thongKeService;

    public ThongKeController(ThongKeService thongKeService) {
        this.thongKeService = thongKeService;
    }

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false, defaultValue = "day") String statType,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String productKeyword
    ) {
        try {
            LocalDate from = parseLocalDateSafe(fromDate);
            LocalDate to = parseLocalDateSafe(toDate);
            if (from == null) from = LocalDate.now();
            if (to == null) to = LocalDate.now();
            if (from.isAfter(to)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Từ ngày không được lớn hơn Đến ngày!"));
            }
            ThongKeOverviewDTO result = thongKeService.getOverview(from, to, statType, brandId, productKeyword);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi lấy dữ liệu thống kê: " + e.getMessage()));
        }
    }

    @GetMapping("/export-excel")
    public ResponseEntity<?> exportExcel(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false, defaultValue = "day") String statType,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String productKeyword
    ) {
        try {
            LocalDate from = parseLocalDateSafe(fromDate);
            LocalDate to = parseLocalDateSafe(toDate);
            if (from == null) from = LocalDate.now();
            if (to == null) to = LocalDate.now();
            if (from.isAfter(to)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Từ ngày không được lớn hơn Đến ngày!"));
            }
            byte[] bytes = thongKeService.exportExcel(from, to, statType, brandId, productKeyword);
            String fileName = "BaoCao_ThongKe_DoanhThu_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi xuất báo cáo Excel: " + e.getMessage()));
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
