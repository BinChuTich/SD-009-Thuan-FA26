package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VaiTroRepository extends JpaRepository<VaiTro, Long> {
    List<VaiTro> findByTrangThaiOrderByIdAsc(Integer trangThai);
    Optional<VaiTro> findByMaVaiTroIgnoreCaseAndTrangThai(String maVaiTro, Integer trangThai);
}