package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.ChiTietSanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, Long> {

    List<ChiTietSanPham> findByIdSanPham_Id(Long idSanPham);

    Optional<ChiTietSanPham> findByMaChiTietSanPham(String maChiTietSanPham);

    boolean existsByMaChiTietSanPham(String maChiTietSanPham);

    boolean existsByMaChiTietSanPhamAndIdNot(String maChiTietSanPham, Long id);

    boolean existsByIdMauSac_Id(Long idMauSac);

    boolean existsByIdKichCo_Id(Long idKichCo);

    boolean existsByIdSanPham_IdAndIdKichCo_IdAndIdMauSac_Id(Long idSanPham, Long idKichCo, Long idMauSac);

    boolean existsByIdSanPham_IdAndIdKichCo_IdAndIdMauSac_IdAndIdNot(Long idSanPham, Long idKichCo, Long idMauSac, Long id);

    @Query("SELECT ct FROM ChiTietSanPham ct " +
           "LEFT JOIN FETCH ct.idSanPham " +
           "LEFT JOIN FETCH ct.idKichCo " +
           "LEFT JOIN FETCH ct.idMauSac " +
           "WHERE ct.id = :id")
    Optional<ChiTietSanPham> findByIdWithDetails(@Param("id") Long id);

    Optional<ChiTietSanPham> findByIdSanPham_IdAndIdKichCo_IdAndIdMauSac_Id(Long idSanPham, Long idKichCo, Long idMauSac);

    @Query("SELECT MIN(ct.giaBan) FROM ChiTietSanPham ct WHERE ct.idSanPham.id = :idSanPham AND ct.trangThai = 1")
    BigDecimal findMinPriceBySanPhamId(@Param("idSanPham") Long idSanPham);

    @Query("SELECT MAX(ct.giaBan) FROM ChiTietSanPham ct WHERE ct.idSanPham.id = :idSanPham AND ct.trangThai = 1")
    BigDecimal findMaxPriceBySanPhamId(@Param("idSanPham") Long idSanPham);

    @Query("SELECT SUM(ct.soLuong) FROM ChiTietSanPham ct WHERE ct.idSanPham.id = :idSanPham AND ct.trangThai = 1")
    Long sumSoLuongBySanPhamId(@Param("idSanPham") Long idSanPham);

    @Query("SELECT ct FROM ChiTietSanPham ct WHERE " +
           "(:idSanPham IS NULL OR ct.idSanPham.id = :idSanPham) " +
           "AND (:idKichCo IS NULL OR ct.idKichCo.id = :idKichCo) " +
           "AND (:idMauSac IS NULL OR ct.idMauSac.id = :idMauSac) " +
           "AND (:trangThai IS NULL OR ct.trangThai = :trangThai) " +
           "AND (:minGia IS NULL OR ct.giaBan >= :minGia) " +
           "AND (:maxGia IS NULL OR ct.giaBan <= :maxGia) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(ct.maChiTietSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(ct.idSanPham.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(ct.idSanPham.maSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(ct.idMauSac.tenMauSac) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(ct.idKichCo.tenKichCo) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ChiTietSanPham> filterVariants(
            @Param("idSanPham") Long idSanPham,
            @Param("keyword") String keyword,
            @Param("idKichCo") Long idKichCo,
            @Param("idMauSac") Long idMauSac,
            @Param("minGia") BigDecimal minGia,
            @Param("maxGia") BigDecimal maxGia,
            @Param("trangThai") Integer trangThai,
            Pageable pageable
    );

    @Query("SELECT ct FROM ChiTietSanPham ct " +
           "LEFT JOIN FETCH ct.idSanPham sp " +
           "LEFT JOIN FETCH sp.idThuongHieu " +
           "LEFT JOIN FETCH ct.idKichCo " +
           "LEFT JOIN FETCH ct.idMauSac")
    List<ChiTietSanPham> findAllWithProductDetails();
}
