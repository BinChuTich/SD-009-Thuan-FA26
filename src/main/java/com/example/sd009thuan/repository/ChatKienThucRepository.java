package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.ChatKienThuc;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatKienThucRepository extends JpaRepository<ChatKienThuc, Long> {
    List<ChatKienThuc> findByKichHoatTrue();
}
