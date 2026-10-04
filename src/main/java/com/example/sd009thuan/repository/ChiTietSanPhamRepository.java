package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.ChiTietSanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, Long>, JpaSpecificationExecutor<ChiTietSanPham> {

    List<ChiTietSanPham> findByIdSanPham_Id(Long idSanPham);

    Page<ChiTietSanPham> findByIdSanPham_Id(Long idSanPham, Pageable pageable);

    boolean existsByMaChiTietSanPham(String maChiTietSanPham);

    boolean existsByMaChiTietSanPhamAndIdNot(String maChiTietSanPham, Long id);

    Optional<ChiTietSanPham> findByIdSanPham_IdAndIdKichCo_IdAndIdMauSac_Id(Long idSanPham, Long idKichCo, Long idMauSac);

    @Query("SELECT MIN(ct.giaBan) FROM ChiTietSanPham ct WHERE ct.idSanPham.id = :idSanPham AND ct.trangThai = 1")
    BigDecimal findMinPriceBySanPhamId(@Param("idSanPham") Long idSanPham);

    @Query("SELECT MAX(ct.giaBan) FROM ChiTietSanPham ct WHERE ct.idSanPham.id = :idSanPham AND ct.trangThai = 1")
    BigDecimal findMaxPriceBySanPhamId(@Param("idSanPham") Long idSanPham);

    @Query("SELECT COALESCE(SUM(ct.soLuong), 0) FROM ChiTietSanPham ct WHERE ct.idSanPham.id = :idSanPham AND ct.trangThai = 1")
    Integer sumSoLuongBySanPhamId(@Param("idSanPham") Long idSanPham);

    @Query("SELECT ct FROM ChiTietSanPham ct WHERE " +
           "ct.idSanPham.id = :idSanPham " +
           "AND (:idKichCo IS NULL OR ct.idKichCo.id = :idKichCo) " +
           "AND (:idMauSac IS NULL OR ct.idMauSac.id = :idMauSac) " +
           "AND (:trangThai IS NULL OR ct.trangThai = :trangThai) " +
           "AND (:minGia IS NULL OR ct.giaBan >= :minGia) " +
           "AND (:maxGia IS NULL OR ct.giaBan <= :maxGia)")
    Page<ChiTietSanPham> filterVariants(
            @Param("idSanPham") Long idSanPham,
            @Param("idKichCo") Long idKichCo,
            @Param("idMauSac") Long idMauSac,
            @Param("minGia") BigDecimal minGia,
            @Param("maxGia") BigDecimal maxGia,
            @Param("trangThai") Integer trangThai,
            Pageable pageable
    );
}
