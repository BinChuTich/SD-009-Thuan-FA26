package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.ChiTietDotGiamGia;
import com.example.sd009thuan.entity.ChiTietSanPham;
import com.example.sd009thuan.entity.DotGiamGia;
import com.example.sd009thuan.repository.ChiTietDotGiamGiaRepository;
import com.example.sd009thuan.repository.ChiTietSanPhamRepository;
import com.example.sd009thuan.repository.DotGiamGiaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DotGiamGiaService {

    private final DotGiamGiaRepository dotGiamGiaRepository;
    private final ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;
    private final ChiTietSanPhamRepository chiTietSanPhamRepository;

    public DotGiamGiaService(
            DotGiamGiaRepository dotGiamGiaRepository,
            ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository,
            ChiTietSanPhamRepository chiTietSanPhamRepository
    ) {
        this.dotGiamGiaRepository = dotGiamGiaRepository;
        this.chiTietDotGiamGiaRepository = chiTietDotGiamGiaRepository;
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
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

    public List<ChiTietDotGiamGia> getChiTietByDotGiamGiaId(Long idDotGiamGia) {
        return chiTietDotGiamGiaRepository.findByIdDotGiamGia_Id(idDotGiamGia);
    }

    /**
     * Validate dữ liệu đầu vào và kiểm tra xung đột trùng sản phẩm
     */
    private void validateDotGiamGia(DotGiamGia dotGiamGia, Long currentId) {
        if (dotGiamGia.getTenDotGiamGia() == null || dotGiamGia.getTenDotGiamGia().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đợt giảm giá không được để trống!");
        }
        if (dotGiamGia.getPhanTramGiam() == null) {
            throw new IllegalArgumentException("Mức giảm giá không được để trống!");
        }
        if (dotGiamGia.getPhanTramGiam().compareTo(BigDecimal.ZERO) <= 0 
                || dotGiamGia.getPhanTramGiam().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Mức giảm phải lớn hơn 0% và không vượt quá 100%!");
        }
        if (dotGiamGia.getNgayBatDau() == null) {
            throw new IllegalArgumentException("Thời gian bắt đầu không được để trống!");
        }
        if (dotGiamGia.getNgayKetThuc() == null) {
            throw new IllegalArgumentException("Thời gian kết thúc không được để trống!");
        }
        if (!dotGiamGia.getNgayKetThuc().isAfter(dotGiamGia.getNgayBatDau())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu!");
        }

        // Kiểm tra quy tắc: 1 sản phẩm chỉ được tạo 1 đợt giảm giá, khi kết thúc mới tạo lại được
        if (dotGiamGia.getChiTietSanPhamIds() != null && !dotGiamGia.getChiTietSanPhamIds().isEmpty()) {
            List<Long> ctspIds = dotGiamGia.getChiTietSanPhamIds();
            List<Long> spIds = new ArrayList<>();
            for (Long ctspId : ctspIds) {
                chiTietSanPhamRepository.findById(ctspId).ifPresent(ctsp -> {
                    if (ctsp.getIdSanPham() != null) {
                        spIds.add(ctsp.getIdSanPham().getId());
                    }
                });
            }
            if (spIds.isEmpty()) {
                spIds.add(-1L);
            }

            List<ChiTietDotGiamGia> conflicts = chiTietDotGiamGiaRepository.findUnendedCampaignConflicts(
                    ctspIds,
                    spIds,
                    currentId,
                    Instant.now()
            );

            if (!conflicts.isEmpty()) {
                ChiTietDotGiamGia conflict = conflicts.get(0);
                String tenSp = "sản phẩm";
                if (conflict.getIdChiTietSanPham() != null) {
                    if (conflict.getIdChiTietSanPham().getIdSanPham() != null) {
                        tenSp = conflict.getIdChiTietSanPham().getIdSanPham().getTenSanPham();
                    } else if (conflict.getIdChiTietSanPham().getMaChiTietSanPham() != null) {
                        tenSp = conflict.getIdChiTietSanPham().getMaChiTietSanPham();
                    }
                }
                String tenDot = conflict.getIdDotGiamGia() != null ? conflict.getIdDotGiamGia().getTenDotGiamGia() : "đợt khác";
                throw new IllegalArgumentException("Sản phẩm [" + tenSp + "] đang trong đợt giảm giá [" + tenDot + "] chưa kết thúc. Chỉ khi đợt giảm giá kết thúc mới có thể tạo đợt mới cho sản phẩm này!");
            }
        }
    }

    public List<Long> getActiveProductIds(Long excludeDotId) {
        return chiTietDotGiamGiaRepository.findActiveProductIds(Instant.now(), excludeDotId);
    }

    private void saveChiTiet(DotGiamGia dotGiamGia, List<Long> chiTietSanPhamIds) {
        if (chiTietSanPhamIds == null || chiTietSanPhamIds.isEmpty()) return;
        List<ChiTietDotGiamGia> list = new ArrayList<>();
        for (Long ctspId : chiTietSanPhamIds) {
            Optional<ChiTietSanPham> opt = chiTietSanPhamRepository.findById(ctspId);
            if (opt.isPresent()) {
                ChiTietDotGiamGia ct = new ChiTietDotGiamGia();
                ct.setIdDotGiamGia(dotGiamGia);
                ct.setIdChiTietSanPham(opt.get());
                ct.setPhanTramGiamBienThe(dotGiamGia.getPhanTramGiam());
                ct.setTrangThai(1);
                list.add(ct);
            }
        }
        if (!list.isEmpty()) {
            chiTietDotGiamGiaRepository.saveAll(list);
        }
    }

    @Transactional
    public DotGiamGia create(DotGiamGia dotGiamGia) {
        validateDotGiamGia(dotGiamGia, null);
        if (dotGiamGia.getMaDotGiamGia() == null || dotGiamGia.getMaDotGiamGia().trim().isEmpty()) {
            dotGiamGia.setMaDotGiamGia("DGG" + (System.currentTimeMillis() % 100000));
        }
        if (dotGiamGia.getTrangThai() == null) {
            dotGiamGia.setTrangThai(1);
        }
        DotGiamGia saved = dotGiamGiaRepository.save(dotGiamGia);
        saveChiTiet(saved, dotGiamGia.getChiTietSanPhamIds());
        return saved;
    }

    @Transactional
    public DotGiamGia update(Long id, DotGiamGia moi) {
        validateDotGiamGia(moi, id);
        DotGiamGia cu = dotGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá với ID: " + id));

        cu.setMaDotGiamGia(moi.getMaDotGiamGia());
        cu.setTenDotGiamGia(moi.getTenDotGiamGia());
        cu.setPhanTramGiam(moi.getPhanTramGiam());
        cu.setNgayBatDau(moi.getNgayBatDau());
        cu.setNgayKetThuc(moi.getNgayKetThuc());
        cu.setMoTa(moi.getMoTa());
        if (moi.getTrangThai() != null) {
            cu.setTrangThai(moi.getTrangThai());
        }

        DotGiamGia saved = dotGiamGiaRepository.save(cu);
        if (moi.getChiTietSanPhamIds() != null) {
            chiTietDotGiamGiaRepository.deleteByIdDotGiamGiaId(id);
            saveChiTiet(saved, moi.getChiTietSanPhamIds());
        }
        return saved;
    }

    @Transactional
    public DotGiamGia toggleStatus(Long id) {
        DotGiamGia item = dotGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá với ID: " + id));

        int newStatus = (item.getTrangThai() != null && item.getTrangThai() == 1) ? 0 : 1;
        item.setTrangThai(newStatus);
        return dotGiamGiaRepository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        chiTietDotGiamGiaRepository.deleteByIdDotGiamGiaId(id);
        dotGiamGiaRepository.deleteById(id);
    }

    @Transactional
    public int autoUpdateExpiredStatus() {
        return dotGiamGiaRepository.updateHetHan(Instant.now());
    }
}
