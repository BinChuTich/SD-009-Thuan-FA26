package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.ChatLieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatLieuRepository extends JpaRepository<ChatLieu, Long> {
    List<ChatLieu> findByTrangThai(Integer trangThai);
    boolean existsByMaChatLieu(String maChatLieu);
    boolean existsByMaChatLieuAndIdNot(String maChatLieu, Long id);
    boolean existsByTenChatLieuIgnoreCase(String tenChatLieu);
    boolean existsByTenChatLieuIgnoreCaseAndIdNot(String tenChatLieu, Long id);
}
