package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ThongKeOverviewDTO;

import java.time.LocalDate;

public interface ThongKeService {
    ThongKeOverviewDTO getOverview(LocalDate fromDate, LocalDate toDate, String statType, Long brandId, String productKeyword);

    byte[] exportExcel(LocalDate fromDate, LocalDate toDate, String statType, Long brandId, String productKeyword);
}

