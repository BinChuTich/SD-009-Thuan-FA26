package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.KhachHangPhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhachHangPhieuGiamGiaRepository extends JpaRepository<KhachHangPhieuGiamGia, Long> {
    @Query("SELECT kp FROM KhachHangPhieuGiamGia kp JOIN FETCH kp.idKhachHang WHERE kp.idPhieuGiamGia.id = :idPhieu")
    List<KhachHangPhieuGiamGia> findByIdPhieuGiamGia_Id(@Param("idPhieu") Long idPhieu);

    @Query("SELECT kp.idKhachHang.id FROM KhachHangPhieuGiamGia kp WHERE kp.idPhieuGiamGia.id = :idPhieu")
    List<Long> findIdKhachHangByIdPhieu(@Param("idPhieu") Long idPhieu);

    void deleteByIdPhieuGiamGia_Id(Long idPhieu);
}

