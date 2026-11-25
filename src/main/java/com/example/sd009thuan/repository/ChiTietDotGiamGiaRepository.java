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

    List<ChiTietDotGiamGia> findByIdDotGiamGia_Id(Long idDotGiamGia);

    @Modifying
    @Query("DELETE FROM ChiTietDotGiamGia ct WHERE ct.idDotGiamGia.id = :idDotGiamGia")
    void deleteByIdDotGiamGiaId(@Param("idDotGiamGia") Long idDotGiamGia);

    @Query("SELECT ct FROM ChiTietDotGiamGia ct " +
           "WHERE (ct.idChiTietSanPham.id IN :ctspIds " +
           "       OR ct.idChiTietSanPham.idSanPham.id IN :spIds) " +
           "AND (:currentDotId IS NULL OR ct.idDotGiamGia.id != :currentDotId) " +
           "AND ct.idDotGiamGia.trangThai = 1 " +
           "AND (ct.idDotGiamGia.ngayKetThuc IS NULL OR ct.idDotGiamGia.ngayKetThuc > :now)")
    List<ChiTietDotGiamGia> findUnendedCampaignConflicts(
            @Param("ctspIds") List<Long> ctspIds,
            @Param("spIds") List<Long> spIds,
            @Param("currentDotId") Long currentDotId,
            @Param("now") Instant now
    );

    /**
     * Lấy danh sách ID các sản phẩm cha đang nằm trong đợt giảm giá chưa kết thúc
     */
    @Query("SELECT DISTINCT ct.idChiTietSanPham.idSanPham.id FROM ChiTietDotGiamGia ct " +
           "WHERE ct.idDotGiamGia.trangThai = 1 " +
           "AND (ct.idDotGiamGia.ngayBatDau IS NULL OR ct.idDotGiamGia.ngayBatDau <= :now) " +
           "AND (ct.idDotGiamGia.ngayKetThuc IS NULL OR ct.idDotGiamGia.ngayKetThuc > :now) " +
           "AND (ct.trangThai = 1 OR ct.trangThai IS NULL) " +
           "AND (:excludeDotId IS NULL OR ct.idDotGiamGia.id != :excludeDotId)")
    List<Long> findActiveProductIds(@Param("now") Instant now, @Param("excludeDotId") Long excludeDotId);

    /**
     * Lấy danh sách chi tiết các đợt giảm giá chưa kết thúc (để ánh xạ sản phẩm -> đợt giảm giá)
     */
    @Query("SELECT ct FROM ChiTietDotGiamGia ct " +
           "WHERE ct.idDotGiamGia.trangThai = 1 " +
           "AND (ct.idDotGiamGia.ngayBatDau IS NULL OR ct.idDotGiamGia.ngayBatDau <= :now) " +
           "AND (ct.idDotGiamGia.ngayKetThuc IS NULL OR ct.idDotGiamGia.ngayKetThuc > :now) " +
           "AND (ct.trangThai = 1 OR ct.trangThai IS NULL) " +
           "AND (:excludeDotId IS NULL OR ct.idDotGiamGia.id != :excludeDotId)")
    List<ChiTietDotGiamGia> findActiveChiTiets(@Param("now") Instant now, @Param("excludeDotId") Long excludeDotId);

    /**
     * Tìm các chi tiết đợt giảm giá đang hoạt động cho một biến thể sản phẩm cụ thể
     */
    @Query("SELECT ct FROM ChiTietDotGiamGia ct " +
           "WHERE ct.idChiTietSanPham.id = :variantId " +
           "AND ct.idDotGiamGia.trangThai = 1 " +
           "AND (ct.idDotGiamGia.ngayBatDau IS NULL OR ct.idDotGiamGia.ngayBatDau <= :now) " +
           "AND (ct.idDotGiamGia.ngayKetThuc IS NULL OR ct.idDotGiamGia.ngayKetThuc > :now) " +
           "AND (ct.trangThai = 1 OR ct.trangThai IS NULL) " +
           "ORDER BY ct.idDotGiamGia.id DESC")
    List<ChiTietDotGiamGia> findActiveDiscountsByVariantId(
            @Param("variantId") Long variantId,
            @Param("now") Instant now
    );

    /**
     * Tìm các chi tiết đợt giảm giá đang hoạt động cho một sản phẩm cha cụ thể
     */
    @Query("SELECT ct FROM ChiTietDotGiamGia ct " +
           "WHERE ct.idChiTietSanPham.idSanPham.id = :sanPhamId " +
           "AND ct.idDotGiamGia.trangThai = 1 " +
           "AND (ct.idDotGiamGia.ngayBatDau IS NULL OR ct.idDotGiamGia.ngayBatDau <= :now) " +
           "AND (ct.idDotGiamGia.ngayKetThuc IS NULL OR ct.idDotGiamGia.ngayKetThuc > :now) " +
           "AND (ct.trangThai = 1 OR ct.trangThai IS NULL) " +
           "ORDER BY ct.idDotGiamGia.id DESC")
    List<ChiTietDotGiamGia> findActiveDiscountsBySanPhamId(
            @Param("sanPhamId") Long sanPhamId,
            @Param("now") Instant now
    );
}
