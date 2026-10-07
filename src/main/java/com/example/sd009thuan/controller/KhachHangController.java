package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.KhachHangRequest;
import com.example.sd009thuan.dto.KhachHangResponse;
import com.example.sd009thuan.service.ExcelExportService;
import com.example.sd009thuan.service.KhachHangService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/khach-hang")
public class KhachHangController {
    private final KhachHangService service;
    private final ExcelExportService excelExportService;

    public KhachHangController(KhachHangService service, ExcelExportService excelExportService) {
        this.service = service;
        this.excelExportService = excelExportService;
    }

    @GetMapping
    public Page<KhachHangResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
        return service.search(keyword, trangThai, pageable);
    }

    @GetMapping({"/export-excel", "/export"})
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer trangThai) {
        byte[] bytes = excelExportService.exportCustomers(service.findAllForExport(keyword, trangThai));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("danh-sach-khach-hang.xlsx").build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/next-code")
    public ResponseEntity<Map<String, String>> nextCode() {
        return ResponseEntity.ok(Map.of("code", service.nextCode()));
    }

    @GetMapping("/{id}")
    public KhachHangResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KhachHangResponse> createMultipart(
            @RequestPart("data") KhachHangRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KhachHangResponse> createJson(@RequestBody KhachHangRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KhachHangResponse updateMultipart(
            @PathVariable Long id,
            @RequestPart("data") KhachHangRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return service.update(id, req);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public KhachHangResponse updateJson(@PathVariable Long id, @RequestBody KhachHangRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Void> toggle(@PathVariable Long id) {
        service.toggleStatus(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
