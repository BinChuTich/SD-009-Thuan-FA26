package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VaiTroRepository extends JpaRepository<VaiTro, Long> {
    List<VaiTro> findByTrangThaiOrderByIdAsc(Integer trangThai);
}
