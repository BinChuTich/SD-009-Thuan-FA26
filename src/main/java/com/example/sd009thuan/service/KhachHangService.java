package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.DiaChiKhachHangDto;
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
import java.time.LocalDate;
import java.time.Period;
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

    @Transactional(readOnly = true)
    public KhachHangResponse getByIdentifier(String identifier) {
        return toResponse(findByIdentifier(identifier));
    }

    public KhachHang findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new EntityNotFoundException("Mã hoặc ID khách hàng không hợp lệ");
        }
        try {
            Long id = Long.parseLong(identifier.trim());
            Optional<KhachHang> byId = repo.findById(id);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (NumberFormatException ignored) {
        }
        return repo.findByMaKhachHangIgnoreCase(identifier.trim())
                .or(() -> repo.findByMaKhachHang(identifier.trim()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng: " + identifier));
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
        String phone = blankToNull(req.soDienThoai()) != null ? req.soDienThoai() : req.soDienThoaiNhan();
        x.setSoDienThoai(blankToNull(phone));
        x.setNgaySinh(req.ngaySinh());
        x.setGioiTinh(req.gioiTinh() == null ? true : req.gioiTinh());
        x.setTrangThai(req.trangThai() == null ? 1 : req.trangThai());
        x.setNgayTao(Instant.now());
        x.setNguoiTao("admin");
        x = repo.save(x);
        saveDefaultAddress(x, req);
        return toResponse(x);
    }

    @Transactional
    public KhachHangResponse update(Long id, KhachHangRequest req) {
        KhachHang x = find(id);
        validateUnique(req, id);
        if (blankToNull(req.maKhachHang()) != null) x.setMaKhachHang(req.maKhachHang().trim());
        if (blankToNull(req.taiKhoan()) != null) x.setTaiKhoan(blankToNull(req.taiKhoan()));
        if (req.tenKhachHang() != null) {
            if (blankToNull(req.tenKhachHang()) == null) {
                throw new IllegalArgumentException("Họ tên khách hàng không được để trống");
            }
            x.setTenKhachHang(req.tenKhachHang().trim());
        }
        if (req.email() != null) {
            if (blankToNull(req.email()) == null) {
                throw new IllegalArgumentException("Email khách hàng không được để trống");
            }
            if (!req.email().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("Email khách hàng không đúng định dạng");
            }
            x.setEmail(blankToNull(req.email()));
        }
        if (blankToNull(req.matKhau()) != null) x.setMatKhau(req.matKhau());
        if (req.soDienThoai() != null || req.soDienThoaiNhan() != null) {
            String phone = req.soDienThoai() != null ? req.soDienThoai() : req.soDienThoaiNhan();
            validateSoDienThoai(phone);
            x.setSoDienThoai(blankToNull(phone));
        }
        if (req.ngaySinh() != null) {
            if (req.ngaySinh().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Ngày sinh không được lớn hơn ngày hiện tại");
            }
            x.setNgaySinh(req.ngaySinh());
        }
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
        repo.save(x);
    }

    @Transactional
    public void deactivate(Long id) {
        KhachHang x = find(id);
        x.setTrangThai(0);
        x.setNgayCapNhat(Instant.now());
        x.setNguoiCapNhat("admin");
        repo.save(x);
    }

    @Transactional(readOnly = true)
    public List<KhachHangResponse> findAllForExport(String keyword, Integer trangThai) {
        Specification<KhachHang> spec = Specification.where(KhachHangSpecification.keyword(keyword))
                .and(KhachHangSpecification.status(trangThai));
        return repo.findAll(spec, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DiaChiKhachHangDto> getAddresses(Long customerId) {
        find(customerId);
        return addressRepo.findByIdKhachHangIdAndTrangThaiOrderByMacDinhDescIdDesc(customerId, 1)
                .stream().map(this::toAddressDto).toList();
    }

    @Transactional
    public DiaChiKhachHangDto addAddress(Long customerId, DiaChiKhachHangDto dto) {
        KhachHang customer = find(customerId);
        validateAddressDto(dto);
        List<DiaChiKhachHang> existing = addressRepo.findByIdKhachHangIdAndTrangThaiOrderByMacDinhDescIdDesc(customerId, 1);
        boolean isFirst = existing.isEmpty();
        boolean setAsDefault = isFirst || Boolean.TRUE.equals(dto.macDinh());

        if (setAsDefault) {
            existing.forEach(a -> {
                a.setMacDinh(false);
                addressRepo.save(a);
            });
        }

        DiaChiKhachHang addr = new DiaChiKhachHang();
        addr.setIdKhachHang(customer);
        addr.setMaDiaChi("DC" + customerId + "_" + (System.currentTimeMillis() % 10000));
        addr.setThanhPho(dto.thanhPho().trim());
        addr.setHuyen(dto.huyen() != null ? dto.huyen().trim() : "");
        addr.setPhuong(dto.phuong().trim());
        addr.setDiaChiCuThe(dto.diaChiCuThe().trim());
        addr.setMacDinh(setAsDefault);
        addr.setTrangThai(1);
        return toAddressDto(addressRepo.save(addr));
    }

    @Transactional
    public DiaChiKhachHangDto updateAddress(Long customerId, Long addressId, DiaChiKhachHangDto dto) {
        find(customerId);
        validateAddressDto(dto);
        DiaChiKhachHang addr = addressRepo.findById(addressId)
                .filter(a -> a.getIdKhachHang().getId().equals(customerId))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ của khách hàng"));

        if (Boolean.TRUE.equals(dto.macDinh())) {
            List<DiaChiKhachHang> existing = addressRepo.findByIdKhachHangIdAndTrangThaiOrderByMacDinhDescIdDesc(customerId, 1);
            existing.forEach(a -> {
                if (!a.getId().equals(addressId)) {
                    a.setMacDinh(false);
                    addressRepo.save(a);
                }
            });
            addr.setMacDinh(true);
        }

        addr.setThanhPho(dto.thanhPho().trim());
        addr.setHuyen(dto.huyen() != null ? dto.huyen().trim() : "");
        addr.setPhuong(dto.phuong().trim());
        addr.setDiaChiCuThe(dto.diaChiCuThe().trim());
        return toAddressDto(addressRepo.save(addr));
    }

    @Transactional
    public void setDefaultAddress(Long customerId, Long addressId) {
        find(customerId);
        DiaChiKhachHang target = addressRepo.findById(addressId)
                .filter(a -> a.getIdKhachHang().getId().equals(customerId))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ của khách hàng"));

        List<DiaChiKhachHang> existing = addressRepo.findByIdKhachHangIdAndTrangThaiOrderByMacDinhDescIdDesc(customerId, 1);
        existing.forEach(a -> {
            a.setMacDinh(a.getId().equals(addressId));
            addressRepo.save(a);
        });
    }

    @Transactional
    public void deleteAddress(Long customerId, Long addressId) {
        find(customerId);
        DiaChiKhachHang addr = addressRepo.findById(addressId)
                .filter(a -> a.getIdKhachHang().getId().equals(customerId))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ"));
        if (Boolean.TRUE.equals(addr.getMacDinh())) {
            throw new IllegalArgumentException("Không thể xóa địa chỉ đang được đặt làm mặc định");
        }
        addr.setTrangThai(0);
        addressRepo.save(addr);
    }

    private void validateAddressDto(DiaChiKhachHangDto dto) {
        if (dto == null) throw new IllegalArgumentException("Thông tin địa chỉ không được để trống");
        if (blankToNull(dto.thanhPho()) == null) throw new IllegalArgumentException("Tỉnh / Thành phố không được để trống");
        if (blankToNull(dto.phuong()) == null) throw new IllegalArgumentException("Phường / Xã không được để trống");
        if (blankToNull(dto.diaChiCuThe()) == null) throw new IllegalArgumentException("Địa chỉ cụ thể không được để trống");
    }

    private DiaChiKhachHangDto toAddressDto(DiaChiKhachHang a) {
        return new DiaChiKhachHangDto(
                a.getId(),
                a.getIdKhachHang().getId(),
                a.getMaDiaChi(),
                a.getThanhPho(),
                a.getHuyen(),
                a.getPhuong(),
                a.getDiaChiCuThe(),
                a.getMacDinh(),
                a.getTrangThai()
        );
    }

    private void validateRequired(KhachHangRequest req) {
        if (blankToNull(req.tenKhachHang()) == null) {
            throw new IllegalArgumentException("Họ tên khách hàng không được để trống");
        }
        if (blankToNull(req.email()) != null && !req.email().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Email khách hàng không đúng định dạng");
        }
        String phone = blankToNull(req.soDienThoai()) != null ? req.soDienThoai() : req.soDienThoaiNhan();
        if (phone != null) {
            validateSoDienThoai(phone);
        }
        if (req.ngaySinh() != null && req.ngaySinh().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không được lớn hơn ngày hiện tại");
        }
    }

    private void validateUnique(KhachHangRequest req, Long currentId) {
        String code = blankToNull(req.maKhachHang());
        if (code != null) {
            boolean exists = currentId == null
                    ? repo.existsByMaKhachHangIgnoreCase(code)
                    : repo.existsByMaKhachHangIgnoreCaseAndIdNot(code, currentId);
            if (exists) throw new IllegalArgumentException("Mã khách hàng đã tồn tại");
        }

        String email = blankToNull(req.email());
        if (email != null) {
            boolean exists = currentId == null
                    ? repo.existsByEmailIgnoreCase(email)
                    : repo.existsByEmailIgnoreCaseAndIdNot(email, currentId);
            if (exists) throw new IllegalArgumentException("Email khách hàng đã tồn tại trong hệ thống");
        }

        String phone = blankToNull(req.soDienThoai()) != null ? req.soDienThoai().trim() : blankToNull(req.soDienThoaiNhan());
        if (phone != null) {
            boolean exists = currentId == null
                    ? repo.existsBySoDienThoai(phone)
                    : repo.existsBySoDienThoaiAndIdNot(phone, currentId);
            if (exists) throw new IllegalArgumentException("Số điện thoại này đã được sử dụng");
        }
    }

    @Transactional(readOnly = true)
    public boolean checkEmailExists(String email, Long excludeId) {
        String val = blankToNull(email);
        if (val == null) return false;
        return excludeId == null
                ? repo.existsByEmailIgnoreCase(val)
                : repo.existsByEmailIgnoreCaseAndIdNot(val, excludeId);
    }

    @Transactional(readOnly = true)
    public boolean checkPhoneExists(String phone, Long excludeId) {
        String val = blankToNull(phone);
        if (val == null) return false;
        return excludeId == null
                ? repo.existsBySoDienThoai(val)
                : repo.existsBySoDienThoaiAndIdNot(val, excludeId);
    }

    public String nextCode() {
        long next = repo.findTopByOrderByIdDesc().map(x -> x.getId() + 1).orElse(1L);
        String code = String.format("KH%03d", next);
        while (repo.existsByMaKhachHangIgnoreCase(code)) {
            next++;
            code = String.format("KH%03d", next);
        }
        return code;
    }

    private void saveDefaultAddress(KhachHang customer, KhachHangRequest req) {
        if (blankToNull(req.thanhPho()) == null && blankToNull(req.diaChiCuThe()) == null) {
            return;
        }
        Optional<DiaChiKhachHang> old = addressRepo.findFirstByIdKhachHangIdAndMacDinhTrueAndTrangThai(customer.getId(), 1);
        DiaChiKhachHang address = old.orElseGet(DiaChiKhachHang::new);
        address.setIdKhachHang(customer);
        address.setMaDiaChi("DC" + customer.getId());
        if (blankToNull(req.thanhPho()) != null) address.setThanhPho(blankToNull(req.thanhPho()));
        address.setHuyen(blankToNull(req.huyen()) != null ? req.huyen().trim() : "");
        if (blankToNull(req.phuong()) != null) address.setPhuong(blankToNull(req.phuong()));
        if (blankToNull(req.diaChiCuThe()) != null) address.setDiaChiCuThe(blankToNull(req.diaChiCuThe()));
        address.setMacDinh(true);
        address.setTrangThai(1);
        addressRepo.save(address);
    }

    private KhachHang find(Long id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng: " + id));
    }

    
    @Transactional(readOnly = true)
    public Optional<KhachHang> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return repo.findByEmail(email.trim());
    }

    @Transactional(readOnly = true)
    public Optional<KhachHang> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) return Optional.empty();
        return repo.findBySoDienThoai(phone.trim());
    }

    public KhachHangResponse toResponse(KhachHang x) {
        DiaChiKhachHang a = addressRepo.findFirstByIdKhachHangIdAndMacDinhTrueAndTrangThai(x.getId(), 1)
                .or(() -> addressRepo.findByIdKhachHangIdAndTrangThaiOrderByMacDinhDescIdDesc(x.getId(), 1).stream().findFirst())
                .or(() -> addressRepo.findByIdKhachHangIdOrderByMacDinhDescIdDesc(x.getId()).stream().findFirst())
                .orElse(null);
        String account = x.getTaiKhoan();
        if (account == null || account.isBlank()) {
            account = x.getMaKhachHang() != null ? x.getMaKhachHang().toLowerCase() : "-";
        }
        return new KhachHangResponse(
                x.getId(), x.getMaKhachHang(), account, x.getTenKhachHang(), x.getEmail(), x.getSoDienThoai(),
                x.getNgaySinh(), x.getGioiTinh(), x.getTrangThai(),
                a == null ? null : a.getThanhPho(),
                a == null ? null : a.getHuyen(),
                a == null ? null : a.getPhuong(),
                a == null ? null : a.getDiaChiCuThe(),
                a == null ? null : a.getTenNguoiNhan(),
                a == null ? null : a.getSoDienThoaiNhan()
        );
    }

    private void validateSoDienThoai(String rawPhone) {
        String phone = blankToNull(rawPhone);
        if (phone == null) {
            throw new IllegalArgumentException("Số điện thoại không được để trống");
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
