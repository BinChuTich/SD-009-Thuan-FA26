package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface NhanVienRepository extends JpaRepository<NhanVien, Long>, JpaSpecificationExecutor<NhanVien> {
    boolean existsByMaNhanVien(String maNhanVien);
    boolean existsByTenTaiKhoan(String tenTaiKhoan);
    boolean existsByEmail(String email);
    Optional<NhanVien> findTopByOrderByIdDesc();
}
