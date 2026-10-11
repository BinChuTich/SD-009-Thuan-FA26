package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.LichSuHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuHoaDonRepository extends JpaRepository<LichSuHoaDon, Long> {
    List<LichSuHoaDon> findByIdHoaDon_IdOrderByThoiGianDesc(Long idHoaDon);

    List<LichSuHoaDon> findByIdHoaDon_MaHoaDonOrderByThoiGianDesc(String maHoaDon);
}

