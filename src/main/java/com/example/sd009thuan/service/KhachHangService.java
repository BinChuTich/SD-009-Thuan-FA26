package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.KhachHangRequest;
import com.example.sd009thuan.dto.KhachHangResponse;
import com.example.sd009thuan.entity.DiaChiKhachHang;
import com.example.sd009thuan.entity.KhachHang;
import com.example.sd009thuan.repository.DiaChiKhachHangRepository;
import com.example.sd009thuan.repository.KhachHangRepository;
import com.example.sd009thuan.spec.KhachHangSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class KhachHangService {
    private final KhachHangRepository repo;
    private final DiaChiKhachHangRepository addressRepo;

    public KhachHangService(KhachHangRepository repo, DiaChiKhachHangRepository addressRepo) {
        this.repo = repo;
        this.addressRepo = addressRepo;
    }

    @Transactional(readOnly = true)
    public Page<KhachHangResponse> search(String keyword, Integer trangThai, Pageable pageable) {
        Specification<KhachHang> spec = Specification.where(KhachHangSpecification.keyword(keyword))
                .and(KhachHangSpecification.status(trangThai));
        return repo.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public KhachHangResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public KhachHangResponse create(KhachHangRequest req) {
        validateRequired(req);
        validateUnique(req, null);
        KhachHang x = new KhachHang();
        String code = blankToNull(req.maKhachHang()) != null ? req.maKhachHang().trim() : nextCode();
        x.setMaKhachHang(code);
        String account = blankToNull(req.taiKhoan());
        x.setTaiKhoan(account != null ? account : code.toLowerCase());
        x.setTenKhachHang(req.tenKhachHang().trim());
        x.setEmail(blankToNull(req.email()));
        x.setMatKhau(blankToNull(req.matKhau()) != null ? req.matKhau() : "123456");
        x.setSoDienThoai(blankToNull(req.soDienThoai()));
        x.setNgaySinh(req.ngaySinh());
        x.setGioiTinh(req.gioiTinh());
        x.setTrangThai(req.trangThai() == null ? 1 : req.trangThai());
        x.setNgayTao(Instant.now());
        x.setNguoiTao("admin");
        x = repo.save(x);
        saveDefaultAddress(x, req);
        return toResponse(x);
    }

    @Transactional
    public KhachHangResponse update(Long id, KhachHangRequest req) {
        validateRequired(req);
        KhachHang x = find(id);
        validateUnique(req, id);
        if (blankToNull(req.maKhachHang()) != null) x.setMaKhachHang(req.maKhachHang().trim());
        if (blankToNull(req.taiKhoan()) != null) x.setTaiKhoan(blankToNull(req.taiKhoan()));
        x.setTenKhachHang(req.tenKhachHang().trim());
        if (blankToNull(req.email()) != null) x.setEmail(blankToNull(req.email()));
        if (blankToNull(req.matKhau()) != null) x.setMatKhau(req.matKhau());
        x.setSoDienThoai(blankToNull(req.soDienThoai()));
        if (req.ngaySinh() != null) x.setNgaySinh(req.ngaySinh());
        if (req.gioiTinh() != null) x.setGioiTinh(req.gioiTinh());
        if (req.trangThai() != null) x.setTrangThai(req.trangThai());
        x.setNgayCapNhat(Instant.now());
        x.setNguoiCapNhat("admin");
        x = repo.save(x);
        saveDefaultAddress(x, req);
        return toResponse(x);
    }

    @Transactional
    public void toggleStatus(Long id) {
        KhachHang x = find(id);
        x.setTrangThai(x.getTrangThai() != null && x.getTrangThai() == 1 ? 0 : 1);
        x.setNgayCapNhat(Instant.now());
        x.setNguoiCapNhat("admin");
    }

    @Transactional
    public void deactivate(Long id) {
        KhachHang x = find(id);
        x.setTrangThai(0);
        x.setNgayCapNhat(Instant.now());
        x.setNguoiCapNhat("admin");
    }

    @Transactional(readOnly = true)
    public List<KhachHangResponse> findAllForExport(String keyword, Integer trangThai) {
        Specification<KhachHang> spec = Specification.where(KhachHangSpecification.keyword(keyword))
                .and(KhachHangSpecification.status(trangThai));
        return repo.findAll(spec, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"))
                .stream().map(this::toResponse).toList();
    }

    private void validateRequired(KhachHangRequest req) {
        if (blankToNull(req.tenKhachHang()) == null) throw new IllegalArgumentException("Họ tên khách hàng không được để trống");
        if (blankToNull(req.soDienThoai()) == null) throw new IllegalArgumentException("Số điện thoại khách hàng không được để trống");
        if (!req.soDienThoai().trim().matches("0\\d{9,10}")) throw new IllegalArgumentException("Số điện thoại phải gồm 10-11 số và bắt đầu bằng 0");
    }

    private void validateUnique(KhachHangRequest req, Long currentId) {
        String code = blankToNull(req.maKhachHang());
        if (code != null) {
            boolean exists = currentId == null
                    ? repo.existsByMaKhachHangIgnoreCase(code)
                    : repo.existsByMaKhachHangIgnoreCaseAndIdNot(code, currentId);
            if (exists) throw new IllegalArgumentException("Mã khách hàng đã tồn tại");
        }

        String account = blankToNull(req.taiKhoan());
        if (account != null) {
            boolean exists = currentId == null
                    ? repo.existsByTaiKhoanIgnoreCase(account)
                    : repo.existsByTaiKhoanIgnoreCaseAndIdNot(account, currentId);
            if (exists) throw new IllegalArgumentException("Tài khoản khách hàng đã tồn tại");
        }

        String email = blankToNull(req.email());
        if (email != null) {
            boolean exists = currentId == null
                    ? repo.existsByEmailIgnoreCase(email)
                    : repo.existsByEmailIgnoreCaseAndIdNot(email, currentId);
            if (exists) throw new IllegalArgumentException("Email khách hàng đã tồn tại");
        }

        if (req.ngaySinh() != null && req.ngaySinh().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không được lớn hơn ngày hiện tại");
        }
    }

    public String nextCode() {
        long next = repo.findTopByOrderByIdDesc().map(x -> x.getId() + 1).orElse(1L);
        return String.format("KH%03d", next);
    }

    private void saveDefaultAddress(KhachHang customer, KhachHangRequest req) {
        if (blankToNull(req.thanhPho()) == null && blankToNull(req.phuong()) == null && blankToNull(req.diaChiCuThe()) == null) return;
        Optional<DiaChiKhachHang> old = addressRepo.findFirstByIdKhachHangIdAndMacDinhTrueAndTrangThai(customer.getId(), 1);
        DiaChiKhachHang address = old.orElseGet(DiaChiKhachHang::new);
        address.setIdKhachHang(customer);
        address.setMaDiaChi("DC" + customer.getId());
        address.setThanhPho(blankToNull(req.thanhPho()));
        address.setHuyen(blankToNull(req.huyen()));
        address.setPhuong(blankToNull(req.phuong()));
        address.setDiaChiCuThe(blankToNull(req.diaChiCuThe()));
        address.setMacDinh(true);
        address.setTrangThai(1);
        addressRepo.save(address);
    }

    private KhachHang find(Long id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng: " + id));
    }

    private KhachHangResponse toResponse(KhachHang x) {
        DiaChiKhachHang a = addressRepo.findFirstByIdKhachHangIdAndMacDinhTrueAndTrangThai(x.getId(), 1).orElse(null);
        return new KhachHangResponse(
                x.getId(), x.getMaKhachHang(), x.getTaiKhoan(), x.getTenKhachHang(), x.getEmail(), x.getSoDienThoai(),
                x.getNgaySinh(), x.getGioiTinh(), x.getTrangThai(),
                a == null ? null : a.getThanhPho(), a == null ? null : a.getHuyen(), a == null ? null : a.getPhuong(),
                a == null ? null : a.getDiaChiCuThe(), null, null
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

