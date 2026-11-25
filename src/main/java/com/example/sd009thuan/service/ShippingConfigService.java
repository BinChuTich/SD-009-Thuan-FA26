package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ShippingConfigDTO;
import com.example.sd009thuan.dto.ShippingRuleDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dịch vụ quản lý cấu hình phí vận chuyển (lưu trữ trên bộ nhớ & file JSON cục bộ,
 * tuyệt đối không tạo bảng hay thêm dữ liệu vào Database).
 */
@Service
public class ShippingConfigService {

    private static final Logger log = LoggerFactory.getLogger(ShippingConfigService.class);
    private static final long MAX_SHIPPING_FEE = 50_000_000L;

    private static final List<String> NEAR_SHOP_KEYWORDS = List.of(
            "my dinh", "me tri", "phu do", "cau dien", "tay mo", "dai mo", "trung van", "nam tu liem"
    );

    private static final List<String> OUTER_HANOI_DISTRICTS = List.of(
            "dong anh", "gia lam", "soc son", "ba vi", "chuong my", "dan phuong",
            "hoai duc", "me linh", "my duc", "phu xuyen", "phuc tho", "quoc oai",
            "thach that", "thanh oai", "thuong tin", "ung hoa", "son tay"
    );

    private static final List<String> NORTH_PROVINCES = List.of(
            "bac ninh", "hung yen", "hai phong", "hai duong", "quang ninh", "ha nam",
            "nam dinh", "ninh binh", "thai binh", "vinh phuc", "phu tho", "thai nguyen",
            "bac giang", "lang son", "hoa binh", "tuyen quang", "yen bai", "lao cai",
            "cao bang", "bac kan", "ha giang", "son la", "dien bien", "lai chau"
    );

    private static final List<String> CENTRAL_PROVINCES = List.of(
            "thanh hoa", "nghe an", "ha tinh", "quang binh", "quang tri", "thua thien hue",
            "hue", "da nang", "quang nam", "quang ngai", "binh dinh", "phu yen",
            "khanh hoa", "ninh thuan", "binh thuan", "kon tum", "gia lai", "dak lak",
            "dak nong", "lam dong"
    );

    private static final List<String> SOUTH_PROVINCES = List.of(
            "ho chi minh", "tp hcm", "tphcm", "sai gon", "binh duong", "dong nai",
            "ba ria", "vung tau", "tay ninh", "binh phuoc", "long an", "tien giang",
            "ben tre", "tra vinh", "vinh long", "dong thap", "an giang", "kien giang",
            "can tho", "hau giang", "soc trang", "bac lieu", "ca mau"
    );

    private final ObjectMapper objectMapper;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private volatile ShippingConfigDTO cachedConfig;

    public ShippingConfigService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        this.cachedConfig = readFromConfigFile();
    }

    public List<ShippingRuleDTO> createDefaultRules() {
        List<ShippingRuleDTO> rules = new ArrayList<>();
        rules.add(new ShippingRuleDTO(1L, 0L, 100000L, 0.0, 1.0, "TINH_PHI", 10000L, 1));
        rules.add(new ShippingRuleDTO(2L, 0L, 500000L, 1.0, 10.0, "TINH_PHI", 20000L, 1));
        rules.add(new ShippingRuleDTO(3L, 500000L, 50000000L, 0.0, 50.0, "MIEN_PHI", 0L, 1));
        return rules;
    }

    public ShippingConfigDTO createDefaultConfig() {
        ShippingConfigDTO dto = new ShippingConfigDTO();
        dto.setCheDoTinhPhi("REGION_TABLE");
        dto.setPhiNoiThanhHaNoi(20000L);
        dto.setPhiNgoaiThanhHaNoi(30000L);
        dto.setPhiMienBac(35000L);
        dto.setPhiMienTrung(40000L);
        dto.setPhiMienNam(45000L);
        dto.setPhiMacDinhToanQuoc(45000L);
        dto.setApDungMienPhiShip(true);
        dto.setNguongMienPhiShip(500000L);
        dto.setApDungFreeshipPos(true);
        dto.setApDungFreeshipOnline(true);
        dto.setDanhSachCauHinh(createDefaultRules());
        dto.setNguoiCapNhat("Hệ thống");
        dto.setNgayCapNhat(Instant.now().toString());
        return dto;
    }

    public synchronized ShippingConfigDTO getConfig() {
        if (cachedConfig == null) {
            cachedConfig = readFromConfigFile();
        }
        return normalizeWithDefaults(cachedConfig);
    }

    public synchronized ShippingConfigDTO updateConfig(
            ShippingConfigDTO incoming,
            Long staffId,
            String roleCode,
            String staffName
    ) {
        verifyAdminPermission(staffId, roleCode);
        ShippingConfigDTO current = getConfig();
        ShippingConfigDTO normalized = validateAndNormalize(incoming, current, staffName);
        this.cachedConfig = normalized;
        writeToConfigFile(normalized);
        return normalized;
    }

    public synchronized ShippingConfigDTO addOrUpdateRule(
            ShippingRuleDTO ruleInput,
            Long staffId,
            String roleCode,
            String staffName
    ) {
        verifyAdminPermission(staffId, roleCode);
        ShippingConfigDTO current = getConfig();
        ShippingRuleDTO cleanRule = validateAndNormalizeRule(ruleInput, nextRuleId(current.getDanhSachCauHinh()));

        List<ShippingRuleDTO> list = new ArrayList<>(current.getDanhSachCauHinh() != null ? current.getDanhSachCauHinh() : List.of());
        boolean replaced = false;
        if (ruleInput != null && ruleInput.getId() != null) {
            for (int i = 0; i < list.size(); i++) {
                if (Objects.equals(list.get(i).getId(), ruleInput.getId())) {
                    cleanRule.setId(ruleInput.getId());
                    list.set(i, cleanRule);
                    replaced = true;
                    break;
                }
            }
        }
        if (!replaced) {
            list.add(cleanRule);
        }

        current.setDanhSachCauHinh(list);
        current.setNguoiCapNhat(staffName != null && !staffName.isBlank() ? staffName.trim() : "Quản trị viên");
        current.setNgayCapNhat(Instant.now().toString());
        this.cachedConfig = current;
        writeToConfigFile(current);
        return current;
    }

    public synchronized ShippingConfigDTO deleteRule(
            Long ruleId,
            Long staffId,
            String roleCode,
            String staffName
    ) {
        verifyAdminPermission(staffId, roleCode);
        ShippingConfigDTO current = getConfig();
        List<ShippingRuleDTO> list = new ArrayList<>(current.getDanhSachCauHinh() != null ? current.getDanhSachCauHinh() : List.of());
        list.removeIf(r -> Objects.equals(r.getId(), ruleId));
        current.setDanhSachCauHinh(list);
        current.setNguoiCapNhat(staffName != null && !staffName.isBlank() ? staffName.trim() : "Quản trị viên");
        current.setNgayCapNhat(Instant.now().toString());
        this.cachedConfig = current;
        writeToConfigFile(current);
        return current;
    }

    public synchronized ShippingConfigDTO resetDefaults(
            Long staffId,
            String roleCode,
            String staffName
    ) {
        verifyAdminPermission(staffId, roleCode);
        ShippingConfigDTO def = createDefaultConfig();
        if (staffName != null && !staffName.isBlank()) {
            def.setNguoiCapNhat(staffName.trim());
        }
        def.setNgayCapNhat(Instant.now().toString());
        this.cachedConfig = def;
        writeToConfigFile(def);
        return def;
    }

    private long nextRuleId(List<ShippingRuleDTO> rules) {
        if (rules == null || rules.isEmpty()) return 1L;
        return rules.stream()
                .map(ShippingRuleDTO::getId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1L;
    }

    private Path getConfigFilePath() {
        String dir = (uploadDir != null && !uploadDir.isBlank()) ? uploadDir : "uploads";
        return Paths.get(dir, "shipping-config.json").toAbsolutePath().normalize();
    }

    private ShippingConfigDTO readFromConfigFile() {
        try {
            Path path = getConfigFilePath();
            if (Files.exists(path)) {
                ShippingConfigDTO fromFile = objectMapper.readValue(Files.readString(path), ShippingConfigDTO.class);
                return normalizeWithDefaults(fromFile);
            }
        } catch (Exception e) {
            log.warn("==> [ShippingConfig] Không thể đọc file cấu hình phí ship, sử dụng mặc định: {}", e.getMessage());
        }
        return createDefaultConfig();
    }

    private void writeToConfigFile(ShippingConfigDTO cfg) {
        try {
            Path path = getConfigFilePath();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cfg));
        } catch (Exception e) {
            log.warn("==> [ShippingConfig] Không thể ghi file cấu hình phí ship: {}", e.getMessage());
        }
    }

    private void verifyAdminPermission(Long staffId, String roleCode) {
        if (roleCode != null && !roleCode.isBlank()) {
            String normalized = roleCode.trim().toUpperCase();
            if ("ADMIN".equals(normalized) || "QL".equals(normalized)
                    || normalized.contains("QUẢN TRỊ") || normalized.contains("QUAN TRI")) {
                return;
            }
            throw new SecurityException("Chỉ Quản trị viên (Admin) mới có quyền chỉnh sửa cấu hình phí ship!");
        }
        if (Long.valueOf(1L).equals(staffId)) {
            return;
        }
        throw new SecurityException("Chỉ Quản trị viên (Admin) mới có quyền chỉnh sửa cấu hình phí ship!");
    }

    public ShippingRuleDTO validateAndNormalizeRule(ShippingRuleDTO raw, long defaultId) {
        if (raw == null) {
            throw new IllegalArgumentException("Thông tin cấu hình phí vận chuyển không hợp lệ!");
        }
        long giaTriTu = raw.getGiaTriTu() != null ? raw.getGiaTriTu() : 0L;
        long giaTriDen = raw.getGiaTriDen() != null ? raw.getGiaTriDen() : 0L;
        if (giaTriTu < 0 || giaTriDen < 0) {
            throw new IllegalArgumentException("Giá trị hóa đơn không được nhỏ hơn 0!");
        }
        if (giaTriDen < giaTriTu) {
            throw new IllegalArgumentException("Giá trị hóa đơn 'Đến' phải lớn hơn hoặc bằng 'Từ'!");
        }

        double kcTu = raw.getKhoangCachTu() != null ? raw.getKhoangCachTu() : 0.0;
        double kcDen = raw.getKhoangCachDen() != null ? raw.getKhoangCachDen() : 0.0;
        if (kcTu < 0 || kcDen < 0) {
            throw new IllegalArgumentException("Khoảng cách (Km) không được nhỏ hơn 0!");
        }
        if (kcDen < kcTu) {
            throw new IllegalArgumentException("Khoảng cách 'Đến' phải lớn hơn hoặc bằng 'Từ'!");
        }

        String hinhThuc = raw.getHinhThucTinhPhi() != null ? raw.getHinhThucTinhPhi().trim().toUpperCase() : "TINH_PHI";
        boolean isFree = "MIEN_PHI".equals(hinhThuc) || hinhThuc.contains("MIỄN PHÍ") || hinhThuc.contains("MIEN PHI");
        String normalizedType = isFree ? "MIEN_PHI" : "TINH_PHI";

        long phi = isFree ? 0L : (raw.getPhiVanChuyen() != null ? raw.getPhiVanChuyen() : 0L);
        if (phi < 0 || phi > MAX_SHIPPING_FEE) {
            throw new IllegalArgumentException("Phí vận chuyển không hợp lệ!");
        }

        ShippingRuleDTO out = new ShippingRuleDTO();
        out.setId(raw.getId() != null ? raw.getId() : defaultId);
        out.setGiaTriTu(giaTriTu);
        out.setGiaTriDen(giaTriDen);
        out.setKhoangCachTu(Math.round(kcTu * 100.0) / 100.0);
        out.setKhoangCachDen(Math.round(kcDen * 100.0) / 100.0);
        out.setHinhThucTinhPhi(normalizedType);
        out.setPhiVanChuyen(phi);
        out.setTrangThai(raw.getTrangThai() != null && raw.getTrangThai() == 0 ? 0 : 1);
        return out;
    }

    public ShippingConfigDTO validateAndNormalize(
            ShippingConfigDTO input,
            ShippingConfigDTO fallback,
            String staffName
    ) {
        ShippingConfigDTO base = normalizeWithDefaults(fallback);
        if (input == null) {
            return base;
        }
        ShippingConfigDTO out = new ShippingConfigDTO();
        String mode = input.getCheDoTinhPhi() != null ? input.getCheDoTinhPhi().trim().toUpperCase() : base.getCheDoTinhPhi();
        out.setCheDoTinhPhi("GHN_PRIORITY".equals(mode) ? "GHN_PRIORITY" : "REGION_TABLE");

        out.setPhiNoiThanhHaNoi(validateAmount(input.getPhiNoiThanhHaNoi(), base.getPhiNoiThanhHaNoi(), "Phí nội thành Hà Nội"));
        out.setPhiNgoaiThanhHaNoi(validateAmount(input.getPhiNgoaiThanhHaNoi(), base.getPhiNgoaiThanhHaNoi(), "Phí ngoại thành Hà Nội"));
        out.setPhiMienBac(validateAmount(input.getPhiMienBac(), base.getPhiMienBac(), "Phí Miền Bắc"));
        out.setPhiMienTrung(validateAmount(input.getPhiMienTrung(), base.getPhiMienTrung(), "Phí Miền Trung"));
        out.setPhiMienNam(validateAmount(input.getPhiMienNam(), base.getPhiMienNam(), "Phí Miền Nam"));
        out.setPhiMacDinhToanQuoc(validateAmount(input.getPhiMacDinhToanQuoc(), base.getPhiMacDinhToanQuoc(), "Phí mặc định toàn quốc"));

        out.setApDungMienPhiShip(input.getApDungMienPhiShip() != null ? input.getApDungMienPhiShip() : base.getApDungMienPhiShip());
        out.setNguongMienPhiShip(validateAmount(input.getNguongMienPhiShip(), base.getNguongMienPhiShip(), "Ngưỡng miễn phí ship"));
        out.setApDungFreeshipPos(input.getApDungFreeshipPos() != null ? input.getApDungFreeshipPos() : base.getApDungFreeshipPos());
        out.setApDungFreeshipOnline(input.getApDungFreeshipOnline() != null ? input.getApDungFreeshipOnline() : base.getApDungFreeshipOnline());

        if (input.getDanhSachCauHinh() != null) {
            List<ShippingRuleDTO> normalizedRules = new ArrayList<>();
            long seq = 1L;
            for (ShippingRuleDTO r : input.getDanhSachCauHinh()) {
                if (r == null) continue;
                ShippingRuleDTO clean = validateAndNormalizeRule(r, seq);
                normalizedRules.add(clean);
                seq = Math.max(seq + 1L, clean.getId() + 1L);
            }
            out.setDanhSachCauHinh(normalizedRules);
        } else {
            out.setDanhSachCauHinh(base.getDanhSachCauHinh());
        }

        String resolvedStaff = (staffName != null && !staffName.isBlank())
                ? staffName.trim()
                : (input.getNguoiCapNhat() != null && !input.getNguoiCapNhat().isBlank() ? input.getNguoiCapNhat().trim() : "Quản trị viên");
        out.setNguoiCapNhat(resolvedStaff);
        out.setNgayCapNhat(Instant.now().toString());
        return out;
    }

    private Long validateAmount(Long value, Long defaultValue, String fieldLabel) {
        if (value == null) {
            return defaultValue != null ? defaultValue : 0L;
        }
        if (value < 0) {
            throw new IllegalArgumentException(fieldLabel + " không được nhỏ hơn 0!");
        }
        if (value > MAX_SHIPPING_FEE) {
            throw new IllegalArgumentException(fieldLabel + " vượt quá giới hạn cho phép!");
        }
        return value;
    }

    private ShippingConfigDTO normalizeWithDefaults(ShippingConfigDTO raw) {
        ShippingConfigDTO def = createDefaultConfig();
        if (raw == null) return def;
        ShippingConfigDTO out = new ShippingConfigDTO();
        String mode = raw.getCheDoTinhPhi() != null ? raw.getCheDoTinhPhi().trim().toUpperCase() : def.getCheDoTinhPhi();
        out.setCheDoTinhPhi("GHN_PRIORITY".equals(mode) ? "GHN_PRIORITY" : "REGION_TABLE");
        out.setPhiNoiThanhHaNoi(raw.getPhiNoiThanhHaNoi() != null && raw.getPhiNoiThanhHaNoi() >= 0 ? raw.getPhiNoiThanhHaNoi() : def.getPhiNoiThanhHaNoi());
        out.setPhiNgoaiThanhHaNoi(raw.getPhiNgoaiThanhHaNoi() != null && raw.getPhiNgoaiThanhHaNoi() >= 0 ? raw.getPhiNgoaiThanhHaNoi() : def.getPhiNgoaiThanhHaNoi());
        out.setPhiMienBac(raw.getPhiMienBac() != null && raw.getPhiMienBac() >= 0 ? raw.getPhiMienBac() : def.getPhiMienBac());
        out.setPhiMienTrung(raw.getPhiMienTrung() != null && raw.getPhiMienTrung() >= 0 ? raw.getPhiMienTrung() : def.getPhiMienTrung());
        out.setPhiMienNam(raw.getPhiMienNam() != null && raw.getPhiMienNam() >= 0 ? raw.getPhiMienNam() : def.getPhiMienNam());
        out.setPhiMacDinhToanQuoc(raw.getPhiMacDinhToanQuoc() != null && raw.getPhiMacDinhToanQuoc() >= 0 ? raw.getPhiMacDinhToanQuoc() : def.getPhiMacDinhToanQuoc());
        out.setApDungMienPhiShip(raw.getApDungMienPhiShip() != null ? raw.getApDungMienPhiShip() : def.getApDungMienPhiShip());
        out.setNguongMienPhiShip(raw.getNguongMienPhiShip() != null && raw.getNguongMienPhiShip() >= 0 ? raw.getNguongMienPhiShip() : def.getNguongMienPhiShip());
        out.setApDungFreeshipPos(raw.getApDungFreeshipPos() != null ? raw.getApDungFreeshipPos() : def.getApDungFreeshipPos());
        out.setApDungFreeshipOnline(raw.getApDungFreeshipOnline() != null ? raw.getApDungFreeshipOnline() : def.getApDungFreeshipOnline());

        if (raw.getDanhSachCauHinh() != null) {
            List<ShippingRuleDTO> rules = new ArrayList<>();
            long seq = 1L;
            for (ShippingRuleDTO r : raw.getDanhSachCauHinh()) {
                if (r == null) continue;
                try {
                    ShippingRuleDTO clean = validateAndNormalizeRule(r, seq);
                    rules.add(clean);
                    seq = Math.max(seq + 1L, clean.getId() + 1L);
                } catch (Exception ignored) {}
            }
            out.setDanhSachCauHinh(rules);
        } else {
            out.setDanhSachCauHinh(def.getDanhSachCauHinh());
        }

        out.setNguoiCapNhat(raw.getNguoiCapNhat() != null ? raw.getNguoiCapNhat() : def.getNguoiCapNhat());
        out.setNgayCapNhat(raw.getNgayCapNhat() != null ? raw.getNgayCapNhat() : def.getNgayCapNhat());
        return out;
    }

    /**
     * Ước tính khoảng cách (Km) từ cửa hàng (Nam Từ Liêm, Hà Nội) tới địa chỉ nhận
     */
    public double estimateDistanceKm(
            String provinceName,
            String districtName,
            String wardName,
            String fullAddress
    ) {
        String p = cleanVietnamese(provinceName);
        String d = cleanVietnamese(districtName);
        String w = cleanVietnamese(wardName);
        String f = cleanVietnamese(fullAddress);
        String combined = (p + " " + d + " " + w + " " + f).trim();

        if (combined.isEmpty()) return 0.0;

        if (p.contains("ha noi") || p.contains("hanoi") || combined.contains("ha noi") || combined.contains("hanoi")) {
            if (NEAR_SHOP_KEYWORDS.stream().anyMatch(combined::contains)) {
                return 0.8; // Trong bán kính 0 - 1 Km quanh cửa hàng
            }
            boolean isOuter = OUTER_HANOI_DISTRICTS.stream().anyMatch(combined::contains);
            return isOuter ? 18.0 : 5.0;
        }
        if (NORTH_PROVINCES.stream().anyMatch(combined::contains)) {
            return 80.0;
        }
        if (CENTRAL_PROVINCES.stream().anyMatch(combined::contains)) {
            return 500.0;
        }
        return 1200.0;
    }

    /**
     * Tìm cấu hình phí vận chuyển theo bậc Giá trị hóa đơn + Khoảng cách (Km)
     */
    public ShippingRuleDTO findMatchingRule(ShippingConfigDTO config, Long orderSubtotal, Double distanceKm) {
        if (config == null || config.getDanhSachCauHinh() == null || config.getDanhSachCauHinh().isEmpty()) {
            return null;
        }
        long subtotal = orderSubtotal != null && orderSubtotal >= 0 ? orderSubtotal : 0L;
        double dist = distanceKm != null && distanceKm >= 0 ? distanceKm : 0.0;

        for (ShippingRuleDTO rule : config.getDanhSachCauHinh()) {
            if (rule == null) continue;
            if (rule.getTrangThai() != null && rule.getTrangThai() == 0) continue;
            long minVal = rule.getGiaTriTu() != null ? rule.getGiaTriTu() : 0L;
            long maxVal = rule.getGiaTriDen() != null ? rule.getGiaTriDen() : Long.MAX_VALUE;
            double minKm = rule.getKhoangCachTu() != null ? rule.getKhoangCachTu() : 0.0;
            double maxKm = rule.getKhoangCachDen() != null ? rule.getKhoangCachDen() : Double.MAX_VALUE;

            if (subtotal >= minVal && subtotal <= maxVal && dist >= minKm && dist <= maxKm) {
                return rule;
            }
        }
        return null;
    }

    public Map<String, Object> evaluateRegionalFee(
            ShippingConfigDTO config,
            String provinceName,
            String districtName,
            String wardName,
            String fullAddress
    ) {
        ShippingConfigDTO cfg = normalizeWithDefaults(config);
        String p = cleanVietnamese(provinceName);
        String d = cleanVietnamese(districtName);
        String w = cleanVietnamese(wardName);
        String f = cleanVietnamese(fullAddress);
        String combined = (p + " " + d + " " + w + " " + f).trim();

        if (combined.isEmpty()) {
            return Map.of(
                    "fee", 0L,
                    "regionCode", "NONE",
                    "regionDescription", "Chưa chọn địa chỉ giao hàng",
                    "leadTime", "",
                    "source", "NONE"
            );
        }

        if (p.contains("ha noi") || p.contains("hanoi") || combined.contains("ha noi") || combined.contains("hanoi")) {
            boolean isOuter = OUTER_HANOI_DISTRICTS.stream().anyMatch(combined::contains);
            long fee = isOuter ? cfg.getPhiNgoaiThanhHaNoi() : cfg.getPhiNoiThanhHaNoi();
            return Map.of(
                    "fee", fee,
                    "regionCode", isOuter ? "HANOI_OUTER" : "HANOI_INNER",
                    "regionDescription", isOuter ? "Ngoại thành Hà Nội (Tiêu chuẩn 1 ngày)" : "Nội thành Hà Nội (Giao nhanh giá rẻ)",
                    "leadTime", isOuter ? "Tiêu chuẩn 1 ngày" : "Giao nhanh 1 ngày",
                    "source", "REGIONAL_POLICY"
            );
        }

        if (NORTH_PROVINCES.stream().anyMatch(combined::contains)) {
            return Map.of(
                    "fee", cfg.getPhiMienBac(),
                    "regionCode", "NORTH",
                    "regionDescription", "Miền Bắc lân cận (1-2 ngày)",
                    "leadTime", "Dự kiến 1 - 2 ngày",
                    "source", "REGIONAL_POLICY"
            );
        }

        if (CENTRAL_PROVINCES.stream().anyMatch(combined::contains)) {
            return Map.of(
                    "fee", cfg.getPhiMienTrung(),
                    "regionCode", "CENTRAL",
                    "regionDescription", "Miền Trung (2-3 ngày)",
                    "leadTime", "Dự kiến 2 - 3 ngày",
                    "source", "REGIONAL_POLICY"
            );
        }

        if (SOUTH_PROVINCES.stream().anyMatch(combined::contains)) {
            return Map.of(
                    "fee", cfg.getPhiMienNam(),
                    "regionCode", "SOUTH",
                    "regionDescription", "Miền Nam (3-4 ngày)",
                    "leadTime", "Dự kiến 3 - 4 ngày",
                    "source", "REGIONAL_POLICY"
            );
        }

        return Map.of(
                "fee", cfg.getPhiMacDinhToanQuoc(),
                "regionCode", "NATIONWIDE",
                "regionDescription", "Toàn quốc (3-4 ngày)",
                "leadTime", "Dự kiến 3 - 4 ngày",
                "source", "REGIONAL_POLICY"
        );
    }

    public boolean isFreeshipEligible(ShippingConfigDTO cfg, Long orderSubtotal, String channel) {
        if (cfg == null || !Boolean.TRUE.equals(cfg.getApDungMienPhiShip())) {
            return false;
        }
        if (orderSubtotal == null || orderSubtotal <= 0) {
            return false;
        }
        long threshold = cfg.getNguongMienPhiShip() != null ? cfg.getNguongMienPhiShip() : 0L;
        if (orderSubtotal < threshold) {
            return false;
        }
        String ch = channel != null ? channel.trim().toUpperCase() : "ONLINE";
        if ("POS".equals(ch)) {
            return Boolean.TRUE.equals(cfg.getApDungFreeshipPos());
        }
        return Boolean.TRUE.equals(cfg.getApDungFreeshipOnline());
    }

    public String cleanVietnamese(String str) {
        if (str == null) return "";
        String normalized = Normalizer.normalize(str.trim().toLowerCase(), Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
