package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.PageResponse;
import com.example.sd009thuan.dto.SanPhamFilterRequest;
import com.example.sd009thuan.dto.SanPhamRequest;
import com.example.sd009thuan.dto.SanPhamResponse;
import com.example.sd009thuan.service.SanPhamService;
import jakarta.validation.Valid;
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

    // GET /api/san-pham (Tìm kiếm, bộ lọc 8 thuộc tính, phân trang, sắp xếp)
    @GetMapping
    public ResponseEntity<PageResponse<SanPhamResponse>> getAll(
            @ModelAttribute SanPhamFilterRequest filter
    ) {
        return ResponseEntity.ok(sanPhamService.getAll(filter));
    }

    // GET /api/san-pham/{id} (Chi tiết sản phẩm)
    @GetMapping("/{id}")
    public ResponseEntity<SanPhamResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sanPhamService.getById(id));
    }

    // POST /api/san-pham (Thêm mới)
    @PostMapping
    public ResponseEntity<SanPhamResponse> create(@Valid @RequestBody SanPhamRequest request) {
        return ResponseEntity.ok(sanPhamService.create(request));
    }

    // PUT /api/san-pham/{id} (Cập nhật)
    @PutMapping("/{id}")
    public ResponseEntity<SanPhamResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SanPhamRequest request
    ) {
        return ResponseEntity.ok(sanPhamService.update(id, request));
    }

    // PATCH /api/san-pham/{id}/status (Đổi trạng thái kinh doanh)
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam Integer trangThai
    ) {
        sanPhamService.changeStatus(id, trangThai);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/san-pham/{id} (Xóa sản phẩm)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sanPhamService.delete(id);
        return ResponseEntity.ok().build();
    }
}
