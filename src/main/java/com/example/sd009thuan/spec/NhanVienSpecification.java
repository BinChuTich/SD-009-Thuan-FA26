package com.example.sd009thuan.spec;

import com.example.sd009thuan.entity.NhanVien;
import org.springframework.data.jpa.domain.Specification;

public final class NhanVienSpecification {
    private NhanVienSpecification() {}

    public static Specification<NhanVien> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String value = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("maNhanVien")), value),
                    cb.like(cb.lower(root.get("tenNhanVien")), value),
                    cb.like(cb.lower(root.get("tenTaiKhoan")), value),
                    cb.like(cb.lower(root.get("email")), value),
                    cb.like(cb.lower(root.get("soDienThoai")), value)
            );
        };
    }

    public static Specification<NhanVien> status(Integer status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("trangThai"), status);
    }

    public static Specification<NhanVien> role(Long roleId) {
        return (root, query, cb) -> roleId == null ? null : cb.equal(root.get("idVaiTro").get("id"), roleId);
    }
}
