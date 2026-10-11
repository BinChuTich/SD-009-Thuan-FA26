package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.DiaChiKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiaChiKhachHangRepository extends JpaRepository<DiaChiKhachHang, Long> {
    Optional<DiaChiKhachHang> findFirstByIdKhachHangIdAndMacDinhTrueAndTrangThai(Long khachHangId, Integer trangThai);
    List<DiaChiKhachHang> findByIdKhachHangIdAndTrangThaiOrderByMacDinhDescIdDesc(Long khachHangId, Integer trangThai);
    List<DiaChiKhachHang> findByIdKhachHangIdOrderByMacDinhDescIdDesc(Long khachHangId);
}
