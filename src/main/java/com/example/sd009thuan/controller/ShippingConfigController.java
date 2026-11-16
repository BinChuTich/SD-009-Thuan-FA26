package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.ShippingConfigDTO;
import com.example.sd009thuan.dto.ShippingRuleDTO;
import com.example.sd009thuan.service.GhnService;
import com.example.sd009thuan.service.ShippingConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/cau-hinh-phi-ship", "/api/shipping-config"})
@CrossOrigin("*")
public class ShippingConfigController {

    private final ShippingConfigService shippingConfigService;
    private final GhnService ghnService;

    public ShippingConfigController(ShippingConfigService shippingConfigService, GhnService ghnService) {
        this.shippingConfigService = shippingConfigService;
        this.ghnService = ghnService;
    }

    @GetMapping
    public ResponseEntity<ShippingConfigDTO> getConfig() {
        return ResponseEntity.ok(shippingConfigService.getConfig());
    }

    @PutMapping
    public ResponseEntity<?> updateConfig(
            @RequestBody Map<String, Object> body,
            @RequestParam(required = false) Long idNhanVien,
            @RequestParam(required = false) String maVaiTro,
            @RequestParam(required = false) String nguoiCapNhat
    ) {
        try {
            ShippingConfigDTO dto = parseDtoFromBody(body);
            Long staffId = idNhanVien != null ? idNhanVien : parseLong(body.get("idNhanVien"));
            if (staffId == null) staffId = parseLong(body.get("nhanVienId"));
            String role = maVaiTro != null ? maVaiTro : str(body.get("maVaiTro"));
            String staffName = nguoiCapNhat != null ? nguoiCapNhat : str(body.get("nguoiCapNhat"));
            return ResponseEntity.ok(shippingConfigService.updateConfig(dto, staffId, role, staffName));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể lưu cấu hình phí ship: " + e.getMessage()));
        }
    }

    @PostMapping("/rules")
    public ResponseEntity<?> saveRule(
            @RequestBody Map<String, Object> body,
            @RequestParam(required = false) Long idNhanVien,
            @RequestParam(required = false) String maVaiTro,
            @RequestParam(required = false) String nguoiCapNhat
    ) {
        try {
            ShippingRuleDTO rule = parseRuleFromMap(body);
            Long staffId = idNhanVien != null ? idNhanVien : parseLong(body.get("idNhanVien"));
            String role = maVaiTro != null ? maVaiTro : str(body.get("maVaiTro"));
            String staffName = nguoiCapNhat != null ? nguoiCapNhat : str(body.get("nguoiCapNhat"));
            return ResponseEntity.ok(shippingConfigService.addOrUpdateRule(rule, staffId, role, staffName));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể lưu cấu hình phí vận chuyển: " + e.getMessage()));
        }
    }

    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<?> deleteRule(
            @PathVariable Long ruleId,
            @RequestParam(required = false) Long idNhanVien,
            @RequestParam(required = false) String maVaiTro,
            @RequestParam(required = false) String nguoiCapNhat
    ) {
        try {
            return ResponseEntity.ok(shippingConfigService.deleteRule(ruleId, idNhanVien, maVaiTro, nguoiCapNhat));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể xóa cấu hình phí vận chuyển: " + e.getMessage()));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetDefaults(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Long idNhanVien,
            @RequestParam(required = false) String maVaiTro,
            @RequestParam(required = false) String nguoiCapNhat
    ) {
        try {
            Map<String, Object> safeBody = body != null ? body : Map.of();
            Long staffId = idNhanVien != null ? idNhanVien : parseLong(safeBody.get("idNhanVien"));
            String role = maVaiTro != null ? maVaiTro : str(safeBody.get("maVaiTro"));
            String staffName = nguoiCapNhat != null ? nguoiCapNhat : str(safeBody.get("nguoiCapNhat"));
            return ResponseEntity.ok(shippingConfigService.resetDefaults(staffId, role, staffName));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể khôi phục cấu hình mặc định: " + e.getMessage()));
        }
    }

    @PostMapping("/preview-fee")
    public ResponseEntity<?> previewFee(@RequestBody Map<String, Object> body) {
        String provinceName = str(body.get("provinceName"));
        String districtName = str(body.get("districtName"));
        String wardName = str(body.get("wardName"));
        String fullAddress = str(body.get("fullAddress"));
        String channel = str(body.get("channel"));
        Long orderSubtotal = parseLong(body.get("orderSubtotal"));
        Double distanceKm = parseDouble(body.get("distanceKm"));
        Integer toDistrictId = parseInt(body.get("toDistrictId"));
        String toWardCode = str(body.get("toWardCode"));
        Integer weight = parseInt(body.get("weight"));
        Integer insuranceValue = parseInt(body.get("insuranceValue"));

        ShippingConfigDTO configToUse = shippingConfigService.getConfig();
        Object overrideRaw = body.get("configOverride");
        if (overrideRaw instanceof Map<?, ?> mapRaw) {
            Map<String, Object> overrideMap = new HashMap<>();
            mapRaw.forEach((k, v) -> overrideMap.put(String.valueOf(k), v));
            configToUse = shippingConfigService.validateAndNormalize(parseDtoFromBody(overrideMap), configToUse, null);
        }

        return ResponseEntity.ok(ghnService.calculateShippingFeeWithConfig(
                configToUse,
                provinceName,
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

    private ShippingConfigDTO parseDtoFromBody(Map<String, Object> body) {
        ShippingConfigDTO dto = new ShippingConfigDTO();
        if (body == null) return dto;
        dto.setCheDoTinhPhi(str(body.get("cheDoTinhPhi")));
        dto.setPhiNoiThanhHaNoi(parseLong(body.get("phiNoiThanhHaNoi")));
        dto.setPhiNgoaiThanhHaNoi(parseLong(body.get("phiNgoaiThanhHaNoi")));
        dto.setPhiMienBac(parseLong(body.get("phiMienBac")));
        dto.setPhiMienTrung(parseLong(body.get("phiMienTrung")));
        dto.setPhiMienNam(parseLong(body.get("phiMienNam")));
        dto.setPhiMacDinhToanQuoc(parseLong(body.get("phiMacDinhToanQuoc")));
        dto.setApDungMienPhiShip(parseBool(body.get("apDungMienPhiShip")));
        dto.setNguongMienPhiShip(parseLong(body.get("nguongMienPhiShip")));
        dto.setApDungFreeshipPos(parseBool(body.get("apDungFreeshipPos")));
        dto.setApDungFreeshipOnline(parseBool(body.get("apDungFreeshipOnline")));
        dto.setNguoiCapNhat(str(body.get("nguoiCapNhat")));

        Object rawRules = body.get("danhSachCauHinh");
        if (rawRules instanceof List<?> listRaw) {
            List<ShippingRuleDTO> rules = new ArrayList<>();
            for (Object item : listRaw) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> mapItem = new HashMap<>();
                    m.forEach((k, v) -> mapItem.put(String.valueOf(k), v));
                    rules.add(parseRuleFromMap(mapItem));
                }
            }
            dto.setDanhSachCauHinh(rules);
        } else {
            dto.setDanhSachCauHinh(null);
        }
        return dto;
    }

    private ShippingRuleDTO parseRuleFromMap(Map<String, Object> map) {
        ShippingRuleDTO rule = new ShippingRuleDTO();
        if (map == null) return rule;
        rule.setId(parseLong(map.get("id")));
        rule.setGiaTriTu(parseLong(map.get("giaTriTu")));
        rule.setGiaTriDen(parseLong(map.get("giaTriDen")));
        rule.setKhoangCachTu(parseDouble(map.get("khoangCachTu")));
        rule.setKhoangCachDen(parseDouble(map.get("khoangCachDen")));
        rule.setHinhThucTinhPhi(str(map.get("hinhThucTinhPhi")));
        rule.setPhiVanChuyen(parseLong(map.get("phiVanChuyen")));
        rule.setTrangThai(parseInt(map.get("trangThai")));
        return rule;
    }

    private String str(Object obj) {
        return obj != null ? obj.toString().trim() : null;
    }

    private Long parseLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.longValue();
        try {
            String s = obj.toString().trim();
            return s.isEmpty() ? null : Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.doubleValue();
        try {
            String s = obj.toString().trim().replace(",", ".");
            return s.isEmpty() ? null : Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInt(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.intValue();
        try {
            String s = obj.toString().trim();
            return s.isEmpty() ? null : Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean parseBool(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Boolean b) return b;
        return Boolean.parseBoolean(obj.toString().trim());
    }
}
