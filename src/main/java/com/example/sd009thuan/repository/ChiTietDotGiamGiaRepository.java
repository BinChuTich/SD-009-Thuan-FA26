package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.ChiTietDotGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ChiTietDotGiamGiaRepository extends JpaRepository<ChiTietDotGiamGia, Long> {

    List<ChiTietDotGiamGia> findByIdChiTietSanPham_Id(Long idChiTietSanPham);

    @Query("SELECT c FROM ChiTietDotGiamGia c WHERE c.idDotGiamGia.id = :idDotGiamGia")
    List<ChiTietDotGiamGia> findByIdDotGiamGia_Id(@Param("idDotGiamGia") Long idDotGiamGia);

    @Modifying
    @Query("DELETE FROM ChiTietDotGiamGia c WHERE c.idDotGiamGia.id = :idDotGiamGia")
    void deleteByIdDotGiamGiaId(@Param("idDotGiamGia") Long idDotGiamGia);

    @Modifying
    @Query("DELETE FROM ChiTietDotGiamGia c WHERE c.idDotGiamGia.id = :idDotGiamGia")
    void deleteByIdDotGiamGia_Id(@Param("idDotGiamGia") Long idDotGiamGia);

    @Query("SELECT c FROM ChiTietDotGiamGia c WHERE " +
           "(c.idChiTietSanPham.id IN :ctspIds OR c.idChiTietSanPham.idSanPham.id IN :spIds) " +
           "AND (:currentId IS NULL OR c.idDotGiamGia.id != :currentId) " +
           "AND c.idDotGiamGia.trangThai = 1 " +
           "AND (c.idDotGiamGia.ngayKetThuc IS NULL OR c.idDotGiamGia.ngayKetThuc > :now)")
    List<ChiTietDotGiamGia> findUnendedCampaignConflicts(
            @Param("ctspIds") List<Long> ctspIds,
            @Param("spIds") List<Long> spIds,
            @Param("currentId") Long currentId,
            @Param("now") Instant now
    );

    @Query("SELECT c FROM ChiTietDotGiamGia c WHERE " +
           "c.idDotGiamGia.trangThai = 1 " +
           "AND (c.idDotGiamGia.ngayKetThuc IS NULL OR c.idDotGiamGia.ngayKetThuc > :now) " +
           "AND (:excludeDotId IS NULL OR c.idDotGiamGia.id != :excludeDotId)")
    List<ChiTietDotGiamGia> findActiveChiTiets(
            @Param("now") Instant now,
            @Param("excludeDotId") Long excludeDotId
    );

    @Query("SELECT c FROM ChiTietDotGiamGia c WHERE c.idChiTietSanPham.idSanPham.id = :idSanPham " +
           "AND c.trangThai = 1 " +
           "AND c.idDotGiamGia.trangThai = 1 " +
           "AND (c.idDotGiamGia.ngayBatDau IS NULL OR c.idDotGiamGia.ngayBatDau <= :now) " +
           "AND (c.idDotGiamGia.ngayKetThuc IS NULL OR c.idDotGiamGia.ngayKetThuc >= :now)")
    List<ChiTietDotGiamGia> findActiveDiscountsBySanPhamId(@Param("idSanPham") Long idSanPham, @Param("now") Instant now);

    @Query("SELECT c FROM ChiTietDotGiamGia c WHERE c.idChiTietSanPham.id IN :variantIds " +
           "AND c.trangThai = 1 " +
           "AND c.idDotGiamGia.trangThai = 1 " +
           "AND (c.idDotGiamGia.ngayBatDau IS NULL OR c.idDotGiamGia.ngayBatDau <= :now) " +
           "AND (c.idDotGiamGia.ngayKetThuc IS NULL OR c.idDotGiamGia.ngayKetThuc >= :now)")
    List<ChiTietDotGiamGia> findActiveDiscountsByVariantIds(@Param("variantIds") List<Long> variantIds, @Param("now") Instant now);

    @Query("SELECT c FROM ChiTietDotGiamGia c WHERE c.idChiTietSanPham.id = :variantId " +
           "AND c.trangThai = 1 " +
           "AND c.idDotGiamGia.trangThai = 1 " +
           "AND (c.idDotGiamGia.ngayBatDau IS NULL OR c.idDotGiamGia.ngayBatDau <= :now) " +
           "AND (c.idDotGiamGia.ngayKetThuc IS NULL OR c.idDotGiamGia.ngayKetThuc >= :now)")
    List<ChiTietDotGiamGia> findActiveDiscountsByVariantId(@Param("variantId") Long variantId, @Param("now") Instant now);
}
