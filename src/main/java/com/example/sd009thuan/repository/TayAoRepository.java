package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.TayAo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TayAoRepository extends JpaRepository<TayAo, Long> {
    List<TayAo> findByTrangThai(Integer trangThai);
}
