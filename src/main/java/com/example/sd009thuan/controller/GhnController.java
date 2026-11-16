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

    @PostMapping({"/fee", "/calculate-fee"})
    public ResponseEntity<?> calculateFee(@RequestBody Map<String, Object> body) {
        Integer toDistrictId = parseInt(body.get("toDistrictId"));
        String toProvinceName = str(body.get("toProvinceName"));
        if (toProvinceName == null) {
            toProvinceName = str(body.get("provinceName"));
        }
        String districtName = str(body.get("districtName"));
        String wardName = str(body.get("wardName"));
        String fullAddress = str(body.get("fullAddress"));
        String toWardCode = str(body.get("toWardCode"));
        Integer weight = parseInt(body.get("weight"));
        if (weight == null) {
            weight = parseInt(body.get("weightGram"));
        }
        Integer insuranceValue = parseInt(body.get("insuranceValue"));
        Long orderSubtotal = parseLong(body.get("orderSubtotal"));
        Double distanceKm = parseDouble(body.get("distanceKm"));
        String channel = str(body.get("channel"));

        return ResponseEntity.ok(ghnService.calculateShippingFee(
                toProvinceName,
                districtName,
                wardName,
                fullAddress,
                toDistrictId,
                toWardCode,
                weight,
                insuranceValue,
                orderSubtotal,
                channel,
                distanceKm
        ));
    }

    private Double parseDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.doubleValue();
        try {
            String s = obj.toString().trim().replace(",", ".");
            return s.isEmpty() ? null : Double.valueOf(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String str(Object obj) {
        if (obj == null) return null;
        String s = obj.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private Integer parseInt(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.intValue();
        try {
            String s = obj.toString().trim();
            return s.isEmpty() ? null : Integer.valueOf(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long parseLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.longValue();
        try {
            String s = obj.toString().trim();
            return s.isEmpty() ? null : Long.valueOf(s);
        } catch (Exception ignored) {
            return null;
        }
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
