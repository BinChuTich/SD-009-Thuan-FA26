package com.example.sd009thuan.controller;

import com.example.sd009thuan.service.GhnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ghn")
@CrossOrigin("*")
public class GhnController {

    private final GhnService ghnService;

    public GhnController(GhnService ghnService) {
        this.ghnService = ghnService;
    }

    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() {
        return ResponseEntity.ok(ghnService.getProvinces());
    }

    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(@RequestParam(required = false) Integer provinceId) {
        return ResponseEntity.ok(ghnService.getDistricts(provinceId));
    }

    @GetMapping("/wards")
    public ResponseEntity<?> getWards(@RequestParam(required = false) Integer districtId) {
        return ResponseEntity.ok(ghnService.getWards(districtId));
    }

    @PostMapping("/fee")
    public ResponseEntity<?> calculateFee(@RequestBody Map<String, Object> body) {
        Integer toDistrictId = null;
        if (body.get("toDistrictId") != null && !body.get("toDistrictId").toString().isBlank()) {
            try {
                toDistrictId = Integer.valueOf(body.get("toDistrictId").toString());
            } catch (Exception ignored) {}
        }
        String toProvinceName = body.get("toProvinceName") != null ? body.get("toProvinceName").toString() : null;
        String toWardCode = body.get("toWardCode") != null ? body.get("toWardCode").toString() : null;
        Integer weight = null;
        if (body.get("weight") != null && !body.get("weight").toString().isBlank()) {
            try {
                weight = Integer.valueOf(body.get("weight").toString());
            } catch (Exception ignored) {}
        }
        Integer insuranceValue = null;
        if (body.get("insuranceValue") != null && !body.get("insuranceValue").toString().isBlank()) {
            try {
                insuranceValue = Integer.valueOf(body.get("insuranceValue").toString());
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(ghnService.calculateShippingFee(toProvinceName, toDistrictId, toWardCode, weight, insuranceValue));
    }

    @PostMapping("/leadtime")
    public ResponseEntity<?> calculateLeadTime(@RequestBody Map<String, Object> body) {
        Integer toDistrictId = null;
        if (body.get("toDistrictId") != null && !body.get("toDistrictId").toString().isBlank()) {
            try {
                toDistrictId = Integer.valueOf(body.get("toDistrictId").toString());
            } catch (Exception ignored) {}
        }
        String toProvinceName = body.get("toProvinceName") != null ? body.get("toProvinceName").toString() : null;
        String toWardCode = body.get("toWardCode") != null ? body.get("toWardCode").toString() : null;
        return ResponseEntity.ok(ghnService.calculateLeadTime(toProvinceName, toDistrictId, toWardCode));
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderPayload) {
        return ResponseEntity.ok(ghnService.createShippingOrder(orderPayload));
    }
}
