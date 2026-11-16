package com.example.sd009thuan.spec;

import com.example.sd009thuan.entity.KhachHang;
import org.springframework.data.jpa.domain.Specification;

public final class KhachHangSpecification {
    private KhachHangSpecification() {}

    public static Specification<KhachHang> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String value = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("maKhachHang")), value),
                    cb.like(cb.lower(root.get("tenKhachHang")), value),
                    cb.like(cb.lower(root.get("taiKhoan")), value),
                    cb.like(cb.lower(root.get("email")), value),
                    cb.like(cb.lower(root.get("soDienThoai")), value)
            );
        };
    }

    public static Specification<KhachHang> status(Integer status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("trangThai"), status);
    }
}
