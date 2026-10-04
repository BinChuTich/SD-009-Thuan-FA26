package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.HoaTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoaTietRepository extends JpaRepository<HoaTiet, Long> {
    List<HoaTiet> findByTrangThai(Integer trangThai);
}
