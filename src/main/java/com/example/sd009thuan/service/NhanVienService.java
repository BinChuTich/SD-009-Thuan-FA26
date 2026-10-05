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

@Service
public class NhanVienService {
    private final NhanVienRepository repo;
    private final VaiTroRepository roleRepo;
    private final FileStorageService fileStorageService;

    public NhanVienService(NhanVienRepository repo, VaiTroRepository roleRepo, FileStorageService fileStorageService) {
        this.repo = repo;
        this.roleRepo = roleRepo;
        this.fileStorageService = fileStorageService;
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
        validateUnique(req, null);
        VaiTro role = roleRepo.findById(req.idVaiTro()).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy vai trò"));
        NhanVien x = new NhanVien();
        x.setIdVaiTro(role);
        x.setMaNhanVien(blank(req.maNhanVien()) == null ? nextCode() : req.maNhanVien().trim());
        x.setTenTaiKhoan(blank(req.tenTaiKhoan()));
        x.setTenNhanVien(req.tenNhanVien().trim());
        x.setMatKhau(blank(req.matKhau()) == null ? "123456" : req.matKhau());
        x.setEmail(blank(req.email()));
        x.setSoDienThoai(blank(req.soDienThoai()));
        String imageUrl = fileStorageService.storeEmployeeImage(file);
        x.setAnhNhanVien(imageUrl != null ? imageUrl : blank(req.anhNhanVien()));
        x.setGioiTinh(req.gioiTinh());
        x.setNgaySinh(req.ngaySinh());
        x.setQueQuan(blank(req.queQuan()));
        x.setPhuong(blank(req.phuong()));
        x.setDiaChiCuThe(blank(req.diaChiCuThe()));
        x.setTrangThai(req.trangThai() == null ? 1 : req.trangThai());
        x.setNgayTao(Instant.now());
        x.setNguoiTao("admin");
        return toResponse(repo.save(x));
    }

    @Transactional
    public NhanVienResponse update(Long id, NhanVienRequest req, MultipartFile file) {
        NhanVien x = find(id);
        validateUnique(req, id);
        VaiTro role = roleRepo.findById(req.idVaiTro()).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy vai trò"));
        x.setIdVaiTro(role);
        if (blank(req.maNhanVien()) != null) x.setMaNhanVien(req.maNhanVien().trim());
        x.setTenTaiKhoan(blank(req.tenTaiKhoan()));
        x.setTenNhanVien(req.tenNhanVien().trim());
        if (blank(req.matKhau()) != null) x.setMatKhau(req.matKhau());
        x.setEmail(blank(req.email()));
        x.setSoDienThoai(blank(req.soDienThoai()));
        String oldImage = x.getAnhNhanVien();
        String imageUrl = fileStorageService.storeEmployeeImage(file);
        if (imageUrl != null) {
            x.setAnhNhanVien(imageUrl);
            fileStorageService.deleteIfLocal(oldImage);
        } else {
            x.setAnhNhanVien(blank(req.anhNhanVien()));
        }
        x.setGioiTinh(req.gioiTinh());
        x.setNgaySinh(req.ngaySinh());
        x.setQueQuan(blank(req.queQuan()));
        x.setPhuong(blank(req.phuong()));
        x.setDiaChiCuThe(blank(req.diaChiCuThe()));
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
    public java.util.List<NhanVienResponse> findAllForExport(String keyword, Long roleId, Integer status) {
        Specification<NhanVien> spec = Specification.where(NhanVienSpecification.keyword(keyword))
                .and(NhanVienSpecification.role(roleId))
                .and(NhanVienSpecification.status(status));
        return repo.findAll(spec, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<VaiTro> roles() {
        return roleRepo.findByTrangThaiOrderByIdAsc(1);
    }

    private void validateUnique(NhanVienRequest req, Long currentId) {
        String code = blank(req.maNhanVien());
        if (code != null) repo.findAll().stream().filter(x -> code.equalsIgnoreCase(x.getMaNhanVien()) && !x.getId().equals(currentId)).findAny()
                .ifPresent(x -> { throw new IllegalArgumentException("Mã nhân viên đã tồn tại"); });
        String account = blank(req.tenTaiKhoan());
        if (account != null) repo.findAll().stream().filter(x -> account.equalsIgnoreCase(x.getTenTaiKhoan()) && !x.getId().equals(currentId)).findAny()
                .ifPresent(x -> { throw new IllegalArgumentException("Tên tài khoản đã tồn tại"); });
        String email = blank(req.email());
        if (email != null) repo.findAll().stream().filter(x -> email.equalsIgnoreCase(x.getEmail()) && !x.getId().equals(currentId)).findAny()
                .ifPresent(x -> { throw new IllegalArgumentException("Email đã tồn tại"); });
    }

    private String nextCode() {
        long next = repo.findTopByOrderByIdDesc().map(x -> x.getId() + 1).orElse(1L);
        return String.format("NV%03d", next);
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

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
