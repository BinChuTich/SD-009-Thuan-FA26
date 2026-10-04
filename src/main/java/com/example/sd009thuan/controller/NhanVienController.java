package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.NhanVienRequest;
import com.example.sd009thuan.dto.NhanVienResponse;
import com.example.sd009thuan.entity.VaiTro;
import com.example.sd009thuan.service.ExcelExportService;
import com.example.sd009thuan.service.NhanVienService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nhan-vien")
public class NhanVienController {
    private final NhanVienService service;
    private final ExcelExportService excelExportService;

    public NhanVienController(NhanVienService service, ExcelExportService excelExportService) {
        this.service = service;
        this.excelExportService = excelExportService;
    }

    @GetMapping
    public Page<NhanVienResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long idVaiTro,
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
        return service.search(keyword, idVaiTro, trangThai, pageable);
    }

    @GetMapping("/vai-tro")
    public List<VaiTro> roles() { return service.roles(); }

    @GetMapping("/next-code")
    public ResponseEntity<Map<String, String>> nextCode() {
        return ResponseEntity.ok(Map.of("code", service.nextCode()));
    }

    @GetMapping({"/export-excel", "/export"})
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long idVaiTro,
            @RequestParam(required = false) Integer trangThai) {
        byte[] bytes = excelExportService.exportEmployees(service.findAllForExport(keyword, idVaiTro, trangThai));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("danh-sach-nhan-vien.xlsx").build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/{id}")
    public NhanVienResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NhanVienResponse> createMultipart(
            @RequestPart("data") NhanVienRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, file));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NhanVienResponse> createJson(@RequestBody NhanVienRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, null));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public NhanVienResponse updateMultipart(
            @PathVariable Long id,
            @RequestPart("data") NhanVienRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return service.update(id, req, file);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public NhanVienResponse updateJson(@PathVariable Long id, @RequestBody NhanVienRequest req) {
        return service.update(id, req, null);
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