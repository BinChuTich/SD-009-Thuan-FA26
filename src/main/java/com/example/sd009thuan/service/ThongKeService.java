package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ThongKeOverviewDTO;
import com.example.sd009thuan.dto.ThongKeSoSanhDTO;

import java.time.LocalDate;
import java.util.Map;

public interface ThongKeService {
    ThongKeOverviewDTO getOverview(LocalDate fromDate, LocalDate toDate, String statType, Long brandId, String productKeyword);

    byte[] exportExcel(LocalDate fromDate, LocalDate toDate, String statType, Long brandId, String productKeyword);

    ThongKeSoSanhDTO getComparison(String compareMode, Integer year, Integer startYear, Integer endYear,
                                   Long brandId, String productKeyword,
                                   String fromDateA, String toDateA, String fromDateB, String toDateB);

    byte[] exportComparisonExcel(String compareMode, Integer year, Integer startYear, Integer endYear,
                                 Long brandId, String productKeyword,
                                 String fromDateA, String toDateA, String fromDateB, String toDateB);

    Map<String, Object> sendReportEmail(String reportType, Integer year, Integer month, String recipientEmail);
}
