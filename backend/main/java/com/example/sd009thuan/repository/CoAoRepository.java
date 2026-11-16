package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.CoAo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoAoRepository extends JpaRepository<CoAo, Long> {
    List<CoAo> findByTrangThai(Integer trangThai);
    boolean existsByMaCoAo(String maCoAo);
    boolean existsByMaCoAoAndIdNot(String maCoAo, Long id);
    boolean existsByTenCoAoIgnoreCase(String tenCoAo);
    boolean existsByTenCoAoIgnoreCaseAndIdNot(String tenCoAo, Long id);
}
