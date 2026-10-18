package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ExcelImportResult;
import com.example.sd009thuan.dto.LichLamViecRequest;
import com.example.sd009thuan.dto.ThayTheNhanVienRequest;
import com.example.sd009thuan.entity.LichLamViec;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LichLamViecService {
    List<LichLamViec> getWeeklySchedule(LocalDate startDate, LocalDate endDate);

    LichLamViec assignShift(LichLamViecRequest request);

    void removeShift(Long id);

    List<LichLamViec> getUpcomingShiftsForEmployee(Long nhanVienId);

    Map<String, Object> checkSyncShiftsWhenDeactivatingEmployee(Long nhanVienId);

    void replaceEmployeeInShifts(ThayTheNhanVienRequest request);

    ExcelImportResult importExcel(MultipartFile file);

    byte[] exportTemplate();
}
