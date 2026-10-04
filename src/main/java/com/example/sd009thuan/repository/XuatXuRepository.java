package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.XuatXu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface XuatXuRepository extends JpaRepository<XuatXu, Long> {
    List<XuatXu> findByTrangThai(Integer trangThai);
}
