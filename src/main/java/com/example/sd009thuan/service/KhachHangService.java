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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    public KhachHangService(
            KhachHangRepository repo,
            DiaChiKhachHangRepository addressRepo
    ) {
        this.repo = repo;
        this.addressRepo = addressRepo;
    }

    @Transactional(readOnly = true)
    public Page<KhachHangResponse> search(
            String keyword,
            Integer trangThai,
            Pageable pageable
    ) {
        Specification<KhachHang> spec =
                Specification.where(KhachHangSpecification.keyword(keyword))
                        .and(KhachHangSpecification.status(trangThai));

        Page<KhachHang> entityPage = repo.findAll(spec, pageable);

        return entityPage.map(this::toResponse);
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

        String code = blankToNull(req.maKhachHang()) != null
                ? req.maKhachHang().trim()
                : nextCode();

        x.setMaKhachHang(code);

        String account = blankToNull(req.taiKhoan());

        x.setTaiKhoan(
                account != null
                        ? account
                        : code.toLowerCase()
        );

        x.setTenKhachHang(req.tenKhachHang().trim());

        x.setEmail(blankToNull(req.email()));

        x.setMatKhau(
                blankToNull(req.matKhau()) != null
                        ? req.matKhau()
                        : "123456"
        );

        String phone =
                blankToNull(req.soDienThoaiNhan()) != null
                        ? req.soDienThoaiNhan()
                        : req.soDienThoai();

        x.setSoDienThoai(blankToNull(phone));

        x.setNgaySinh(req.ngaySinh());

        x.setGioiTinh(
                req.gioiTinh() == null
                        ? true
                        : req.gioiTinh()
        );

        x.setTrangThai(
                req.trangThai() == null
                        ? 1
                        : req.trangThai()
        );

        x.setNgayTao(Instant.now());
        x.setNguoiTao("admin");

        x = repo.save(x);

        saveDefaultAddress(x, req);

        return toResponse(x);
    }

    @Transactional
    public KhachHangResponse update(
            Long id,
            KhachHangRequest req
    ) {

        KhachHang x = find(id);

        validateUnique(req, id);

        if (blankToNull(req.maKhachHang()) != null) {
            x.setMaKhachHang(req.maKhachHang().trim());
        }

        if (blankToNull(req.taiKhoan()) != null) {
            x.setTaiKhoan(blankToNull(req.taiKhoan()));
        }

        if (blankToNull(req.tenKhachHang()) != null) {
            x.setTenKhachHang(req.tenKhachHang().trim());
        }

        if (blankToNull(req.email()) != null) {

            if (!req.email().trim()
                    .matches("^[A-Za-z0-9+_.-]+@(.+)$")) {

                throw new IllegalArgumentException(
                        "Email khách hàng không đúng định dạng"
                );
            }

            x.setEmail(blankToNull(req.email()));
        }

        if (blankToNull(req.matKhau()) != null) {
            x.setMatKhau(req.matKhau());
        }

        String phone =
                blankToNull(req.soDienThoaiNhan()) != null
                        ? req.soDienThoaiNhan()
                        : req.soDienThoai();

        if (phone != null) {

            if (!phone.trim()
                    .matches("^0\\d{9,10}$")) {

                throw new IllegalArgumentException(
                        "Số điện thoại phải gồm 10-11 số và bắt đầu bằng 0"
                );
            }

            x.setSoDienThoai(blankToNull(phone));
        }

        if (req.ngaySinh() != null) {
            x.setNgaySinh(req.ngaySinh());
        }

        if (req.gioiTinh() != null) {
            x.setGioiTinh(req.gioiTinh());
        }

        if (req.trangThai() != null) {
            x.setTrangThai(req.trangThai());
        }

        x.setNgayCapNhat(Instant.now());
        x.setNguoiCapNhat("admin");

        x = repo.save(x);

        saveDefaultAddress(x, req);

        return toResponse(x);
    }

    @Transactional
    public void toggleStatus(Long id) {

        KhachHang x = find(id);

        x.setTrangThai(
                x.getTrangThai() != null
                && x.getTrangThai() == 1
                        ? 0
                        : 1
        );

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
    public List<KhachHangResponse> findAllForExport(
            String keyword,
            Integer trangThai
    ) {

        Specification<KhachHang> spec =
                Specification.where(KhachHangSpecification.keyword(keyword))
                        .and(KhachHangSpecification.status(trangThai));

        return repo.findAll(
                        spec,
                        PageRequest.of(
                                0,
                                Integer.MAX_VALUE,
                                Sort.by(
                                        Sort.Direction.ASC,
                                        "id"
                                )
                        )
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateRequired(KhachHangRequest req) {

        if (blankToNull(req.tenKhachHang()) == null) {
            throw new IllegalArgumentException(
                    "Họ tên khách hàng không được để trống"
            );
        }

        if (blankToNull(req.email()) == null) {
            throw new IllegalArgumentException(
                    "Email khách hàng không được để trống"
            );
        }

        if (!req.email().trim()
                .matches("^[A-Za-z0-9+_.-]+@(.+)$")) {

            throw new IllegalArgumentException(
                    "Email khách hàng không đúng định dạng"
            );
        }

        if (blankToNull(req.nguoiNhan()) == null) {
            throw new IllegalArgumentException(
                    "Họ tên người nhận không được để trống"
            );
        }

        String phoneNhan =
                blankToNull(req.soDienThoaiNhan()) != null
                        ? req.soDienThoaiNhan()
                        : req.soDienThoai();

        if (blankToNull(phoneNhan) == null) {
            throw new IllegalArgumentException(
                    "Số điện thoại người nhận không được để trống"
            );
        }

        if (!phoneNhan.trim()
                .matches("^0\\d{9,10}$")) {

            throw new IllegalArgumentException(
                    "Số điện thoại người nhận phải gồm 10-11 số và bắt đầu bằng 0"
            );
        }

        if (blankToNull(req.thanhPho()) == null) {
            throw new IllegalArgumentException(
                    "Tỉnh / Thành phố nhận hàng không được để trống"
            );
        }

        if (blankToNull(req.huyen()) == null) {
            throw new IllegalArgumentException(
                    "Quận / Huyện nhận hàng không được để trống"
            );
        }

        if (blankToNull(req.phuong()) == null) {
            throw new IllegalArgumentException(
                    "Phường / Xã nhận hàng không được để trống"
            );
        }

        if (blankToNull(req.diaChiCuThe()) == null) {
            throw new IllegalArgumentException(
                    "Địa chỉ cụ thể nhận hàng không được để trống"
            );
        }
    }

    private void validateUnique(
            KhachHangRequest req,
            Long currentId
    ) {

        String code = blankToNull(req.maKhachHang());

        if (code != null) {

            boolean exists =
                    currentId == null
                            ? repo.existsByMaKhachHangIgnoreCase(code)
                            : repo.existsByMaKhachHangIgnoreCaseAndIdNot(
                            code,
                            currentId
                    );

            if (exists) {
                throw new IllegalArgumentException(
                        "Mã khách hàng đã tồn tại"
                );
            }
        }

        String account = blankToNull(req.taiKhoan());

        if (account != null) {

            boolean exists =
                    currentId == null
                            ? repo.existsByTaiKhoanIgnoreCase(account)
                            : repo.existsByTaiKhoanIgnoreCaseAndIdNot(
                            account,
                            currentId
                    );

            if (exists) {
                throw new IllegalArgumentException(
                        "Tài khoản khách hàng đã tồn tại"
                );
            }
        }

        String email = blankToNull(req.email());

        if (email != null) {

            boolean exists =
                    currentId == null
                            ? repo.existsByEmailIgnoreCase(email)
                            : repo.existsByEmailIgnoreCaseAndIdNot(
                            email,
                            currentId
                    );

            if (exists) {
                throw new IllegalArgumentException(
                        "Email khách hàng đã tồn tại"
                );
            }
        }

        if (req.ngaySinh() != null
            && req.ngaySinh().isAfter(java.time.LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Ngày sinh không được lớn hơn ngày hiện tại"
            );
        }
    }

    public String nextCode() {

        long next =
                repo.findTopByOrderByIdDesc()
                        .map(x -> x.getId() + 1)
                        .orElse(1L);

        return String.format("KH%03d", next);
    }

    private void saveDefaultAddress(
            KhachHang customer,
            KhachHangRequest req
    ) {

        Optional<DiaChiKhachHang> old =
                addressRepo
                        .findFirstByIdKhachHangIdAndMacDinhTrueAndTrangThai(
                                customer.getId(),
                                1
                        );

        DiaChiKhachHang address =
                old.orElseGet(DiaChiKhachHang::new);

        address.setIdKhachHang(customer);
        address.setMaDiaChi("DC" + customer.getId());

        if (blankToNull(req.nguoiNhan()) != null) {
            address.setTenNguoiNhan(
                    blankToNull(req.nguoiNhan())
            );
        }

        String phoneNhan =
                blankToNull(req.soDienThoaiNhan()) != null
                        ? req.soDienThoaiNhan()
                        : req.soDienThoai();

        if (phoneNhan != null) {
            address.setSoDienThoaiNhan(
                    blankToNull(phoneNhan)
            );
        }

        if (blankToNull(req.thanhPho()) != null) {
            address.setThanhPho(
                    blankToNull(req.thanhPho())
            );
        }

        if (blankToNull(req.huyen()) != null) {
            address.setHuyen(
                    blankToNull(req.huyen())
            );
        }

        if (blankToNull(req.phuong()) != null) {
            address.setPhuong(
                    blankToNull(req.phuong())
            );
        }

        if (blankToNull(req.diaChiCuThe()) != null) {
            address.setDiaChiCuThe(
                    blankToNull(req.diaChiCuThe())
            );
        }

        address.setMacDinh(true);
        address.setTrangThai(1);

        addressRepo.save(address);
    }

    private KhachHang find(Long id) {

        return repo.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy khách hàng: " + id
                        )
                );
    }

    private KhachHangResponse toResponse(KhachHang x) {

        DiaChiKhachHang a =
                addressRepo
                        .findFirstByIdKhachHangIdAndMacDinhTrueAndTrangThai(
                                x.getId(),
                                1
                        )
                        .orElse(null);

        return new KhachHangResponse(
                x.getId(),
                x.getMaKhachHang(),
                x.getTaiKhoan(),
                x.getTenKhachHang(),
                x.getEmail(),
                x.getSoDienThoai(),
                x.getNgaySinh(),
                x.getGioiTinh(),
                x.getTrangThai(),
                a == null ? null : a.getThanhPho(),
                a == null ? null : a.getHuyen(),
                a == null ? null : a.getPhuong(),
                a == null ? null : a.getDiaChiCuThe(),
                a == null ? null : a.getTenNguoiNhan(),
                a == null ? null : a.getSoDienThoaiNhan()
        );
    }

    private String blankToNull(String value) {

        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
