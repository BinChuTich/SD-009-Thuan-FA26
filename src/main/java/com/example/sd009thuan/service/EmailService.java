package com.example.sd009thuan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendAccountInfoEmail(String toEmail, String fullName, String employeeCode, String username, String password) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Không thể gửi email vì địa chỉ email trống.");
            return false;
        }

        String subject = "[FF-Tshirt] Thông tin tài khoản nhân viên mới - " + employeeCode;
        String content = String.format(
                "Xin chào %s,\n\n" +
                "Tài khoản nhân viên của bạn tại hệ thống FF-Tshirt đã được tạo thành công:\n" +
                "- Mã nhân viên: %s\n" +
                "- Tên đăng nhập: %s\n" +
                "- Mật khẩu: %s\n\n" +
                "Vui lòng bảo mật thông tin tài khoản này và không chia sẻ cho người khác.\n\n" +
                "Trân trọng,\n" +
                "Hệ thống quản lý FF-Tshirt",
                fullName, employeeCode, username, password
        );

        if (mailSender == null) {
            log.warn("JavaMailSender chưa được cấu hình. Chi tiết tài khoản nhân viên:\n{}", content);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail.trim());
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("Đã gửi email thông tin tài khoản thành công tới: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Gửi email thất bại cho {}: {}. Chi tiết tài khoản:\n{}", toEmail, e.getMessage(), content);
            return false;
        }
    }
}
