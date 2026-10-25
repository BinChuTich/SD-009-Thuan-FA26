package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.ChiTietHoaDon;
import com.example.sd009thuan.entity.HoaDon;
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
        if (amount == null) return "0 Ä‘";
        return String.format("%,d Ä‘", amount.longValue()).replace(',', '.');
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
                    <span style="font-weight: 700; color: #4a3427;">Giáº£m tá»‘i Ä‘a:</span>
                    <span style="color: #b91c1c; font-weight: 600;">%s</span>
                </div>
            """.formatted(formatMoney(phieu.getGiamToiDa()));
        }
        return "";
    }

    private String formatTime(Instant instant) {
        if (instant == null) return "KhÃ´ng giá»›i háº¡n";
        return DATE_FORMATTER.format(instant);
    }

    /**
     * Gá»­i email thÃ´ng bÃ¡o táº·ng phiáº¿u giáº£m giÃ¡ má»›i cho khÃ¡ch hÃ ng theo form chuáº©n thanh lá»‹ch
     */
    @Async
    public void sendVoucherIssuedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            System.out.println("==> [EmailService] Bá» qua gá»­i email phÃ¡t hÃ nh voucher vÃ¬ khÃ¡ch hÃ ng khÃ´ng cÃ³ email há»£p lá»‡.");
            return;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] ÄÃ£ gá»­i email phÃ¡t hÃ nh voucher " + phieu.getMaPhieuGiamGia() + " tá»›i " + kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(kh.getEmail().trim());
            helper.setSubject("[FF T-Shirt] Báº¡n vá»«a nháº­n Ä‘Æ°á»£c Phiáº¿u giáº£m giÃ¡: " + phieu.getMaPhieuGiamGia());

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <h2 style="margin: 0 0 14px 0; text-align: center; color: #4a3427; font-size: 20px; font-weight: 700;">
                        ChÃ o %s,
                    </h2>
                    <p style="margin: 0 0 24px 0; text-align: center; color: #555555; font-size: 14px; line-height: 1.6;">
                        Cáº£m Æ¡n báº¡n Ä‘Ã£ luÃ´n Ä‘á»“ng hÃ nh cÃ¹ng <b>FF T-Shirt</b>. ChÃºng tÃ´i xin dÃ nh táº·ng riÃªng báº¡n má»™t pháº§n quÃ  Ä‘áº·c biá»‡t:
                    </p>

                    <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #8c6d58; border-radius: 8px; padding: 20px 24px; margin: 0 0 24px 0; text-align: left;">
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Voucher:</span> %s
                        </div>
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427; margin-right: 8px;">MÃ£ sá»­ dá»¥ng:</span>
                            <span style="display: inline-block; background-color: #f1ebe5; color: #4a3427; font-weight: 700; font-size: 14px; padding: 3px 12px; border-radius: 4px; letter-spacing: 0.5px;">%s</span>
                        </div>
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">GiÃ¡ trá»‹ giáº£m:</span> <span style="color: #b91c1c; font-weight: 700;">%s</span>
                        </div>
                        %s
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Ãp dá»¥ng cho Ä‘Æ¡n tá»«:</span> %s
                        </div>
                        <div style="font-size: 14px; color: #333333; line-height: 1.5;">
                            <span style="font-weight: 700; color: #4a3427;">Thá»i gian Ã¡p dá»¥ng:</span> Tá»« %s Ä‘áº¿n háº¿t ngÃ y %s
                        </div>
                    </div>

                    <p style="margin: 0 0 24px 0; text-align: center; color: #666666; font-size: 13.5px; line-height: 1.5;">
                        HÃ£y nhanh tay sá»­ dá»¥ng mÃ£ giáº£m giÃ¡ nÃ y cho láº§n mua sáº¯m tiáº¿p theo nhÃ©!
                    </p>

                    <div style="text-align: center; margin: 0 0 10px 0;">
                        <a href="http://localhost:5173" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 11px 34px; border-radius: 24px; box-shadow: 0 2px 6px rgba(123,89,67,0.25);">
                            Mua sáº¯m ngay
                        </a>
                    </div>
                </div>
            """.formatted(
                    kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "KhÃ¡ch hÃ ng",
                    phieu.getTenPhieuGiamGia(),
                    phieu.getMaPhieuGiamGia(),
                    formatGiaTriGiam(phieu),
                    buildGiamToiDaHtml(phieu),
                    formatMoney(phieu.getHoaDonToiThieu()),
                    formatTime(phieu.getNgayBatDau()),
                    formatTime(phieu.getNgayKetThuc())
            );

            helper.setText(html, true);
            System.out.println("==> [EmailService] Äang káº¿t ná»‘i SMTP Ä‘á»ƒ gá»­i email phÃ¡t hÃ nh voucher tá»›i: " + kh.getEmail());
            mailSender.send(message);
            System.out.println("==> [EmailService] ÄÃ£ gá»­i email phÃ¡t hÃ nh voucher THÃ€NH CÃ”NG tá»›i: " + kh.getEmail());
        } catch (Exception e) {
            System.err.println("==> [EmailService Lá»—i] KhÃ´ng thá»ƒ gá»­i email phÃ¡t hÃ nh tá»›i " + kh.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gá»­i email cáº­p nháº­t thÃ´ng tin voucher má»›i cho khÃ¡ch hÃ ng Ä‘ang sá»Ÿ há»¯u
     */
    @Async
    public void sendVoucherUpdatedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            System.out.println("==> [EmailService] Bá» qua gá»­i email cáº­p nháº­t voucher vÃ¬ khÃ¡ch hÃ ng khÃ´ng cÃ³ email há»£p lá»‡.");
            return;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] ÄÃ£ gá»­i email cáº­p nháº­t voucher " + phieu.getMaPhieuGiamGia() + " tá»›i " + kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(kh.getEmail().trim());
            helper.setSubject("[FF T-Shirt] Cáº­p nháº­t thÃ´ng tin Æ°u Ä‘Ã£i Phiáº¿u giáº£m giÃ¡: " + phieu.getMaPhieuGiamGia());

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <h2 style="margin: 0 0 14px 0; text-align: center; color: #4a3427; font-size: 20px; font-weight: 700;">
                        ChÃ o %s,
                    </h2>
                    <p style="margin: 0 0 24px 0; text-align: center; color: #555555; font-size: 14px; line-height: 1.6;">
                        Phiáº¿u giáº£m giÃ¡ cá»§a báº¡n táº¡i <b>FF T-Shirt</b> vá»«a Ä‘Æ°á»£c cáº­p nháº­t thÃªm cÃ¡c Æ°u Ä‘Ã£i vÃ  thá»i háº¡n má»›i:
                    </p>

                    <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #8c6d58; border-radius: 8px; padding: 20px 24px; margin: 0 0 24px 0; text-align: left;">
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Voucher:</span> %s
                        </div>
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427; margin-right: 8px;">MÃ£ sá»­ dá»¥ng:</span>
                            <span style="display: inline-block; background-color: #f1ebe5; color: #4a3427; font-weight: 700; font-size: 14px; padding: 3px 12px; border-radius: 4px; letter-spacing: 0.5px;">%s</span>
                        </div>
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">GiÃ¡ trá»‹ giáº£m:</span> <span style="color: #b91c1c; font-weight: 700;">%s</span>
                        </div>
                        %s
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Ãp dá»¥ng cho Ä‘Æ¡n tá»«:</span> %s
                        </div>
                        <div style="font-size: 14px; color: #333333; line-height: 1.5;">
                            <span style="font-weight: 700; color: #4a3427;">Thá»i gian Ã¡p dá»¥ng:</span> Tá»« %s Ä‘áº¿n háº¿t ngÃ y %s
                        </div>
                    </div>

                    <p style="margin: 0 0 24px 0; text-align: center; color: #666666; font-size: 13.5px; line-height: 1.5;">
                        HÃ£y nhanh tay sá»­ dá»¥ng mÃ£ giáº£m giÃ¡ nÃ y cho láº§n mua sáº¯m tiáº¿p theo nhÃ©!
                    </p>

                    <div style="text-align: center; margin: 0 0 10px 0;">
                        <a href="http://localhost:5173" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 11px 34px; border-radius: 24px; box-shadow: 0 2px 6px rgba(123,89,67,0.25);">
                            Mua sáº¯m ngay
                        </a>
                    </div>
                </div>
            """.formatted(
                    kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "KhÃ¡ch hÃ ng",
                    phieu.getTenPhieuGiamGia(),
                    phieu.getMaPhieuGiamGia(),
                    formatGiaTriGiam(phieu),
                    buildGiamToiDaHtml(phieu),
                    formatMoney(phieu.getHoaDonToiThieu()),
                    formatTime(phieu.getNgayBatDau()),
                    formatTime(phieu.getNgayKetThuc())
            );

            helper.setText(html, true);
            System.out.println("==> [EmailService] Äang gá»­i email Cáº¬P NHáº¬T voucher tá»›i: " + kh.getEmail());
            mailSender.send(message);
            System.out.println("==> [EmailService] ÄÃ£ gá»­i email cáº­p nháº­t voucher THÃ€NH CÃ”NG tá»›i: " + kh.getEmail());
        } catch (Exception e) {
            System.err.println("==> [EmailService Lá»—i] KhÃ´ng thá»ƒ gá»­i email cáº­p nháº­t tá»›i " + kh.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gá»­i email thÃ´ng bÃ¡o há»§y / thu há»“i voucher khi khÃ¡ch bá»‹ gá»¡ khá»i danh sÃ¡ch Ã¡p dá»¥ng
     */
    @Async
    public void sendVoucherRevokedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            System.out.println("==> [EmailService] Bá» qua gá»­i email thu há»“i voucher vÃ¬ khÃ¡ch hÃ ng khÃ´ng cÃ³ email há»£p lá»‡.");
            return;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] ÄÃ£ gá»­i email thu há»“i voucher " + phieu.getMaPhieuGiamGia() + " tá»›i " + kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(kh.getEmail().trim());
            helper.setSubject("[FF T-Shirt] ThÃ´ng bÃ¡o káº¿t thÃºc Ã¡p dá»¥ng Phiáº¿u giáº£m giÃ¡: " + phieu.getMaPhieuGiamGia());

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <h2 style="margin: 0 0 14px 0; text-align: center; color: #4a3427; font-size: 20px; font-weight: 700;">
                        ChÃ o %s,
                    </h2>
                    <p style="margin: 0 0 24px 0; text-align: center; color: #555555; font-size: 14px; line-height: 1.6;">
                        Há»‡ thá»‘ng xin thÃ´ng bÃ¡o: Phiáº¿u giáº£m giÃ¡ <b>%s</b> (%s) Ä‘Ã£ káº¿t thÃºc Ã¡p dá»¥ng cho tÃ i khoáº£n cá»§a báº¡n theo chÃ­nh sÃ¡ch cáº­p nháº­t cá»§a chÆ°Æ¡ng trÃ¬nh.
                    </p>
                    <p style="margin: 0 0 24px 0; text-align: center; color: #666666; font-size: 13.5px; line-height: 1.5;">
                        ChÃºng tÃ´i sáº½ sá»›m gá»­i tá»›i báº¡n cÃ¡c chÆ°Æ¡ng trÃ¬nh Æ°u Ä‘Ã£i háº¥p dáº«n khÃ¡c trong thá»i gian tá»›i. Cáº£m Æ¡n báº¡n Ä‘Ã£ luÃ´n Ä‘á»“ng hÃ nh cÃ¹ng <b>FF T-Shirt</b>!
                    </p>
                    <div style="text-align: center; margin: 0 0 10px 0;">
                        <a href="http://localhost:5173" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 11px 34px; border-radius: 24px; box-shadow: 0 2px 6px rgba(123,89,67,0.25);">
                            KhÃ¡m phÃ¡ cá»­a hÃ ng
                        </a>
                    </div>
                </div>
            """.formatted(
                    kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "KhÃ¡ch hÃ ng",
                    phieu.getMaPhieuGiamGia(),
                    phieu.getTenPhieuGiamGia()
            );

            helper.setText(html, true);
            System.out.println("==> [EmailService] Äang gá»­i email THU Há»’I voucher tá»›i: " + kh.getEmail());
            mailSender.send(message);
            System.out.println("==> [EmailService] ÄÃ£ gá»­i email thu há»“i voucher THÃ€NH CÃ”NG tá»›i: " + kh.getEmail());
        } catch (Exception e) {
            System.err.println("==> [EmailService Lá»—i] KhÃ´ng thá»ƒ gá»­i email thu há»“i tá»›i " + kh.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gá»­i email thÃ´ng bÃ¡o táº¡m ngá»«ng hoáº¡t Ä‘á»™ng phiáº¿u giáº£m giÃ¡ khi Admin báº¥m táº¯t
     */
    @Async
    public void sendVoucherDeactivatedEmail(KhachHang kh, PhieuGiamGia phieu) {
        if (kh == null || kh.getEmail() == null || kh.getEmail().trim().isEmpty()) {
            System.out.println("==> [EmailService] Bá» qua gá»­i email ngá»«ng hoáº¡t Ä‘á»™ng: KhÃ¡ch hÃ ng khÃ´ng cÃ³ email há»£p lá»‡.");
            return;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] ÄÃ£ gá»­i email ngá»«ng hoáº¡t Ä‘á»™ng voucher " + phieu.getMaPhieuGiamGia() + " tá»›i " + kh.getEmail());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(kh.getEmail().trim());
            helper.setSubject("[FF T-Shirt] ThÃ´ng bÃ¡o táº¡m ngá»«ng hoáº¡t Ä‘á»™ng Phiáº¿u giáº£m giÃ¡: " + phieu.getMaPhieuGiamGia());

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <h2 style="margin: 0 0 14px 0; text-align: center; color: #4a3427; font-size: 20px; font-weight: 700;">
                        ChÃ o %s,
                    </h2>
                    <p style="margin: 0 0 24px 0; text-align: center; color: #555555; font-size: 14px; line-height: 1.6;">
                        Há»‡ thá»‘ng xin thÃ´ng bÃ¡o: Phiáº¿u giáº£m giÃ¡ <b>%s</b> (%s) báº¡n Ä‘ang sá»Ÿ há»¯u Ä‘Ã£ <b>táº¡m ngá»«ng hoáº¡t Ä‘á»™ng</b> theo quyáº¿t Ä‘á»‹nh tá»« ban quáº£n lÃ½ cá»­a hÃ ng <b>FF T-Shirt</b>.
                    </p>

                    <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #8c6d58; border-radius: 8px; padding: 20px 24px; margin: 0 0 24px 0; text-align: left;">
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Voucher:</span> %s
                        </div>
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427; margin-right: 8px;">MÃ£ sá»­ dá»¥ng:</span>
                            <span style="display: inline-block; background-color: #f1ebe5; color: #4a3427; font-weight: 700; font-size: 14px; padding: 3px 12px; border-radius: 4px; letter-spacing: 0.5px;">%s</span>
                        </div>
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Tráº¡ng thÃ¡i:</span>
                            <span style="color: #b91c1c; font-weight: 700;">Táº¡m ngá»«ng hoáº¡t Ä‘á»™ng</span>
                        </div>
                        <div style="font-size: 14px; color: #333333; line-height: 1.5;">
                            <span style="font-weight: 700; color: #4a3427;">Thá»i gian thÃ´ng bÃ¡o:</span> %s
                        </div>
                    </div>

                    <p style="margin: 0 0 24px 0; text-align: center; color: #666666; font-size: 13.5px; line-height: 1.5;">
                        MÃ£ giáº£m giÃ¡ nÃ y hiá»‡n khÃ´ng thá»ƒ Ã¡p dá»¥ng khi thanh toÃ¡n. ChÃºng tÃ´i sáº½ thÃ´ng bÃ¡o láº¡i tá»›i báº¡n ngay khi mÃ£ Ä‘Æ°á»£c kÃ­ch hoáº¡t trá»Ÿ láº¡i. Cáº£m Æ¡n báº¡n Ä‘Ã£ luÃ´n Ä‘á»“ng hÃ nh cÃ¹ng <b>FF T-Shirt</b>!
                    </p>
                    <div style="text-align: center; margin: 0 0 10px 0;">
                        <a href="http://localhost:5173" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 11px 34px; border-radius: 24px; box-shadow: 0 2px 6px rgba(123,89,67,0.25);">
                            KhÃ¡m phÃ¡ cá»­a hÃ ng
                        </a>
                    </div>
                </div>
            """.formatted(
                    kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "KhÃ¡ch hÃ ng",
                    phieu.getMaPhieuGiamGia(),
                    phieu.getTenPhieuGiamGia(),
                    phieu.getTenPhieuGiamGia(),
                    phieu.getMaPhieuGiamGia(),
                    formatTime(Instant.now())
            );

            helper.setText(html, true);
            System.out.println("==> [EmailService] Äang gá»­i email NGá»ªNG HOáº T Äá»˜NG voucher tá»›i: " + kh.getEmail());
            mailSender.send(message);
            System.out.println("==> [EmailService] ÄÃ£ gá»­i email ngá»«ng hoáº¡t Ä‘á»™ng voucher THÃ€NH CÃ”NG tá»›i: " + kh.getEmail());
        } catch (Exception e) {
            System.err.println("==> [EmailService Lá»—i] KhÃ´ng thá»ƒ gá»­i email ngá»«ng hoáº¡t Ä‘á»™ng tá»›i " + kh.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gá»­i email thÃ´ng tin tÃ i khoáº£n vÃ  máº­t kháº©u cho nhÃ¢n viÃªn má»›i
     */
    public boolean sendAccountInfoEmail(String toEmail, String employeeName, String code, String account, String password) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return false;
        }
        try {
            if (mailSender == null) {
                System.out.println("==> [EmailService Mock] ÄÃ£ gá»­i thÃ´ng tin tÃ i khoáº£n nhÃ¢n viÃªn " + code + " tá»›i " + toEmail);
                return true;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail.trim());
            helper.setSubject("[FF T-Shirt] Cung cáº¥p thÃ´ng tin tÃ i khoáº£n nhÃ¢n viÃªn má»›i - " + code);

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 580px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.04); color: #333333; line-height: 1.6;">
                    <h2 style="margin: 0 0 14px 0; text-align: center; color: #4a3427; font-size: 20px; font-weight: 700;">
                        ChÃ o má»«ng %s,
                    </h2>
                    <p style="margin: 0 0 24px 0; text-align: center; color: #555555; font-size: 14px; line-height: 1.6;">
                        ChÃ o má»«ng báº¡n Ä‘Ã£ gia nháº­p Ä‘á»™i ngÅ© <b>FF T-Shirt</b>. DÆ°á»›i Ä‘Ã¢y lÃ  thÃ´ng tin tÃ i khoáº£n lÃ m viá»‡c cá»§a báº¡n:
                    </p>

                    <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-left: 4px solid #8c6d58; border-radius: 8px; padding: 20px 24px; margin: 0 0 24px 0; text-align: left;">
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">MÃ£ nhÃ¢n viÃªn:</span>
                            <span style="display: inline-block; background-color: #f1ebe5; color: #4a3427; font-weight: 700; font-size: 14px; padding: 3px 12px; border-radius: 4px; letter-spacing: 0.5px; margin-left: 6px;">%s</span>
                        </div>
                        <div style="margin-bottom: 12px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">TÃªn tÃ i khoáº£n:</span>
                            <span style="font-weight: 600; color: #1e3a8a; margin-left: 6px;">%s</span>
                        </div>
                        <div style="margin-bottom: 10px; font-size: 14px; color: #333333;">
                            <span style="font-weight: 700; color: #4a3427;">Máº­t kháº©u táº¡m thá»i:</span>
                            <span style="font-weight: 700; color: #b91c1c; font-size: 15px; margin-left: 6px; letter-spacing: 1px;">%s</span>
                        </div>
                        <div style="font-size: 13px; color: #666666; line-height: 1.5; margin-top: 14px; border-top: 1px dashed #d5cbc1; padding-top: 10px;">
                            <i>LÆ°u Ã½: VÃ¬ lÃ½ do báº£o máº­t, vui lÃ²ng Ä‘Äƒng nháº­p vÃ  Ä‘á»•i máº­t kháº©u trong láº§n Ä‘áº§u tiÃªn sá»­ dá»¥ng há»‡ thá»‘ng.</i>
                        </div>
                    </div>

                    <div style="text-align: center; margin: 0 0 10px 0;">
                        <a href="http://localhost:5173" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 11px 34px; border-radius: 24px; box-shadow: 0 2px 6px rgba(123,89,67,0.25);">
                            ÄÄƒng nháº­p há»‡ thá»‘ng
                        </a>
                    </div>
                </div>
            """.formatted(
                    employeeName != null ? employeeName : "Báº¡n",
                    code,
                    account,
                    password
            );

            helper.setText(html, true);
            System.out.println("==> [EmailService] Äang gá»­i thÃ´ng tin tÃ i khoáº£n nhÃ¢n viÃªn tá»›i: " + toEmail);
            mailSender.send(message);
            System.out.println("==> [EmailService] ÄÃ£ gá»­i thÃ´ng tin tÃ i khoáº£n nhÃ¢n viÃªn THÃ€NH CÃ”NG tá»›i: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("==> [EmailService Lá»—i] KhÃ´ng thá»ƒ gá»­i email tÃ i khoáº£n tá»›i " + toEmail + ": " + e.getMessage());
            return false;
        }
    }

    public record AccountEmailDto(String email, String name, String code, String account, String password) {}

    /**
     * Gá»­i email danh sÃ¡ch thÃ´ng tin tÃ i khoáº£n nhÃ¢n viÃªn má»›i báº¥t Ä‘á»“ng bá»™ (@Async ngáº§m khÃ´ng block request)
     */
    @Async
    public void sendBulkAccountInfoEmailsAsync(java.util.List<AccountEmailDto> emailsToSend) {
        if (emailsToSend == null || emailsToSend.isEmpty()) {
            return;
        }
        log.info("==> [EmailService Async] Báº¯t Ä‘áº§u gá»­i email cháº¡y ngáº§m cho {} nhÃ¢n viÃªn má»›i...", emailsToSend.size());
        for (AccountEmailDto item : emailsToSend) {
            try {
                sendAccountInfoEmail(item.email(), item.name(), item.code(), item.account(), item.password());
            } catch (Exception ex) {
                log.warn("==> [EmailService Async] KhÃ´ng thá»ƒ gá»­i email thÃ´ng tin tÃ i khoáº£n tá»›i {}: {}", item.email(), ex.getMessage());
            }
        }
        log.info("==> [EmailService Async] HoÃ n táº¥t tiáº¿n trÃ¬nh gá»­i email cháº¡y ngáº§m cho {} nhÃ¢n viÃªn má»›i.", emailsToSend.size());
    }

    public boolean sendCustomEmail(String toEmail, String fullName, String subject, String content) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("KhÃ´ng thá»ƒ gá»­i email vÃ¬ Ä‘á»‹a chá»‰ email trá»‘ng.");
            return false;
        }

        String actualSubject = (subject == null || subject.isBlank()) ? "[FF-Tshirt] ThÃ´ng bÃ¡o há»‡ thá»‘ng" : subject.trim();
        String fullContent = String.format(
                "Xin chÃ o %s,\n\n" +
                "%s\n\n" +
                "TrÃ¢n trá»ng,\n" +
                "Ban quáº£n trá»‹ há»‡ thá»‘ng FF-Tshirt",
                (fullName != null && !fullName.isBlank()) ? fullName : "báº¡n",
                (content != null && !content.isBlank()) ? content.trim() : "Báº¡n nháº­n Ä‘Æ°á»£c thÃ´ng bÃ¡o nÃ y tá»« há»‡ thá»‘ng."
        );

        if (mailSender == null) {
            log.warn("JavaMailSender chÆ°a Ä‘Æ°á»£c cáº¥u hÃ¬nh. Ná»™i dung email gá»­i tá»›i {}:\n{}", toEmail, fullContent);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail.trim());
            message.setSubject(actualSubject);
            message.setText(fullContent);
            mailSender.send(message);
            log.info("ÄÃ£ gá»­i email thÃ´ng bÃ¡o thÃ nh cÃ´ng tá»›i: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Gá»­i email tháº¥t báº¡i cho {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    /**
     * Gá»­i email xÃ¡c nháº­n Ä‘Æ¡n hÃ ng tá»± Ä‘á»™ng cho khÃ¡ch hÃ ng khi Ä‘Æ¡n hÃ ng Ä‘Æ°á»£c xÃ¡c nháº­n
     */
    @Async
    public void sendOrderConfirmationEmail(HoaDon hoaDon, java.util.List<ChiTietHoaDon> items, String recipientEmail) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty() || !recipientEmail.contains("@")) {
            log.info("==> [EmailService] Bá» qua gá»­i email xÃ¡c nháº­n: Äá»‹a chá»‰ email ngÆ°á»i nháº­n khÃ´ng há»£p lá»‡ ('{}')", recipientEmail);
            return;
        }
        if (hoaDon == null) {
            return;
        }

        try {
            if (mailSender == null) {
                log.info("==> [EmailService Mock] ÄÃ£ gá»­i email xÃ¡c nháº­n Ä‘Æ¡n hÃ ng {} tá»›i {}", hoaDon.getMaHoaDon(), recipientEmail);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "FF T-Shirt");
            helper.setTo(recipientEmail.trim());
            helper.setSubject("[FF T-Shirt] XÃ¡c nháº­n Ä‘Æ¡n hÃ ng thÃ nh cÃ´ng #" + (hoaDon.getMaHoaDon() != null ? hoaDon.getMaHoaDon() : ""));

            // Build item rows HTML
            StringBuilder itemsHtml = new StringBuilder();
            if (items != null && !items.isEmpty()) {
                int idx = 1;
                for (ChiTietHoaDon item : items) {
                    String productName = "Sáº£n pháº©m";
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

                    itemsHtml.append("<tr style=\"border-bottom: 1px solid #f0eae1;\">")
                            .append("<td style=\"padding: 10px 8px; text-align: center; color: #777; font-size: 13px;\">").append(idx++).append("</td>")
                            .append("<td style=\"padding: 10px 8px; text-align: left;\">")
                            .append("<div style=\"font-weight: 600; color: #2b353b; font-size: 13.5px;\">").append(productName).append("</div>");
                    if (!variantInfo.isEmpty()) {
                        itemsHtml.append("<div style=\"font-size: 12.5px; color: #666; margin: 2px 0;\">PhÃ¢n loáº¡i: ").append(variantInfo).append("</div>");
                    }
                    if (!productCode.isEmpty()) {
                        itemsHtml.append("<div style=\"font-size: 12px; color: #888; font-family: monospace;\">MÃ£: ").append(productCode).append("</div>");
                    }
                    itemsHtml.append("</td>")
                            .append("<td style=\"padding: 10px 8px; text-align: center; font-weight: 600; color: #333; font-size: 13.5px;\">").append(qty).append("</td>")
                            .append("<td style=\"padding: 10px 8px; text-align: right; color: #555; font-size: 13.5px;\">").append(formatMoney(price)).append("</td>")
                            .append("<td style=\"padding: 10px 8px; text-align: right; font-weight: 700; color: #3e5c76; font-size: 13.5px;\">").append(formatMoney(total)).append("</td>")
                            .append("</tr>");
                }
            } else {
                itemsHtml.append("<tr><td colspan=\"5\" style=\"padding: 16px; text-align: center; color: #888;\">Chi tiáº¿t sáº£n pháº©m Ä‘Æ¡n hÃ ng Ä‘ang Ä‘Æ°á»£c cáº­p nháº­t</td></tr>");
            }

            // Calculation values
            BigDecimal subtotal = hoaDon.getTongTien() != null ? hoaDon.getTongTien() : BigDecimal.ZERO;
            BigDecimal shipping = hoaDon.getPhiVanChuyen() != null ? hoaDon.getPhiVanChuyen() : BigDecimal.ZERO;
            BigDecimal finalTotal = hoaDon.getTienSauGiamGia() != null ? hoaDon.getTienSauGiamGia() : subtotal.add(shipping);
            BigDecimal discount = subtotal.add(shipping).subtract(finalTotal);
            if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO;

            String voucherRowHtml = "";
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                String voucherName = hoaDon.getIdPhieuGiamGia() != null ? hoaDon.getIdPhieuGiamGia().getTenPhieuGiamGia() : "Phiáº¿u giáº£m giÃ¡";
                voucherRowHtml = "<div style=\"display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13.5px; color: #16a34a;\">" +
                        "<span>Giáº£m giÃ¡ (" + voucherName + "):</span>" +
                        "<span style=\"font-weight: 600;\">-" + formatMoney(discount) + "</span>" +
                        "</div>";
            }

            String customerName = hoaDon.getTenKhachHangHienThi() != null ? hoaDon.getTenKhachHangHienThi() : "QuÃ½ khÃ¡ch";
            String paymentStatusText = (hoaDon.getTrangThaiThanhToan() != null && hoaDon.getTrangThaiThanhToan() == 1)
                    ? "<span style=\"color: #16a34a; font-weight: 700;\">ÄÃ£ thanh toÃ¡n</span>"
                    : "<span style=\"color: #d97706; font-weight: 700;\">ChÆ°a thanh toÃ¡n (COD / Chá» thanh toÃ¡n)</span>";

            String deliveryAddress = (hoaDon.getDiaChiNhanHang() != null && !hoaDon.getDiaChiNhanHang().isBlank())
                    ? hoaDon.getDiaChiNhanHang()
                    : (Integer.valueOf(1).equals(hoaDon.getLoaiDon()) ? "Nháº­n trá»±c tiáº¿p táº¡i quáº§y" : "ChÆ°a cáº­p nháº­t");

            String phone = (hoaDon.getSoDienThoaiKhachHang() != null && !hoaDon.getSoDienThoaiKhachHang().isBlank())
                    ? hoaDon.getSoDienThoaiKhachHang()
                    : "---";

            String maDon = hoaDon.getMaHoaDon() != null ? hoaDon.getMaHoaDon() : "";
            String orderDate = formatTime(hoaDon.getNgayTao());

            String template = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 620px; margin: 20px auto; background-color: #ffffff; border: 1px solid #e8e4de; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 18px rgba(0,0,0,0.06); color: #333333; line-height: 1.6;">
                    
                    <!-- Header -->
                    <div style="background: linear-gradient(135deg, #4a3427 0%, #7b5943 100%); padding: 28px 32px; text-align: center; color: #ffffff;">
                        <h1 style="margin: 0; font-size: 24px; font-weight: 800; letter-spacing: 1.5px; text-transform: uppercase;">FF T-SHIRT STORE</h1>
                        <p style="margin: 6px 0 0; font-size: 13.5px; opacity: 0.9;">Thá»i trang Ã¡o phÃ´ng cao cáº¥p â€¢ Hotline: 0988.888.888</p>
                    </div>

                    <!-- Main body -->
                    <div style="padding: 32px 30px;">
                        
                        <!-- Confirmation Banner -->
                        <div style="background-color: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 10px; padding: 16px 20px; margin-bottom: 24px; text-align: center;">
                            <div style="font-size: 18px; font-weight: 700; color: #065f46; margin-bottom: 4px;">
                                ÄÆ¡n hÃ ng Ä‘Ã£ Ä‘Æ°á»£c xÃ¡c nháº­n thÃ nh cÃ´ng!
                            </div>
                            <div style="font-size: 13.5px; color: #047857;">
                                Cáº£m Æ¡n báº¡n Ä‘Ã£ mua sáº¯m táº¡i <b>FF T-Shirt</b>. ChÃºng tÃ´i Ä‘ang chuáº©n bá»‹ Ä‘Æ¡n hÃ ng Ä‘á»ƒ giao Ä‘áº¿n báº¡n sá»›m nháº¥t.
                            </div>
                        </div>

                        <!-- Greeting -->
                        <p style="margin: 0 0 16px; font-size: 14.5px; color: #333;">
                            Xin chÃ o <b>{{customerName}}</b>,
                        </p>

                        <!-- Order info summary -->
                        <div style="background-color: #faf7f2; border: 1px solid #f0eae1; border-radius: 10px; padding: 18px 20px; margin-bottom: 24px;">
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13.5px;">
                                <span style="color: #666;">MÃ£ Ä‘Æ¡n hÃ ng:</span>
                                <span style="font-weight: 700; color: #4a3427; font-family: monospace; font-size: 14px;">{{maDon}}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13.5px;">
                                <span style="color: #666;">Thá»i gian Ä‘áº·t:</span>
                                <span style="font-weight: 600; color: #333;">{{orderDate}}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13.5px;">
                                <span style="color: #666;">Tráº¡ng thÃ¡i thanh toÃ¡n:</span>
                                <span>{{paymentStatusText}}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13.5px;">
                                <span style="color: #666;">Sá»‘ Ä‘iá»‡n thoáº¡i:</span>
                                <span style="font-weight: 600; color: #333;">{{phone}}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; font-size: 13.5px;">
                                <span style="color: #666; flex-shrink: 0; margin-right: 12px;">Äá»‹a chá»‰ nháº­n hÃ ng:</span>
                                <span style="font-weight: 600; color: #333; text-align: right;">{{deliveryAddress}}</span>
                            </div>
                        </div>

                        <!-- Product List Table -->
                        <div style="margin-bottom: 24px;">
                            <div style="font-weight: 700; font-size: 15px; color: #4a3427; margin-bottom: 12px;">
                                Chi tiáº¿t sáº£n pháº©m Ä‘Ã£ Ä‘áº·t:
                            </div>
                            <table style="width: 100%; border-collapse: collapse; border: 1px solid #ebd9c8; border-radius: 8px; overflow: hidden;">
                                <thead>
                                    <tr style="background-color: #f7f2ea; border-bottom: 2px solid #ebd9c8; font-size: 13px; color: #4a3427;">
                                        <th style="padding: 10px 8px; text-align: center; width: 35px;">#</th>
                                        <th style="padding: 10px 8px; text-align: left;">Sáº£n pháº©m</th>
                                        <th style="padding: 10px 8px; text-align: center; width: 50px;">SL</th>
                                        <th style="padding: 10px 8px; text-align: right; width: 85px;">ÄÆ¡n giÃ¡</th>
                                        <th style="padding: 10px 8px; text-align: right; width: 95px;">ThÃ nh tiá»n</th>
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
                                <span>Tiá»n hÃ ng:</span>
                                <span style="font-weight: 600;">{{subtotal}}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13.5px; color: #555;">
                                <span>PhÃ­ váº­n chuyá»ƒn:</span>
                                <span style="font-weight: 600;">{{shipping}}</span>
                            </div>
                            {{voucherRowHtml}}
                            <div style="border-top: 1px dashed #d5cbc1; margin: 10px 0;"></div>
                            <div style="display: flex; justify-content: space-between; font-size: 15.5px;">
                                <span style="font-weight: 700; color: #2b353b;">Tá»•ng thanh toÃ¡n:</span>
                                <span style="font-weight: 800; color: #b91c1c; font-size: 17px;">{{finalTotal}}</span>
                            </div>
                        </div>

                        <!-- Button CTA -->
                        <div style="text-align: center; margin-bottom: 10px;">
                            <a href="http://localhost:5173/hoa-don/{{maDon}}" style="display: inline-block; background-color: #7b5943; color: #ffffff; text-decoration: none; font-size: 14.5px; font-weight: 600; padding: 12px 36px; border-radius: 24px; box-shadow: 0 2px 8px rgba(123,89,67,0.3);">
                                Xem chi tiáº¿t Ä‘Æ¡n hÃ ng
                            </a>
                        </div>
                    </div>

                    <!-- Footer -->
                    <div style="background-color: #f7f5ef; border-top: 1px solid #e8e4de; padding: 20px 30px; text-align: center; font-size: 12.5px; color: #777;">
                        <p style="margin: 0 0 6px;">Má»i tháº¯c máº¯c vá» Ä‘Æ¡n hÃ ng, vui lÃ²ng liÃªn há»‡ Hotline: <b>0988.888.888</b> hoáº·c email há»— trá»£.</p>
                        <p style="margin: 0; color: #999;">Â© 2026 FF T-Shirt Store. All rights reserved.</p>
                    </div>
                </div>
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
            log.info("==> [EmailService] Äang káº¿t ná»‘i SMTP gá»­i email xÃ¡c nháº­n Ä‘Æ¡n hÃ ng {} tá»›i {}", maDon, recipientEmail);
            mailSender.send(message);
            log.info("==> [EmailService] ÄÃ£ gá»­i email xÃ¡c nháº­n Ä‘Æ¡n hÃ ng {} THÃ€NH CÃ”NG tá»›i {}", maDon, recipientEmail);
        } catch (Exception e) {
            log.error("==> [EmailService Lá»—i] KhÃ´ng thá»ƒ gá»­i email xÃ¡c nháº­n Ä‘Æ¡n hÃ ng tá»›i {}: {}", recipientEmail, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gửi email báo cáo tổng kết doanh thu và so sánh định kỳ cho Ban quản trị (Admin)
     */
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
}