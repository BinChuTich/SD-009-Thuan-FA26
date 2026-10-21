package com.example.sd009thuan.controller;

import com.example.sd009thuan.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lien-he")
@CrossOrigin(origins = "*")
public class LienHeController {

    @Autowired
    private EmailService emailService;

    public record LienHeRequest(
            String name,
            String phone,
            String email,
            String message
    ) {}

    @PostMapping("/gui-tin-nhan")
    public ResponseEntity<?> guiTinNhan(@RequestBody LienHeRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập họ và tên!"));
        }
        if (req.phone() == null || req.phone().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập số điện thoại!"));
        }
        String cleanPhone = req.phone().replaceAll("[\\s.-]+", "");
        if (!cleanPhone.matches("^(03|05|07|08|09)\\d{8}$")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại không hợp lệ! Vui lòng nhập số điện thoại 10 chữ số (bắt đầu bằng 03, 05, 07, 08 hoặc 09)."));
        }
        if (req.message() == null || req.message().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập nội dung cần hỗ trợ!"));
        }

        emailService.sendContactEmail(req.name(), cleanPhone, req.email(), req.message());
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã gửi thông tin liên hệ thành công tới fftshirt.vn@gmail.com"));
    }
}

