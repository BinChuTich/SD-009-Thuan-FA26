package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.ThuongHieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, Long> {
    List<ThuongHieu> findByTrangThai(Integer trangThai);
}
