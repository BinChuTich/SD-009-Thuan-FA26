package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.HinhAnh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HinhAnhRepository extends JpaRepository<HinhAnh, Long> {
    List<HinhAnh> findByIdSanPham_Id(Long idSanPham);
    List<HinhAnh> findByIdSanPham_IdAndTrangThai(Long idSanPham, Integer trangThai);
}
