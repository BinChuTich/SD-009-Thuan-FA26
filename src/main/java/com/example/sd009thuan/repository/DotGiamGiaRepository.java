package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.DotGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface DotGiamGiaRepository extends JpaRepository<DotGiamGia, Long> {

    Optional<DotGiamGia> findByMaDotGiamGia(String maDotGiamGia);

    boolean existsByMaDotGiamGia(String maDotGiamGia);

    @Query("SELECT d FROM DotGiamGia d WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(d.maDotGiamGia) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(d.tenDotGiamGia) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:trangThai IS NULL OR d.trangThai = :trangThai) AND " +
            "(:tuNgay IS NULL OR d.ngayBatDau >= :tuNgay) AND " +
            "(:denNgay IS NULL OR d.ngayKetThuc <= :denNgay)")
    Page<DotGiamGia> search(
            @Param("keyword") String keyword,
            @Param("trangThai") Integer trangThai,
            @Param("tuNgay") Instant tuNgay,
            @Param("denNgay") Instant denNgay,
            Pageable pageable
    );
}
