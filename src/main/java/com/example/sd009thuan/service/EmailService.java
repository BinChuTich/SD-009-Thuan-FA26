package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.KhachHang;
import com.example.sd009thuan.entity.PhieuGiamGia;
import jakarta.mail.internet.MimeMessage;
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

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 đ";
        return String.format("%,d đ", amount.longValue()).replace(',', '.');
    }

    private String formatDiscount(PhieuGiamGia phieu) {
        if (phieu.getLoaiPhieuGiamGia() == 1) {
            String res = phieu.getGiaTriGiamGia() + "%";
            if (phieu.getGiamToiDa() != null) {
                res += " (Tối đa " + formatMoney(phieu.getGiamToiDa()) + ")";
            }
            return res;
        } else {
            return formatMoney(phieu.getGiaTriGiamGia());
        }
    }

    private String formatTime(Instant instant) {
        if (instant == null) return "Không giới hạn";
        return DATE_FORMATTER.format(instant);
    }

    /**
     * Gửi email thông báo tặng phiếu giảm giá mới cho khách hàng
     */
    @Async
    public void sendVoucherIssuedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            return;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] Đã gửi email phát hành voucher " + phieu.getMaPhieuGiamGia() + " tới " + kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(kh.getEmail());
            helper.setSubject("[FF T-Shirt] Bạn vừa nhận được Phiếu giảm giá đặc biệt: " + phieu.getMaPhieuGiamGia());

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);">
                    <div style="background-color: #2563eb; color: #ffffff; padding: 24px; text-align: center;">
                        <h2 style="margin: 0; font-size: 24px; letter-spacing: 0.5px;">FF T-SHIRT</h2>
                        <p style="margin: 6px 0 0; font-size: 14px; opacity: 0.9;">Thời trang trẻ trung & Hiện đại</p>
                    </div>
                    <div style="padding: 28px 24px; background-color: #ffffff;">
                        <p style="font-size: 16px; color: #374151; margin-top: 0;">Xin chào <b>%s</b>,</p>
                        <p style="font-size: 15px; color: #4b5563; line-height: 1.6;">
                            Hệ thống cửa hàng <b>FF T-Shirt</b> xin trân trọng gửi tặng bạn phiếu giảm giá độc quyền dành riêng cho bạn!
                        </p>
                        
                        <div style="background-color: #f8fafc; border: 2px dashed #3b82f6; border-radius: 10px; padding: 20px; text-align: center; margin: 24px 0;">
                            <div style="font-size: 13px; color: #64748b; text-transform: uppercase; font-weight: bold;">MÃ GIẢM GIÁ CỦA BẠN</div>
                            <div style="font-size: 28px; font-weight: 800; color: #1d4ed8; letter-spacing: 2px; margin: 10px 0;">%s</div>
                            <div style="font-size: 16px; font-weight: 600; color: #dc2626;">%s</div>
                        </div>

                        <table style="width: 100%%; font-size: 14px; border-collapse: collapse; margin-bottom: 24px;">
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 10px 0; color: #64748b;">Tên chương trình:</td>
                                <td style="padding: 10px 0; color: #1e293b; font-weight: 600; text-align: right;">%s</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 10px 0; color: #64748b;">Đơn hàng tối thiểu:</td>
                                <td style="padding: 10px 0; color: #1e293b; font-weight: 600; text-align: right;">%s</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 10px 0; color: #64748b;">Thời gian áp dụng:</td>
                                <td style="padding: 10px 0; color: #1e293b; font-weight: 600; text-align: right;">%s - %s</td>
                            </tr>
                        </table>

                        <p style="font-size: 14px; color: #6b7280; line-height: 1.5; margin-bottom: 0;">
                            Hãy nhanh tay ghé thăm cửa hàng hoặc mua sắm trực tuyến để tận hưởng ưu đãi đặc biệt này nhé!
                        </p>
                    </div>
                    <div style="background-color: #f9fafb; padding: 16px; text-align: center; font-size: 12px; color: #9ca3af; border-top: 1px solid #f3f4f6;">
                        Hotline hỗ trợ: 1900 6868 | Email: cskh@fftshirt.vn
                    </div>
                </div>
            """.formatted(
                    kh.getTenKhachHang(),
                    phieu.getMaPhieuGiamGia(),
                    formatDiscount(phieu),
                    phieu.getTenPhieuGiamGia(),
                    formatMoney(phieu.getHoaDonToiThieu()),
                    formatTime(phieu.getNgayBatDau()),
                    formatTime(phieu.getNgayKetThuc())
            );

            helper.setText(html, true);
            System.out.println("==> [EmailService] Đang kết nối SMTP để gửi email phát hành voucher tới: " + kh.getEmail());
            mailSender.send(message);
            System.out.println("==> [EmailService] Đã gửi email phát hành voucher THÀNH CÔNG tới: " + kh.getEmail());
        } catch (Exception e) {
            System.err.println("==> [EmailService Lỗi] Không thể gửi email tới " + kh.getEmail() + ": " + e.getMessage());
            System.err.println("==> [Gợi ý cấu hình Gmail] Để gửi email thành công về hòm thư thật, bạn cần cấu hình tài khoản Gmail và Mật khẩu ứng dụng (App Password 16 chữ cái) trong file application.properties.");
        }
    }

    /**
     * Gửi email cập nhật thông tin voucher mới cho khách hàng đang sở hữu
     */
    @Async
    public void sendVoucherUpdatedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            return;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] Đã gửi email cập nhật voucher " + phieu.getMaPhieuGiamGia() + " tới " + kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(kh.getEmail());
            helper.setSubject("[FF T-Shirt] Cập nhật thông tin ưu đãi Phiếu giảm giá: " + phieu.getMaPhieuGiamGia());

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden;">
                    <div style="background-color: #0891b2; color: #ffffff; padding: 24px; text-align: center;">
                        <h2 style="margin: 0; font-size: 24px;">FF T-SHIRT</h2>
                        <p style="margin: 6px 0 0; font-size: 14px; opacity: 0.9;">Thông báo cập nhật ưu đãi</p>
                    </div>
                    <div style="padding: 28px 24px; background-color: #ffffff;">
                        <p style="font-size: 16px; color: #374151; margin-top: 0;">Xin chào <b>%s</b>,</p>
                        <p style="font-size: 15px; color: #4b5563; line-height: 1.6;">
                            Phiếu giảm giá <b>%s</b> bạn đang sở hữu vừa được cập nhật thêm các ưu đãi và thời hạn mới như sau:
                        </p>
                        
                        <div style="background-color: #f0fdfa; border: 2px dashed #0d9488; border-radius: 10px; padding: 20px; text-align: center; margin: 24px 0;">
                            <div style="font-size: 26px; font-weight: 800; color: #0f766e; letter-spacing: 2px;">%s</div>
                            <div style="font-size: 16px; font-weight: 600; color: #0d9488; margin-top: 6px;">Ưu đãi: %s</div>
                        </div>

                        <table style="width: 100%%; font-size: 14px; border-collapse: collapse; margin-bottom: 24px;">
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 10px 0; color: #64748b;">Đơn tối thiểu:</td>
                                <td style="padding: 10px 0; color: #1e293b; font-weight: 600; text-align: right;">%s</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 10px 0; color: #64748b;">Thời hạn mới:</td>
                                <td style="padding: 10px 0; color: #1e293b; font-weight: 600; text-align: right;">%s - %s</td>
                            </tr>
                        </table>
                    </div>
                </div>
            """.formatted(
                    kh.getTenKhachHang(),
                    phieu.getMaPhieuGiamGia(),
                    phieu.getMaPhieuGiamGia(),
                    formatDiscount(phieu),
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
        }
    }

    /**
     * Gửi email thông báo hủy / thu hồi voucher khi khách bị gỡ khỏi danh sách áp dụng
     */
    @Async
    public void sendVoucherRevokedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            return;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] Đã gửi email thu hồi voucher " + phieu.getMaPhieuGiamGia() + " tới " + kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(kh.getEmail());
            helper.setSubject("[FF T-Shirt] Thông báo kết thúc áp dụng Phiếu giảm giá: " + phieu.getMaPhieuGiamGia());

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden;">
                    <div style="background-color: #64748b; color: #ffffff; padding: 24px; text-align: center;">
                        <h2 style="margin: 0; font-size: 24px;">FF T-SHIRT</h2>
                        <p style="margin: 6px 0 0; font-size: 14px; opacity: 0.9;">Thông báo kết thúc áp dụng ưu đãi</p>
                    </div>
                    <div style="padding: 28px 24px; background-color: #ffffff;">
                        <p style="font-size: 16px; color: #374151; margin-top: 0;">Xin chào <b>%s</b>,</p>
                        <p style="font-size: 15px; color: #4b5563; line-height: 1.6;">
                            Hệ thống xin thông báo: Phiếu giảm giá <b>%s</b> (%s) đã được ngừng áp dụng cho tài khoản của bạn theo chính sách cập nhật của chương trình.
                        </p>
                        <p style="font-size: 14px; color: #6b7280; line-height: 1.5;">
                            Chúng tôi sẽ sớm gửi tới bạn các chương trình ưu đãi hấp dẫn khác trong thời gian tới. Cảm ơn bạn đã luôn đồng hành cùng FF T-Shirt!
                        </p>
                    </div>
                </div>
            """.formatted(
                    kh.getTenKhachHang(),
                    phieu.getMaPhieuGiamGia(),
                    phieu.getTenPhieuGiamGia()
            );

            helper.setText(html, true);
            System.out.println("==> [EmailService] Đang gửi email THU HỒI voucher tới: " + kh.getEmail());
            mailSender.send(message);
            System.out.println("==> [EmailService] Đã gửi email thu hồi voucher THÀNH CÔNG tới: " + kh.getEmail());
        } catch (Exception e) {
            System.err.println("==> [EmailService Lỗi] Không thể gửi email thu hồi tới " + kh.getEmail() + ": " + e.getMessage());
        }
    }
}

