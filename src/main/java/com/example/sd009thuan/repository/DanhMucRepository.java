package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.DanhMuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DanhMucRepository extends JpaRepository<DanhMuc, Long> {
    List<DanhMuc> findByTrangThai(Integer trangThai);
    boolean existsByMaDanhMuc(String maDanhMuc);
    boolean existsByMaDanhMucAndIdNot(String maDanhMuc, Long id);
    boolean existsByTenDanhMucIgnoreCase(String tenDanhMuc);
    boolean existsByTenDanhMucIgnoreCaseAndIdNot(String tenDanhMuc, Long id);
}
