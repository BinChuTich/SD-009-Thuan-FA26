package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.DotGiamGia;
import com.example.sd009thuan.repository.DotGiamGiaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class DotGiamGiaService {

    private final DotGiamGiaRepository dotGiamGiaRepository;

    public DotGiamGiaService(DotGiamGiaRepository dotGiamGiaRepository) {
        this.dotGiamGiaRepository = dotGiamGiaRepository;
    }

    public Page<DotGiamGia> search(
            String keyword,
            Integer trangThai,
            String startDateStr,
            String endDateStr,
            Pageable pageable
    ) {
        Instant tuNgay = null;
        Instant denNgay = null;

        if (startDateStr != null && !startDateStr.trim().isEmpty()) {
            try {
                LocalDate start = LocalDate.parse(startDateStr.trim());
                tuNgay = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
            } catch (Exception ignored) {
            }
        }

        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            try {
                LocalDate end = LocalDate.parse(endDateStr.trim());
                denNgay = end.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
            } catch (Exception ignored) {
            }
        }

        String searchKw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        return dotGiamGiaRepository.search(searchKw, trangThai, tuNgay, denNgay, pageable);
    }

    public List<DotGiamGia> getAll() {
        return dotGiamGiaRepository.findAll();
    }

    public Optional<DotGiamGia> getById(Long id) {
        return dotGiamGiaRepository.findById(id);
    }

    public Optional<DotGiamGia> getByMa(String maDotGiamGia) {
        return dotGiamGiaRepository.findByMaDotGiamGia(maDotGiamGia);
    }

    public DotGiamGia create(DotGiamGia dotGiamGia) {
        if (dotGiamGia.getTrangThai() == null) {
            dotGiamGia.setTrangThai(1);
        }
        return dotGiamGiaRepository.save(dotGiamGia);
    }

    public DotGiamGia update(Long id, DotGiamGia moi) {
        DotGiamGia cu = dotGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá với ID: " + id));

        cu.setMaDotGiamGia(moi.getMaDotGiamGia());
        cu.setTenDotGiamGia(moi.getTenDotGiamGia());
        cu.setPhanTramGiam(moi.getPhanTramGiam());
        cu.setNgayBatDau(moi.getNgayBatDau());
        cu.setNgayKetThuc(moi.getNgayKetThuc());
        if (moi.getTrangThai() != null) {
            cu.setTrangThai(moi.getTrangThai());
        }

        return dotGiamGiaRepository.save(cu);
    }

    public DotGiamGia toggleStatus(Long id) {
        DotGiamGia item = dotGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá với ID: " + id));

        int newStatus = (item.getTrangThai() != null && item.getTrangThai() == 1) ? 0 : 1;
        item.setTrangThai(newStatus);
        return dotGiamGiaRepository.save(item);
    }

    public void delete(Long id) {
        dotGiamGiaRepository.deleteById(id);
    }
}
