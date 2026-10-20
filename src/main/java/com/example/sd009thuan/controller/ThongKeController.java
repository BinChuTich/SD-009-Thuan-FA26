package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.thongke.ThongKeFilterRequest;
import com.example.sd009thuan.dto.thongke.ThongKeHoaDonDTO;
import com.example.sd009thuan.dto.thongke.ThongKeSanPhamDTO;
import com.example.sd009thuan.dto.thongke.ThongKeTongQuanResponse;
import com.example.sd009thuan.service.ThongKeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/thong-ke")
@CrossOrigin(origins = "*")
public class ThongKeController {

    private final ThongKeService thongKeService;

    public ThongKeController(ThongKeService thongKeService) {
        this.thongKeService = thongKeService;
    }

    /**
     * Lấy các chỉ số thống kê tổng quan (KPIs, biểu đồ ngày, tỷ trọng thương hiệu, trạng thái đơn, mốc thời gian)
     */
    @GetMapping("/tong-quan")
    public ResponseEntity<ThongKeTongQuanResponse> getTongQuan(
            @ModelAttribute ThongKeFilterRequest filter
    ) {
        return ResponseEntity.ok(thongKeService.getTongQuan(filter));
    }

    /**
     * Thống kê theo sản phẩm kết hợp với hóa đơn (kèm danh sách các hóa đơn đã mua từng sản phẩm)
     */
    @GetMapping("/san-pham")
    public ResponseEntity<List<ThongKeSanPhamDTO>> getThongKeSanPham(
            @ModelAttribute ThongKeFilterRequest filter
    ) {
        return ResponseEntity.ok(thongKeService.getThongKeSanPham(filter));
    }

    /**
     * Thống kê theo hóa đơn kết hợp với sản phẩm (kèm danh sách các sản phẩm trong từng hóa đơn)
     */
    @GetMapping("/hoa-don")
    public ResponseEntity<List<ThongKeHoaDonDTO>> getThongKeHoaDon(
            @ModelAttribute ThongKeFilterRequest filter
    ) {
        return ResponseEntity.ok(thongKeService.getThongKeHoaDon(filter));
    }
}

