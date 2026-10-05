package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.DiaChiKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiaChiKhachHangRepository extends JpaRepository<DiaChiKhachHang, Integer> {
    Optional<DiaChiKhachHang> findFirstByIdKhachHangIdAndMacDinhTrueAndTrangThai(Long khachHangId, Integer trangThai);

}
