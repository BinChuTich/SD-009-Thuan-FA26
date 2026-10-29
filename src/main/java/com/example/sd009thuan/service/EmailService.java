package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.ChiTietHoaDon;
import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.entity.KhachHang;
import com.example.sd009thuan.entity.PhieuGiamGia;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:thuanminh2908200334.1@gmail.com}")
    private String senderEmail;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 đ";
        return String.format("%,d đ", amount.longValue()).replace(',', '.');
    }

    private String formatGiaTriGiam(PhieuGiamGia phieu) {
        if (phieu.getLoaiPhieuGiamGia() == 1) {
            return phieu.getGiaTriGiamGia() + "%";
        } else {
            return formatMoney(phieu.getGiaTriGiamGia());
        }
    }

    private String buildGiamToiDaHtml(PhieuGiamGia phieu) {
        if (phieu.getLoaiPhieuGiamGia() == 1 && phieu.getGiamToiDa() != null) {
            return """
                <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                    <span style="font-weight: 700; color: #4a3427;">Giảm tối đa:</span>
                    <span style="color: #b91c1c; font-weight: 600;">%s</span>
                </div>
            """.formatted(formatMoney(phieu.getGiamToiDa()));
        }
        return "";
    }

    private String formatTime(Instant instant) {
        if (instant == null) return "Không giới hạn";
        return DATE_FORMATTER.format(instant);
    }

    @Async
    public void sendVoucherIssuedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            log.info("==> [EmailService] Bỏ qua gửi email phát hành voucher vì khách hàng không có email hợp lệ.");
            return;
        }
        try {
            if (mailSender == null) {
                log.info("==> [EmailService Mock] Đã gửi email phát hành voucher {} tới {}", phieu.getMaPhieuGiamGia(), kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(kh.getEmail().trim());
            helper.setSubject("[FF T-Shirt] Bạn vừa nhận được Phiếu giảm giá: " + phieu.getMaPhieuGiamGia());

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                </head>
                <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <div style="max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <h2 style="margin: 0 0 14px 0; text-align: center; color: #4a3427; font-size: 20px; font-weight: 700;">
                        Chào %s,
                    </h2>
                    <p style="margin: 0 0 24px 0; text-align: center; color: #555555; font-size: 14px; line-height: 1.6;">
                        Cảm ơn bạn đã luôn đồng hành cùng <b>FF T-Shirt</b>. Chúng tôi xin dành tặng riêng bạn một phần quà đặc biệt:
                    </p>

                    <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #8c6d58; border-radius: 8px; padding: 20px 24px; margin: 0 0 24px 0; text-align: left;">
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Voucher:</span> %s
                        </div>
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427; margin-right: 8px;">Mã sử dụng:</span>
                            <span style="display: inline-block; background-color: #f1ebe5; color: #4a3427; font-weight: 700; font-size: 14px; padding: 3px 12px; border-radius: 4px; letter-spacing: 0.5px;">%s</span>
                        </div>
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Giá trị giảm:</span> <span style="color: #b91c1c; font-weight: 700;">%s</span>
                        </div>
                        %s
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Áp dụng cho đơn từ:</span> %s
                        </div>
                        <div style="font-size: 14px; color: #333333; line-height: 1.5;">
                            <span style="font-weight: 700; color: #4a3427;">Thời gian áp dụng:</span> Từ %s đến hết ngày %s
                        </div>
                    </div>

                    <p style="margin: 0 0 24px 0; text-align: center; color: #666666; font-size: 13.5px; line-height: 1.5;">
                        Hãy nhanh tay sử dụng mã giảm giá này cho lần mua sắm tiếp theo nhé!
                    </p>

                    <div style="text-align: center; margin: 0 0 10px 0;">
                        <a href="http://localhost:5173" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 11px 34px; border-radius: 24px; box-shadow: 0 2px 6px rgba(123,89,67,0.25);">
                            Mua sắm ngay
                        </a>
                    </div>
                </div>
                </body>
                </html>
            """.formatted(
                    kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "Khách hàng",
                    phieu.getTenPhieuGiamGia(),
                    phieu.getMaPhieuGiamGia(),
                    formatGiaTriGiam(phieu),
                    buildGiamToiDaHtml(phieu),
                    formatMoney(phieu.getHoaDonToiThieu()),
                    formatTime(phieu.getNgayBatDau()),
                    formatTime(phieu.getNgayKetThuc())
            );

            helper.setText(html, true);
            log.info("==> [EmailService] Đang gửi email phát hành voucher tới: {}", kh.getEmail());
            mailSender.send(message);
            log.info("==> [EmailService] Đã gửi email phát hành voucher THÀNH CÔNG tới: {}", kh.getEmail());
        } catch (Exception e) {
            log.error("==> [EmailService Lỗi] Không thể gửi email phát hành tới {}: {}", kh.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendVoucherUpdatedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            log.info("==> [EmailService] Bỏ qua gửi email cập nhật voucher vì khách hàng không có email hợp lệ.");
            return;
        }
        try {
            if (mailSender == null) {
                log.info("==> [EmailService Mock] Đã gửi email cập nhật voucher {} tới {}", phieu.getMaPhieuGiamGia(), kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(kh.getEmail().trim());
            helper.setSubject("[FF T-Shirt] Cập nhật thông tin ưu đãi Phiếu giảm giá: " + phieu.getMaPhieuGiamGia());

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                </head>
                <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <div style="max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <h2 style="margin: 0 0 14px 0; text-align: center; color: #4a3427; font-size: 20px; font-weight: 700;">
                        Chào %s,
                    </h2>
                    <p style="margin: 0 0 24px 0; text-align: center; color: #555555; font-size: 14px; line-height: 1.6;">
                        Phiếu giảm giá của bạn tại <b>FF T-Shirt</b> vừa được cập nhật thêm các ưu đãi và thời hạn mới:
                    </p>

                    <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #8c6d58; border-radius: 8px; padding: 20px 24px; margin: 0 0 24px 0; text-align: left;">
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Voucher:</span> %s
                        </div>
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427; margin-right: 8px;">Mã sử dụng:</span>
                            <span style="display: inline-block; background-color: #f1ebe5; color: #4a3427; font-weight: 700; font-size: 14px; padding: 3px 12px; border-radius: 4px; letter-spacing: 0.5px;">%s</span>
                        </div>
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Giá trị giảm:</span> <span style="color: #b91c1c; font-weight: 700;">%s</span>
                        </div>
                        %s
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Áp dụng cho đơn từ:</span> %s
                        </div>
                        <div style="font-size: 14px; color: #333333; line-height: 1.5;">
                            <span style="font-weight: 700; color: #4a3427;">Thời gian áp dụng:</span> Từ %s đến hết ngày %s
                        </div>
                    </div>

                    <p style="margin: 0 0 24px 0; text-align: center; color: #666666; font-size: 13.5px; line-height: 1.5;">
                        Hãy nhanh tay sử dụng mã giảm giá này cho lần mua sắm tiếp theo nhé!
                    </p>

                    <div style="text-align: center; margin: 0 0 10px 0;">
                        <a href="http://localhost:5173" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 11px 34px; border-radius: 24px; box-shadow: 0 2px 6px rgba(123,89,67,0.25);">
                            Mua sắm ngay
                        </a>
                    </div>
                </div>
                </body>
                </html>
            """.formatted(
                    kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "Khách hàng",
                    phieu.getTenPhieuGiamGia(),
                    phieu.getMaPhieuGiamGia(),
                    formatGiaTriGiam(phieu),
                    buildGiamToiDaHtml(phieu),
                    formatMoney(phieu.getHoaDonToiThieu()),
                    formatTime(phieu.getNgayBatDau()),
                    formatTime(phieu.getNgayKetThuc())
            );

            helper.setText(html, true);
            log.info("==> [EmailService] Đang gửi email CẬP NHẬT voucher tới: {}", kh.getEmail());
            mailSender.send(message);
            log.info("==> [EmailService] Đã gửi email cập nhật voucher THÀNH CÔNG tới: {}", kh.getEmail());
        } catch (Exception e) {
            log.error("==> [EmailService Lỗi] Không thể gửi email cập nhật tới {}: {}", kh.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendVoucherRevokedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            log.info("==> [EmailService] Bỏ qua gửi email thu hồi voucher vì khách hàng không có email hợp lệ.");
            return;
        }
        try {
            if (mailSender == null) {
                log.info("==> [EmailService Mock] Đã gửi email thu hồi voucher {} tới {}", phieu.getMaPhieuGiamGia(), kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(kh.getEmail().trim());
            helper.setSubject("[FF T-Shirt] Thông báo kết thúc áp dụng Phiếu giảm giá: " + phieu.getMaPhieuGiamGia());

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                </head>
                <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <div style="max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <h2 style="margin: 0 0 14px 0; text-align: center; color: #4a3427; font-size: 20px; font-weight: 700;">
                        Chào %s,
                    </h2>
                    <p style="margin: 0 0 24px 0; text-align: center; color: #555555; font-size: 14px; line-height: 1.6;">
                        Hệ thống xin thông báo: Phiếu giảm giá <b>%s</b> (%s) đã kết thúc áp dụng cho tài khoản của bạn theo chính sách cập nhật của chương trình.
                    </p>
                    <p style="margin: 0 0 24px 0; text-align: center; color: #666666; font-size: 13.5px; line-height: 1.5;">
                        Chúng tôi sẽ sớm gửi tới bạn các chương trình ưu đãi hấp dẫn khác trong thời gian tới. Cảm ơn bạn đã luôn đồng hành cùng <b>FF T-Shirt</b>!
                    </p>
                    <div style="text-align: center; margin: 0 0 10px 0;">
                        <a href="http://localhost:5173" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 11px 34px; border-radius: 24px; box-shadow: 0 2px 6px rgba(123,89,67,0.25);">
                            Khám phá cửa hàng
                        </a>
                    </div>
                </div>
                </body>
                </html>
            """.formatted(
                    kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "Khách hàng",
                    phieu.getMaPhieuGiamGia(),
                    phieu.getTenPhieuGiamGia()
            );

            helper.setText(html, true);
            log.info("==> [EmailService] Đang gửi email THU HỒI voucher tới: {}", kh.getEmail());
            mailSender.send(message);
            log.info("==> [EmailService] Đã gửi email thu hồi voucher THÀNH CÔNG tới: {}", kh.getEmail());
        } catch (Exception e) {
            log.error("==> [EmailService Lỗi] Không thể gửi email thu hồi tới {}: {}", kh.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendVoucherDeactivatedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            log.info("==> [EmailService] Bỏ qua gửi email ngừng hoạt động: Khách hàng không có email hợp lệ.");
            return;
        }
        try {
            if (mailSender == null) {
                log.info("==> [EmailService Mock] Đã gửi email ngừng hoạt động voucher {} tới {}", phieu.getMaPhieuGiamGia(), kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(kh.getEmail().trim());
            helper.setSubject("[FF T-Shirt] Thông báo tạm ngừng hoạt động Phiếu giảm giá: " + phieu.getMaPhieuGiamGia());

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                </head>
                <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <div style="max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <h2 style="margin: 0 0 14px 0; text-align: center; color: #4a3427; font-size: 20px; font-weight: 700;">
                        Chào %s,
                    </h2>
                    <p style="margin: 0 0 24px 0; text-align: center; color: #555555; font-size: 14px; line-height: 1.6;">
                        Hệ thống xin thông báo: Phiếu giảm giá <b>%s</b> (%s) bạn đang sở hữu đã <b>tạm ngừng hoạt động</b> theo quyết định từ ban quản lý cửa hàng <b>FF T-Shirt</b>.
                    </p>

                    <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #8c6d58; border-radius: 8px; padding: 20px 24px; margin: 0 0 24px 0; text-align: left;">
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Voucher:</span> %s
                        </div>
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427; margin-right: 8px;">Mã sử dụng:</span>
                            <span style="display: inline-block; background-color: #f1ebe5; color: #4a3427; font-weight: 700; font-size: 14px; padding: 3px 12px; border-radius: 4px; letter-spacing: 0.5px;">%s</span>
                        </div>
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Trạng thái:</span>
                            <span style="color: #b91c1c; font-weight: 700;">Tạm ngừng hoạt động</span>
                        </div>
                        <div style="font-size: 14px; color: #333333; line-height: 1.5;">
                            <span style="font-weight: 700; color: #4a3427;">Thời gian thông báo:</span> %s
                        </div>
                    </div>

                    <p style="margin: 0 0 24px 0; text-align: center; color: #666666; font-size: 13.5px; line-height: 1.5;">
                        Mã giảm giá này hiện không thể áp dụng khi thanh toán. Chúng tôi sẽ thông báo lại tới bạn ngay khi mã được kích hoạt trở lại. Cảm ơn bạn đã luôn đồng hành cùng <b>FF T-Shirt</b>!
                    </p>
                    <div style="text-align: center; margin: 0 0 10px 0;">
                        <a href="http://localhost:5173" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 11px 34px; border-radius: 24px; box-shadow: 0 2px 6px rgba(123,89,67,0.25);">
                            Khám phá cửa hàng
                        </a>
                    </div>
                </div>
                </body>
                </html>
            """.formatted(
                    kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "Khách hàng",
                    phieu.getMaPhieuGiamGia(),
                    phieu.getTenPhieuGiamGia(),
                    phieu.getTenPhieuGiamGia(),
                    phieu.getMaPhieuGiamGia(),
                    formatTime(Instant.now())
            );

            helper.setText(html, true);
            log.info("==> [EmailService] Đang gửi email NGỪNG HOẠT ĐỘNG voucher tới: {}", kh.getEmail());
            mailSender.send(message);
            log.info("==> [EmailService] Đã gửi email ngừng hoạt động voucher THÀNH CÔNG tới: {}", kh.getEmail());
        } catch (Exception e) {
            log.error("==> [EmailService Lỗi] Không thể gửi email ngừng hoạt động tới {}: {}", kh.getEmail(), e.getMessage());
        }
    }

    public boolean sendAccountInfoEmail(String toEmail, String employeeName, String code, String account, String password) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return false;
        }
        try {
            if (mailSender == null) {
                log.info("==> [EmailService Mock] Đã gửi thông tin tài khoản nhân viên {} tới {}", code, toEmail);
                return true;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(toEmail.trim());
            helper.setSubject("[FF T-Shirt] Cung cấp thông tin tài khoản nhân viên mới - " + code);

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                </head>
                <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <div style="max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <h2 style="margin: 0 0 14px 0; text-align: center; color: #4a3427; font-size: 20px; font-weight: 700;">
                        Chào mừng %s,
                    </h2>
                    <p style="margin: 0 0 24px 0; text-align: center; color: #555555; font-size: 14px; line-height: 1.6;">
                        Chào mừng bạn đã gia nhập đội ngũ <b>FF T-Shirt</b>. Dưới đây là thông tin tài khoản làm việc của bạn:
                    </p>

                    <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #8c6d58; border-radius: 8px; padding: 20px 24px; margin: 0 0 24px 0; text-align: left;">
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Mã nhân viên:</span>
                            <span style="display: inline-block; background-color: #f1ebe5; color: #4a3427; font-weight: 700; font-size: 14px; padding: 3px 12px; border-radius: 4px; letter-spacing: 0.5px; margin-left: 6px;">%s</span>
                        </div>
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Tên tài khoản:</span>
                            <span style="font-weight: 600; color: #1e3a8a; margin-left: 6px;">%s</span>
                        </div>
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Mật khẩu tạm thời:</span>
                            <span style="font-weight: 700; color: #b91c1c; font-size: 15px; margin-left: 6px; letter-spacing: 1px;">%s</span>
                        </div>
                        <div style="font-size: 13px; color: #666666; line-height: 1.5; margin-top: 14px; border-top: 1px dashed #d5cbc1; padding-top: 10px;">
                            <i>Lưu ý: Vì lý do bảo mật, vui lòng đăng nhập và đổi mật khẩu trong lần đầu tiên sử dụng hệ thống.</i>
                        </div>
                    </div>

                    <div style="text-align: center; margin: 0 0 10px 0;">
                        <a href="http://localhost:5173" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 11px 34px; border-radius: 24px; box-shadow: 0 2px 6px rgba(123,89,67,0.25);">
                            Đăng nhập hệ thống
                        </a>
                    </div>
                </div>
                </body>
                </html>
            """.formatted(
                    employeeName != null ? employeeName : "Bạn",
                    code,
                    account,
                    password
            );

            helper.setText(html, true);
            log.info("==> [EmailService] Đang gửi thông tin tài khoản nhân viên tới: {}", toEmail);
            mailSender.send(message);
            log.info("==> [EmailService] Đã gửi thông tin tài khoản nhân viên THÀNH CÔNG tới: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("==> [EmailService Lỗi] Không thể gửi email tài khoản tới {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    public record AccountEmailDto(String email, String name, String code, String account, String password) {}

    @Async
    public void sendBulkAccountInfoEmailsAsync(java.util.List<AccountEmailDto> emailsToSend) {
        if (emailsToSend == null || emailsToSend.isEmpty()) {
            return;
        }
        log.info("==> [EmailService Async] Bắt đầu gửi email chạy ngầm cho {} nhân viên mới...", emailsToSend.size());
        for (AccountEmailDto item : emailsToSend) {
            try {
                sendAccountInfoEmail(item.email(), item.name(), item.code(), item.account(), item.password());
            } catch (Exception ex) {
                log.warn("==> [EmailService Async] Không thể gửi email thông tin tài khoản tới {}: {}", item.email(), ex.getMessage());
            }
        }
        log.info("==> [EmailService Async] Hoàn tất tiến trình gửi email chạy ngầm cho {} nhân viên mới.", emailsToSend.size());
    }

    public boolean sendCustomEmail(String toEmail, String fullName, String subject, String content) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Không thể gửi email vì địa chỉ email trống.");
            return false;
        }

        String actualSubject = (subject == null || subject.isBlank()) ? "[FF-Tshirt] Thông báo hệ thống" : subject.trim();
        String fullContent = String.format(
                "Xin chào %s,\n\n" +
                "%s\n\n" +
                "Trân trọng,\n" +
                "Ban quản trị hệ thống FF-Tshirt",
                (fullName != null && !fullName.isBlank()) ? fullName : "bạn",
                (content != null && !content.isBlank()) ? content.trim() : "Bạn nhận được thông báo này từ hệ thống."
        );

        if (mailSender == null) {
            log.warn("JavaMailSender chưa được cấu hình. Nội dung email gửi tới {}:\n{}", toEmail, fullContent);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(toEmail.trim());
            helper.setSubject(actualSubject);
            helper.setText(fullContent, false);
            mailSender.send(message);
            log.info("Đã gửi email thông báo thành công tới: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Gửi email thất bại cho {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    @Async
    public void sendOrderConfirmationEmail(HoaDon hoaDon, java.util.List<ChiTietHoaDon> items, String recipientEmail) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty() || !recipientEmail.contains("@")) {
            log.info("==> [EmailService] Bỏ qua gửi email xác nhận: Địa chỉ email người nhận không hợp lệ ('{}')", recipientEmail);
            return;
        }
        if (hoaDon == null) {
            return;
        }

        try {
            if (mailSender == null) {
                log.info("==> [EmailService Mock] Đã gửi email xác nhận đơn hàng {} tới {}", hoaDon.getMaHoaDon(), recipientEmail);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(recipientEmail.trim());
            helper.setSubject("[FF T-Shirt] Xác nhận đơn hàng thành công #" + (hoaDon.getMaHoaDon() != null ? hoaDon.getMaHoaDon() : ""));

            // Build item rows HTML
            StringBuilder itemsHtml = new StringBuilder();
            if (items != null && !items.isEmpty()) {
                int idx = 1;
                for (ChiTietHoaDon item : items) {
                    String productName = "Sản phẩm";
                    String variantInfo = "";
                    String productCode = item.getMaHoaDonChiTiet() != null ? item.getMaHoaDonChiTiet() : "";
                    if (item.getIdChiTietSanPham() != null) {
                        var ctsp = item.getIdChiTietSanPham();
                        if (ctsp.getIdSanPham() != null && ctsp.getIdSanPham().getTenSanPham() != null) {
                            productName = ctsp.getIdSanPham().getTenSanPham();
                        }
                        if (ctsp.getMaChiTietSanPham() != null) {
                            productCode = ctsp.getMaChiTietSanPham();
                        }
                        String color = (ctsp.getIdMauSac() != null && ctsp.getIdMauSac().getTenMauSac() != null) ? ctsp.getIdMauSac().getTenMauSac() : "";
                        String size = (ctsp.getIdKichCo() != null && ctsp.getIdKichCo().getTenKichCo() != null) ? ctsp.getIdKichCo().getTenKichCo() : "";
                        if (!color.isEmpty() || !size.isEmpty()) {
                            variantInfo = (color.isEmpty() ? "" : color) + (!color.isEmpty() && !size.isEmpty() ? " / " : "") + (!size.isEmpty() ? ("Size " + size) : "");
                        }
                    }

                    int qty = item.getSoLuong() != null ? item.getSoLuong() : 1;
                    BigDecimal price = item.getDonGia() != null ? item.getDonGia() : BigDecimal.ZERO;
                    BigDecimal total = item.getThanhTien() != null ? item.getThanhTien() : price.multiply(BigDecimal.valueOf(qty));

                    itemsHtml.append("<tr style=\"border-bottom: 1px solid #ebd9c8;\">")
                            .append("<td style=\"padding: 10px 8px; text-align: center; color: #777; font-size: 13px;\">").append(idx++).append("</td>")
                            .append("<td style=\"padding: 10px 8px;\">")
                            .append("<div style=\"font-weight: 600; color: #333; font-size: 13.5px;\">").append(escapeHtml(productName)).append("</div>");
                    if (!variantInfo.isEmpty()) {
                        itemsHtml.append("<div style=\"font-size: 12px; color: #8c6d58; margin-top: 2px;\">Phân loại: ").append(escapeHtml(variantInfo)).append("</div>");
                    }
                    if (!productCode.isEmpty()) {
                        itemsHtml.append("<div style=\"font-size: 11px; color: #999; margin-top: 1px;\">Mã: ").append(escapeHtml(productCode)).append("</div>");
                    }
                    itemsHtml.append("</td>")
                            .append("<td style=\"padding: 10px 8px; text-align: center; font-weight: 600; color: #333; font-size: 13.5px;\">").append(qty).append("</td>")
                            .append("<td style=\"padding: 10px 8px; text-align: right; color: #555; font-size: 13px;\">").append(formatMoney(price)).append("</td>")
                            .append("<td style=\"padding: 10px 8px; text-align: right; font-weight: 700; color: #b91c1c; font-size: 13.5px;\">").append(formatMoney(total)).append("</td>")
                            .append("</tr>");
                }
            } else {
                itemsHtml.append("<tr><td colspan=\"5\" style=\"padding: 16px; text-align: center; color: #888;\">Không có chi tiết sản phẩm.</td></tr>");
            }

            BigDecimal subtotal = hoaDon.getTongTien() != null ? hoaDon.getTongTien() : BigDecimal.ZERO;
            BigDecimal shipping = hoaDon.getPhiVanChuyen() != null ? hoaDon.getPhiVanChuyen() : BigDecimal.ZERO;
            BigDecimal discount = hoaDon.getTongTienGiamGia() != null ? hoaDon.getTongTienGiamGia() : BigDecimal.ZERO;
            BigDecimal finalTotal = hoaDon.getTienSauGiamGia() != null ? hoaDon.getTienSauGiamGia() : subtotal.add(shipping).subtract(discount);

            String voucherRowHtml = "";
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                String voucherName = hoaDon.getIdPhieuGiamGia() != null ? hoaDon.getIdPhieuGiamGia().getTenPhieuGiamGia() : "Phiếu giảm giá";
                voucherRowHtml = "<div style=\"display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13.5px; color: #16a34a;\">" +
                        "<span>Giảm giá (" + voucherName + "):</span>" +
                        "<span style=\"font-weight: 600;\">-" + formatMoney(discount) + "</span>" +
                        "</div>";
            }

            String customerName = hoaDon.getTenKhachHangHienThi() != null ? hoaDon.getTenKhachHangHienThi() : "Quý khách";
            String paymentStatusText = (hoaDon.getTrangThaiThanhToan() != null && hoaDon.getTrangThaiThanhToan() == 1)
                    ? "<span style=\"color: #16a34a; font-weight: 700;\">Đã thanh toán</span>"
                    : "<span style=\"color: #d97706; font-weight: 700;\">Chưa thanh toán (COD / Chờ thanh toán)</span>";
            String deliveryAddress = (hoaDon.getDiaChiNhanHang() != null && !hoaDon.getDiaChiNhanHang().isBlank())
                    ? hoaDon.getDiaChiNhanHang()
                    : (Integer.valueOf(1).equals(hoaDon.getLoaiDon()) ? "Nhận trực tiếp tại quầy" : "Chưa cập nhật");
            String phone = (hoaDon.getSoDienThoaiKhachHang() != null && !hoaDon.getSoDienThoaiKhachHang().isBlank())
                    ? hoaDon.getSoDienThoaiKhachHang()
                    : "---";

            String maDon = hoaDon.getMaHoaDon() != null ? hoaDon.getMaHoaDon() : "";
            String orderDate = formatTime(hoaDon.getNgayTao());

            String template = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="margin: 0; padding: 0; background-color: #f4f6f8; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
                <div style="max-width: 620px; margin: 24px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 18px rgba(0,0,0,0.06); color: #333333; line-height: 1.6;">
                    
                    <!-- Header -->
                    <div style="background: linear-gradient(135deg, #4a3427 0%, #7b5943 100%); padding: 28px 32px; text-align: center; color: #ffffff;">
                        <h1 style="margin: 0; font-size: 24px; font-weight: 800; letter-spacing: 1.5px; text-transform: uppercase;">FF T-SHIRT STORE</h1>
                        <p style="margin: 6px 0 0; font-size: 13.5px; opacity: 0.9;">Thời trang áo phông cao cấp • Hotline: 0988.888.888</p>
                    </div>

                    <!-- Main body -->
                    <div style="padding: 32px 30px;">
                        
                        <!-- Confirmation Banner -->
                        <div style="background-color: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 10px; padding: 16px 20px; margin-bottom: 24px; text-align: center;">
                            <div style="font-size: 18px; font-weight: 700; color: #065f46; margin-bottom: 4px;">
                                Đơn hàng đã được xác nhận thành công!
                            </div>
                            <div style="font-size: 13.5px; color: #047857;">
                                Cảm ơn bạn đã mua sắm tại <b>FF T-Shirt</b>. Chúng tôi đang chuẩn bị đơn hàng để giao đến bạn sớm nhất.
                            </div>
                        </div>

                        <!-- Greeting -->
                        <p style="margin: 0 0 16px; font-size: 14.5px; color: #333;">
                            Xin chào <b>{{customerName}}</b>,
                        </p>

                        <!-- Order info summary -->
                        <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-radius: 10px; padding: 18px 20px; margin-bottom: 24px;">
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13.5px;">
                                <span style="color: #666;">Mã đơn hàng:</span>
                                <span style="font-weight: 700; color: #4a3427; font-family: monospace; font-size: 14px;">{{maDon}}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13.5px;">
                                <span style="color: #666;">Thời gian đặt:</span>
                                <span style="font-weight: 600; color: #333;">{{orderDate}}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13.5px;">
                                <span style="color: #666;">Trạng thái thanh toán:</span>
                                <span>{{paymentStatusText}}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13.5px;">
                                <span style="color: #666;">Số điện thoại:</span>
                                <span style="font-weight: 600; color: #333;">{{phone}}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; font-size: 13.5px;">
                                <span style="color: #666; flex-shrink: 0; margin-right: 12px;">Địa chỉ nhận hàng:</span>
                                <span style="font-weight: 600; color: #333; text-align: right;">{{deliveryAddress}}</span>
                            </div>
                        </div>

                        <!-- Product List Table -->
                        <div style="margin-bottom: 24px;">
                            <div style="font-weight: 700; font-size: 15px; color: #4a3427; margin-bottom: 12px;">
                                Chi tiết sản phẩm đã đặt:
                            </div>
                            <table style="width: 100%; border-collapse: collapse; border: 1px solid #ebd9c8; border-radius: 8px; overflow: hidden;">
                                <thead>
                                    <tr style="background-color: #f7f2ea; border-bottom: 2px solid #ebd9c8; font-size: 13px; color: #4a3427;">
                                        <th style="padding: 10px 8px; text-align: center; width: 35px;">#</th>
                                        <th style="padding: 10px 8px; text-align: left;">Sản phẩm</th>
                                        <th style="padding: 10px 8px; text-align: center; width: 50px;">SL</th>
                                        <th style="padding: 10px 8px; text-align: right; width: 85px;">Đơn giá</th>
                                        <th style="padding: 10px 8px; text-align: right; width: 95px;">Thành tiền</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {{itemsHtml}}
                                </tbody>
                            </table>
                        </div>

                        <!-- Price calculation summary -->
                        <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-radius: 10px; padding: 18px 20px; margin-bottom: 28px;">
                            <div style="display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13.5px; color: #555;">
                                <span>Tiền hàng:</span>
                                <span style="font-weight: 600;">{{subtotal}}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13.5px; color: #555;">
                                <span>Phí vận chuyển:</span>
                                <span style="font-weight: 600;">{{shipping}}</span>
                            </div>
                            {{voucherRowHtml}}
                            <div style="border-top: 1px dashed #d5cbc1; margin: 10px 0;"></div>
                            <div style="display: flex; justify-content: space-between; font-size: 15.5px;">
                                <span style="font-weight: 700; color: #2b353b;">Tổng thanh toán:</span>
                                <span style="font-weight: 800; color: #b91c1c; font-size: 17px;">{{finalTotal}}</span>
                            </div>
                        </div>

                        <!-- Button CTA -->
                        <div style="text-align: center; margin-bottom: 10px;">
                            <a href="http://localhost:5173/hoa-don/{{maDon}}" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14.5px; font-weight: 600; padding: 12px 36px; border-radius: 24px; box-shadow: 0 2px 8px rgba(123,89,67,0.3);">
                                Xem chi tiết đơn hàng
                            </a>
                        </div>
                    </div>

                    <!-- Footer -->
                    <div style="background-color: #f7f5ef; border-top: 1px solid #e8e4de; padding: 20px 30px; text-align: center; font-size: 12.5px; color: #777;">
                        <p style="margin: 0 0 6px;">Mọi thắc mắc về đơn hàng, vui lòng liên hệ Hotline: <b>0988.888.888</b> hoặc email hỗ trợ.</p>
                        <p style="margin: 0; color: #999;">&copy; 2026 FF T-Shirt Store. All rights reserved.</p>
                    </div>
                </div>
                </body>
                </html>
            """;

            String html = template
                    .replace("{{customerName}}", customerName)
                    .replace("{{maDon}}", maDon)
                    .replace("{{orderDate}}", orderDate)
                    .replace("{{paymentStatusText}}", paymentStatusText)
                    .replace("{{phone}}", phone)
                    .replace("{{deliveryAddress}}", deliveryAddress)
                    .replace("{{itemsHtml}}", itemsHtml.toString())
                    .replace("{{subtotal}}", formatMoney(subtotal))
                    .replace("{{shipping}}", formatMoney(shipping))
                    .replace("{{voucherRowHtml}}", voucherRowHtml)
                    .replace("{{finalTotal}}", formatMoney(finalTotal));

            helper.setText(html, true);
            log.info("==> [EmailService] Đang kết nối SMTP gửi email xác nhận đơn hàng {} tới {}", maDon, recipientEmail);
            mailSender.send(message);
            log.info("==> [EmailService] Đã gửi email xác nhận đơn hàng {} THÀNH CÔNG tới {}", maDon, recipientEmail);
        } catch (Exception e) {
            log.error("==> [EmailService Lỗi] Không thể gửi email xác nhận đơn hàng tới {}: {}", recipientEmail, e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean sendRevenueReportEmail(String toEmail, String subject, String htmlContent) {
        if (toEmail == null || toEmail.trim().isEmpty() || !toEmail.contains("@")) {
            log.warn("==> [EmailService] Bỏ qua gửi email báo cáo doanh thu: Địa chỉ email người nhận không hợp lệ ('{}')", toEmail);
            return false;
        }

        try {
            if (mailSender == null) {
                log.info("==> [EmailService Mock] Đã gửi email báo cáo doanh thu tới {}", toEmail);
                return true;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt Analytics");
            helper.setTo(toEmail.trim());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            log.info("==> [EmailService] Đang gửi email báo cáo doanh thu tới {}", toEmail);
            mailSender.send(message);
            log.info("==> [EmailService] Đã gửi email báo cáo doanh thu THÀNH CÔNG tới {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("==> [EmailService Lỗi] Không thể gửi email báo cáo doanh thu tới {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Lỗi gửi email: " + e.getMessage(), e);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Async
    public void sendContactEmail(String name, String phone, String email, String messageContent) {
        String recipientEmail = "fftshirt.vn@gmail.com";
        if (mailSender == null) {
            log.warn("==> [EmailService] JavaMailSender chưa sẵn sàng. Không thể gửi email liên hệ.");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt Official");
            helper.setTo(recipientEmail);
            if (email != null && !email.isBlank()) {
                helper.setReplyTo(email.trim());
            }
            helper.setSubject("[FF T-Shirt Hỗ Trợ] Yêu cầu mới từ " + (name != null ? name : "Khách hàng"));

            String timeNow = DATE_FORMATTER.format(Instant.now());

            String content = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 14px; overflow: hidden; box-shadow: 0 4px 18px rgba(0,0,0,0.06); color: #333333; line-height: 1.6;">
                    <div style="background: linear-gradient(135deg, #1e293b 0%, #334155 100%); padding: 24px; text-align: center; color: #ffffff;">
                        <h2 style="margin: 0; font-size: 20px; font-weight: 800; letter-spacing: 1px;">YÊU CẦU LIÊN HỆ & HỖ TRỢ KHÁCH HÀNG</h2>
                        <p style="margin: 6px 0 0; font-size: 13px; color: #ecd8af;">Hệ Thống Website FF T-Shirt</p>
                    </div>
                    <div style="padding: 24px;">
                        <p style="font-size: 14px; color: #64748b; margin-top: 0;">Thời gian tiếp nhận: <strong>{{timeNow}}</strong></p>
                        
                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin-bottom: 20px;">
                            <p style="margin: 0 0 8px; font-size: 14px;"><strong>Họ và tên khách hàng:</strong> {{name}}</p>
                            <p style="margin: 0 0 8px; font-size: 14px;"><strong>Số điện thoại:</strong> <a href="tel:{{phone}}" style="color: #0284c7; text-decoration: none; font-weight: 700;">{{phone}}</a></p>
                            <p style="margin: 0; font-size: 14px;"><strong>Email liên hệ:</strong> <a href="mailto:{{email}}" style="color: #0284c7; text-decoration: none;">{{email}}</a></p>
                        </div>

                        <div style="margin-bottom: 15px;">
                            <strong style="font-size: 14.5px; color: #1e293b;">Nội dung tin nhắn cần hỗ trợ:</strong>
                            <div style="background-color: #fffdfa; border-left: 4px solid #d2a764; border-radius: 4px; padding: 16px; margin-top: 8px; font-size: 14px; color: #334155; line-height: 1.6; white-space: pre-wrap;">{{messageContent}}</div>
                        </div>
                    </div>
                    <div style="background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 14px 20px; text-align: center; font-size: 12px; color: #94a3b8;">
                        Email này được gửi tự động từ hệ thống website FF T-Shirt trực tiếp tới hòm thư <strong>fftshirt.vn@gmail.com</strong>.
                    </div>
                </div>
            """
            .replace("{{timeNow}}", timeNow)
            .replace("{{name}}", name != null ? name : "Chưa cung cấp")
            .replace("{{phone}}", phone != null ? phone : "Chưa cung cấp")
            .replace("{{email}}", (email != null && !email.isBlank()) ? email : "Chưa cung cấp")
            .replace("{{messageContent}}", messageContent != null ? messageContent : "");

            helper.setText(content, true);
            log.info("==> [EmailService] Đang kết nối SMTP gửi email liên hệ của {} tới {}", name, recipientEmail);
            mailSender.send(message);
            log.info("==> [EmailService] Đã gửi email liên hệ tới {} THÀNH CÔNG!", recipientEmail);
        } catch (Exception e) {
            log.error("==> [EmailService Lỗi] Không thể gửi email liên hệ tới {}: {}", recipientEmail, e.getMessage(), e);
        }
    }

    @Async
    public void sendForgotPasswordEmail(String toEmail, String fullName, String employeeCode, String account, String newPassword) {
        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            log.warn("Bỏ qua email cấp lại mật khẩu vì địa chỉ email không hợp lệ: {}", toEmail);
            return;
        }

        try {
            if (mailSender == null) {
                log.info("[EmailService Mock] Cấp lại mật khẩu cho tài khoản '{}' ({}) thành công. Mật khẩu mới: {}", account, toEmail, newPassword);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(toEmail.trim());
            helper.setSubject("[FF T-Shirt] Cấp lại mật khẩu đăng nhập tài khoản " + (account != null ? account : employeeCode));

            String safeName = escapeHtml(fullName != null && !fullName.isBlank() ? fullName : "bạn");
            String safeCode = escapeHtml(employeeCode != null && !employeeCode.isBlank() ? employeeCode : "---");
            String safeAccount = escapeHtml(account != null && !account.isBlank() ? account : "Không xác định");
            String safePassword = escapeHtml(newPassword != null ? newPassword : "");

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #4a3427; margin: 0 0 6px 0; font-size: 22px; font-weight: 700;">FF T-Shirt</h2>
                        <p style="color: #777; margin: 0; font-size: 14px;">Thông báo cấp lại mật khẩu tài khoản</p>
                    </div>

                    <p style="margin: 0 0 12px 0; font-size: 14px;">Xin chào <strong>%s</strong>,</p>
                    <p style="margin: 0 0 20px 0; font-size: 14px; color: #555555; line-height: 1.6;">
                        Hệ thống đã nhận được yêu cầu cấp lại mật khẩu cho tài khoản của bạn. Mật khẩu đăng nhập mới đã được tạo thành công như sau:
                    </p>

                    <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #8c6d58; border-radius: 8px; padding: 20px 24px; margin: 0 0 24px 0; text-align: left;">
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Mã nhân viên:</span>
                            <span style="display: inline-block; background-color: #f1ebe5; color: #4a3427; font-weight: 700; font-size: 13.5px; padding: 2px 10px; border-radius: 4px; letter-spacing: 0.5px; margin-left: 6px;">%s</span>
                        </div>
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Tên tài khoản:</span>
                            <span style="font-weight: 600; color: #1e3a8a; margin-left: 6px;">%s</span>
                        </div>
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Mật khẩu mới:</span>
                            <span style="display: inline-block; background-color: #fce7e7; color: #b91c1c; font-weight: 700; font-size: 16px; padding: 4px 14px; border-radius: 4px; letter-spacing: 1px; margin-left: 6px; border: 1px dashed #f87171;">%s</span>
                        </div>
                        <div style="font-size: 13px; color: #666666; line-height: 1.5; margin-top: 14px; border-top: 1px dashed #d5cbc1; padding-top: 10px;">
                            <i>Lưu ý: Vì lý do bảo mật, vui lòng đăng nhập ngay và đổi mật khẩu trong phần cài đặt tài khoản.</i>
                        </div>
                    </div>

                    <div style="text-align: center; margin: 0 0 16px 0;">
                        <a href="http://localhost:5173" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 11px 34px; border-radius: 24px; box-shadow: 0 2px 6px rgba(123,89,67,0.25);">
                            Đăng nhập hệ thống
                        </a>
                    </div>

                    <p style="font-size: 12.5px; color: #888888; text-align: center; margin: 0 0 4px 0;">
                        Nếu bạn không yêu cầu cấp lại mật khẩu, vui lòng liên hệ ngay với Quản trị viên để được hỗ trợ.
                    </p>
                    <p style="font-size: 12px; color: #aaaaaa; text-align: center; margin: 0;">
                        Email tự động từ hệ thống FF T-Shirt. Vui lòng không trả lời email này.
                    </p>
                </div>
                """.formatted(safeName, safeCode, safeAccount, safePassword);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Đã gửi email cấp lại mật khẩu mới tới {}", toEmail);
        } catch (Exception e) {
            log.error("Không thể gửi email cấp lại mật khẩu tới {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async
    public void sendResetPasswordEmail(String toEmail, String fullName, String account,
                                       String resetToken, String resetUrl) {
        if (resetUrl != null && !resetUrl.startsWith("http://") && !resetUrl.startsWith("https://")) {
            sendForgotPasswordEmail(toEmail, fullName, account, resetToken, resetUrl);
            return;
        }

        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            log.warn("Bỏ qua email đặt lại mật khẩu vì địa chỉ email không hợp lệ.");
            return;
        }

        try {
            if (mailSender == null) {
                log.info("[EmailService Mock] Đặt lại mật khẩu cho tài khoản '{}': URL: {}", account, resetUrl);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(toEmail.trim());
            helper.setSubject("[FF T-Shirt] Yêu cầu đặt lại mật khẩu tài khoản " + account);

            String safeName = escapeHtml(fullName != null && !fullName.isBlank() ? fullName : "bạn");
            String safeAccount = escapeHtml(account != null && !account.isBlank() ? account : "Không xác định");
            String safeUrl = escapeHtml(resetUrl != null ? resetUrl : "http://localhost:5173");

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <h2 style="color: #4a3427; text-align: center; margin-bottom: 20px;">Đặt lại mật khẩu FF T-Shirt</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Hệ thống nhận được yêu cầu đặt lại mật khẩu cho tài khoản <strong>%s</strong>.</p>
                    <p>Vui lòng bấm vào nút bên dưới để tiến hành thiết lập mật khẩu mới (liên kết có hiệu lực trong vòng 15 phút):</p>
                    <div style="text-align: center; margin: 25px 0;">
                        <a href="%s" style="background-color: #7b5943; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: 700; display: inline-block;">
                            Đặt lại mật khẩu ngay
                        </a>
                    </div>
                </div>
            """.formatted(safeName, safeAccount, safeUrl);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Không thể gửi email đặt lại mật khẩu tới {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public boolean sendOtpVerificationEmail(String toEmail, String fullName, String otpCode) {
        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            log.warn("Bỏ qua email xác thực OTP vì địa chỉ email không hợp lệ: {}", toEmail);
            return false;
        }

        try {
            if (mailSender == null) {
                log.info("[EmailService Mock] Gửi mã OTP xác thực tới '{}' (Tên: {}): Mã OTP: {}", toEmail, fullName, otpCode);
                return true;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(toEmail.trim());
            helper.setSubject("[FF T-Shirt] Mã xác thực đăng ký tài khoản: " + otpCode);

            String safeName = escapeHtml(fullName != null && !fullName.isBlank() ? fullName : "Quý khách");
            String safeOtp = escapeHtml(otpCode != null ? otpCode : "");

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #4a3427; margin: 0 0 6px 0; font-size: 24px; font-weight: 700;">FF T-Shirt</h2>
                        <p style="color: #7b5943; margin: 0; font-size: 15px; font-weight: 600;">Xác thực đăng ký tài khoản</p>
                    </div>

                    <p style="margin: 0 0 14px 0; font-size: 15px;">Xin chào <strong>%s</strong>,</p>
                    <p style="margin: 0 0 16px 0; font-size: 14px; color: #555555; line-height: 1.6;">
                        Cảm ơn bạn đã đăng ký tài khoản thành viên tại <strong>FF T-Shirt Store</strong>. 
                        Dưới đây là mã xác thực (OTP) để hoàn tất quá trình kích hoạt tài khoản của bạn:
                    </p>

                    <div style="text-align: center; margin: 28px 0; background-color: #fdf8f4; border: 2px dashed #7b5943; border-radius: 12px; padding: 20px;">
                        <span style="font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #7b5943; font-family: 'Courier New', monospace;">%s</span>
                        <p style="margin: 8px 0 0 0; font-size: 13px; color: #888;">Mã xác thực có hiệu lực trong vòng <strong>10 phút</strong></p>
                    </div>

                    <p style="margin: 0 0 12px 0; font-size: 13px; color: #666666; line-height: 1.6;">
                        ⚠️ <strong>Lưu ý bảo mật:</strong> Tuyệt đối không chia sẻ mã xác thực này cho bất kỳ ai để đảm bảo an toàn cho tài khoản của bạn.
                    </p>

                    <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;" />
                    <p style="margin: 0; font-size: 12px; color: #999999; text-align: center;">
                        Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc liên hệ với bộ phận hỗ trợ khách hàng của FF T-Shirt.
                    </p>
                </div>
            """.formatted(safeName, safeOtp);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Đã gửi email mã xác thực OTP thành công tới {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Không thể gửi email mã xác thực OTP tới {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }
}
