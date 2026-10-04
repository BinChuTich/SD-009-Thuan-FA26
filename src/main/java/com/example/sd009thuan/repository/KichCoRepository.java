package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.KichCo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KichCoRepository extends JpaRepository<KichCo, Long> {
    List<KichCo> findByTrangThai(Integer trangThai);
}
