package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.KhachHang;
import com.example.sd009thuan.entity.PhieuGiamGia;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
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

    /**
     * Gửi email thông báo tặng phiếu giảm giá mới cho khách hàng theo form chuẩn thanh lịch
     */
    @Async
    public void sendVoucherIssuedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            System.out.println("==> [EmailService] Bỏ qua gửi email phát hành voucher vì khách hàng không có email hợp lệ.");
            return;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] Đã gửi email phát hành voucher " + phieu.getMaPhieuGiamGia() + " tới " + kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(kh.getEmail().trim());
            helper.setSubject("[FF T-Shirt] Bạn vừa nhận được Phiếu giảm giá: " + phieu.getMaPhieuGiamGia());

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
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
            System.out.println("==> [EmailService] Đang kết nối SMTP để gửi email phát hành voucher tới: " + kh.getEmail());
            mailSender.send(message);
            System.out.println("==> [EmailService] Đã gửi email phát hành voucher THÀNH CÔNG tới: " + kh.getEmail());
        } catch (Exception e) {
            System.err.println("==> [EmailService Lỗi] Không thể gửi email phát hành tới " + kh.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gửi email cập nhật thông tin voucher mới cho khách hàng đang sở hữu
     */
    @Async
    public void sendVoucherUpdatedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            System.out.println("==> [EmailService] Bỏ qua gửi email cập nhật voucher vì khách hàng không có email hợp lệ.");
            return;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] Đã gửi email cập nhật voucher " + phieu.getMaPhieuGiamGia() + " tới " + kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(kh.getEmail().trim());
            helper.setSubject("[FF T-Shirt] Cập nhật thông tin ưu đãi Phiếu giảm giá: " + phieu.getMaPhieuGiamGia());

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
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
            System.out.println("==> [EmailService] Đang gửi email CẬP NHẬT voucher tới: " + kh.getEmail());
            mailSender.send(message);
            System.out.println("==> [EmailService] Đã gửi email cập nhật voucher THÀNH CÔNG tới: " + kh.getEmail());
        } catch (Exception e) {
            System.err.println("==> [EmailService Lỗi] Không thể gửi email cập nhật tới " + kh.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gửi email thông báo hủy / thu hồi voucher khi khách bị gỡ khỏi danh sách áp dụng
     */
    @Async
    public void sendVoucherRevokedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            System.out.println("==> [EmailService] Bỏ qua gửi email thu hồi voucher vì khách hàng không có email hợp lệ.");
            return;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] Đã gửi email thu hồi voucher " + phieu.getMaPhieuGiamGia() + " tới " + kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(kh.getEmail().trim());
            helper.setSubject("[FF T-Shirt] Thông báo kết thúc áp dụng Phiếu giảm giá: " + phieu.getMaPhieuGiamGia());

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
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
            """.formatted(
                    kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "Khách hàng",
                    phieu.getMaPhieuGiamGia(),
                    phieu.getTenPhieuGiamGia()
            );

            helper.setText(html, true);
            System.out.println("==> [EmailService] Đang gửi email THU HỒI voucher tới: " + kh.getEmail());
            mailSender.send(message);
            System.out.println("==> [EmailService] Đã gửi email thu hồi voucher THÀNH CÔNG tới: " + kh.getEmail());
        } catch (Exception e) {
            System.err.println("==> [EmailService Lỗi] Không thể gửi email thu hồi tới " + kh.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gửi email thông báo tạm ngừng hoạt động phiếu giảm giá khi Admin bấm tắt
     */
    @Async
    public void sendVoucherDeactivatedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            System.out.println("==> [EmailService] Bỏ qua gửi email ngừng hoạt động: Khách hàng không có email hợp lệ.");
            return;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] Đã gửi email ngừng hoạt động voucher " + phieu.getMaPhieuGiamGia() + " tới " + kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(kh.getEmail().trim());
            helper.setSubject("[FF T-Shirt] Thông báo tạm ngừng hoạt động Phiếu giảm giá: " + phieu.getMaPhieuGiamGia());

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
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
            """.formatted(
                    kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "Khách hàng",
                    phieu.getMaPhieuGiamGia(),
                    phieu.getTenPhieuGiamGia(),
                    phieu.getTenPhieuGiamGia(),
                    phieu.getMaPhieuGiamGia(),
                    formatTime(Instant.now())
            );

            helper.setText(html, true);
            System.out.println("==> [EmailService] Đang gửi email NGỪNG HOẠT ĐỘNG voucher tới: " + kh.getEmail());
            mailSender.send(message);
            System.out.println("==> [EmailService] Đã gửi email ngừng hoạt động voucher THÀNH CÔNG tới: " + kh.getEmail());
        } catch (Exception e) {
            System.err.println("==> [EmailService Lỗi] Không thể gửi email ngừng hoạt động tới " + kh.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gửi email thông tin tài khoản và mật khẩu cho nhân viên mới
     */
    public boolean sendAccountInfoEmail(String toEmail, String employeeName, String code, String account, String password) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return false;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] Đã gửi thông tin tài khoản nhân viên " + code + " tới " + toEmail);
                return true;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail.trim());
            helper.setSubject("[FF T-Shirt] Cung cấp thông tin tài khoản nhân viên mới - " + code);

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
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
            """.formatted(
                    employeeName != null ? employeeName : "Bạn",
                    code,
                    account,
                    password
            );

            helper.setText(html, true);
            System.out.println("==> [EmailService] Đang gửi thông tin tài khoản nhân viên tới: " + toEmail);
            mailSender.send(message);
            System.out.println("==> [EmailService] Đã gửi thông tin tài khoản nhân viên THÀNH CÔNG tới: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("==> [EmailService Lỗi] Không thể gửi email tài khoản tới " + toEmail + ": " + e.getMessage());
            return false;
        }
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
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail.trim());
            message.setSubject(actualSubject);
            message.setText(fullContent);
            mailSender.send(message);
            log.info("Đã gửi email thông báo thành công tới: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Gửi email thất bại cho {}: {}", toEmail, e.getMessage());
            return false;
        }
    }
}
