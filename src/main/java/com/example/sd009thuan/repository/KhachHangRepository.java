package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {
    List<KhachHang> findAllByOrderByTenKhachHangAsc();
}

