package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.ExcelImportResult;
import com.example.sd009thuan.dto.PageResponse;
import com.example.sd009thuan.dto.SanPhamFilterRequest;
import com.example.sd009thuan.dto.SanPhamRequest;
import com.example.sd009thuan.dto.SanPhamResponse;
import com.example.sd009thuan.service.ExcelImportService;
import com.example.sd009thuan.service.SanPhamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/san-pham")
@CrossOrigin("*")
public class SanPhamController {

    private final SanPhamService sanPhamService;
    private final ExcelImportService excelImportService;

    public SanPhamController(SanPhamService sanPhamService, ExcelImportService excelImportService) {
        this.sanPhamService = sanPhamService;
        this.excelImportService = excelImportService;
    }

    @GetMapping("/next-code")
    public ResponseEntity<Map<String, String>> nextCode() {
        return ResponseEntity.ok(Map.of("code", sanPhamService.nextCode()));
    }

    @GetMapping("/check-duplicate")
    public ResponseEntity<Map<String, Object>> checkDuplicate(
            @RequestParam(required = false) String maSanPham,
            @RequestParam(required = false) String tenSanPham,
            @RequestParam(required = false) Long idDanhMuc,
            @RequestParam(required = false) Long idThuongHieu,
            @RequestParam(required = false) Long idChatLieu,
            @RequestParam(required = false) Long idCoAo,
            @RequestParam(required = false) Long idTayAo,
            @RequestParam(required = false) Long idXuatXu,
            @RequestParam(required = false) Long idHoaTiet
    ) {
        return ResponseEntity.ok(sanPhamService.checkDuplicate(
                maSanPham, tenSanPham, idDanhMuc, idThuongHieu, idChatLieu, idCoAo, idTayAo, idXuatXu, idHoaTiet
        ));
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

    @GetMapping("/excel-template")
    public ResponseEntity<byte[]> getExcelTemplate() {
        byte[] bytes = excelImportService.generateProductTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Mau_Import_SanPham.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExcelImportResult> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "xacNhanGop", defaultValue = "false") boolean xacNhanGop
    ) {
        return ResponseEntity.ok(excelImportService.importProducts(file, xacNhanGop));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<SanPhamResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sanPhamService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SanPhamResponse> create(@Valid @RequestBody SanPhamRequest request) {
        return ResponseEntity.ok(sanPhamService.create(request));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<SanPhamResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SanPhamRequest request
    ) {
        return ResponseEntity.ok(sanPhamService.update(id, request));
    }

    @PatchMapping("/{id:\\d+}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam Integer trangThai
    ) {
        sanPhamService.changeStatus(id, trangThai);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sanPhamService.delete(id);
        return ResponseEntity.ok().build();
    }
}
