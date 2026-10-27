package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.CuocHoiThoai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CuocHoiThoaiRepository extends JpaRepository<CuocHoiThoai, Long> {

    Optional<CuocHoiThoai> findTopByKhachHang_IdAndTrangThaiInOrderByIdDesc(Long khachHangId, Collection<Integer> trangThais);

    Optional<CuocHoiThoai> findTopBySoDienThoaiAndTrangThaiInOrderByIdDesc(String soDienThoai, Collection<Integer> trangThais);

    Optional<CuocHoiThoai> findTopByKhachHang_IdAndTrangThaiOrderByIdDesc(Long khachHangId, Integer trangThai);

    Optional<CuocHoiThoai> findTopBySoDienThoaiAndTrangThaiOrderByIdDesc(String soDienThoai, Integer trangThai);

    List<CuocHoiThoai> findAllByOrderByNgayCapNhatDesc();

    @Query("SELECT c FROM CuocHoiThoai c WHERE (:trangThai IS NULL OR c.trangThai = :trangThai) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(c.tenKhachHang) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR c.soDienThoai LIKE CONCAT('%', :keyword, '%')) ORDER BY c.ngayCapNhat DESC")
    List<CuocHoiThoai> searchCuocHoiThoai(@Param("keyword") String keyword, @Param("trangThai") Integer trangThai);
}

