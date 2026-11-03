package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.KhachHangRequest;
import com.example.sd009thuan.dto.KhachHangResponse;
import com.example.sd009thuan.service.ExcelExportService;
import com.example.sd009thuan.service.KhachHangService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.sd009thuan.service.EmailService;
import com.example.sd009thuan.entity.KhachHang;

@RestController
@RequestMapping("/api/khach-hang")
public class KhachHangController {
    private final KhachHangService service;
    private final ExcelExportService excelExportService;

    public KhachHangController(KhachHangService service, ExcelExportService excelExportService) {
        this.service = service;
        this.excelExportService = excelExportService;
    }

    @GetMapping
    public Page<KhachHangResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
        return service.search(keyword, trangThai, pageable);
    }

    @GetMapping({"/export-excel", "/export"})
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer trangThai) {
        byte[] bytes = excelExportService.exportCustomers(service.findAllForExport(keyword, trangThai));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("danh-sach-khach-hang.xlsx").build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/next-code")
    public ResponseEntity<Map<String, String>> nextCode() {
        return ResponseEntity.ok(Map.of("code", service.nextCode()));
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(
            @RequestParam String email,
            @RequestParam(required = false) Long id) {
        boolean exists = service.checkEmailExists(email, id);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/check-phone")
    public ResponseEntity<Map<String, Boolean>> checkPhone(
            @RequestParam String phone,
            @RequestParam(required = false) Long id) {
        boolean exists = service.checkPhoneExists(phone, id);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/{id}")
    public KhachHangResponse get(@PathVariable String id) {
        return service.getByIdentifier(id);
    }

    @PostMapping
    public ResponseEntity<KhachHangResponse> create(@RequestBody KhachHangRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public KhachHangResponse update(@PathVariable String id, @RequestBody KhachHangRequest req) {
        com.example.sd009thuan.entity.KhachHang customer = service.findByIdentifier(id);
        return service.update(customer.getId(), req);
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Void> toggle(@PathVariable String id) {
        com.example.sd009thuan.entity.KhachHang customer = service.findByIdentifier(id);
        service.toggleStatus(customer.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        com.example.sd009thuan.entity.KhachHang customer = service.findByIdentifier(id);
        service.deactivate(customer.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/dia-chi")
    public List<com.example.sd009thuan.dto.DiaChiKhachHangDto> getAddresses(@PathVariable String id) {
        return service.getAddresses(service.findByIdentifier(id).getId());
    }

    @PostMapping("/{id}/dia-chi")
    public ResponseEntity<com.example.sd009thuan.dto.DiaChiKhachHangDto> addAddress(
            @PathVariable String id,
            @RequestBody com.example.sd009thuan.dto.DiaChiKhachHangDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addAddress(service.findByIdentifier(id).getId(), dto));
    }

    @PutMapping("/{id}/dia-chi/{addressId}")
    public com.example.sd009thuan.dto.DiaChiKhachHangDto updateAddress(
            @PathVariable String id,
            @PathVariable Long addressId,
            @RequestBody com.example.sd009thuan.dto.DiaChiKhachHangDto dto) {
        return service.updateAddress(service.findByIdentifier(id).getId(), addressId, dto);
    }

    @PutMapping("/{id}/dia-chi/{addressId}/mac-dinh")
    public ResponseEntity<Void> setDefaultAddress(
            @PathVariable String id,
            @PathVariable Long addressId) {
        service.setDefaultAddress(service.findByIdentifier(id).getId(), addressId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/dia-chi/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable String id,
            @PathVariable Long addressId) {
        service.deleteAddress(service.findByIdentifier(id).getId(), addressId);
        return ResponseEntity.noContent().build();
    }

    @Autowired(required = false)
    private EmailService emailService;

    private static class OtpEntry {
        String code;
        long expireAt;

        OtpEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }

    private final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> forgotOtpCache = new ConcurrentHashMap<>();

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email != null ? email : "";
        int atIndex = email.indexOf("@");
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 3) {
            return name.charAt(0) + "***" + domain;
        }
        return name.substring(0, 3) + "***" + domain;
    }

    @PostMapping({"/gui-otp", "/send-otp"})
    public ResponseEntity<?> guiOtp(@RequestBody Map<String, String> body) {
        String type = body != null ? (body.get("type") != null ? body.get("type") : body.get("purpose")) : null;
        if (type != null && ("forgot".equalsIgnoreCase(type) || "quen-mat-khau".equalsIgnoreCase(type) || "reset".equalsIgnoreCase(type))) {
            return quenMatKhauGuiOtp(body);
        }

        String email = body != null ? body.get("email") : null;
        String fullName = body != null ? body.get("fullName") : null;
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email không hợp lệ!"));
        }
        email = email.trim().toLowerCase();

        // Kiểm tra xem email đã được đăng ký tài khoản chưa
        if (service.checkEmailExists(email, null)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email '" + email + "' đã tồn tại trong hệ thống. Vui lòng chuyển sang tab Đăng nhập!"
            ));
        }

        // Tạo mã OTP 6 chữ số ngẫu nhiên
        int randomOtp = 100000 + new java.util.Random().nextInt(900000);
        String otpCode = String.valueOf(randomOtp);

        // Lưu vào cache có hạn 10 phút
        otpCache.put(email, new OtpEntry(otpCode, System.currentTimeMillis() + 10 * 60 * 1000L));

        boolean sent = emailService != null && emailService.sendOtpVerificationEmail(email, fullName, otpCode);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mã xác thực 6 số đã được gửi tới email " + email + ". Vui lòng kiểm tra hộp thư đến hoặc hòm thư Spam!",
                "otp", otpCode
        ));
    }

    @PostMapping({"/quen-mat-khau/gui-otp", "/forgot-password/send-otp"})
    public ResponseEntity<?> quenMatKhauGuiOtp(@RequestBody Map<String, String> body) {
        String rawInput = body != null ? (
                body.get("emailOrPhone") != null ? body.get("emailOrPhone") :
                body.get("email") != null ? body.get("email") :
                body.get("taiKhoan") != null ? body.get("taiKhoan") :
                body.get("phoneOrEmail") != null ? body.get("phoneOrEmail") :
                body.get("phone")
        ) : null;

        if (rawInput == null || rawInput.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Vui lòng nhập Email hoặc Số điện thoại tài khoản để nhận mã OTP!"
            ));
        }

        String searchKey = rawInput.trim();
        Optional<KhachHang> khOpt = Optional.empty();

        if (searchKey.contains("@")) {
            khOpt = service.findByEmailIgnoreCase(searchKey)
                    .or(() -> service.findByEmail(searchKey));
        } else {
            khOpt = service.findByPhone(searchKey)
                    .or(() -> service.findByTaiKhoanIgnoreCase(searchKey));
        }

        if (khOpt.isEmpty()) {
            try {
                khOpt = Optional.ofNullable(service.findByIdentifier(searchKey));
            } catch (Exception ignored) {}
        }

        if (khOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Tài khoản không tồn tại trên hệ thống. Vui lòng kiểm tra lại email hoặc số điện thoại!"
            ));
        }

        KhachHang kh = khOpt.get();

        if (kh.getTrangThai() != null && kh.getTrangThai() == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Tài khoản này đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ Quản trị viên!"
            ));
        }

        String targetEmail = kh.getEmail() != null ? kh.getEmail().trim().toLowerCase() : null;
        if (targetEmail == null || targetEmail.isEmpty() || !targetEmail.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Tài khoản khách hàng '" + (kh.getTenKhachHang() != null ? kh.getTenKhachHang() : searchKey) + "' chưa cập nhật địa chỉ Email để nhận mã OTP. Vui lòng liên hệ Hotline để được hỗ trợ!"
            ));
        }

        // Tạo mã OTP 6 chữ số
        int randomOtp = 100000 + new java.util.Random().nextInt(900000);
        String otpCode = String.valueOf(randomOtp);

        OtpEntry entry = new OtpEntry(otpCode, System.currentTimeMillis() + 10 * 60 * 1000L);
        forgotOtpCache.put(targetEmail, entry);
        otpCache.put(targetEmail, entry);
        if (kh.getSoDienThoai() != null && !kh.getSoDienThoai().trim().isEmpty()) {
            forgotOtpCache.put(kh.getSoDienThoai().trim(), entry);
            otpCache.put(kh.getSoDienThoai().trim(), entry);
        }
        if (!searchKey.equalsIgnoreCase(targetEmail)) {
            forgotOtpCache.put(searchKey.toLowerCase(), entry);
            otpCache.put(searchKey.toLowerCase(), entry);
        }

        boolean sent = emailService != null && emailService.sendPasswordResetOtpEmail(targetEmail, kh.getTenKhachHang(), otpCode);
        String masked = maskEmail(targetEmail);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mã xác thực OTP 6 số đã được gửi tới email " + masked + ". Mã có hiệu lực trong vòng 10 phút.",
                "email", targetEmail,
                "maskedEmail", masked,
                "fullName", kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "",
                "otp", otpCode
        ));
    }

    @PostMapping({"/xac-thuc-otp", "/verify-otp"})
    public ResponseEntity<?> xacThucOtp(@RequestBody Map<String, String> body) {
        String email = body != null ? (body.get("email") != null ? body.get("email") : body.get("phoneOrEmail")) : null;
        String otp = body != null ? (body.get("otp") != null ? body.get("otp") : body.get("otpCode")) : null;
        String newPass = body != null ? (body.get("matKhauMoi") != null ? body.get("matKhauMoi") : (body.get("newPassword") != null ? body.get("newPassword") : body.get("password"))) : null;

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Vui lòng nhập email và mã xác thực!"));
        }
        email = email.trim().toLowerCase();
        otp = otp.trim();

        OtpEntry entry = forgotOtpCache.get(email);
        if (entry == null) {
            entry = otpCache.get(email);
        }

        if (entry == null) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Mã xác thực không tồn tại hoặc đã hết hạn! Vui lòng nhấn gửi lại mã."));
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            forgotOtpCache.remove(email);
            otpCache.remove(email);
            return ResponseEntity.ok(Map.of("valid", false, "message", "Mã xác thực đã hết hạn! Vui lòng nhấn gửi lại mã mới."));
        }
        if (!entry.code.equals(otp)) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Mã xác thực OTP không chính xác! Vui lòng kiểm tra lại."));
        }

        // Nếu có truyền mật khẩu mới -> cập nhật luôn vào DB
        if (newPass != null && !newPass.trim().isEmpty()) {
            if (newPass.trim().length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Mật khẩu mới phải có tối thiểu 6 ký tự!"));
            }
            final String finalEmail = email;
            Optional<KhachHang> khOpt = service.findByEmailIgnoreCase(finalEmail)
                    .or(() -> service.findByEmail(finalEmail))
                    .or(() -> service.findByPhone(finalEmail));
            if (khOpt.isPresent()) {
                KhachHang kh = khOpt.get();
                service.updatePassword(kh.getId(), newPass.trim());
            }
            forgotOtpCache.remove(email);
            otpCache.remove(email);
            return ResponseEntity.ok(Map.of("valid", true, "message", "Đổi mật khẩu thành công!"));
        }

        // Bước 2 chỉ xác thực OTP: Giữ lại OTP trong cache để bước 3 đặt lại mật khẩu mới gọi datLaiMatKhau thành công
        return ResponseEntity.ok(Map.of("valid", true, "success", true, "message", "Xác thực mã OTP thành công!"));
    }

    @PostMapping({"/quen-mat-khau/dat-lai-mat-khau", "/forgot-password/reset"})
    public ResponseEntity<?> datLaiMatKhau(@RequestBody Map<String, String> body) {
        String email = body != null ? (body.get("email") != null ? body.get("email") : body.get("phoneOrEmail")) : null;
        String otp = body != null ? (body.get("otp") != null ? body.get("otp") : body.get("otpCode")) : null;
        String newPass = body != null ? (body.get("matKhauMoi") != null ? body.get("matKhauMoi") : (body.get("newPassword") != null ? body.get("newPassword") : body.get("password"))) : null;

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng nhập Email hoặc Số điện thoại!"));
        }
        if (otp == null || otp.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng nhập mã OTP!"));
        }
        if (newPass == null || newPass.trim().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mật khẩu mới phải có tối thiểu 6 ký tự!"));
        }

        String searchEmail = email.trim().toLowerCase();
        OtpEntry entry = forgotOtpCache.get(searchEmail);
        if (entry == null) {
            entry = otpCache.get(searchEmail);
        }
        if (entry == null) {
            Optional<KhachHang> lookupKh = service.findByEmailIgnoreCase(searchEmail)
                    .or(() -> service.findByEmail(searchEmail))
                    .or(() -> service.findByPhone(searchEmail));
            if (lookupKh.isPresent()) {
                KhachHang k = lookupKh.get();
                if (k.getEmail() != null) {
                    entry = forgotOtpCache.get(k.getEmail().trim().toLowerCase());
                    if (entry == null) entry = otpCache.get(k.getEmail().trim().toLowerCase());
                }
                if (entry == null && k.getSoDienThoai() != null) {
                    entry = forgotOtpCache.get(k.getSoDienThoai().trim());
                    if (entry == null) entry = otpCache.get(k.getSoDienThoai().trim());
                }
            }
        }

        if (entry == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mã xác thực không tồn tại hoặc đã hết hạn! Vui lòng nhấn gửi lại mã OTP."));
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            forgotOtpCache.remove(searchEmail);
            otpCache.remove(searchEmail);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mã xác thực OTP đã hết hạn! Vui lòng nhấn gửi lại mã mới."));
        }
        if (!entry.code.equals(otp.trim())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mã xác thực OTP không chính xác! Vui lòng kiểm tra lại."));
        }

        Optional<KhachHang> khOpt = service.findByEmailIgnoreCase(searchEmail)
                .or(() -> service.findByEmail(searchEmail))
                .or(() -> service.findByPhone(searchEmail));

        if (khOpt.isEmpty()) {
            try {
                khOpt = Optional.ofNullable(service.findByIdentifier(searchEmail));
            } catch (Exception ignored) {}
        }

        if (khOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy thông tin tài khoản khách hàng!"));
        }

        KhachHang kh = khOpt.get();
        service.updatePassword(kh.getId(), newPass.trim());

        forgotOtpCache.remove(searchEmail);
        otpCache.remove(searchEmail);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập ngay với mật khẩu mới.",
                "email", searchEmail,
                "fullName", kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "",
                "loginIdentifier", kh.getEmail() != null ? kh.getEmail() : (kh.getSoDienThoai() != null ? kh.getSoDienThoai() : searchEmail)
        ));
    }

    @PostMapping({"/dang-nhap", "/login"})
    public ResponseEntity<?> dangNhap(@RequestBody Map<String, String> body) {
        String identifier = body != null ? body.get("taiKhoan") : null;
        if (identifier == null || identifier.trim().isEmpty()) {
            identifier = body != null ? body.get("phoneOrEmail") : null;
        }
        String matKhau = body != null ? body.get("matKhau") : null;
        if (matKhau == null) {
            matKhau = body != null ? body.get("password") : null;
        }

        if (identifier == null || identifier.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng nhập Email hoặc Số điện thoại!"));
        }
        if (matKhau == null || matKhau.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng nhập mật khẩu!"));
        }

        String searchKey = identifier.trim();
        Optional<KhachHang> optCustomer = Optional.empty();

        if (searchKey.contains("@")) {
            optCustomer = service.findByEmailIgnoreCase(searchKey)
                    .or(() -> service.findByEmail(searchKey));
        } else {
            optCustomer = service.findByPhone(searchKey)
                    .or(() -> service.findByTaiKhoanIgnoreCase(searchKey));
        }

        if (optCustomer.isEmpty()) {
            try {
                optCustomer = Optional.ofNullable(service.findByIdentifier(searchKey));
            } catch (Exception ignored) {}
        }

        if (optCustomer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Tài khoản không tồn tại trên hệ thống. Vui lòng kiểm tra lại hoặc chuyển sang tab Đăng ký!"
            ));
        }

        KhachHang customer = optCustomer.get();

        if (customer.getTrangThai() != null && customer.getTrangThai() == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Tài khoản này đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ quản trị viên!"
            ));
        }

        // Kiểm tra mật khẩu
        String savedPass = customer.getMatKhau();
        String inputPass = matKhau.trim();
        if (savedPass == null || savedPass.trim().isEmpty()) {
            // Mật khẩu mặc định hệ thống nếu chưa khởi tạo là 123456
            if ("123456".equals(inputPass)) {
                service.updatePassword(customer.getId(), "123456");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Mật khẩu không chính xác! (Mật khẩu mặc định của tài khoản là 123456, hoặc bạn có thể dùng tính năng 'Quên mật khẩu' để tạo mới)."
                ));
            }
        } else {
            if (!savedPass.trim().equals(inputPass)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Mật khẩu không chính xác! Vui lòng thử lại hoặc chọn 'Quên mật khẩu'."
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đăng nhập thành công!",
                "customer", service.toResponse(customer)
        ));
    }

    @PostMapping({"/dang-nhap-google", "/login-google"})
    public ResponseEntity<?> dangNhapGoogle(@RequestBody Map<String, String> body) {
        String input = body != null ? (body.get("email") != null ? body.get("email") : body.get("identifier")) : null;
        String name = body != null ? body.get("name") : null;
        if (input == null || input.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng nhập Email hoặc Số điện thoại tài khoản Google!"));
        }
        String cleanInput = input.trim();
        Optional<KhachHang> optCustomer = Optional.empty();

        if (cleanInput.contains("@")) {
            optCustomer = service.findByEmail(cleanInput.toLowerCase());
        } else {
            optCustomer = service.findByPhone(cleanInput);
            if (optCustomer.isEmpty()) {
                optCustomer = service.findByEmail(cleanInput.toLowerCase() + "@gmail.com");
            }
        }

        if (optCustomer.isEmpty()) {
            try {
                optCustomer = Optional.ofNullable(service.findByIdentifier(cleanInput));
            } catch (Exception ignored) {}
        }

        KhachHang customer;
        if (optCustomer.isPresent()) {
            customer = optCustomer.get();
        } else {
            String emailToSave = cleanInput.contains("@") ? cleanInput.toLowerCase() : (cleanInput + "@gmail.com");
            String displayName = (name != null && !name.trim().isEmpty()) ? name.trim() : emailToSave.split("@")[0];
            String phoneToSave = cleanInput.matches("^0[0-9]{9,10}$") ? cleanInput : null;
            KhachHangRequest req = new KhachHangRequest(
                    null,
                    emailToSave.split("@")[0],
                    displayName,
                    emailToSave,
                    "123456",
                    phoneToSave,
                    null,
                    true,
                    1,
                    null, null, null, null, null, null
            );
            KhachHangResponse resp = service.create(req);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đăng nhập bằng Google thành công!",
                    "customer", resp
            ));
        }

        if (customer.getTrangThai() != null && customer.getTrangThai() == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Tài khoản này đã bị khóa. Vui lòng liên hệ hỗ trợ!"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đăng nhập bằng Google thành công!",
                "customer", service.toResponse(customer)
        ));
    }

}
