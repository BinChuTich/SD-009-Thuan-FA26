package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.KhachHangPhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhachHangPhieuGiamGiaRepository extends JpaRepository<KhachHangPhieuGiamGia, Long> {
    List<KhachHangPhieuGiamGia> findByIdPhieuGiamGia_Id(Long idPhieu);
    void deleteByIdPhieuGiamGia_Id(Long idPhieu);
}

