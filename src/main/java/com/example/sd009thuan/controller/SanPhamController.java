package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.PageResponse;
import com.example.sd009thuan.dto.SanPhamFilterRequest;
import com.example.sd009thuan.dto.SanPhamRequest;
import com.example.sd009thuan.dto.SanPhamResponse;
import com.example.sd009thuan.service.SanPhamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/san-pham")
@CrossOrigin("*")
public class SanPhamController {

    private final SanPhamService sanPhamService;

    public SanPhamController(SanPhamService sanPhamService) {
        this.sanPhamService = sanPhamService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<SanPhamResponse>> getAll(
            @ModelAttribute SanPhamFilterRequest filter
    ) {
        return ResponseEntity.ok(sanPhamService.getAll(filter));
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(@ModelAttribute SanPhamFilterRequest filter) {
        byte[] excelBytes = sanPhamService.exportExcel(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh-sach-san-pham.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SanPhamResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sanPhamService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SanPhamResponse> create(@Valid @RequestBody SanPhamRequest request) {
        return ResponseEntity.ok(sanPhamService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SanPhamResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SanPhamRequest request
    ) {
        return ResponseEntity.ok(sanPhamService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam Integer trangThai
    ) {
        sanPhamService.changeStatus(id, trangThai);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sanPhamService.delete(id);
        return ResponseEntity.ok().build();
    }
}
