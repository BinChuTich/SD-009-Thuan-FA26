package com.example.sd009thuan.repository;

import com.example.sd009thuan.entity.TinNhanChat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TinNhanChatRepository extends JpaRepository<TinNhanChat, Long> {
    List<TinNhanChat> findByIdCuocHoiThoai_IdOrderByNgayGuiAscIdAsc(Long conversationId);
}
