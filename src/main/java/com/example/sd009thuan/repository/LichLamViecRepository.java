package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.LichLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LichLamViecRepository extends JpaRepository<LichLamViec, Long> {
    List<LichLamViec> findByNgayLamViecBetweenAndTrangThaiOrderByNgayLamViecAsc(
            LocalDate fromDate, LocalDate toDate, Integer trangThai);

    List<LichLamViec> findByNgayLamViecBetweenOrderByNgayLamViecAsc(
            LocalDate fromDate, LocalDate toDate);

    List<LichLamViec> findByNhanVienIdAndNgayLamViecAndTrangThai(
            Long nhanVienId, LocalDate ngayLamViec, Integer trangThai);

    @Query("SELECT l FROM LichLamViec l WHERE l.nhanVien.id = :nhanVienId AND l.caLamViec.id = :caId AND l.ngayLamViec = :ngayLamViec")
    Optional<LichLamViec> findByNhanVienIdAndCaLamViecIdAndNgayLamViec(@Param("nhanVienId") Long nhanVienId, @Param("caId") Long caId, @Param("ngayLamViec") LocalDate ngayLamViec);



    List<LichLamViec> findByCaLamViecIdAndNgayLamViecGreaterThanEqualAndTrangThai(
            Long caId, LocalDate fromDate, Integer trangThai);

    List<LichLamViec> findByNhanVienIdAndNgayLamViecGreaterThanEqualAndTrangThai(
            Long nhanVienId, LocalDate fromDate, Integer trangThai);

    @Query("SELECT COUNT(l) FROM LichLamViec l WHERE l.caLamViec.id = :caId AND l.ngayLamViec >= :today AND l.trangThai = 1")
    long countUpcomingActiveAssignmentsByCa(@Param("caId") Long caId, @Param("today") LocalDate today);

    @Query("SELECT l FROM LichLamViec l WHERE l.nhanVien.id = :nhanVienId AND l.ngayLamViec >= :today AND l.trangThai = 1 ORDER BY l.ngayLamViec ASC, l.caLamViec.gioBatDau ASC")
    List<LichLamViec> findUpcomingActiveByNhanVien(@Param("nhanVienId") Long nhanVienId, @Param("today") LocalDate today);
}

