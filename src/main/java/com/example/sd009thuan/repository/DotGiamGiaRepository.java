package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.DotGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DotGiamGiaRepository extends JpaRepository<DotGiamGia, Long> {

    Optional<DotGiamGia> findByMaDotGiamGia(String maDotGiamGia);

    boolean existsByMaDotGiamGia(String maDotGiamGia);

    @Query("SELECT d FROM DotGiamGia d WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(d.maDotGiamGia) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(d.tenDotGiamGia) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " (:giaTri IS NOT NULL AND d.phanTramGiam = :giaTri) OR " +
            " (:cleanKw IS NOT NULL AND :cleanKw != '' AND CAST(d.phanTramGiam AS string) LIKE CONCAT('%', :cleanKw, '%'))) AND " +
            "(:trangThaiStr IS NULL OR :trangThaiStr = '' OR " +
            " (:trangThaiStr = '0' AND d.trangThai = 0) OR " +
            " (:trangThaiStr = '1' AND d.trangThai = 1) OR " +
            " (:trangThaiStr = 'ACTIVE' AND d.trangThai = 1 AND (d.ngayBatDau IS NULL OR d.ngayBatDau <= :now) AND (d.ngayKetThuc IS NULL OR d.ngayKetThuc > :now)) OR " +
            " (:trangThaiStr = 'UPCOMING' AND d.trangThai = 1 AND d.ngayBatDau IS NOT NULL AND d.ngayBatDau > :now) OR " +
            " (:trangThaiStr = 'ENDED' AND d.trangThai = 1 AND d.ngayKetThuc IS NOT NULL AND d.ngayKetThuc <= :now)) AND " +
            "(:tuNgayBatDau IS NULL OR d.ngayBatDau >= :tuNgayBatDau) AND " +
            "(:denNgayBatDau IS NULL OR d.ngayBatDau <= :denNgayBatDau) AND " +
            "(:tuNgayKetThuc IS NULL OR d.ngayKetThuc >= :tuNgayKetThuc) AND " +
            "(:denNgayKetThuc IS NULL OR d.ngayKetThuc <= :denNgayKetThuc)")
    Page<DotGiamGia> search(
            @Param("keyword") String keyword,
            @Param("cleanKw") String cleanKw,
            @Param("giaTri") BigDecimal giaTri,
            @Param("trangThaiStr") String trangThaiStr,
            @Param("now") Instant now,
            @Param("tuNgayBatDau") Instant tuNgayBatDau,
            @Param("denNgayBatDau") Instant denNgayBatDau,
            @Param("tuNgayKetThuc") Instant tuNgayKetThuc,
            @Param("denNgayKetThuc") Instant denNgayKetThuc,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE DotGiamGia d SET d.trangThai = 0 WHERE d.trangThai = 1 AND d.ngayKetThuc <= :now")
    int updateHetHan(@Param("now") Instant now);

    @Query("SELECT d FROM DotGiamGia d WHERE d.trangThai = 1 " +
           "AND (d.ngayBatDau IS NULL OR d.ngayBatDau <= :now) " +
           "AND (d.ngayKetThuc IS NULL OR d.ngayKetThuc >= :now)")
    List<DotGiamGia> findActiveCampaigns(@Param("now") Instant now);
}
