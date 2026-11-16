package com.example.sd009thuan.controller;

import com.example.sd009thuan.service.MomoPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/momo")
@CrossOrigin(origins = "*")
public class MomoPaymentController {
    private final MomoPaymentService momoPaymentService;

    public MomoPaymentController(MomoPaymentService momoPaymentService) {
        this.momoPaymentService = momoPaymentService;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config() {
        return ResponseEntity.ok(momoPaymentService.getConfiguration());
    }

    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(momoPaymentService.createPayment(String.valueOf(body.getOrDefault("orderId", ""))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", errorMessage(e)));
        }
    }

    @PostMapping("/mock/result")
    public ResponseEntity<?> processMockResult(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(momoPaymentService.processMockResult(body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", errorMessage(e)));
        }
    }

    private String errorMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Không thể xử lý giao dịch MoMo thử nghiệm!";
    }
}
