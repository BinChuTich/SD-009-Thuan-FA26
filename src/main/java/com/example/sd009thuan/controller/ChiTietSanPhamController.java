package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.ChiTietSanPhamRequest;
import com.example.sd009thuan.dto.ChiTietSanPhamResponse;
import com.example.sd009thuan.dto.PageResponse;
import com.example.sd009thuan.service.ChiTietSanPhamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-san-pham")
@CrossOrigin("*")
public class ChiTietSanPhamController {

    private final ChiTietSanPhamService chiTietSanPhamService;

    public ChiTietSanPhamController(ChiTietSanPhamService chiTietSanPhamService) {
        this.chiTietSanPhamService = chiTietSanPhamService;
    }

    // GET /api/chi-tiet-san-pham/by-san-pham/{idSanPham}
    @GetMapping("/by-san-pham/{idSanPham}")
    public ResponseEntity<List<ChiTietSanPhamResponse>> getBySanPhamId(@PathVariable Long idSanPham) {
        return ResponseEntity.ok(chiTietSanPhamService.getBySanPhamId(idSanPham));
    }

    // GET /api/chi-tiet-san-pham/filter
    @GetMapping("/filter")
    public ResponseEntity<PageResponse<ChiTietSanPhamResponse>> filterVariants(
            @RequestParam Long idSanPham,
            @RequestParam(required = false) Long idKichCo,
            @RequestParam(required = false) Long idMauSac,
            @RequestParam(required = false) BigDecimal minGia,
            @RequestParam(required = false) BigDecimal maxGia,
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(chiTietSanPhamService.filterVariants(
                idSanPham, idKichCo, idMauSac, minGia, maxGia, trangThai, page, size
        ));
    }

    // GET /api/chi-tiet-san-pham/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ChiTietSanPhamResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(chiTietSanPhamService.getById(id));
    }

    // POST /api/chi-tiet-san-pham
    @PostMapping
    public ResponseEntity<ChiTietSanPhamResponse> create(@Valid @RequestBody ChiTietSanPhamRequest request) {
        return ResponseEntity.ok(chiTietSanPhamService.create(request));
    }

    // PUT /api/chi-tiet-san-pham/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ChiTietSanPhamResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ChiTietSanPhamRequest request
    ) {
        return ResponseEntity.ok(chiTietSanPhamService.update(id, request));
    }

    // PATCH /api/chi-tiet-san-pham/{id}/status
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam Integer trangThai
    ) {
        chiTietSanPhamService.changeStatus(id, trangThai);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/chi-tiet-san-pham/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        chiTietSanPhamService.delete(id);
        return ResponseEntity.ok().build();
    }
}
