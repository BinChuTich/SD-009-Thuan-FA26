package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ghn.GhnFeeRequestDTO;
import com.example.sd009thuan.dto.ghn.GhnFeeResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Service
public class GhnService {

    private static final Logger log = LoggerFactory.getLogger(GhnService.class);

    @Value("${ghn.api-url:https://dev-online-gateway.ghn.vn/shiip/public-api}")
    private String ghnApiUrl;

    @Value("${ghn.token:d007d135-e1fb-11ee-8bfa-8a2dda8ec551}")
    private String ghnToken;

    @Value("${ghn.shop-id:191437}")
    private Integer ghnShopId;

    @Value("${ghn.shop-district-id:1454}")
    private Integer shopDistrictId;

    @Value("${ghn.shop-ward-code:20109}")
    private String shopWardCode;

    private final RestTemplate restTemplate;

    public GhnService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(4))
                .setReadTimeout(Duration.ofSeconds(6))
                .build();
    }

    /**
     * Tính phí giao hàng qua API GHN hoặc theo chính sách khu vực (Fallback an toàn)
     */
    public GhnFeeResponseDTO calculateShippingFee(GhnFeeRequestDTO req) {
        // 1. Thử gọi API GHN chính thức nếu có toDistrictId & toWardCode hợp lệ
        if (req.getToDistrictId() != null && req.getToDistrictId() > 0 && req.getToWardCode() != null && !req.getToWardCode().isBlank()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Token", ghnToken);
                if (ghnShopId != null && ghnShopId > 0) {
                    headers.set("ShopId", String.valueOf(ghnShopId));
                }

                Map<String, Object> body = new HashMap<>();
                body.put("service_type_id", 2); // 2: Chuẩn thương mại điện tử
                body.put("from_district_id", req.getFromDistrictId() != null ? req.getFromDistrictId() : shopDistrictId);
                body.put("to_district_id", req.getToDistrictId());
                body.put("to_ward_code", req.getToWardCode().trim());
                body.put("height", req.getHeight() != null ? req.getHeight() : 10);
                body.put("length", req.getLength() != null ? req.getLength() : 20);
                body.put("width", req.getWidth() != null ? req.getWidth() : 15);
                body.put("weight", req.getWeight() != null ? req.getWeight() : 500); // 500g cho áo phông

                if (req.getInsuranceValue() != null && req.getInsuranceValue().compareTo(BigDecimal.ZERO) > 0) {
                    // GHN quy định giá trị bảo hiểm tối đa 5.000.000 VNĐ
                    BigDecimal safeInsurance = req.getInsuranceValue().min(new BigDecimal("5000000"));
                    body.put("insurance_value", safeInsurance.intValue());
                } else {
                    body.put("insurance_value", 0);
                }

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                String feeUrl = ghnApiUrl + "/v2/shipping-order/fee";

                ResponseEntity<Map> response = restTemplate.postForEntity(feeUrl, entity, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> resBody = response.getBody();
                    Integer code = (Integer) resBody.get("code");
                    if (code != null && code == 200 && resBody.containsKey("data")) {
                        Map<String, Object> data = (Map<String, Object>) resBody.get("data");
                        Object totalObj = data.get("total");
                        if (totalObj != null) {
                            BigDecimal ghnTotal = new BigDecimal(totalObj.toString());
                            log.info("Tính phí ship thành công qua GHN API: {} VNĐ (Quận: {}, Phường: {})", ghnTotal, req.getToDistrictId(), req.getToWardCode());
                            return GhnFeeResponseDTO.builder()
                                    .success(true)
                                    .fee(ghnTotal)
                                    .currency("VND")
                                    .source("GHN_API")
                                    .regionDescription("Tính phí tự động trực tiếp từ Giao Hàng Nhanh (GHN)")
                                    .message("Tính phí vận chuyển thành công qua API GHN")
                                    .build();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Không thể gọi API GHN trực tiếp (sẽ dùng chính sách khu vực): {}", e.getMessage());
            }
        }

        // 2. CHÍNH SÁCH: Luôn tính phí theo khu vực địa lý
        BigDecimal regionalFee = calculateRegionalFee(req.getProvinceName(), req.getDistrictName());
        String regionDesc = determineRegionName(req.getProvinceName(), req.getDistrictName());

        return GhnFeeResponseDTO.builder()
                .success(true)
                .fee(regionalFee)
                .currency("VND")
                .source("REGIONAL_POLICY")
                .regionDescription(regionDesc)
                .message("Tính phí theo chính sách khu vực giao hàng (" + regionDesc + ")")
                .build();
    }

    /**
     * Chính sách giá cước chuẩn theo khu vực địa lý:
     * - Nội thành Hà Nội (Shop): 25.000 VNĐ
     * - Ngoại thành Hà Nội: 30.000 VNĐ
     * - Miền Bắc lân cận: 35.000 VNĐ
     * - Miền Trung: 40.000 VNĐ
     * - Miền Nam: 45.000 VNĐ
     */
    public BigDecimal calculateRegionalFee(String provinceName, String districtName) {
        if (provinceName == null || provinceName.isBlank()) {
            return new BigDecimal("35000"); // Mặc định liên tỉnh
        }

        String p = cleanText(provinceName);
        String d = cleanText(districtName);

        // 1. Hà Nội (Khu vực Shop đặt kho)
        if (p.contains("ha noi") || p.contains("hanoi")) {
            // Danh sách quận nội thành Hà Nội
            List<String> innerDistricts = Arrays.asList(
                    "ba dinh", "hoan kiem", "dong da", "cau giay", "nam tu liem",
                    "bac tu liem", "thanh xuan", "hai ba trung", "tay ho", "hoang mai",
                    "long bien", "ha dong"
            );
            boolean isInner = innerDistricts.stream().anyMatch(d::contains);
            return isInner ? new BigDecimal("25000") : new BigDecimal("30000");
        }

        // 2. Miền Bắc lân cận
        List<String> northProvinces = Arrays.asList(
                "bac ninh", "hung yen", "hai phong", "hai duong", "quang ninh",
                "ha nam", "nam dinh", "ninh binh", "thai binh", "vinh phuc",
                "phu tho", "thai nguyen", "bac giang", "lang son", "hoa binh"
        );
        if (northProvinces.stream().anyMatch(p::contains)) {
            return new BigDecimal("35000");
        }

        // 3. Miền Trung
        List<String> centralProvinces = Arrays.asList(
                "thanh hoa", "nghe an", "ha tinh", "quang binh", "quang tri",
                "thua thien hue", "hue", "da nang", "quang nam", "quang ngai",
                "binh dinh", "phu yen", "khanh hoa", "ninh thuan", "binh thuan",
                "kon tum", "gia lai", "dak lak", "dak nong", "lam dong"
        );
        if (centralProvinces.stream().anyMatch(p::contains)) {
            return new BigDecimal("40000");
        }

        // 4. Miền Nam
        return new BigDecimal("45000");
    }

    public String determineRegionName(String provinceName, String districtName) {
        if (provinceName == null || provinceName.isBlank()) return "Giao hàng tiêu chuẩn toàn quốc";
        String p = cleanText(provinceName);
        String d = cleanText(districtName);

        if (p.contains("ha noi")) {
            List<String> inner = Arrays.asList("ba dinh", "hoan kiem", "dong da", "cau giay", "nam tu liem", "bac tu liem", "thanh xuan", "hai ba trung", "tay ho", "hoang mai", "long bien", "ha dong");
            return inner.stream().anyMatch(d::contains) ? "Nội thành Hà Nội (Hỏa tốc 2-4h)" : "Ngoại thành Hà Nội (Trong ngày)";
        }
        List<String> north = Arrays.asList("bac ninh", "hung yen", "hai phong", "hai duong", "quang ninh", "ha nam", "nam dinh", "ninh binh", "thai binh", "vinh phuc", "phu tho", "thai nguyen", "bac giang");
        if (north.stream().anyMatch(p::contains)) return "Miền Bắc lân cận (1-2 ngày)";

        List<String> central = Arrays.asList("thanh hoa", "nghe an", "ha tinh", "quang binh", "quang tri", "thua thien hue", "hue", "da nang", "quang nam", "quang ngai", "binh dinh", "khanh hoa");
        if (central.stream().anyMatch(p::contains)) return "Miền Trung (2-3 ngày)";

        return "Miền Nam & Toàn quốc (3-4 ngày)";
    }

    private String cleanText(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace("đ", "d")
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
    }
}

