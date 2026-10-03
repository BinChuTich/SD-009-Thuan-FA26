package com.example.sd009thuan.controller;

import com.example.sd009thuan.entity.DotGiamGia;
import com.example.sd009thuan.service.DotGiamGiaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/dot-giam-gia")
@CrossOrigin("*")
public class DotGiamGiaController {

    private final DotGiamGiaService dotGiamGiaService;

    public DotGiamGiaController(DotGiamGiaService dotGiamGiaService) {
        this.dotGiamGiaService = dotGiamGiaService;
    }

    /**
     * API Phân trang, Tìm kiếm và Lọc Đợt Giảm Giá
     * GET /api/dot-giam-gia?keyword=...&trangThai=...&startDate=...&endDate=...&page=0&size=5
     */
    @GetMapping
    public ResponseEntity<Page<DotGiamGia>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DotGiamGia> result = dotGiamGiaService.search(keyword, trangThai, startDate, endDate, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Lấy chi tiết theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DotGiamGia> getById(@PathVariable Long id) {
        return dotGiamGiaService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lấy chi tiết theo Mã Đợt Giảm Giá
     */
    @GetMapping("/code/{maDotGiamGia}")
    public ResponseEntity<DotGiamGia> getByCode(@PathVariable String maDotGiamGia) {
        return dotGiamGiaService.getByMa(maDotGiamGia)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Tạo mới đợt giảm giá
     */
    @PostMapping
    public ResponseEntity<DotGiamGia> create(@RequestBody DotGiamGia dotGiamGia) {
        return ResponseEntity.ok(dotGiamGiaService.create(dotGiamGia));
    }

    /**
     * Cập nhật đợt giảm giá
     */
    @PutMapping("/{id}")
    public ResponseEntity<DotGiamGia> update(
            @PathVariable Long id,
            @RequestBody DotGiamGia dotGiamGia
    ) {
        try {
            return ResponseEntity.ok(dotGiamGiaService.update(id, dotGiamGia));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Đổi trạng thái (Bật/Tắt hoạt động)
     */
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<DotGiamGia> toggleStatus(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(dotGiamGiaService.toggleStatus(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Xóa đợt giảm giá
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Optional<DotGiamGia> existing = dotGiamGiaService.getById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        dotGiamGiaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
