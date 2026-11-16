package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.CaLamViecRequest;
import com.example.sd009thuan.entity.CaLamViec;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface CaLamViecService {
    List<CaLamViec> getAll();

    List<CaLamViec> search(String keyword, Integer trangThai, LocalTime gioBatDau, LocalTime gioKetThuc);

    CaLamViec getById(Long id);

    CaLamViec create(CaLamViecRequest request);

    CaLamViec update(Long id, CaLamViecRequest request);

    CaLamViec toggleStatus(Long id);

    void delete(Long id);

    Map<String, Object> checkTenCa(String tenCa, Long excludeId);

    void validateCa(CaLamViecRequest req, Long excludeId);
}

