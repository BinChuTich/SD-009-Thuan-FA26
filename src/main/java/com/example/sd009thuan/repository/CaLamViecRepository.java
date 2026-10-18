package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.CaLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaLamViecRepository extends JpaRepository<CaLamViec, Long> {
    Optional<CaLamViec> findByMaCa(String maCa);

    Optional<CaLamViec> findByTenCaIgnoreCase(String tenCa);

    boolean existsByTenCaIgnoreCase(String tenCa);

    boolean existsByTenCaIgnoreCaseAndIdNot(String tenCa, Long id);

    List<CaLamViec> findByTrangThaiOrderByGioBatDauAsc(Integer trangThai);

    @Query("SELECT c FROM CaLamViec c WHERE " +
           "(:keyword IS NULL OR LOWER(c.tenCa) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.maCa) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:trangThai IS NULL OR c.trangThai = :trangThai) " +
           "AND (:gioBatDau IS NULL OR c.gioBatDau = :gioBatDau) " +
           "AND (:gioKetThuc IS NULL OR c.gioKetThuc = :gioKetThuc) " +
           "ORDER BY c.gioBatDau ASC")
    List<CaLamViec> searchCaLamViec(
            @Param("keyword") String keyword,
            @Param("trangThai") Integer trangThai,
            @Param("gioBatDau") LocalTime gioBatDau,
            @Param("gioKetThuc") LocalTime gioKetThuc
    );
}

