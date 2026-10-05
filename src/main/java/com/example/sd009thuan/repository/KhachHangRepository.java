package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.KhachHang;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Long>, JpaSpecificationExecutor<KhachHang> {
    List<KhachHang> findAllByOrderByTenKhachHangAsc();
    boolean existsByMaKhachHangIgnoreCase(String maKhachHang);
    boolean existsByTaiKhoanIgnoreCase(String taiKhoan);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByMaKhachHangIgnoreCaseAndIdNot(String maKhachHang, Long id);
    boolean existsByTaiKhoanIgnoreCaseAndIdNot(String taiKhoan, Long id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    Optional<KhachHang> findTopByOrderByIdDesc();


}

