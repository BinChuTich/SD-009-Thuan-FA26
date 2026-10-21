package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface NhanVienRepository extends JpaRepository<NhanVien, Long>, JpaSpecificationExecutor<NhanVien> {
    boolean existsByMaNhanVienIgnoreCase(String maNhanVien);
    boolean existsByTenTaiKhoanIgnoreCase(String tenTaiKhoan);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsBySoDienThoai(String soDienThoai);
    boolean existsByMaNhanVienIgnoreCaseAndIdNot(String maNhanVien, Long id);
    boolean existsByTenTaiKhoanIgnoreCaseAndIdNot(String tenTaiKhoan, Long id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsBySoDienThoaiAndIdNot(String soDienThoai, Long id);
    Optional<NhanVien> findByMaNhanVien(String maNhanVien);
    Optional<NhanVien> findByMaNhanVienIgnoreCase(String maNhanVien);
    Optional<NhanVien> findByTenTaiKhoanIgnoreCase(String tenTaiKhoan);
    Optional<NhanVien> findByEmailIgnoreCase(String email);
    Optional<NhanVien> findTopByOrderByIdDesc();
}
