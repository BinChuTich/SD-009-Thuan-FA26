package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.payment.ConfirmPaymentRequest;
import com.example.sd009thuan.dto.payment.PaymentInfoRequest;
import com.example.sd009thuan.dto.payment.PaymentInfoResponse;
import com.example.sd009thuan.dto.payment.PaymentStatusResponse;
import com.example.sd009thuan.entity.HinhThucThanhToan;
import com.example.sd009thuan.entity.HoaDon;
import com.example.sd009thuan.entity.LichSuHoaDon;
import com.example.sd009thuan.entity.PhuongThucThanhToan;
import com.example.sd009thuan.repository.HinhThucThanhToanRepository;
import com.example.sd009thuan.repository.HoaDonRepository;
import com.example.sd009thuan.repository.LichSuHoaDonRepository;
import com.example.sd009thuan.repository.PhuongThucThanhToanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentService {

    public static final String DEFAULT_BANK_ID = "MB";
    public static final String DEFAULT_BANK_NAME = "VNPAY-QR (MBBank)";
    public static final String DEFAULT_ACCOUNT_NO = "0988888888";
    public static final String DEFAULT_ACCOUNT_NAME = "FF T-SHIRT STORE";
    public static final String VNP_TMN_CODE = "DEMOV210";
    public static final String VNP_HASH_SECRET = "VNPAYHASHSECRETKEYDEMO";
    public static final String VNP_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    private final HoaDonRepository hoaDonRepository;
    private final HinhThucThanhToanRepository hinhThucThanhToanRepository;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;

    // Bộ nhớ tạm lưu trạng thái thanh toán chuyển khoản nhanh (hỗ trợ cả hóa đơn chờ POS)
    private final Map<String, PaymentStatusResponse> transactionCache = new ConcurrentHashMap<>();

    public PaymentService(
            HoaDonRepository hoaDonRepository,
            HinhThucThanhToanRepository hinhThucThanhToanRepository,
            PhuongThucThanhToanRepository phuongThucThanhToanRepository,
            LichSuHoaDonRepository lichSuHoaDonRepository
    ) {
        this.hoaDonRepository = hoaDonRepository;
        this.hinhThucThanhToanRepository = hinhThucThanhToanRepository;
        this.phuongThucThanhToanRepository = phuongThucThanhToanRepository;
        this.lichSuHoaDonRepository = lichSuHoaDonRepository;
    }

    /**
     * Tạo thông tin chuyển khoản và mã VietQR động theo đơn hàng
     */
    public PaymentInfoResponse generateTransferInfo(PaymentInfoRequest req) {
        String maHoaDon = (req != null && req.getMaHoaDon() != null && !req.getMaHoaDon().trim().isEmpty())
                ? req.getMaHoaDon().trim() : "POS";
        BigDecimal soTien = (req != null && req.getSoTien() != null)
                ? req.getSoTien() : BigDecimal.ZERO;

        String bankId = (req != null && req.getBankId() != null && !req.getBankId().trim().isEmpty())
                ? req.getBankId().trim() : DEFAULT_BANK_ID;
        String accountNo = (req != null && req.getAccountNo() != null && !req.getAccountNo().trim().isEmpty())
                ? req.getAccountNo().trim() : DEFAULT_ACCOUNT_NO;
        String accountName = (req != null && req.getAccountName() != null && !req.getAccountName().trim().isEmpty())
                ? req.getAccountName().trim() : DEFAULT_ACCOUNT_NAME;

        // Nội dung chuyển khoản chuẩn hóa không dấu: THANHTOAN <MA_HOA_DON>
        String cleanCode = maHoaDon.replaceAll("[^a-zA-Z0-9_-]", "");
        String noiDung = (req != null && req.getNoiDung() != null && !req.getNoiDung().trim().isEmpty())
                ? req.getNoiDung().trim() : ("THANHTOAN " + cleanCode);

        // Sinh mã giao dịch duy nhất
        String maGiaoDich = "TRANS-" + cleanCode + "-" + (System.currentTimeMillis() % 1000000);

        // Tạo VietQR Quick Link định dạng chuẩn Napas 247
        long roundedAmount = Math.max(0, soTien.longValue());
        String encodedContent = URLEncoder.encode(noiDung, StandardCharsets.UTF_8);
        String encodedAccName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        String qrCodeUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bankId, accountNo, roundedAmount, encodedContent, encodedAccName
        );

        // Kiểm tra xem đơn hàng đã được thanh toán chưa
        boolean daThanhToan = false;
        PaymentStatusResponse cached = transactionCache.get(maHoaDon);
        if (cached != null && Boolean.TRUE.equals(cached.getDaThanhToan())) {
            daThanhToan = true;
        } else {
            Optional<HoaDon> hdOpt = hoaDonRepository.findByMaHoaDon(maHoaDon);
            if (hdOpt.isPresent() && Integer.valueOf(1).equals(hdOpt.get().getTrangThaiThanhToan())) {
                daThanhToan = true;
            }
        }

        // Sinh đường link thanh toán qua cổng VNPAY
        String vnpayUrl = createVNPayPaymentUrl(cleanCode, soTien, "127.0.0.1");

        return PaymentInfoResponse.builder()
                .maHoaDon(maHoaDon)
                .soTien(soTien)
                .nganHang("VNPAY-QR")
                .maNganHang("VNPAY")
                .soTaiKhoan(accountNo)
                .tenChuTaiKhoan(accountName)
                .noiDungChuyenKhoan(noiDung)
                .maGiaoDich(maGiaoDich)
                .qrCodeUrl(qrCodeUrl)
                .vnpayUrl(vnpayUrl)
                .daThanhToan(daThanhToan)
                .thoiGianTao(Instant.now())
                .build();
    }

    /**
     * Kiểm tra trạng thái thanh toán của hóa đơn
     */
    public PaymentStatusResponse checkPaymentStatus(String maHoaDon) {
        if (maHoaDon == null || maHoaDon.trim().isEmpty()) {
            return PaymentStatusResponse.builder()
                    .maHoaDon("")
                    .daThanhToan(false)
                    .trangThaiThanhToan(0)
                    .message("Mã hóa đơn không hợp lệ")
                    .build();
        }

        String code = maHoaDon.trim();

        // 1. Kiểm tra trong bộ nhớ tạm nhanh (hỗ trợ cả chữ hoa/thường và mã đã chuẩn hóa)
        PaymentStatusResponse cached = transactionCache.get(code);
        if (cached == null) {
            cached = transactionCache.get(code.toUpperCase());
        }
        if (cached == null) {
            String clean = code.replaceAll("[^a-zA-Z0-9_-]", "");
            cached = transactionCache.get(clean);
        }
        if (cached != null && Boolean.TRUE.equals(cached.getDaThanhToan())) {
            return cached;
        }

        // 2. Kiểm tra trong cơ sở dữ liệu
        Optional<HoaDon> hdOpt = hoaDonRepository.findByMaHoaDon(code);
        if (!hdOpt.isPresent()) {
            hdOpt = hoaDonRepository.findByMaHoaDon(code.toUpperCase());
        }
        if (hdOpt.isPresent()) {
            HoaDon hd = hdOpt.get();
            if (Integer.valueOf(1).equals(hd.getTrangThaiThanhToan())) {
                // Đã thanh toán trong DB
                List<HinhThucThanhToan> htttList = hinhThucThanhToanRepository.findByIdHoaDon_Id(hd.getId());
                String transCode = "TRANS-" + code;
                BigDecimal receivedAmount = hd.getTienSauGiamGia() != null ? hd.getTienSauGiamGia() : hd.getTongTien();
                Instant payTime = hd.getNgayCapNhat() != null ? hd.getNgayCapNhat() : Instant.now();

                if (htttList != null && !htttList.isEmpty()) {
                    HinhThucThanhToan first = htttList.get(0);
                    if (first.getMaGiaoDich() != null) transCode = first.getMaGiaoDich();
                    if (first.getSoTien() != null) receivedAmount = first.getSoTien();
                    if (first.getThoiGian() != null) payTime = first.getThoiGian();
                }

                PaymentStatusResponse res = PaymentStatusResponse.builder()
                        .maHoaDon(code)
                        .daThanhToan(true)
                        .trangThaiThanhToan(1)
                        .soTienDaNhan(receivedAmount)
                        .maGiaoDich(transCode)
                        .thoiGianThanhToan(payTime)
                        .message("Đã nhận thanh toán thành công")
                        .build();
                transactionCache.put(code, res);
                transactionCache.put(code.toUpperCase(), res);
                return res;
            }
        }

        return PaymentStatusResponse.builder()
                .maHoaDon(code)
                .daThanhToan(false)
                .trangThaiThanhToan(0)
                .message("Đang chờ khách hàng quét mã và thanh toán...")
                .build();
    }

    /**
     * Xác nhận thanh toán chuyển khoản (Thủ công hoặc qua Webhook ngân hàng)
     */
    @Transactional
    public PaymentStatusResponse confirmPayment(ConfirmPaymentRequest req) {
        if (req == null || req.getMaHoaDon() == null || req.getMaHoaDon().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã hóa đơn không được để trống");
        }

        String code = req.getMaHoaDon().trim();
        BigDecimal amount = req.getSoTien() != null ? req.getSoTien() : BigDecimal.ZERO;
        String transCode = (req.getMaGiaoDich() != null && !req.getMaGiaoDich().trim().isEmpty())
                ? req.getMaGiaoDich().trim() : ("TRANS-" + code + "-" + (System.currentTimeMillis() % 1000000));
        Instant now = Instant.now();

        // 1. Lưu vào transaction cache (cả code gốc, chữ hoa, và mã chuẩn hóa)
        PaymentStatusResponse status = PaymentStatusResponse.builder()
                .maHoaDon(code)
                .daThanhToan(true)
                .trangThaiThanhToan(1)
                .soTienDaNhan(amount)
                .maGiaoDich(transCode)
                .thoiGianThanhToan(now)
                .message("Xác nhận thanh toán chuyển khoản thành công!")
                .build();
        transactionCache.put(code, status);
        transactionCache.put(code.toUpperCase(), status);
        String clean = code.replaceAll("[^a-zA-Z0-9_-]", "");
        if (!clean.isEmpty()) {
            transactionCache.put(clean, status);
            transactionCache.put(clean.toUpperCase(), status);
        }

        // 2. Nếu hóa đơn đã tồn tại trong DB, cập nhật hóa đơn và lưu hình thức thanh toán
        Optional<HoaDon> hdOpt = hoaDonRepository.findByMaHoaDon(code);
        if (!hdOpt.isPresent()) {
            hdOpt = hoaDonRepository.findByMaHoaDon(code.toUpperCase());
        }
        if (hdOpt.isPresent()) {
            HoaDon hd = hdOpt.get();
            hd.setTrangThaiThanhToan(1);
            hd.setNgayCapNhat(now);
            hoaDonRepository.save(hd);

            // Tìm phương thức chuyển khoản
            PhuongThucThanhToan ptCk = null;
            List<PhuongThucThanhToan> allPts = phuongThucThanhToanRepository.findAll();
            for (PhuongThucThanhToan pt : allPts) {
                if (pt.getTenPhuongThuc() != null) {
                    String t = pt.getTenPhuongThuc().toLowerCase();
                    if (t.contains("chuyển khoản") || t.contains("ngân hàng") || t.contains("chuyen khoan")) {
                        ptCk = pt;
                        break;
                    }
                }
            }
            if (ptCk == null && !allPts.isEmpty()) {
                ptCk = allPts.get(0);
            }

            if (ptCk != null) {
                HinhThucThanhToan httt = new HinhThucThanhToan();
                httt.setIdHoaDon(hd);
                httt.setIdPhuongThuc(ptCk);
                httt.setSoTien(amount.compareTo(BigDecimal.ZERO) > 0 ? amount : (hd.getTienSauGiamGia() != null ? hd.getTienSauGiamGia() : hd.getTongTien()));
                httt.setMaGiaoDich(transCode);
                httt.setThoiGian(now);
                httt.setTrangThai(1);
                hinhThucThanhToanRepository.save(httt);
            }

            // Ghi nhận lịch sử đơn hàng
            try {
                LichSuHoaDon log = new LichSuHoaDon();
                log.setIdHoaDon(hd);
                log.setThoiGian(now);
                log.setHanhDong("Thanh toán chuyển khoản thành công qua cổng VietQR (" + transCode + ")");
                log.setNguoiThucHien("Hệ thống thanh toán");
                log.setVaiTroNguoiThucHien("Hệ thống");
                lichSuHoaDonRepository.save(log);
            } catch (Exception ignored) {}
        }

        return status;
    }

    /**
     * Xử lý webhook từ các cổng thanh toán ngân hàng (Casso, SePay, Bank SMS, Generic Webhook)
     */
    @Transactional
    public PaymentStatusResponse processBankWebhook(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Dữ liệu webhook trống");
        }

        String content = "";
        BigDecimal amount = BigDecimal.ZERO;
        String transNo = "WEBHOOK-" + System.currentTimeMillis();

        // 1. Định dạng Casso: { data: [ { description, amount, tid, ... } ] }
        if (payload.containsKey("data") && payload.get("data") instanceof List) {
            List<?> list = (List<?>) payload.get("data");
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                Map<?, ?> first = (Map<?, ?>) list.get(0);
                if (first.get("description") != null) content = String.valueOf(first.get("description"));
                if (first.get("amount") != null) amount = new BigDecimal(String.valueOf(first.get("amount")));
                if (first.get("tid") != null) transNo = String.valueOf(first.get("tid"));
            }
        }
        // 2. Định dạng SePay: { content, transferAmount / amountIn, referenceCode, ... }
        else if (payload.containsKey("content") || payload.containsKey("transferAmount") || payload.containsKey("amountIn")) {
            if (payload.get("content") != null) content = String.valueOf(payload.get("content"));
            Object amtObj = payload.get("transferAmount") != null ? payload.get("transferAmount") : payload.get("amountIn");
            if (amtObj != null) amount = new BigDecimal(String.valueOf(amtObj));
            if (payload.get("referenceCode") != null) transNo = String.valueOf(payload.get("referenceCode"));
        }
        // 3. Định dạng trực tiếp: { maHoaDon, soTien, ... }
        else {
            if (payload.get("maHoaDon") != null) content = String.valueOf(payload.get("maHoaDon"));
            else if (payload.get("noiDung") != null) content = String.valueOf(payload.get("noiDung"));
            else if (payload.get("description") != null) content = String.valueOf(payload.get("description"));

            Object amtObj = payload.get("soTien") != null ? payload.get("soTien") : payload.get("amount");
            if (amtObj != null) amount = new BigDecimal(String.valueOf(amtObj));
            if (payload.get("maGiaoDich") != null) transNo = String.valueOf(payload.get("maGiaoDich"));
        }

        // Tách mã hóa đơn từ nội dung chuyển khoản
        String extractedCode = extractInvoiceCodeFromContent(content);
        if (extractedCode == null || extractedCode.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy mã hóa đơn hợp lệ trong nội dung chuyển khoản: " + content);
        }

        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setMaHoaDon(extractedCode);
        req.setSoTien(amount);
        req.setMaGiaoDich(transNo);
        req.setGhiChu("Tự động xác nhận qua Bank Webhook: " + content);
        req.setPhuongThuc("Chuyển khoản (Webhook)");

        return confirmPayment(req);
    }

    /**
     * Trích xuất mã hóa đơn từ chuỗi nội dung chuyển khoản (VD: 'THANHTOAN HD0001', 'HD10005', 'VNPAY POS002')
     */
    public String extractInvoiceCodeFromContent(String content) {
        if (content == null || content.trim().isEmpty()) return null;
        String upper = content.toUpperCase();

        // Match regex: tìm mẫu mã hóa đơn HDxxxxx hoặc POSxxxxx hoặc HD-xxxxx
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(HD[-_]?[0-9A-Z]+|POS[-_]?[0-9A-Z]+)");
        java.util.regex.Matcher matcher = pattern.matcher(upper);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Nếu không khớp regex, kiểm tra xem có chuỗi nào khớp trực tiếp với hóa đơn trong DB không
        List<HoaDon> allWaiting = hoaDonRepository.findAll();
        for (HoaDon hd : allWaiting) {
            if (hd.getMaHoaDon() != null && !hd.getMaHoaDon().isEmpty()) {
                String cleanHd = hd.getMaHoaDon().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
                String cleanContent = upper.replaceAll("[^a-zA-Z0-9]", "");
                if (cleanContent.contains(cleanHd)) {
                    return hd.getMaHoaDon();
                }
            }
        }
        return null;
    }

    /**
     * Xử lý IPN từ VNPAY
     */
    @Transactional
    public PaymentStatusResponse processVNPayIpn(Map<String, String> vnpParams) {
        if (vnpParams == null || vnpParams.isEmpty()) {
            throw new IllegalArgumentException("Dữ liệu VNPAY IPN trống");
        }

        String vnpResponseCode = vnpParams.get("vnp_ResponseCode");
        String vnpTxnRef = vnpParams.get("vnp_TxnRef");
        String vnpAmountStr = vnpParams.get("vnp_Amount");
        String vnpTransNo = vnpParams.get("vnp_TransactionNo");

        // "00" là thành công
        if ("00".equals(vnpResponseCode)) {
            String maHoaDon = vnpTxnRef;
            if (vnpTxnRef != null && vnpTxnRef.contains("_")) {
                maHoaDon = vnpTxnRef.substring(0, vnpTxnRef.indexOf("_"));
            }

            BigDecimal amount = BigDecimal.ZERO;
            if (vnpAmountStr != null && !vnpAmountStr.isEmpty()) {
                try {
                    amount = new BigDecimal(vnpAmountStr).divide(BigDecimal.valueOf(100));
                } catch (Exception ignored) {}
            }

            ConfirmPaymentRequest req = new ConfirmPaymentRequest();
            req.setMaHoaDon(maHoaDon);
            req.setSoTien(amount);
            req.setMaGiaoDich("VNPAY-" + (vnpTransNo != null ? vnpTransNo : System.currentTimeMillis()));
            req.setGhiChu("Thanh toán thành công qua cổng VNPAY (IPN)");
            req.setPhuongThuc("VNPAY-QR");

            return confirmPayment(req);
        } else {
            throw new IllegalStateException("Giao dịch VNPAY không thành công, mã lỗi: " + vnpResponseCode);
        }
    }

    /**
     * Hủy / Reset trạng thái thanh toán chuyển khoản cho hóa đơn
     */
    public void resetPaymentStatus(String maHoaDon) {
        if (maHoaDon != null) {
            String code = maHoaDon.trim();
            transactionCache.remove(code);
            transactionCache.remove(code.toUpperCase());
            String clean = code.replaceAll("[^a-zA-Z0-9_-]", "");
            if (!clean.isEmpty()) {
                transactionCache.remove(clean);
                transactionCache.remove(clean.toUpperCase());
            }
        }
    }

    /**
     * Tạo đường dẫn thanh toán qua cổng VNPAY Sandbox
     */
    public String createVNPayPaymentUrl(String maHoaDon, BigDecimal amount, String ipAddress) {
        try {
            long vnpAmount = (amount != null ? amount.max(BigDecimal.ZERO) : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(100)).longValue();
            String cleanCode = (maHoaDon != null) ? maHoaDon.replaceAll("[^a-zA-Z0-9_-]", "") : "POS";
            String vnpTxnRef = cleanCode + "_" + System.currentTimeMillis();

            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", VNP_TMN_CODE);
            vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", vnpTxnRef);
            vnpParams.put("vnp_OrderInfo", "Thanh toan hoa don " + cleanCode);
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", "http://localhost:5173/ban-hang");
            vnpParams.put("vnp_IpAddr", ipAddress != null && !ipAddress.isEmpty() ? ipAddress : "127.0.0.1");

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnpCreateDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_CreateDate", vnpCreateDate);

            cld.add(Calendar.MINUTE, 15);
            String vnpExpireDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_ExpireDate", vnpExpireDate);

            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnpParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            String queryUrl = query.toString();
            String vnpSecureHash = hmacSHA512(VNP_HASH_SECRET, hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
            return VNP_PAY_URL + "?" + queryUrl;
        } catch (Exception ex) {
            return VNP_PAY_URL;
        }
    }

    private String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) return "";
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}

