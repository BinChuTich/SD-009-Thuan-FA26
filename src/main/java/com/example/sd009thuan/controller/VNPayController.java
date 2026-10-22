package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.VNPayDTO;
import com.example.sd009thuan.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vnpay")
@CrossOrigin(origins = "*")
public class VNPayController {

    @Autowired
    private VNPayService vnPayService;

    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(HttpServletRequest request, @RequestBody VNPayDTO.PaymentRequest req) {
        if (req.getOrderCode() == null || req.getOrderCode().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã đơn hàng không được để trống!"));
        }
        if (req.getAmount() == null || req.getAmount() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số tiền thanh toán phải lớn hơn 0!"));
        }

        String paymentUrl = vnPayService.createPaymentUrl(
                request,
                req.getOrderCode().trim(),
                req.getAmount(),
                req.getOrderInfo()
        );

        return ResponseEntity.ok(new VNPayDTO.PaymentResponse("00", "Tạo liên kết thanh toán VNPAY thành công", paymentUrl));
    }

    @GetMapping("/payment-return")
    public ResponseEntity<?> paymentReturn(@RequestParam Map<String, String> queryParams) {
        Map<String, Object> result = vnPayService.verifyPayment(queryParams);
        return ResponseEntity.ok(result);
    }
}
