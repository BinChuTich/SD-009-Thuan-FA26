package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.PhuongThucThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhuongThucThanhToanRepository extends JpaRepository<PhuongThucThanhToan, Long> {
    Optional<PhuongThucThanhToan> findByTenPhuongThuc(String tenPhuongThuc);
}

