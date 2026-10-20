package com.example.sd009thuan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service xác thực địa giới hành chính Việt Nam (chuẩn 34 Tỉnh / Thành phố sau sáp nhập 2025)
 * Dữ liệu đồng bộ theo https://provinces.open-api.vn/api/v2/
 */
@Service
public class AdministrativeRegionService {
    private static final Logger log = LoggerFactory.getLogger(AdministrativeRegionService.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private List<ProvinceDto> provinces = new ArrayList<>();
    private final Map<String, ProvinceDto> normalizedProvinceIndex = new HashMap<>();

    public record WardDto(
            Integer code,
            String name,
            String codename,
            String division_type
    ) {}

    public record ProvinceDto(
            Integer code,
            String name,
            String codename,
            String division_type,
            List<WardDto> wards
    ) {}

    public record AddressValidationResult(
            boolean valid,
            List<String> errors,
            ProvinceDto matchedProvince,
            WardDto matchedWard
    ) {
        public static AddressValidationResult success(ProvinceDto p, WardDto w) {
            return new AddressValidationResult(true, Collections.emptyList(), p, w);
        }

        public static AddressValidationResult failure(List<String> errors) {
            return new AddressValidationResult(false, errors, null, null);
        }
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/administrative_regions_v2.json")) {
            if (is == null) {
                log.error("Không tìm thấy file tài nguyên administrative_regions_v2.json trong classpath!");
                return;
            }
            provinces = objectMapper.readValue(is, new TypeReference<List<ProvinceDto>>() {});
            for (ProvinceDto p : provinces) {
                if (p.name() != null) {
                    normalizedProvinceIndex.put(normalize(p.name()), p);
                }
            }
            int totalWards = provinces.stream().mapToInt(p -> p.wards() != null ? p.wards().size() : 0).sum();
            log.info("Đã nạp thành công {} tỉnh thành và {} phường xã sau sáp nhập 2025 vào bộ nhớ.", provinces.size(), totalWards);
        } catch (Exception e) {
            log.error("Lỗi khi đọc file dữ liệu địa giới hành chính: {}", e.getMessage(), e);
        }
    }

    /**
     * Chuẩn hóa chuỗi tiếng Việt: bỏ dấu, bỏ tiền tố hành chính, chuyển chữ thường, loại bỏ ký tự đặc biệt
     */
    public static String normalize(String input) {
        if (input == null) return "";
        String s = input.trim().toLowerCase();

        // Chuẩn hóa Unicode và bỏ dấu tiếng Việt
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        s = pattern.matcher(s).replaceAll("");
        s = s.replace("đ", "d").replace("Đ", "d");

        // Loại bỏ các tiền tố hành chính phổ biến
        s = s.replaceAll("^(thanh pho|tinh|tp\\.|tp)\\s+", "");
        s = s.replaceAll("^(phuong|xa|thi tran|tt\\.|tt)\\s+", "");

        // Chỉ giữ chữ cái và số
        s = s.replaceAll("[^a-z0-9]", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    /**
     * Tìm Tỉnh / Thành phố từ tên người dùng nhập (hỗ trợ nhập có dấu, không dấu, có tiền tố, không tiền tố)
     */
    public ProvinceDto findProvince(String provinceName) {
        if (provinceName == null || provinceName.isBlank() || provinces == null || provinces.isEmpty()) {
            return null;
        }

        String raw = provinceName.trim().toLowerCase();
        String norm = normalize(provinceName);

        // 1. Khớp chính xác tên gốc
        for (ProvinceDto p : provinces) {
            if (p.name() != null && p.name().trim().equalsIgnoreCase(raw)) {
                return p;
            }
        }

        // 2. Khớp chuỗi đã chuẩn hóa
        if (normalizedProvinceIndex.containsKey(norm)) {
            return normalizedProvinceIndex.get(norm);
        }

        // 3. Khớp một phần chuỗi (chứa nhau)
        for (ProvinceDto p : provinces) {
            String pNorm = normalize(p.name());
            if (!norm.isBlank() && !pNorm.isBlank()) {
                if (norm.equals(pNorm) || norm.contains(pNorm) || pNorm.contains(norm)) {
                    return p;
                }
            }
        }

        // 4. Khớp theo codename
        for (ProvinceDto p : provinces) {
            if (p.codename() != null && p.codename().equalsIgnoreCase(norm.replace(" ", "_"))) {
                return p;
            }
        }

        return null;
    }

    /**
     * Tìm Phường / Xã trong danh sách phường xã của một Tỉnh
     */
    public WardDto findWardInProvince(ProvinceDto province, String wardName) {
        if (province == null || province.wards() == null || wardName == null || wardName.isBlank()) {
            return null;
        }

        String raw = wardName.trim().toLowerCase();
        String norm = normalize(wardName);

        // 1. Khớp chính xác tên gốc
        for (WardDto w : province.wards()) {
            if (w.name() != null && w.name().trim().equalsIgnoreCase(raw)) {
                return w;
            }
        }

        // 2. Khớp theo tên chuẩn hóa hoàn toàn
        for (WardDto w : province.wards()) {
            String wNorm = normalize(w.name());
            if (wNorm.equals(norm)) {
                return w;
            }
        }

        // 3. Khớp chứa nhau nếu chuỗi đủ dài (>= 3 ký tự)
        if (norm.length() >= 3) {
            for (WardDto w : province.wards()) {
                String wNorm = normalize(w.name());
                if (wNorm.length() >= 3 && (norm.contains(wNorm) || wNorm.contains(norm))) {
                    return w;
                }
            }
        }

        // 4. Khớp theo codename
        String codeNorm = norm.replace(" ", "_");
        for (WardDto w : province.wards()) {
            if (w.codename() != null && w.codename().equalsIgnoreCase(codeNorm)) {
                return w;
            }
        }

        return null;
    }

    /**
     * Xác thực địa chỉ: kiểm tra Tỉnh/Thành phố có hợp lệ không và Phường/Xã có thuộc Tỉnh/Thành phố đó không
     */
    public AddressValidationResult validateAddress(String provinceName, String wardName) {
        List<String> errors = new ArrayList<>();

        if (provinceName == null || provinceName.isBlank()) {
            errors.add("Tỉnh / Thành phố không được để trống");
        }
        if (wardName == null || wardName.isBlank()) {
            errors.add("Phường / Xã không được để trống");
        }

        if (!errors.isEmpty()) {
            return AddressValidationResult.failure(errors);
        }

        ProvinceDto province = findProvince(provinceName);
        if (province == null) {
            errors.add(String.format("Tỉnh / Thành phố '%s' không tồn tại trong danh mục 34 tỉnh thành sau sáp nhập", provinceName));
            return AddressValidationResult.failure(errors);
        }

        WardDto ward = findWardInProvince(province, wardName);
        if (ward == null) {
            errors.add(String.format("Phường / Xã '%s' không thuộc Tỉnh / Thành phố '%s'", wardName, province.name()));
            return AddressValidationResult.failure(errors);
        }

        return AddressValidationResult.success(province, ward);
    }
}

