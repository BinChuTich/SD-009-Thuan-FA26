package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {
    Optional<HoaDon> findByMaHoaDon(String maHoaDon);

    @Query("SELECT h FROM HoaDon h " +
           "LEFT JOIN FETCH h.idNhanVien " +
           "LEFT JOIN FETCH h.idKhachHang " +
           "LEFT JOIN FETCH h.idPhieuGiamGia " +
           "ORDER BY h.id DESC")
    List<HoaDon> findAllWithForeignKeys();

    @Query("SELECT h FROM HoaDon h " +
           "LEFT JOIN FETCH h.idNhanVien " +
           "LEFT JOIN FETCH h.idKhachHang " +
           "LEFT JOIN FETCH h.idPhieuGiamGia " +
           "WHERE h.maHoaDon = :maHoaDon")
    Optional<HoaDon> findByMaHoaDonWithForeignKeys(@Param("maHoaDon") String maHoaDon);

    @Query("SELECT h FROM HoaDon h " +
           "LEFT JOIN FETCH h.idNhanVien " +
           "LEFT JOIN FETCH h.idKhachHang " +
           "LEFT JOIN FETCH h.idPhieuGiamGia " +
           "WHERE h.id = :id")
    Optional<HoaDon> findByIdWithForeignKeys(@Param("id") Long id);
}
