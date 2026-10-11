package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.ChiTietHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietHoaDonRepository extends JpaRepository<ChiTietHoaDon, Long> {
    // Khớp theo trường `idHoaDon` và thuộc tính `id` của entity HoaDon
    @Query("SELECT c FROM ChiTietHoaDon c " +
           "LEFT JOIN FETCH c.idChiTietSanPham ct " +
           "LEFT JOIN FETCH ct.idSanPham " +
           "LEFT JOIN FETCH ct.idKichCo " +
           "LEFT JOIN FETCH ct.idMauSac " +
           "WHERE c.idHoaDon.id = :idHoaDon")
    List<ChiTietHoaDon> findByIdHoaDon_Id(@Param("idHoaDon") Long idHoaDon);

    // Khớp theo trường `idHoaDon` và thuộc tính `maHoaDon` của entity HoaDon
    @Query("SELECT c FROM ChiTietHoaDon c " +
           "LEFT JOIN FETCH c.idChiTietSanPham ct " +
           "LEFT JOIN FETCH ct.idSanPham " +
           "LEFT JOIN FETCH ct.idKichCo " +
           "LEFT JOIN FETCH ct.idMauSac " +
           "WHERE c.idHoaDon.maHoaDon = :maHoaDon")
    List<ChiTietHoaDon> findByIdHoaDon_MaHoaDon(@Param("maHoaDon") String maHoaDon);
}
