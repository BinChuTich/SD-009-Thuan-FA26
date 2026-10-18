package com.example.sd009thuan.service.impl;

import com.example.sd009thuan.dto.CaLamViecRequest;
import com.example.sd009thuan.entity.CaLamViec;
import com.example.sd009thuan.repository.CaLamViecRepository;
import com.example.sd009thuan.repository.LichLamViecRepository;
import com.example.sd009thuan.service.CaLamViecService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class CaLamViecServiceImpl implements CaLamViecService {

    private final CaLamViecRepository caLamViecRepository;
    private final LichLamViecRepository lichLamViecRepository;

    public CaLamViecServiceImpl(CaLamViecRepository caLamViecRepository, LichLamViecRepository lichLamViecRepository) {
        this.caLamViecRepository = caLamViecRepository;
        this.lichLamViecRepository = lichLamViecRepository;
    }

    @Override
    public List<CaLamViec> getAll() {
        return caLamViecRepository.findAll();
    }

    @Override
    public List<CaLamViec> search(String keyword, Integer trangThai, LocalTime gioBatDau, LocalTime gioKetThuc) {
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return caLamViecRepository.searchCaLamViec(kw, trangThai, gioBatDau, gioKetThuc);
    }

    @Override
    public CaLamViec getById(Long id) {
        return caLamViecRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca làm việc với ID: " + id));
    }

    @Override
    public void validateCa(CaLamViecRequest req, Long excludeId) {
        if (req.getTenCa() == null || req.getTenCa().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên ca làm việc không được để trống");
        }
        String trimmedTenCa = req.getTenCa().trim();
        if (trimmedTenCa.length() < 2 || trimmedTenCa.length() > 50) {
            throw new IllegalArgumentException("Tên ca làm việc phải từ 2 đến 50 ký tự");
        }

        // Check duplicate name
        boolean isDuplicate = (excludeId != null)
                ? caLamViecRepository.existsByTenCaIgnoreCaseAndIdNot(trimmedTenCa, excludeId)
                : caLamViecRepository.existsByTenCaIgnoreCase(trimmedTenCa);
        if (isDuplicate) {
            throw new IllegalArgumentException("Tên ca làm việc này đã tồn tại trong hệ thống");
        }

        // Check time
        if (req.getGioBatDau() == null) {
            throw new IllegalArgumentException("Vui lòng chọn Giờ bắt đầu");
        }
        if (req.getGioKetThuc() == null) {
            throw new IllegalArgumentException("Vui lòng chọn Giờ kết thúc");
        }
        if (!req.getGioKetThuc().isAfter(req.getGioBatDau())) {
            throw new IllegalArgumentException("Giờ kết thúc phải lớn hơn Giờ bắt đầu");
        }

        long minutes = Duration.between(req.getGioBatDau(), req.getGioKetThuc()).toMinutes();
        if (minutes < 30) {
            throw new IllegalArgumentException("Thời lượng ca làm việc tối thiểu phải từ 30 phút trở lên");
        }

        // Overlap check with active shifts
        List<CaLamViec> activeShifts = caLamViecRepository.findByTrangThaiOrderByGioBatDauAsc(1);
        for (CaLamViec existing : activeShifts) {
            if (excludeId != null && existing.getId().equals(excludeId)) {
                continue;
            }
            // Overlap condition: startA < endB && endA > startB
            boolean isOverlap = req.getGioBatDau().isBefore(existing.getGioKetThuc()) &&
                                req.getGioKetThuc().isAfter(existing.getGioBatDau());
            if (isOverlap) {
                throw new IllegalArgumentException(String.format(
                        "Khung giờ (%s - %s) bị chồng lấn với ca '%s' (%s - %s)",
                        req.getGioBatDau(), req.getGioKetThuc(),
                        existing.getTenCa(), existing.getGioBatDau(), existing.getGioKetThuc()
                ));
            }
        }

        if (req.getMoTa() != null && req.getMoTa().length() > 255) {
            throw new IllegalArgumentException("Mô tả chi tiết tối đa 255 ký tự");
        }
    }

    @Override
    public Map<String, Object> checkTenCa(String tenCa, Long excludeId) {
        Map<String, Object> res = new HashMap<>();
        if (tenCa == null || tenCa.trim().isEmpty()) {
            res.put("exists", false);
            return res;
        }
        String trimmed = tenCa.trim();
        boolean exists = (excludeId != null)
                ? caLamViecRepository.existsByTenCaIgnoreCaseAndIdNot(trimmed, excludeId)
                : caLamViecRepository.existsByTenCaIgnoreCase(trimmed);
        res.put("exists", exists);
        if (exists) {
            res.put("message", "Tên ca làm việc này đã tồn tại trong hệ thống");
        }
        return res;
    }

    @Override
    public CaLamViec create(CaLamViecRequest request) {
        validateCa(request, null);

        CaLamViec ca = new CaLamViec();
        ca.setTenCa(request.getTenCa().trim());
        ca.setGioBatDau(request.getGioBatDau());
        ca.setGioKetThuc(request.getGioKetThuc());
        ca.setMoTa(request.getMoTa() != null ? request.getMoTa().trim() : null);
        ca.setTrangThai(request.getTrangThai() != null ? request.getTrangThai() : 1);
        ca.setNgayTao(LocalDateTime.now());

        // Sinh mã ca tự động nếu chưa có
        long count = caLamViecRepository.count() + 1;
        ca.setMaCa(String.format("CA%02d", count));

        return caLamViecRepository.save(ca);
    }

    @Override
    public CaLamViec update(Long id, CaLamViecRequest request) {
        CaLamViec ca = getById(id);
        validateCa(request, id);

        ca.setTenCa(request.getTenCa().trim());
        ca.setGioBatDau(request.getGioBatDau());
        ca.setGioKetThuc(request.getGioKetThuc());
        ca.setMoTa(request.getMoTa() != null ? request.getMoTa().trim() : null);
        if (request.getTrangThai() != null) {
            // Nếu chuyển từ 1 sang 0, kiểm tra lịch tương lai
            if (ca.getTrangThai() == 1 && request.getTrangThai() == 0) {
                long upcoming = lichLamViecRepository.countUpcomingActiveAssignmentsByCa(id, LocalDate.now());
                if (upcoming > 0) {
                    throw new IllegalArgumentException(
                            "Không thể ngừng hoạt động ca này vì hiện có " + upcoming +
                            " nhân viên đang được phân lịch từ hôm nay trở đi. Vui lòng điều chỉnh lịch trước."
                    );
                }
            }
            ca.setTrangThai(request.getTrangThai());
        }
        ca.setNgayCapNhat(LocalDateTime.now());
        return caLamViecRepository.save(ca);
    }

    @Override
    public CaLamViec toggleStatus(Long id) {
        CaLamViec ca = getById(id);
        int newStatus = (ca.getTrangThai() == 1) ? 0 : 1;

        if (newStatus == 0) {
            long upcoming = lichLamViecRepository.countUpcomingActiveAssignmentsByCa(id, LocalDate.now());
            if (upcoming > 0) {
                throw new IllegalArgumentException(
                        "Không thể tắt trạng thái ca này vì có " + upcoming +
                        " lịch làm việc từ hôm nay trở đi đang sử dụng ca. Vui lòng điều chỉnh lịch trước."
                );
            }
        }

        ca.setTrangThai(newStatus);
        ca.setNgayCapNhat(LocalDateTime.now());
        return caLamViecRepository.save(ca);
    }

    @Override
    public void delete(Long id) {
        CaLamViec ca = getById(id);
        long upcoming = lichLamViecRepository.countUpcomingActiveAssignmentsByCa(id, LocalDate.now());
        if (upcoming > 0) {
            throw new IllegalArgumentException("Không thể xóa ca làm việc đang có nhân viên phân lịch làm việc.");
        }
        caLamViecRepository.delete(ca);
    }
}

