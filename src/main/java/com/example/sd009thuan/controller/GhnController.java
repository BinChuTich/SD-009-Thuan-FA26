package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.ghn.GhnFeeRequestDTO;
import com.example.sd009thuan.dto.ghn.GhnFeeResponseDTO;
import com.example.sd009thuan.service.GhnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ghn")
@CrossOrigin(origins = "*")
public class GhnController {

    private final GhnService ghnService;

    public GhnController(GhnService ghnService) {
        this.ghnService = ghnService;
    }

    /**
     * API Tính phí vận chuyển (Tự động gọi GHN API hoặc áp dụng chính sách khu vực)
     */
    @PostMapping("/calculate-fee")
    public ResponseEntity<GhnFeeResponseDTO> calculateShippingFee(@RequestBody GhnFeeRequestDTO request) {
        GhnFeeResponseDTO response = ghnService.calculateShippingFee(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API Lấy cấu hình kho Shop gửi hàng
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getGhnConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("shopProvince", "Hà Nội");
        config.put("shopDistrict", "Quận Nam Từ Liêm");
        config.put("shopDistrictId", 1454);
        config.put("shopWardCode", "20109");
        config.put("defaultWeightGram", 500);
        return ResponseEntity.ok(config);
    }
}

