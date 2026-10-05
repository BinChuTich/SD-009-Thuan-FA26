package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.MauSac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MauSacRepository extends JpaRepository<MauSac, Long> {
    List<MauSac> findByTrangThai(Integer trangThai);
}
