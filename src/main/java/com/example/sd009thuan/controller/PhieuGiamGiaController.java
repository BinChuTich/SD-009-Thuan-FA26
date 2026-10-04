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
@CrossOrigin(origins = "*") // Cho phép Frontend gọi API mà không bị chặn CORS
public class PhieuGiamGiaController {
    // Tiêm (Inject) Service để xử lý dữ liệu
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
}
