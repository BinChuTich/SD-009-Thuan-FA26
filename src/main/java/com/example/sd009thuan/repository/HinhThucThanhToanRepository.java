package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.HinhThucThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HinhThucThanhToanRepository extends JpaRepository<HinhThucThanhToan, Long> {
    @Query("SELECT h FROM HinhThucThanhToan h " +
           "LEFT JOIN FETCH h.idPhuongThuc " +
           "WHERE h.idHoaDon.id = :idHoaDon " +
           "ORDER BY h.thoiGian DESC")
    List<HinhThucThanhToan> findByIdHoaDon_Id(@Param("idHoaDon") Long idHoaDon);

    @Query("SELECT h FROM HinhThucThanhToan h " +
           "LEFT JOIN FETCH h.idPhuongThuc " +
           "WHERE h.idHoaDon.maHoaDon = :maHoaDon " +
           "ORDER BY h.thoiGian DESC")
    List<HinhThucThanhToan> findByIdHoaDon_MaHoaDon(@Param("maHoaDon") String maHoaDon);

    @Query("SELECT h FROM HinhThucThanhToan h " +
           "LEFT JOIN FETCH h.idPhuongThuc " +
           "WHERE h.idHoaDon.id IN :hoaDonIds " +
           "ORDER BY h.thoiGian DESC")
    List<HinhThucThanhToan> findByIdHoaDon_IdIn(@Param("hoaDonIds") List<Long> hoaDonIds);
}

