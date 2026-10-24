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
        if (body.get("toDistrictId") != null) {
            toDistrictId = Integer.valueOf(body.get("toDistrictId").toString());
        }
        String toWardCode = body.get("toWardCode") != null ? body.get("toWardCode").toString() : null;
        Integer weight = null;
        if (body.get("weight") != null) {
            weight = Integer.valueOf(body.get("weight").toString());
        }
        Integer insuranceValue = null;
        if (body.get("insuranceValue") != null) {
            insuranceValue = Integer.valueOf(body.get("insuranceValue").toString());
        }

        return ResponseEntity.ok(ghnService.calculateShippingFee(toDistrictId, toWardCode, weight, insuranceValue));
    }

    @PostMapping("/leadtime")
    public ResponseEntity<?> calculateLeadTime(@RequestBody Map<String, Object> body) {
        Integer toDistrictId = null;
        if (body.get("toDistrictId") != null) {
            toDistrictId = Integer.valueOf(body.get("toDistrictId").toString());
        }
        String toWardCode = body.get("toWardCode") != null ? body.get("toWardCode").toString() : null;
        return ResponseEntity.ok(ghnService.calculateLeadTime(toDistrictId, toWardCode));
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderPayload) {
        return ResponseEntity.ok(ghnService.createShippingOrder(orderPayload));
    }
}
