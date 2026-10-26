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

    @PostMapping({"/gui-otp", "/send-otp"})
    public ResponseEntity<?> guiOtp(@RequestBody Map<String, String> body) {
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

    @PostMapping({"/xac-thuc-otp", "/verify-otp"})
    public ResponseEntity<?> xacThucOtp(@RequestBody Map<String, String> body) {
        String email = body != null ? body.get("email") : null;
        String otp = body != null ? body.get("otp") : null;
        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Vui lòng nhập email và mã xác thực!"));
        }
        email = email.trim().toLowerCase();
        otp = otp.trim();

        OtpEntry entry = otpCache.get(email);
        if (entry == null) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Mã xác thực không tồn tại hoặc đã hết hạn! Vui lòng nhấn gửi lại mã."));
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            otpCache.remove(email);
            return ResponseEntity.ok(Map.of("valid", false, "message", "Mã xác thực đã hết hạn! Vui lòng nhấn gửi lại mã mới."));
        }
        if (!entry.code.equals(otp)) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Mã xác thực OTP không chính xác! Vui lòng kiểm tra lại."));
        }

        otpCache.remove(email);
        return ResponseEntity.ok(Map.of("valid", true, "message", "Xác thực mã OTP thành công!"));
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
            optCustomer = service.findByEmail(searchKey);
        } else {
            optCustomer = service.findByPhone(searchKey);
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
        if (savedPass != null && !savedPass.trim().isEmpty()) {
            if (!savedPass.trim().equals(matKhau.trim())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Mật khẩu không chính xác! Vui lòng thử lại."
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
