package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.MauSac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MauSacRepository extends JpaRepository<MauSac, Long> {
    List<MauSac> findByTrangThai(Integer trangThai);
    boolean existsByMaMauSac(String maMauSac);
    boolean existsByMaMauSacAndIdNot(String maMauSac, Long id);
    boolean existsByTenMauSacIgnoreCase(String tenMauSac);
    boolean existsByTenMauSacIgnoreCaseAndIdNot(String tenMauSac, Long id);
}
