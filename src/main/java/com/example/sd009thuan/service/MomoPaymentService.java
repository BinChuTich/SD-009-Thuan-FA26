package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.HinhThucThanhToan;
import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.entity.LichSuHoaDon;
import com.example.sd009thuan.entity.PhuongThucThanhToan;
import com.example.sd009thuan.repository.HinhThucThanhToanRepository;
import com.example.sd009thuan.repository.HoaDonRepository;
import com.example.sd009thuan.repository.LichSuHoaDonRepository;
import com.example.sd009thuan.repository.PhuongThucThanhToanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MomoPaymentService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final HoaDonRepository hoaDonRepository;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    private final HinhThucThanhToanRepository hinhThucThanhToanRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;

    @Value("${momo.mode:mock}")
    private String mode;

    @Value("${momo.mock-secret:fft-shirt-momo-mock-only-change-before-production}")
    private String mockSecret;

    @Value("${momo.client-base-url:http://localhost:5174}")
    private String clientBaseUrl;

    public MomoPaymentService(
            HoaDonRepository hoaDonRepository,
            PhuongThucThanhToanRepository phuongThucThanhToanRepository,
            HinhThucThanhToanRepository hinhThucThanhToanRepository,
            LichSuHoaDonRepository lichSuHoaDonRepository
    ) {
        this.hoaDonRepository = hoaDonRepository;
        this.phuongThucThanhToanRepository = phuongThucThanhToanRepository;
        this.hinhThucThanhToanRepository = hinhThucThanhToanRepository;
        this.lichSuHoaDonRepository = lichSuHoaDonRepository;
    }

    public Map<String, Object> getConfiguration() {
        return Map.of(
                "mode", normalizedMode(),
                "mock", isMockMode(),
                "displayName", isMockMode() ? "MoMo - Chế độ thử nghiệm" : "MoMo Sandbox"
        );
    }

    public Map<String, Object> createPayment(String orderCode) {
        ensureMockMode();
        HoaDon order = findOrder(orderCode);
        String selectedNote = order.getGhiChu() != null ? order.getGhiChu().toUpperCase(Locale.ROOT) : "";
        if (!selectedNote.contains("[PHƯƠNG THỨC: MOMO]")) {
            throw new IllegalStateException("Đơn hàng hiện không chọn phương thức MoMo!");
        }
        if (Integer.valueOf(1).equals(order.getTrangThaiThanhToan())) {
            throw new IllegalStateException("Đơn hàng đã được thanh toán trước đó!");
        }
        if (Integer.valueOf(6).equals(order.getTrangThai())) {
            throw new IllegalStateException("Đơn hàng đã hủy, không thể tạo giao dịch MoMo!");
        }

        long amount = resolveAmount(order).longValue();
        if (amount <= 0) {
            throw new IllegalStateException("Số tiền thanh toán MoMo không hợp lệ!");
        }

        long expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES).toEpochMilli();
        String token = sign(order.getMaHoaDon(), amount, expiresAt);
        String paymentUrl = clientBaseUrl.replaceAll("/+$", "")
                + "/thanh-toan/momo-test?orderId=" + encode(order.getMaHoaDon())
                + "&amount=" + amount
                + "&expiresAt=" + expiresAt
                + "&token=" + encode(token);

        return Map.of(
                "mode", "mock",
                "orderId", order.getMaHoaDon(),
                "amount", amount,
                "expiresAt", expiresAt,
                "paymentUrl", paymentUrl
        );
    }

    @Transactional
    public Map<String, Object> processMockResult(Map<String, Object> body) {
        ensureMockMode();
        String orderCode = stringValue(body.get("orderId"));
        String result = stringValue(body.get("result")).toUpperCase(Locale.ROOT);
        String token = stringValue(body.get("token"));
        long expiresAt = longValue(body.get("expiresAt"));

        HoaDon order = findOrder(orderCode);
        String selectedNote = order.getGhiChu() != null ? order.getGhiChu().toUpperCase(Locale.ROOT) : "";
        if (!selectedNote.contains("[PHƯƠNG THỨC: MOMO]")) {
            throw new IllegalStateException("Phiên MoMo đã bị vô hiệu hóa vì khách hàng đã đổi phương thức thanh toán!");
        }
        long expectedAmount = resolveAmount(order).longValue();
        verifyToken(order.getMaHoaDon(), expectedAmount, expiresAt, token);

        if (!List.of("SUCCESS", "FAILED", "CANCELLED").contains(result)) {
            throw new IllegalArgumentException("Kết quả mô phỏng MoMo không hợp lệ!");
        }

        if (!"SUCCESS".equals(result)) {
            saveHistory(order, "CANCELLED".equals(result)
                    ? "Khách hàng đã hủy giao dịch MoMo thử nghiệm"
                    : "Giao dịch MoMo thử nghiệm thất bại");
            return Map.of(
                    "success", false,
                    "result", result,
                    "orderId", order.getMaHoaDon(),
                    "paid", false,
                    "message", "CANCELLED".equals(result) ? "Bạn đã hủy giao dịch thử nghiệm." : "Giao dịch thử nghiệm thất bại."
            );
        }

        if (!Integer.valueOf(1).equals(order.getTrangThaiThanhToan())) {
            order.setTrangThaiThanhToan(1);
            order.setNgayCapNhat(Instant.now());
            order.setNguoiCapNhat("Khách hàng online");
            hoaDonRepository.save(order);

            List<HinhThucThanhToan> oldPayments = hinhThucThanhToanRepository.findByIdHoaDon_Id(order.getId());
            if (oldPayments != null && !oldPayments.isEmpty()) {
                hinhThucThanhToanRepository.deleteAll(oldPayments);
            }

            HinhThucThanhToan payment = new HinhThucThanhToan();
            payment.setIdHoaDon(order);
            payment.setIdPhuongThuc(findOrCreateMomoMethod());
            payment.setSoTien(BigDecimal.valueOf(expectedAmount));
            payment.setMaGiaoDich("MOMO-MOCK-" + System.currentTimeMillis());
            payment.setThoiGian(Instant.now());
            payment.setTrangThai(1);
            hinhThucThanhToanRepository.save(payment);
            saveHistory(order, "Thanh toán MoMo thử nghiệm thành công - Mã giao dịch: " + payment.getMaGiaoDich());
        }

        return Map.of(
                "success", true,
                "result", "SUCCESS",
                "orderId", order.getMaHoaDon(),
                "amount", expectedAmount,
                "paid", true,
                "message", "Thanh toán MoMo thử nghiệm thành công!"
        );
    }

    private HoaDon findOrder(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            throw new IllegalArgumentException("Mã đơn hàng không được để trống!");
        }
        return hoaDonRepository.findByMaHoaDon(orderCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng " + orderCode));
    }

    private BigDecimal resolveAmount(HoaDon order) {
        BigDecimal amount = order.getTienSauGiamGia() != null ? order.getTienSauGiamGia() : order.getTongTien();
        return amount != null ? amount.max(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    private synchronized PhuongThucThanhToan findOrCreateMomoMethod() {
        return phuongThucThanhToanRepository.findAll().stream()
                .filter(item -> item.getTenPhuongThuc() != null && item.getTenPhuongThuc().toLowerCase().contains("momo"))
                .findFirst()
                .orElseGet(() -> {
                    PhuongThucThanhToan method = new PhuongThucThanhToan();
                    method.setTenPhuongThuc("Ví MoMo");
                    method.setTrangThai(1);
                    return phuongThucThanhToanRepository.save(method);
                });
    }

    private void saveHistory(HoaDon order, String note) {
        LichSuHoaDon history = new LichSuHoaDon();
        history.setIdHoaDon(order);
        history.setTrangThai(order.getTrangThai());
        history.setThoiGian(Instant.now());
        history.setHanhDong(note);
        history.setGhiChu(note);
        history.setNguoiThucHien("Khách hàng online");
        history.setVaiTroNguoiThucHien("Khách hàng");
        lichSuHoaDonRepository.save(history);
    }

    private void verifyToken(String orderCode, long amount, long expiresAt, String token) {
        if (expiresAt < System.currentTimeMillis()) {
            throw new IllegalStateException("Giao dịch MoMo thử nghiệm đã hết hạn!");
        }
        byte[] expected = sign(orderCode, amount, expiresAt).getBytes(StandardCharsets.UTF_8);
        byte[] actual = token.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new IllegalArgumentException("Chữ ký giao dịch MoMo thử nghiệm không hợp lệ!");
        }
    }

    private String sign(String orderCode, long amount, long expiresAt) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(mockSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal((orderCode + "|" + amount + "|" + expiresAt).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký MoMo thử nghiệm", e);
        }
    }

    private void ensureMockMode() {
        if (!isMockMode()) {
            throw new IllegalStateException("MoMo Sandbox thật chưa được cấu hình khóa tích hợp!");
        }
    }

    private boolean isMockMode() {
        return "mock".equals(normalizedMode());
    }

    private String normalizedMode() {
        return mode == null ? "mock" : mode.trim().toLowerCase(Locale.ROOT);
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private long longValue(Object value) {
        try {
            return Long.parseLong(stringValue(value));
        } catch (Exception e) {
            throw new IllegalArgumentException("Thời hạn giao dịch MoMo không hợp lệ!");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
