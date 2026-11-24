package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.payment.ConfirmPaymentRequest;
import com.example.sd009thuan.dto.payment.PaymentInfoRequest;
import com.example.sd009thuan.dto.payment.PaymentInfoResponse;
import com.example.sd009thuan.dto.payment.PaymentStatusResponse;
import com.example.sd009thuan.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${sepay.webhook-api-key:}")
    private String sePayWebhookApiKey;

    @Value("${payment.simulation.enabled:false}")
    private boolean paymentSimulationEnabled;

    @Value("${payment.demo-local.enabled:true}")
    private boolean localDemoPaymentEnabled;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Lấy thông tin tài khoản chuyển khoản và link mã VietQR động
     */
    @PostMapping("/transfer-info")
    public ResponseEntity<PaymentInfoResponse> getTransferInfoPost(@RequestBody PaymentInfoRequest request) {
        return ResponseEntity.ok(paymentService.generateTransferInfo(request));
    }

    /**
     * Lấy thông tin tài khoản chuyển khoản và link mã VietQR động qua GET params
     */
    @GetMapping("/transfer-info")
    public ResponseEntity<PaymentInfoResponse> getTransferInfoGet(
            @RequestParam(name = "maHoaDon", required = false, defaultValue = "POS") String maHoaDon,
            @RequestParam(name = "soTien", required = false, defaultValue = "0") BigDecimal soTien
    ) {
        PaymentInfoRequest req = new PaymentInfoRequest();
        req.setMaHoaDon(maHoaDon);
        req.setSoTien(soTien);
        return ResponseEntity.ok(paymentService.generateTransferInfo(req));
    }

    /**
     * Kiểm tra trạng thái thanh toán của đơn hàng (Polling từ frontend)
     */
    @GetMapping("/check-status/{maHoaDon}")
    public ResponseEntity<PaymentStatusResponse> checkPaymentStatus(@PathVariable String maHoaDon) {
        return ResponseEntity.ok(paymentService.checkPaymentStatus(maHoaDon));
    }

    /**
     * Xác nhận thanh toán chuyển khoản (Thủ công từ thu ngân hoặc từ webhook ngân hàng)
     */
    @PostMapping("/confirm-transfer")
    public ResponseEntity<PaymentStatusResponse> confirmPayment(@RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(paymentService.confirmPayment(request));
    }

    /**
     * Giả lập nhận được tiền chuyển khoản thành công từ ngân hàng (Hỗ trợ demo/test)
     */
    @PostMapping("/simulate-transfer")
    public ResponseEntity<PaymentStatusResponse> simulateTransfer(
            @RequestParam(name = "maHoaDon") String maHoaDon,
            @RequestParam(name = "soTien", required = false, defaultValue = "0") BigDecimal soTien
    ) {
        if (!paymentSimulationEnabled) {
            return ResponseEntity.notFound().build();
        }
        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setMaHoaDon(maHoaDon);
        req.setSoTien(soTien);
        req.setGhiChu("Mô phỏng thanh toán chuyển khoản thành công");
        req.setPhuongThuc("Chuyển khoản");
        return ResponseEntity.ok(paymentService.confirmPayment(req));
    }

    @PostMapping("/demo/create")
    public ResponseEntity<java.util.Map<String, Object>> createDemoPayment(
            @RequestParam String maHoaDon,
            @RequestParam BigDecimal soTien
    ) {
        if (!localDemoPaymentEnabled) return ResponseEntity.notFound().build();
        String token = paymentService.createDemoPaymentSession(maHoaDon, soTien);
        return ResponseEntity.ok(java.util.Map.of(
                "token", token,
                "maHoaDon", maHoaDon,
                "soTien", soTien,
                "expiresIn", 300
        ));
    }

    @PostMapping("/demo/confirm")
    public ResponseEntity<PaymentStatusResponse> confirmDemoPayment(@RequestParam String token) {
        if (!localDemoPaymentEnabled) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(paymentService.confirmDemoPayment(token));
    }

    /**
     * Tạo link thanh toán qua cổng VNPAY (theo chuẩn vnpay_jsp)
     */
    @GetMapping("/vnpay/create-url")
    public ResponseEntity<java.util.Map<String, String>> createVNPayUrl(
            @RequestParam(name = "maHoaDon") String maHoaDon,
            @RequestParam(name = "soTien", required = false, defaultValue = "0") BigDecimal soTien,
            @RequestParam(name = "bankCode", required = false, defaultValue = "") String bankCode,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        String ip = request.getRemoteAddr();
        String url = paymentService.createVNPayPaymentUrl(maHoaDon, soTien, bankCode, ip);
        return ResponseEntity.ok(java.util.Map.of("url", url));
    }

    /**
     * Xử lý kết quả trả về từ VNPAY (return URL)
     */
    @GetMapping("/vnpay/payment-return")
    public ResponseEntity<PaymentStatusResponse> handleVNPayReturn(@RequestParam java.util.Map<String, String> allParams) {
        return ResponseEntity.ok(paymentService.processVNPayIpn(allParams));
    }

    /**
     * Webhook nhận thông báo biến động số dư / thanh toán từ ngân hàng (Casso, SePay, PayOS, Ngân hàng...)
     */
    @PostMapping({"/webhook", "/webhook/sepay"})
    public ResponseEntity<java.util.Map<String, Object>> handleBankWebhook(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody java.util.Map<String, Object> payload
    ) {
        if (sePayWebhookApiKey == null || sePayWebhookApiKey.isBlank()) {
            return ResponseEntity.status(503).body(java.util.Map.of(
                    "success", false,
                    "message", "Webhook chưa được cấu hình API key"
            ));
        }

        String expected = "Apikey " + sePayWebhookApiKey.trim();
        if (authorization == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                authorization.getBytes(StandardCharsets.UTF_8)
        )) {
            return ResponseEntity.status(401).body(java.util.Map.of(
                    "success", false,
                    "message", "Webhook không hợp lệ"
            ));
        }

        try {
            paymentService.processBankWebhook(payload);
            // SePay yêu cầu chính xác body này để ghi nhận webhook thành công.
            return ResponseEntity.ok(java.util.Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Lỗi xử lý webhook"
            ));
        }
    }

    /**
     * IPN callback từ cổng VNPAY
     */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<java.util.Map<String, String>> handleVNPayIpn(@RequestParam java.util.Map<String, String> allParams) {
        try {
            paymentService.processVNPayIpn(allParams);
            return ResponseEntity.ok(java.util.Map.of("RspCode", "00", "Message", "Confirm Success"));
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of("RspCode", "99", "Message", e.getMessage() != null ? e.getMessage() : "Fail"));
        }
    }

    /**
     * Reset trạng thái thanh toán chuyển khoản
     */
    @PostMapping("/reset-status/{maHoaDon}")
    public ResponseEntity<Void> resetPaymentStatus(@PathVariable String maHoaDon) {
        paymentService.resetPaymentStatus(maHoaDon);
        return ResponseEntity.ok().build();
    }
}
