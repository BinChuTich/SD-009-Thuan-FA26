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

    /**
     * Tìm các đợt giảm giá chưa kết thúc đang chứa các biến thể hoặc sản phẩm cha này
     * Quy tắc: 1 sản phẩm chỉ được tạo 1 đợt giảm giá, khi đợt đó kết thúc mới tạo lại được
     */
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
           "AND (ct.idDotGiamGia.ngayKetThuc IS NULL OR ct.idDotGiamGia.ngayKetThuc > :now) " +
           "AND (:excludeDotId IS NULL OR ct.idDotGiamGia.id != :excludeDotId)")
    List<Long> findActiveProductIds(@Param("now") Instant now, @Param("excludeDotId") Long excludeDotId);

    /**
     * Lấy danh sách chi tiết các đợt giảm giá chưa kết thúc (để ánh xạ sản phẩm -> đợt giảm giá)
     */
    @Query("SELECT ct FROM ChiTietDotGiamGia ct " +
           "WHERE ct.idDotGiamGia.trangThai = 1 " +
           "AND (ct.idDotGiamGia.ngayKetThuc IS NULL OR ct.idDotGiamGia.ngayKetThuc > :now) " +
           "AND (:excludeDotId IS NULL OR ct.idDotGiamGia.id != :excludeDotId)")
    List<ChiTietDotGiamGia> findActiveChiTiets(@Param("now") Instant now, @Param("excludeDotId") Long excludeDotId);
}
