package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.DongMoCaRequest;
import com.example.sd009thuan.dto.ShiftCashSummaryDTO;
import com.example.sd009thuan.entity.LichSuCaLamViec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface LichSuCaLamViecService {
    Page<LichSuCaLamViec> searchHistory(String keyword, LocalDate fromDate, LocalDate toDate, Integer trangThai, Pageable pageable);

    LichSuCaLamViec moCa(DongMoCaRequest request);

    LichSuCaLamViec dongCa(DongMoCaRequest request);

    Optional<LichSuCaLamViec> getCurrentOpenShift(Long nhanVienId);

    ShiftCashSummaryDTO getShiftSummary(Long id);

    Optional<LichSuCaLamViec> getById(Long id);

    Optional<LichSuCaLamViec> getLastClosedShift();
}
