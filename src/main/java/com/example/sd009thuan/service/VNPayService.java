package com.example.sd009thuan.service;

import com.example.sd009thuan.config.VNPayConfig;
import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

@Service
public class VNPayService {

    @Autowired
    private VNPayConfig vnPayConfig;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private PhuongThucThanhToanRepository phuongThucThanhToanRepository;

    @Autowired
    private HinhThucThanhToanRepository hinhThucThanhToanRepository;

    @Autowired
    private LichSuHoaDonRepository lichSuHoaDonRepository;

    @Autowired
    private ChiTietHoaDonRepository chiTietHoaDonRepository;

    @Autowired
    private EmailService emailService;

    public String createPaymentUrl(HttpServletRequest request, String orderCode, long amount, String orderInfo) {
        long amountInCents = amount * 100L;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amountInCents));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderCode);
        vnp_Params.put("vnp_OrderInfo", (orderInfo != null && !orderInfo.isBlank()) ? orderInfo : ("Thanh toan don hang " + orderCode));
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", VNPayConfig.getIpAddress(request));

        TimeZone tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar cld = Calendar.getInstance(tz);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(tz);
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString())).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                } catch (Exception ignored) {}
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return vnPayConfig.getPayUrl() + "?" + queryUrl;
    }

    @Transactional
    public Map<String, Object> verifyPayment(Map<String, String> queryParams) {
        Map<String, Object> result = new HashMap<>();

        if (queryParams == null || queryParams.isEmpty()) {
            result.put("success", false);
            result.put("message", "Tham số giao dịch không hợp lệ!");
            return result;
        }

        String vnp_SecureHash = queryParams.get("vnp_SecureHash");
        Map<String, String> fields = new HashMap<>(queryParams);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                } catch (Exception ignored) {}
            }
        }

        String signValue = VNPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        boolean isValidSignature = signValue.equalsIgnoreCase(vnp_SecureHash);

        String vnp_ResponseCode = queryParams.get("vnp_ResponseCode");
        String vnp_TxnRef = queryParams.get("vnp_TxnRef");
        String vnp_TransactionNo = queryParams.get("vnp_TransactionNo");
        String vnp_BankCode = queryParams.get("vnp_BankCode");
        String vnp_Amount = queryParams.get("vnp_Amount");
        String vnp_PayDate = queryParams.get("vnp_PayDate");

        long amount = 0L;
        if (vnp_Amount != null) {
            try {
                amount = Long.parseLong(vnp_Amount) / 100L;
            } catch (Exception ignored) {}
        }

        result.put("orderCode", vnp_TxnRef);
        result.put("transactionNo", vnp_TransactionNo);
        result.put("bankCode", vnp_BankCode);
        result.put("amount", amount);
        result.put("payDate", vnp_PayDate);
        result.put("responseCode", vnp_ResponseCode);

        if (!isValidSignature) {
            result.put("success", false);
            result.put("message", "Chữ ký checksum không hợp lệ! Dữ liệu có thể đã bị thay đổi.");
            return result;
        }

        if ("00".equals(vnp_ResponseCode)) {
            // Thanh toán thành công!
            if (vnp_TxnRef != null && !vnp_TxnRef.isBlank()) {
                Optional<HoaDon> hdOpt = hoaDonRepository.findByMaHoaDon(vnp_TxnRef.trim());
                if (hdOpt.isPresent()) {
                    HoaDon hd = hdOpt.get();
                    hd.setTrangThaiThanhToan(1); // 1 = Đã thanh toán
                    hd.setNgayCapNhat(Instant.now());
                    hd.setNguoiCapNhat("Cổng thanh toán VNPAY");
                    hoaDonRepository.save(hd);

                    // Ghi nhận hình thức thanh toán VNPAY
                    try {
                        List<PhuongThucThanhToan> allPts = phuongThucThanhToanRepository.findAll();
                        PhuongThucThanhToan ptVnpay = null;
                        for (PhuongThucThanhToan p : allPts) {
                            if (p.getTenPhuongThuc() != null) {
                                String t = p.getTenPhuongThuc().toLowerCase();
                                if (t.contains("vnpay") || t.contains("chuyển khoản") || t.contains("ngân hàng")) {
                                    ptVnpay = p;
                                    break;
                                }
                            }
                        }
                        if (ptVnpay == null && !allPts.isEmpty()) {
                            ptVnpay = allPts.get(0);
                        }

                        if (ptVnpay != null) {
                            HinhThucThanhToan httt = new HinhThucThanhToan();
                            httt.setIdHoaDon(hd);
                            httt.setIdPhuongThuc(ptVnpay);
                            httt.setSoTien(hd.getTienSauGiamGia() != null ? hd.getTienSauGiamGia() : java.math.BigDecimal.valueOf(amount));
                            httt.setMaGiaoDich("VNPAY-" + (vnp_TransactionNo != null ? vnp_TransactionNo : System.currentTimeMillis()));
                            httt.setThoiGian(Instant.now());
                            httt.setTrangThai(1);
                            hinhThucThanhToanRepository.save(httt);
                        }
                    } catch (Exception ex) {
                        System.err.println("==> Lỗi lưu HinhThucThanhToan VNPAY: " + ex.getMessage());
                    }

                    // Ghi nhận lịch sử thao tác
                    try {
                        LichSuHoaDon ls = new LichSuHoaDon();
                        ls.setIdHoaDon(hd);
                        ls.setTrangThai(hd.getTrangThai());
                        ls.setThoiGian(Instant.now());
                        ls.setGhiChu(String.format("Khách hàng đã thanh toán thành công qua VNPAY (Mã GD: %s, Ngân hàng: %s, Số tiền: %,d VNĐ)",
                                vnp_TransactionNo, vnp_BankCode, amount));
                        ls.setNguoiThucHien("Cổng VNPAY");
                        lichSuHoaDonRepository.save(ls);
                    } catch (Exception ex) {
                        System.err.println("==> Lỗi ghi LichSuHoaDon VNPAY: " + ex.getMessage());
                    }

                    // Gửi email xác nhận đơn hàng đã thanh toán
                    try {
                        String recipient = hd.getEmailKhachHang();
                        if (recipient != null && !recipient.isBlank() && emailService != null) {
                            List<ChiTietHoaDon> items = chiTietHoaDonRepository.findByIdHoaDon_Id(hd.getId());
                            emailService.sendOrderConfirmationEmail(hd, items, recipient);
                        }
                    } catch (Exception ex) {
                        System.err.println("==> Lỗi gửi email xác nhận sau khi thanh toán VNPAY: " + ex.getMessage());
                    }
                }
            }

            result.put("success", true);
            result.put("message", "Giao dịch thanh toán qua VNPAY thành công!");
        } else {
            result.put("success", false);
            result.put("message", "Giao dịch không thành công hoặc khách hàng đã hủy thanh toán (Mã phản hồi: " + vnp_ResponseCode + ").");
        }

        return result;
    }
}

