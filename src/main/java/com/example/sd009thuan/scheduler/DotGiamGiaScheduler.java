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
     * Tự động quét và cập nhật trạng thái đợt giảm giá hết hạn mỗi 5 giây
     */
    @Scheduled(fixedRate = 5000)
    public void tuDongCapNhatHetHan() {
        try {
            dotGiamGiaService.autoUpdateExpiredStatus();
        } catch (Exception ignored) {
        }
    }
}

