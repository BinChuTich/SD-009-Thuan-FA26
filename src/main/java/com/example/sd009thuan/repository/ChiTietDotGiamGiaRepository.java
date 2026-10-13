package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.ChiTietDotGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDotGiamGiaRepository extends JpaRepository<ChiTietDotGiamGia, Long> {

    List<ChiTietDotGiamGia> findByIdChiTietSanPham_IdAndTrangThai(Long idChiTietSanPham, Integer trangThai);

    List<ChiTietDotGiamGia> findByIdDotGiamGia_Id(Long idDotGiamGia);

    void deleteByIdDotGiamGia_Id(Long idDotGiamGia);

    void deleteByIdChiTietSanPham_Id(Long idChiTietSanPham);

    @Query("SELECT cd FROM ChiTietDotGiamGia cd " +
           "JOIN FETCH cd.idDotGiamGia d " +
           "WHERE cd.idChiTietSanPham.idSanPham.id = :idSanPham " +
           "AND cd.trangThai = 1 AND d.trangThai = 1")
    List<ChiTietDotGiamGia> findActiveDiscountsBySanPhamId(@Param("idSanPham") Long idSanPham);

    @Query("SELECT cd FROM ChiTietDotGiamGia cd " +
           "JOIN FETCH cd.idDotGiamGia d " +
           "WHERE cd.idChiTietSanPham.id = :idCtsp " +
           "AND cd.trangThai = 1 AND d.trangThai = 1")
    List<ChiTietDotGiamGia> findActiveDiscountsByCtspId(@Param("idCtsp") Long idCtsp);
}
