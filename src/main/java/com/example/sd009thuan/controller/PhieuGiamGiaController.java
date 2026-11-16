package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.PhieuGiamGiaDTO;
import com.example.sd009thuan.service.PhieuGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/phieu-giam-gia")
@CrossOrigin(origins = "*") 
public class PhieuGiamGiaController {
    
    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;

    @GetMapping
    public ResponseEntity<List<PhieuGiamGiaDTO>> getAll() {
        List<PhieuGiamGiaDTO> list = phieuGiamGiaService.getAllPhieuGiamGia();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/phan-trang")
    public ResponseEntity<Page<PhieuGiamGiaDTO>> getPhanTrang(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PhieuGiamGiaDTO> result = phieuGiamGiaService.getPhanTrangPhieuGiamGia(pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return phieuGiamGiaService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        PhieuGiamGiaDTO updated = phieuGiamGiaService.toggleTrangThai(id);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PhieuGiamGiaDTO dto) {
        try {
            PhieuGiamGiaDTO updated = phieuGiamGiaService.updatePhieuGiamGia(id, dto);
            if (updated != null) {
                return ResponseEntity.ok(updated);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi cập nhật phiếu giảm giá: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PhieuGiamGiaDTO dto) {
        try {
            PhieuGiamGiaDTO created = phieuGiamGiaService.createPhieuGiamGia(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi thêm phiếu giảm giá: " + e.getMessage());
        }
    }

    @GetMapping("/khach-hang")
    public ResponseEntity<?> getDanhSachKhachHang() {
        return ResponseEntity.ok(phieuGiamGiaService.getAllKhachHang());
    }
}
