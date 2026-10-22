package com.example.sd009thuan.controller;

import com.example.sd009thuan.entity.ChiTietDotGiamGia;
import com.example.sd009thuan.entity.DotGiamGia;
import com.example.sd009thuan.service.DotGiamGiaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
     */
    @GetMapping
    public ResponseEntity<Page<DotGiamGia>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
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
     * Lấy danh sách các đợt giảm giá đang có hiệu lực hoạt động
     */
    @GetMapping({"/active", "/active-campaigns", "/active-products"})
    public ResponseEntity<List<DotGiamGia>> getActiveCampaigns() {
        return ResponseEntity.ok(dotGiamGiaService.getActiveCampaigns());
    }

    /**
     * Lấy bản đồ ID sản phẩm cha -> Đợt giảm giá đang áp dụng
     */
    @GetMapping("/active-products-map")
    public ResponseEntity<Map<Long, DotGiamGia>> getActiveProductsMap(@RequestParam(required = false) Long excludeDotId) {
        return ResponseEntity.ok(dotGiamGiaService.getActiveProductMap(excludeDotId));
    }

    /**
     * Lấy bản đồ ID biến thể -> Đợt giảm giá đang áp dụng chi tiết
     */
    @GetMapping("/active-variants-map")
    public ResponseEntity<Map<Long, Map<String, Object>>> getActiveVariantsMap(@RequestParam(required = false) Long excludeDotId) {
        return ResponseEntity.ok(dotGiamGiaService.getActiveVariantMap(excludeDotId));
    }

    /**
     * Lấy đợt giảm giá đang áp dụng sản phẩm cha theo ID sản phẩm
     */
    @GetMapping("/by-product/{sanPhamId}")
    public ResponseEntity<DotGiamGia> getByProductId(@PathVariable Long sanPhamId) {
        return dotGiamGiaService.getActiveCampaignByProductId(sanPhamId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lấy danh sách ID các sản phẩm cha đang nằm trong đợt giảm giá chưa kết thúc
     */
    @GetMapping("/active-product-ids")
    public ResponseEntity<List<Long>> getActiveProductIds(@RequestParam(required = false) Long excludeDotId) {
        return ResponseEntity.ok(dotGiamGiaService.getActiveProductIds(excludeDotId));
    }

    /**
     * Lấy chi tiết theo ID
     */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<DotGiamGia> getById(@PathVariable Long id) {
        return dotGiamGiaService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lấy danh sách chi tiết biến thể theo ID Đợt Giảm Giá
     */
    @GetMapping("/{id:\\d+}/chi-tiet")
    public ResponseEntity<List<ChiTietDotGiamGia>> getChiTietByDotGiamGiaId(@PathVariable Long id) {
        return ResponseEntity.ok(dotGiamGiaService.getChiTietByDotGiamGiaId(id));
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
     * Tạo mới đợt giảm giá (Kèm danh sách biến thể chi tiết)
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody DotGiamGia dotGiamGia) {
        try {
            return ResponseEntity.ok(dotGiamGiaService.create(dotGiamGia));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    /**
     * Cập nhật đợt giảm giá (Kèm danh sách biến thể chi tiết)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody DotGiamGia dotGiamGia
    ) {
        try {
            return ResponseEntity.ok(dotGiamGiaService.update(id, dotGiamGia));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
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
