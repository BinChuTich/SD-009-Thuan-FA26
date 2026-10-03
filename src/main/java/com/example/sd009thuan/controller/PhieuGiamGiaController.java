package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.PhieuGiamGiaDTO;
import com.example.sd009thuan.service.PhieuGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/phieu-giam-gia")
@CrossOrigin(origins = "*")
public class PhieuGiamGiaController {
    // Tiêm (Inject) Service để xử lý dữ liệu
    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;


    @GetMapping
    public ResponseEntity<List<PhieuGiamGiaDTO>> getAll() {
        List<PhieuGiamGiaDTO> list = phieuGiamGiaService.getAllPhieuGiamGia();
        return ResponseEntity.ok(list);
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
