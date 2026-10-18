package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.BulkEmailRequest;
import com.example.sd009thuan.dto.BulkEmailResponse;
import com.example.sd009thuan.dto.NhanVienRequest;
import com.example.sd009thuan.dto.NhanVienResponse;
import com.example.sd009thuan.entity.NhanVien;
import com.example.sd009thuan.entity.VaiTro;
import com.example.sd009thuan.repository.NhanVienRepository;
import com.example.sd009thuan.repository.VaiTroRepository;
import com.example.sd009thuan.spec.NhanVienSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class NhanVienService {
    private final NhanVienRepository repo;
    private final VaiTroRepository roleRepo;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    public NhanVienService(NhanVienRepository repo, VaiTroRepository roleRepo, FileStorageService fileStorageService, EmailService emailService) {
        this.repo = repo;
        this.roleRepo = roleRepo;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
    }

    @jakarta.annotation.PostConstruct
    @Transactional
    public void normalizeRoles() {
        try {
            List<VaiTro> allRoles = roleRepo.findAll();
            for (VaiTro r : allRoles) {
                if ("ADMIN".equalsIgnoreCase(r.getMaVaiTro())) {
                    r.setTenVaiTro("Quản trị viên");
                    r.setTrangThai(1);
                    roleRepo.save(r);
                } else if ("NV".equalsIgnoreCase(r.getMaVaiTro())) {
                    r.setTenVaiTro("Nhân viên");
                    r.setTrangThai(1);
                    roleRepo.save(r);
                } else {
                    // Chuyển nhân viên có vai trò khác sang vai trò NV (nếu có)
                    VaiTro nvRole = roleRepo.findByMaVaiTroIgnoreCaseAndTrangThai("NV", 1).orElse(null);
                    if (nvRole != null) {
                        List<NhanVien> emps = repo.findAll().stream()
                                .filter(e -> e.getIdVaiTro() != null && e.getIdVaiTro().getId().equals(r.getId()))
                                .toList();
                        for (NhanVien emp : emps) {
                            emp.setIdVaiTro(nvRole);
                            repo.save(emp);
                        }
                    }
                    // Loại bỏ hoàn toàn vai trò này
                    r.setTrangThai(0);
                    roleRepo.save(r);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Transactional(readOnly = true)
    public Page<NhanVienResponse> search(String keyword, Long roleId, Integer status, Pageable pageable) {
        Specification<NhanVien> spec = Specification.where(NhanVienSpecification.keyword(keyword))
                .and(NhanVienSpecification.role(roleId))
                .and(NhanVienSpecification.status(status));
        return repo.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public NhanVienResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public NhanVienResponse create(NhanVienRequest req, MultipartFile file) {
        if (blank(req.tenNhanVien()) == null) {
            throw new IllegalArgumentException("Họ tên nhân viên không được để trống");
        }
        if (blank(req.email()) == null) {
            throw new IllegalArgumentException("Email nhân viên không được để trống");
        }
        if (!req.email().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Email nhân viên không đúng định dạng");
        }
        validateSoDienThoai(req.soDienThoai());
        if (req.gioiTinh() == null) {
            throw new IllegalArgumentException("Vui lòng chọn giới tính cho nhân viên");
        }
        if (req.ngaySinh() == null) {
            throw new IllegalArgumentException("Ngày sinh nhân viên không được để trống");
        }
        if (req.ngaySinh().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không được lớn hơn ngày hiện tại");
        }
        if (java.time.Period.between(req.ngaySinh(), java.time.LocalDate.now()).getYears() < 18) {
            throw new IllegalArgumentException("Nhân viên phải từ đủ 18 tuổi trở lên");
        }
        if (blank(req.queQuan()) == null) {
            throw new IllegalArgumentException("Tỉnh / Thành phố không được để trống");
        }
        if (blank(req.phuong()) == null) {
            throw new IllegalArgumentException("Phường / Xã không được để trống");
        }
        validateUnique(req, null);

        Long roleId = req.idVaiTro();
        if (roleId == null) {
            roleId = roleRepo.findByMaVaiTroIgnoreCaseAndTrangThai("NV", 1)
                    .map(VaiTro::getId)
                    .orElseGet(() -> roleRepo.findByTrangThaiOrderByIdAsc(1).stream()
                            .filter(r -> "ADMIN".equalsIgnoreCase(r.getMaVaiTro()) || "NV".equalsIgnoreCase(r.getMaVaiTro()))
                            .map(VaiTro::getId).findFirst().orElse(null));
        }
        if (roleId == null) {
            throw new IllegalArgumentException("Vui lòng chọn vai trò cho nhân viên");
        }
        VaiTro role = roleRepo.findById(roleId).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy vai trò"));

        NhanVien x = new NhanVien();
        x.setIdVaiTro(role);

        // Mã nhân viên: tên viết tắt tự sinh theo họ tên + số cộng thêm 1 so với mã đã tồn tại
        String code = generateEmployeeCode(req.tenNhanVien());
        x.setMaNhanVien(code);

        // Tên tài khoản và mật khẩu do hệ thống cấp qua email, không thể thay đổi
        String account = code.toLowerCase();
        x.setTenTaiKhoan(account);
        String generatedPassword = generateRandomPassword();
        x.setMatKhau(generatedPassword);

        x.setTenNhanVien(req.tenNhanVien().trim());
        x.setEmail(blank(req.email()));
        x.setSoDienThoai(blank(req.soDienThoai()));
        String imageUrl = fileStorageService.storeEmployeeImage(file);
        x.setAnhNhanVien(imageUrl != null ? imageUrl : blank(req.anhNhanVien()));
        x.setGioiTinh(req.gioiTinh() == null ? true : req.gioiTinh());
        x.setNgaySinh(req.ngaySinh());
        x.setQueQuan(blank(req.queQuan()));
        x.setPhuong(blank(req.phuong()));
        x.setDiaChiCuThe(blank(req.diaChiCuThe()));
        x.setTrangThai(req.trangThai() == null ? 1 : req.trangThai());
        x.setNgayTao(Instant.now());
        x.setNguoiTao("admin");
        x = repo.save(x);

        // Gửi email thông tin tài khoản cho nhân viên
        emailService.sendAccountInfoEmail(x.getEmail(), x.getTenNhanVien(), code, account, generatedPassword);

        return toResponse(x);
    }

    @Transactional
    public NhanVienResponse update(Long id, NhanVienRequest req, MultipartFile file) {
        NhanVien x = find(id);
        validateUnique(req, id);
        if (req.idVaiTro() != null) {
            VaiTro role = roleRepo.findById(req.idVaiTro()).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy vai trò"));
            x.setIdVaiTro(role);
        }
        // Mã nhân viên, tên tài khoản, mật khẩu do hệ thống cấp, KHÔNG THỂ THAY ĐỔI
        if (blank(req.tenNhanVien()) != null) x.setTenNhanVien(req.tenNhanVien().trim());
        if (blank(req.email()) != null) {
            if (!req.email().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("Email nhân viên không đúng định dạng");
            }
            x.setEmail(blank(req.email()));
        }
        if (blank(req.soDienThoai()) != null) {
            validateSoDienThoai(req.soDienThoai());
            x.setSoDienThoai(blank(req.soDienThoai()));
        }
        String oldImage = x.getAnhNhanVien();
        String imageUrl = fileStorageService.storeEmployeeImage(file);
        if (imageUrl != null) {
            x.setAnhNhanVien(imageUrl);
            fileStorageService.deleteIfLocal(oldImage);
        } else if (blank(req.anhNhanVien()) != null) {
            x.setAnhNhanVien(blank(req.anhNhanVien()));
        }
        if (req.gioiTinh() != null) x.setGioiTinh(req.gioiTinh() == null ? true : req.gioiTinh());
        if (req.ngaySinh() != null) x.setNgaySinh(req.ngaySinh());
        if (blank(req.queQuan()) != null) x.setQueQuan(blank(req.queQuan()));
        if (blank(req.phuong()) != null) x.setPhuong(blank(req.phuong()));
        if (blank(req.diaChiCuThe()) != null) x.setDiaChiCuThe(blank(req.diaChiCuThe()));
        if (req.trangThai() != null) x.setTrangThai(req.trangThai());
        x.setNgayCapNhat(Instant.now());
        x.setNguoiCapNhat("admin");
        return toResponse(repo.save(x));
    }

    @Transactional
    public NhanVienResponse update(Long id, NhanVienRequest req, MultipartFile file, Long currentUserId) {
        if (currentUserId != null && currentUserId.equals(id) && req.trangThai() != null && req.trangThai() == 0) {
            throw new IllegalArgumentException("Bạn không thể chuyển trạng thái ngừng hoạt động đối với tài khoản đang đăng nhập hệ thống");
        }
        return update(id, req, file);
    }

    @Transactional
    public void toggleStatus(Long id) {
        NhanVien x = find(id);
        x.setTrangThai(x.getTrangThai() != null && x.getTrangThai() == 1 ? 0 : 1);
        x.setNgayCapNhat(Instant.now());
        x.setNguoiCapNhat("admin");
        repo.save(x);
    }

    @Transactional
    public void toggleStatus(Long id, Long currentUserId) {
        if (currentUserId != null && currentUserId.equals(id)) {
            NhanVien x = find(id);
            if (x.getTrangThai() != null && x.getTrangThai() == 1) {
                throw new IllegalArgumentException("Bạn không thể chuyển trạng thái ngừng hoạt động đối với tài khoản đang đăng nhập hệ thống");
            }
        }
        toggleStatus(id);
    }

    @Transactional
    public void deactivate(Long id) {
        NhanVien x = find(id);
        x.setTrangThai(0);
        x.setNgayCapNhat(Instant.now());
        x.setNguoiCapNhat("admin");
        repo.save(x);
    }

    @Transactional
    public void deactivate(Long id, Long currentUserId) {
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new IllegalArgumentException("Bạn không thể chuyển trạng thái ngừng hoạt động đối với tài khoản đang đăng nhập hệ thống");
        }
        deactivate(id);
    }

    /**
     * Gửi email hàng loạt cho nhân viên
     * RÀNG BUỘC: Chỉ gửi đến những nhân viên đang ở trạng thái Hoạt động (trangThai == 1)
     */
    @Transactional(readOnly = true)
    public BulkEmailResponse sendBulkEmail(BulkEmailRequest req) {
        if (req.employeeIds() == null || req.employeeIds().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một nhân viên để gửi email");
        }

        List<NhanVien> employees = repo.findAllById(req.employeeIds());

        // Lọc nghiêm ngặt: Chỉ những nhân viên ĐANG HOẠT ĐỘNG (trangThai == 1) và có email hợp lệ
        List<NhanVien> activeEmployees = employees.stream()
                .filter(e -> e.getTrangThai() != null && e.getTrangThai() == 1)
                .filter(e -> e.getEmail() != null && !e.getEmail().isBlank())
                .toList();

        if (activeEmployees.isEmpty()) {
            return new BulkEmailResponse(
                    req.employeeIds().size(),
                    0,
                    0,
                    List.of(),
                    "Không tìm thấy nhân viên nào đang hoạt động có địa chỉ email hợp lệ trong danh sách đã chọn."
            );
        }

        int sentCount = 0;
        int failedCount = 0;
        List<String> failedEmails = new ArrayList<>();

        boolean isAccountInfo = "account_info".equalsIgnoreCase(req.emailType());

        for (NhanVien emp : activeEmployees) {
            boolean success;
            if (isAccountInfo) {
                String pwd = (emp.getMatKhau() != null && !emp.getMatKhau().isBlank()) ? emp.getMatKhau() : "NV@123456";
                success = emailService.sendAccountInfoEmail(
                        emp.getEmail(),
                        emp.getTenNhanVien(),
                        emp.getMaNhanVien(),
                        emp.getTenTaiKhoan(),
                        pwd
                );
            } else {
                success = emailService.sendCustomEmail(
                        emp.getEmail(),
                        emp.getTenNhanVien(),
                        req.subject(),
                        req.content()
                );
            }

            if (success) {
                sentCount++;
            } else {
                failedCount++;
                failedEmails.add(emp.getEmail());
            }
        }

        String msg = String.format("Đã gửi email thành công tới %d/%d nhân viên đang hoạt động.", sentCount, activeEmployees.size());
        return new BulkEmailResponse(req.employeeIds().size(), sentCount, failedCount, failedEmails, msg);
    }

    @Transactional(readOnly = true)
    public List<NhanVienResponse> findAllForExport(String keyword, Long roleId, Integer status) {
        Specification<NhanVien> spec = Specification.where(NhanVienSpecification.keyword(keyword))
                .and(NhanVienSpecification.role(roleId))
                .and(NhanVienSpecification.status(status));
        return repo.findAll(spec, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VaiTro> roles() {
        return roleRepo.findByTrangThaiOrderByIdAsc(1).stream()
                .filter(r -> "ADMIN".equalsIgnoreCase(r.getMaVaiTro()) || "NV".equalsIgnoreCase(r.getMaVaiTro()))
                .toList();
    }

    private void validateUnique(NhanVienRequest req, Long currentId) {
        String email = blank(req.email());
        if (email != null) {
            boolean exists = currentId == null
                    ? repo.existsByEmailIgnoreCase(email)
                    : repo.existsByEmailIgnoreCaseAndIdNot(email, currentId);
            if (exists) throw new IllegalArgumentException("Email này đã tồn tại trong hệ thống");
        }
        String phone = blank(req.soDienThoai());
        if (phone != null) {
            boolean exists = currentId == null
                    ? repo.existsBySoDienThoai(phone)
                    : repo.existsBySoDienThoaiAndIdNot(phone, currentId);
            if (exists) throw new IllegalArgumentException("Số điện thoại này đã được sử dụng");
        }
    }

    @Transactional(readOnly = true)
    public boolean checkEmailExists(String email, Long excludeId) {
        String val = blank(email);
        if (val == null) return false;
        return excludeId == null
                ? repo.existsByEmailIgnoreCase(val)
                : repo.existsByEmailIgnoreCaseAndIdNot(val, excludeId);
    }

    @Transactional(readOnly = true)
    public boolean checkPhoneExists(String phone, Long excludeId) {
        String val = blank(phone);
        if (val == null) return false;
        return excludeId == null
                ? repo.existsBySoDienThoai(val)
                : repo.existsBySoDienThoaiAndIdNot(val, excludeId);
    }

    public static String removeAccents(String text) {
        if (text == null) return "";
        String normalized = java.text.Normalizer.normalize(text.trim(), java.text.Normalizer.Form.NFD);
        String pattern = "\\p{InCombiningDiacriticalMarks}+";
        String withoutAccents = normalized.replaceAll(pattern, "");
        return withoutAccents.replace("đ", "d").replace("Đ", "d");
    }

    public static String generateEmployeePrefix(String fullName) {
        if (fullName == null || fullName.isBlank()) return "nv";
        String clean = removeAccents(fullName.toLowerCase()).replaceAll("[^a-z\\s]", " ").trim();
        String[] parts = clean.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "nv";
        if (parts.length == 1) return parts[0];

        String firstName = parts[parts.length - 1];
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (!parts[i].isEmpty()) {
                initials.append(parts[i].charAt(0));
            }
        }
        return firstName + initials.toString();
    }

    public String generateEmployeeCode(String fullName) {
        String prefix = generateEmployeePrefix(fullName);
        List<NhanVien> all = repo.findAll();
        int maxIndex = 0;
        for (NhanVien nv : all) {
            String code = nv.getMaNhanVien();
            if (code != null) {
                String lower = code.toLowerCase().trim();
                if (lower.startsWith(prefix)) {
                    String numPart = lower.substring(prefix.length());
                    if (numPart.matches("\\d+")) {
                        try {
                            int val = Integer.parseInt(numPart);
                            if (val > maxIndex) {
                                maxIndex = val;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        String candidate = prefix + (maxIndex + 1);
        while (repo.existsByMaNhanVienIgnoreCase(candidate)) {
            maxIndex++;
            candidate = prefix + (maxIndex + 1);
        }
        return candidate;
    }

    private String generateRandomPassword() {
        int randomNum = 100000 + new Random().nextInt(900000);
        return "NV@" + randomNum;
    }

    public String nextCode() {
        return generateEmployeeCode("Nhan Vien");
    }

    private NhanVien find(Long id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên: " + id));
    }

    private NhanVienResponse toResponse(NhanVien x) {
        VaiTro role = x.getIdVaiTro();
        return new NhanVienResponse(
                x.getId(), x.getMaNhanVien(), x.getTenTaiKhoan(), x.getTenNhanVien(), x.getEmail(), x.getSoDienThoai(),
                x.getAnhNhanVien(), x.getGioiTinh(), x.getNgaySinh(), x.getQueQuan(), x.getPhuong(), x.getDiaChiCuThe(),
                role == null ? null : role.getId(), role == null ? null : role.getMaVaiTro(), role == null ? null : role.getTenVaiTro(),
                x.getTrangThai()
        );
    }

    private void validateSoDienThoai(String rawPhone) {
        String phone = blank(rawPhone);
        if (phone == null) {
            throw new IllegalArgumentException("Số điện thoại nhân viên không được để trống");
        }
        if (!phone.matches("\\d+")) {
            throw new IllegalArgumentException("Số điện thoại chỉ được chứa các chữ số");
        }
        if (!phone.startsWith("0")) {
            throw new IllegalArgumentException("Số điện thoại phải bắt đầu bằng số 0");
        }
        if (phone.length() < 10) {
            throw new IllegalArgumentException("Số điện thoại không được dưới 10 số");
        }
        if (phone.length() > 11) {
            throw new IllegalArgumentException("Số điện thoại không được trên 11 số");
        }
    }

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}