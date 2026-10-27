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
 * Quy tắc nghiêm ngặt:
 *  - Bắt buộc phải có tiền tố chuẩn ("Tỉnh ", "Thành phố ", "Phường ", "Xã ", "Thị trấn ")
 *  - Không chấp nhận viết tắt ("TP.", "P.", "X.", "TT.") hoặc thiếu tiền tố ("Hà Nội", "Ba Đình")
 *  - Phải kiểm tra tính đồng bộ và trực thuộc (Phường/Xã phải thực sự thuộc Tỉnh/Thành phố đó)
 */
@Service
public class AdministrativeRegionService {
    private static final Logger log = LoggerFactory.getLogger(AdministrativeRegionService.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private List<ProvinceDto> provinces = new ArrayList<>();
    private final Map<String, ProvinceDto> provinceByName = new HashMap<>();
    private final Map<String, ProvinceDto> provinceByCoreName = new HashMap<>();
    private final Map<String, ProvinceDto> normalizedProvinceIndex = new HashMap<>();
    private final Set<String> allKnownWardNames = new HashSet<>();
    private final Set<String> allKnownWardCoreNames = new HashSet<>();

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
                    String raw = p.name().trim();
                    provinceByName.put(raw.toLowerCase(), p);
                    String core = extractCoreProvinceName(raw).toLowerCase();
                    if (!core.isBlank()) {
                        provinceByCoreName.put(core, p);
                    }
                    normalizedProvinceIndex.put(normalize(raw), p);
                }
                if (p.wards() != null) {
                    for (WardDto w : p.wards()) {
                        if (w.name() != null) {
                            String wRaw = w.name().trim();
                            allKnownWardNames.add(wRaw.toLowerCase());
                            String wCore = extractCoreWardName(wRaw).toLowerCase();
                            if (!wCore.isBlank()) {
                                allKnownWardCoreNames.add(wCore);
                            }
                        }
                    }
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
     * Kiểm tra tên Tỉnh / Thành phố có bắt đầu bằng tiền tố chuẩn ("Tỉnh ", "Thành phố ") hay không
     */
    public static boolean hasValidProvincePrefix(String name) {
        if (name == null || name.isBlank()) return false;
        String s = name.trim().toLowerCase();
        return s.startsWith("thành phố ") || s.startsWith("tỉnh ");
    }

    /**
     * Kiểm tra tên Tỉnh / Thành phố có bị viết tắt tiền tố ("TP.", "TP ", "T.", "T ") hay không
     */
    public static boolean hasAbbreviatedProvincePrefix(String name) {
        if (name == null || name.isBlank()) return false;
        String s = name.trim().toLowerCase();
        return s.matches("^(?i)(tp\\.|tp\\s+|t\\.|t\\s+).*");
    }

    /**
     * Tách phần tên riêng (lõi) của Tỉnh / Thành phố
     */
    public static String extractCoreProvinceName(String name) {
        if (name == null) return "";
        String s = name.trim();
        String lower = s.toLowerCase();
        if (lower.startsWith("thành phố ")) {
            return s.substring(10).trim();
        }
        if (lower.startsWith("tỉnh ")) {
            return s.substring(5).trim();
        }
        return s;
    }

    /**
     * Kiểm tra tên Phường / Xã có bắt đầu bằng tiền tố chuẩn ("Phường ", "Xã ", "Thị trấn ", "Đặc khu ") hay không
     */
    public static boolean hasValidWardPrefix(String name) {
        if (name == null || name.isBlank()) return false;
        String s = name.trim().toLowerCase();
        return s.startsWith("phường ") || s.startsWith("xã ")
                || s.startsWith("thị trấn ") || s.startsWith("đặc khu ");
    }

    /**
     * Kiểm tra tên Phường / Xã có bị viết tắt tiền tố ("P.", "P ", "X.", "X ", "TT.", "TT ") hay không
     */
    public static boolean hasAbbreviatedWardPrefix(String name) {
        if (name == null || name.isBlank()) return false;
        String s = name.trim().toLowerCase();
        return s.matches("^(?i)(p\\.|p\\s+|x\\.|x\\s+|tt\\.|tt\\s+).*");
    }

    /**
     * Tách phần tên riêng (lõi) của Phường / Xã
     */
    public static String extractCoreWardName(String name) {
        if (name == null) return "";
        String s = name.trim();
        String lower = s.toLowerCase();
        if (lower.startsWith("thị trấn ")) {
            return s.substring(9).trim();
        }
        if (lower.startsWith("đặc khu ")) {
            return s.substring(8).trim();
        }
        if (lower.startsWith("phường ")) {
            return s.substring(7).trim();
        }
        if (lower.startsWith("xã ")) {
            return s.substring(3).trim();
        }
        return s;
    }

    /**
     * Tìm Tỉnh / Thành phố nghiêm ngặt (chỉ tìm khi đã có tiền tố chuẩn)
     */
    public ProvinceDto findProvinceStrict(String provinceName) {
        if (provinceName == null || provinceName.isBlank() || provinces == null || provinces.isEmpty()) {
            return null;
        }

        String raw = provinceName.trim().toLowerCase();
        String core = extractCoreProvinceName(provinceName).toLowerCase();

        // 1. Khớp chính xác tên đầy đủ
        if (provinceByName.containsKey(raw)) {
            return provinceByName.get(raw);
        }

        // 2. Khớp theo tên lõi (Ví dụ: "Tỉnh Bắc Ninh" khớp với "Thành phố Bắc Ninh")
        if (!core.isBlank() && provinceByCoreName.containsKey(core)) {
            return provinceByCoreName.get(core);
        }

        return null;
    }

    /**
     * Tìm Phường / Xã nghiêm ngặt trong một Tỉnh / Thành phố
     */
    public WardDto findWardInProvinceStrict(ProvinceDto province, String wardName) {
        if (province == null || province.wards() == null || wardName == null || wardName.isBlank()) {
            return null;
        }

        String raw = wardName.trim().toLowerCase();
        String core = extractCoreWardName(wardName).toLowerCase();

        // 1. Khớp chính xác tên đầy đủ bao gồm tiền tố
        for (WardDto w : province.wards()) {
            if (w.name() != null && w.name().trim().equalsIgnoreCase(raw)) {
                return w;
            }
        }

        // 2. Khớp theo tên lõi trong cùng tỉnh
        if (!core.isBlank()) {
            for (WardDto w : province.wards()) {
                if (w.name() != null && extractCoreWardName(w.name()).equalsIgnoreCase(core)) {
                    return w;
                }
            }
        }

        return null;
    }

    /**
     * Kiểm tra xem Phường/Xã có tồn tại ở bất kỳ tỉnh thành nào khác trong hệ thống hay không
     */
    public boolean isWardKnownInOtherProvinces(String wardName) {
        if (wardName == null || wardName.isBlank()) return false;
        String raw = wardName.trim().toLowerCase();
        if (allKnownWardNames.contains(raw)) return true;
        String core = extractCoreWardName(wardName).toLowerCase();
        return !core.isBlank() && allKnownWardCoreNames.contains(core);
    }

    /**
     * Kiểm tra tính hợp lệ của địa chỉ theo tiêu chuẩn nghiêm ngặt:
     * 1. Tỉnh/Thành phố phải có tiền tố chuẩn ("Tỉnh ", "Thành phố "), không được viết tắt, không được thiếu tiền tố
     * 2. Phường/Xã phải có tiền tố chuẩn ("Phường ", "Xã ", "Thị trấn "), không được viết tắt, không được thiếu tiền tố
     * 3. Tính đồng bộ và trực thuộc (Mapping): Phường/Xã phải thực sự thuộc Tỉnh/Thành phố tương ứng
     */
    public AddressValidationResult validateAddress(String provinceName, String wardName) {
        List<String> errors = new ArrayList<>();

        String pName = provinceName != null ? provinceName.trim() : "";
        String wName = wardName != null ? wardName.trim() : "";

        // 1. Kiểm tra Tỉnh / Thành phố
        ProvinceDto matchedProvince = null;
        if (pName.isBlank()) {
            errors.add("Tỉnh / Thành phố không được để trống");
        } else if (hasValidProvincePrefix(pName)) {
            matchedProvince = findProvinceStrict(pName);
            if (matchedProvince == null) {
                errors.add(String.format("Tỉnh / Thành phố '%s' không tồn tại trong danh mục địa chính hệ thống", pName));
            }
        } else if (hasAbbreviatedProvincePrefix(pName)) {
            errors.add(String.format("Tỉnh / Thành phố '%s' viết tắt tiền tố. Vui lòng ghi đầy đủ tiền tố chuẩn 'Tỉnh' hoặc 'Thành phố' (ví dụ: 'Thành phố Hà Nội', 'Tỉnh Bắc Ninh')", pName));
        } else {
            errors.add(String.format("Tỉnh / Thành phố '%s' thiếu tiền tố chuẩn (phải bắt đầu bằng 'Tỉnh ' hoặc 'Thành phố ', ví dụ: 'Thành phố Hà Nội', 'Tỉnh Bắc Ninh')", pName));
        }

        // 2. Kiểm tra Phường / Xã
        WardDto matchedWard = null;
        if (wName.isBlank()) {
            errors.add("Phường / Xã không được để trống");
        } else if (hasValidWardPrefix(wName)) {
            if (matchedProvince != null) {
                matchedWard = findWardInProvinceStrict(matchedProvince, wName);
                if (matchedWard == null) {
                    if (isWardKnownInOtherProvinces(wName)) {
                        errors.add(String.format("Phường / Xã '%s' không thuộc Tỉnh / Thành phố '%s'", wName, matchedProvince.name()));
                    } else {
                        errors.add(String.format("Phường / Xã '%s' không tồn tại trong hệ thống hoặc không thuộc Tỉnh / Thành phố '%s'", wName, matchedProvince.name()));
                    }
                }
            } else {
                if (!isWardKnownInOtherProvinces(wName)) {
                    errors.add(String.format("Phường / Xã '%s' không tồn tại trong hệ thống", wName));
                }
            }
        } else if (hasAbbreviatedWardPrefix(wName)) {
            errors.add(String.format("Phường / Xã '%s' viết tắt tiền tố. Vui lòng ghi đầy đủ tiền tố chuẩn 'Phường', 'Xã' hoặc 'Thị trấn' (ví dụ: 'Phường Ba Đình', 'Xã Tiên Du')", wName));
        } else {
            errors.add(String.format("Phường / Xã '%s' thiếu tiền tố chuẩn (phải bắt đầu bằng 'Phường ', 'Xã ' hoặc 'Thị trấn ', ví dụ: 'Phường Ba Đình', 'Xã Tiên Du')", wName));
        }

        if (!errors.isEmpty()) {
            return AddressValidationResult.failure(errors);
        }

        return AddressValidationResult.success(matchedProvince, matchedWard);
    }

    /**
     * Tìm Tỉnh / Thành phố tương thích cũ (fallback)
     */
    public ProvinceDto findProvince(String provinceName) {
        ProvinceDto p = findProvinceStrict(provinceName);
        if (p != null) return p;

        if (provinceName == null || provinceName.isBlank() || provinces == null || provinces.isEmpty()) {
            return null;
        }

        String norm = normalize(provinceName);
        if (normalizedProvinceIndex.containsKey(norm)) {
            return normalizedProvinceIndex.get(norm);
        }

        for (ProvinceDto item : provinces) {
            String pNorm = normalize(item.name());
            if (!norm.isBlank() && !pNorm.isBlank() && (norm.equals(pNorm) || norm.contains(pNorm) || pNorm.contains(norm))) {
                return item;
            }
        }
        return null;
    }

    /**
     * Tìm Phường / Xã tương thích cũ (fallback)
     */
    public WardDto findWardInProvince(ProvinceDto province, String wardName) {
        WardDto w = findWardInProvinceStrict(province, wardName);
        if (w != null) return w;

        if (province == null || province.wards() == null || wardName == null || wardName.isBlank()) {
            return null;
        }

        String norm = normalize(wardName);
        for (WardDto item : province.wards()) {
            if (normalize(item.name()).equals(norm)) {
                return item;
            }
        }
        return null;
    }
}
