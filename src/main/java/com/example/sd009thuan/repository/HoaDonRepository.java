package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {
    Optional<HoaDon> findByMaHoaDon(String maHoaDon);
}