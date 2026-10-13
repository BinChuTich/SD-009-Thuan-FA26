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

import java.util.List;
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
    public KhachHangResponse get(@PathVariable String id) {
        return service.getByIdentifier(id);
    }

    @PostMapping
    public ResponseEntity<KhachHangResponse> create(@RequestBody KhachHangRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public KhachHangResponse update(@PathVariable String id, @RequestBody KhachHangRequest req) {
        com.example.sd009thuan.entity.KhachHang customer = service.findByIdentifier(id);
        return service.update(customer.getId(), req);
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Void> toggle(@PathVariable String id) {
        com.example.sd009thuan.entity.KhachHang customer = service.findByIdentifier(id);
        service.toggleStatus(customer.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        com.example.sd009thuan.entity.KhachHang customer = service.findByIdentifier(id);
        service.deactivate(customer.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/dia-chi")
    public List<com.example.sd009thuan.dto.DiaChiKhachHangDto> getAddresses(@PathVariable String id) {
        return service.getAddresses(service.findByIdentifier(id).getId());
    }

    @PostMapping("/{id}/dia-chi")
    public ResponseEntity<com.example.sd009thuan.dto.DiaChiKhachHangDto> addAddress(
            @PathVariable String id,
            @RequestBody com.example.sd009thuan.dto.DiaChiKhachHangDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addAddress(service.findByIdentifier(id).getId(), dto));
    }

    @PutMapping("/{id}/dia-chi/{addressId}")
    public com.example.sd009thuan.dto.DiaChiKhachHangDto updateAddress(
            @PathVariable String id,
            @PathVariable Long addressId,
            @RequestBody com.example.sd009thuan.dto.DiaChiKhachHangDto dto) {
        return service.updateAddress(service.findByIdentifier(id).getId(), addressId, dto);
    }

    @PutMapping("/{id}/dia-chi/{addressId}/mac-dinh")
    public ResponseEntity<Void> setDefaultAddress(
            @PathVariable String id,
            @PathVariable Long addressId) {
        service.setDefaultAddress(service.findByIdentifier(id).getId(), addressId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/dia-chi/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable String id,
            @PathVariable Long addressId) {
        service.deleteAddress(service.findByIdentifier(id).getId(), addressId);
        return ResponseEntity.noContent().build();
    }
}
