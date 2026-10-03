package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface KhachHangRepository extends JpaRepository<KhachHang, Long>, JpaSpecificationExecutor<KhachHang> {
    boolean existsByMaKhachHang(String maKhachHang);
    boolean existsByTaiKhoan(String taiKhoan);
    boolean existsByEmail(String email);
    Optional<KhachHang> findTopByOrderByIdDesc();
}
