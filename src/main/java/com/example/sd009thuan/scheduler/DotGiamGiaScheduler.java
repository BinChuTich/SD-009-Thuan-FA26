package com.example.sd009thuan.scheduler;

import com.example.sd009thuan.service.DotGiamGiaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DotGiamGiaScheduler {

    private final DotGiamGiaService dotGiamGiaService;

    public DotGiamGiaScheduler(DotGiamGiaService dotGiamGiaService) {
        this.dotGiamGiaService = dotGiamGiaService;
    }

    /**
     * Tự động quét trạng thái đợt giảm giá (trạng thái hiển thị theo thời gian thực)
     */
    @Scheduled(fixedRate = 10000)
    public void tuDongCapNhatHetHan() {
        // Trạng thái (Sắp diễn ra, Đang diễn ra, Đã kết thúc) được tính toán theo thời gian thực (real-time)
        // Không ép trang_thai = 0 để tránh bị nhầm sang "Ngừng hoạt động"
    }
}

