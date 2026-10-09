package com.example.sd009thuan.service;

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
    public void toggleStatus(Long id) {
        NhanVien x = find(id);
        x.setTrangThai(x.getTrangThai() != null && x.getTrangThai() == 1 ? 0 : 1);
        x.setNgayCapNhat(Instant.now());
        x.setNguoiCapNhat("admin");
    }

    @Transactional
    public void deactivate(Long id) {
        NhanVien x = find(id);
        x.setTrangThai(0);
        x.setNgayCapNhat(Instant.now());
        x.setNguoiCapNhat("admin");
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
        if (email != null) repo.findAll().stream().filter(x -> email.equalsIgnoreCase(x.getEmail()) && !x.getId().equals(currentId)).findAny()
                .ifPresent(x -> { throw new IllegalArgumentException("Email đã tồn tại"); });
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
        return prefix + (maxIndex + 1);
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