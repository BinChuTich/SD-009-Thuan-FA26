package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.LichSuCaLamViec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface LichSuCaLamViecRepository extends JpaRepository<LichSuCaLamViec, Long> {
    Optional<LichSuCaLamViec> findTopByNhanVienIdAndTrangThaiOrderByThoiGianMoCaDesc(Long nhanVienId, Integer trangThai);

    Optional<LichSuCaLamViec> findTopByTrangThaiOrderByThoiGianDongCaDesc(Integer trangThai);

    @Query("SELECT l FROM LichSuCaLamViec l WHERE " +
           "(:keyword IS NULL OR LOWER(l.nhanVien.tenTaiKhoan) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "                  OR LOWER(l.nhanVien.tenNhanVien) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "                  OR LOWER(l.nhanVien.maNhanVien) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "                  OR (l.maGiaoCa IS NOT NULL AND LOWER(l.maGiaoCa) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "                  OR (l.lichLamViec IS NOT NULL AND LOWER(l.lichLamViec.caLamViec.maCa) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "                  OR (l.lichLamViec IS NOT NULL AND LOWER(l.lichLamViec.caLamViec.tenCa) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "                  OR EXISTS (SELECT 1 FROM HoaDon hd WHERE hd.idNhanVien.id = l.nhanVien.id AND LOWER(hd.maHoaDon) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "AND (:fromDate IS NULL OR l.thoiGianMoCa >= :fromDate) " +
           "AND (:toDate IS NULL OR l.thoiGianMoCa <= :toDate) " +
           "AND (:trangThai IS NULL OR l.trangThai = :trangThai) " +
           "ORDER BY l.thoiGianMoCa DESC")
    Page<LichSuCaLamViec> searchHistory(
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("trangThai") Integer trangThai,
            Pageable pageable
    );
}
