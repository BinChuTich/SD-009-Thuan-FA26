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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
            String trangThai,
            String startDateStr,
            String endDateStr,
            Pageable pageable
    ) {
        Instant tuNgayBatDau = null;
        Instant denNgayBatDau = null;
        Instant tuNgayKetThuc = null;
        Instant denNgayKetThuc = null;

        boolean hasStart = startDateStr != null && !startDateStr.trim().isEmpty();
        boolean hasEnd = endDateStr != null && !endDateStr.trim().isEmpty();

        if (hasStart && hasEnd) {
            // Người dùng chọn cả khoảng thời gian: Từ ngày bắt đầu đến ngày kết thúc
            try {
                LocalDate start = LocalDate.parse(startDateStr.trim());
                LocalDate end = LocalDate.parse(endDateStr.trim());
                tuNgayBatDau = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
                denNgayKetThuc = end.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
            } catch (Exception ignored) {
            }
        } else if (hasEnd) {
            // Người dùng CHỈ chọn Ngày kết thúc: Lọc đúng các đợt có ngày kết thúc rơi vào đúng ngày đó
            try {
                LocalDate end = LocalDate.parse(endDateStr.trim());
                tuNgayKetThuc = end.atStartOfDay(ZoneId.systemDefault()).toInstant();
                denNgayKetThuc = end.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
            } catch (Exception ignored) {
            }
        } else if (hasStart) {
            // Người dùng CHỈ chọn Ngày bắt đầu: Lọc các đợt có ngày bắt đầu từ ngày đó trở đi
            try {
                LocalDate start = LocalDate.parse(startDateStr.trim());
                tuNgayBatDau = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
            } catch (Exception ignored) {
            }
        }

        String searchKw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String cleanKw = null;
        BigDecimal giaTri = null;

        if (searchKw != null) {
            String tempClean = searchKw.replace("%", "").trim();
            if (!tempClean.isEmpty()) {
                cleanKw = tempClean;
                try {
                    giaTri = new BigDecimal(tempClean.replace(",", "."));
                } catch (Exception ignored) {
                }
            }
        }

        String statusStr = (trangThai != null && !trangThai.trim().isEmpty()) ? trangThai.trim() : null;

        return dotGiamGiaRepository.search(
                searchKw,
                cleanKw,
                giaTri,
                statusStr,
                Instant.now(),
                tuNgayBatDau,
                denNgayBatDau,
                tuNgayKetThuc,
                denNgayKetThuc,
                pageable
        );
    }

    public Page<DotGiamGia> search(
            String keyword,
            Integer trangThai,
            String startDateStr,
            String endDateStr,
            Pageable pageable
    ) {
        return search(keyword, trangThai != null ? String.valueOf(trangThai) : null, startDateStr, endDateStr, pageable);
    }

    public List<DotGiamGia> getAll() {
        return dotGiamGiaRepository.findAll();
    }

    public List<DotGiamGia> getActiveCampaigns() {
        return dotGiamGiaRepository.findActiveCampaigns(Instant.now());
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

        Instant nowThreshold = Instant.now().minus(java.time.Duration.ofMinutes(5));
        if (currentId == null) {
            if (dotGiamGia.getNgayBatDau().isBefore(nowThreshold)) {
                throw new IllegalArgumentException("Thời gian bắt đầu không được trước thời gian hiện tại!");
            }
        } else {
            DotGiamGia old = dotGiamGiaRepository.findById(currentId).orElse(null);
            if (old != null && !old.getNgayBatDau().equals(dotGiamGia.getNgayBatDau())) {
                if (dotGiamGia.getNgayBatDau().isBefore(nowThreshold)) {
                    throw new IllegalArgumentException("Thời gian bắt đầu không được trước thời gian hiện tại!");
                }
            }
        }
        if (dotGiamGia.getNgayKetThuc().isBefore(nowThreshold)) {
            throw new IllegalArgumentException("Thời gian kết thúc không được trước thời gian hiện tại!");
        }
    }

    public Map<Long, DotGiamGia> getActiveProductMap(Long excludeDotId) {
        List<ChiTietDotGiamGia> chiTiets = chiTietDotGiamGiaRepository.findActiveChiTiets(Instant.now(), excludeDotId);
        Map<Long, DotGiamGia> map = new HashMap<>();
        for (ChiTietDotGiamGia ct : chiTiets) {
            if (ct.getIdChiTietSanPham() != null
                    && ct.getIdChiTietSanPham().getIdSanPham() != null
                    && ct.getIdDotGiamGia() != null) {
                Long spId = ct.getIdChiTietSanPham().getIdSanPham().getId();
                if (map.containsKey(spId)) {
                    DotGiamGia existing = map.get(spId);
                    BigDecimal existingPt = existing.getPhanTramGiam() != null ? existing.getPhanTramGiam() : BigDecimal.ZERO;
                    BigDecimal currentPt = ct.getIdDotGiamGia().getPhanTramGiam() != null ? ct.getIdDotGiamGia().getPhanTramGiam() : BigDecimal.ZERO;
                    if (currentPt.compareTo(existingPt) > 0) {
                        map.put(spId, ct.getIdDotGiamGia());
                    }
                } else {
                    map.put(spId, ct.getIdDotGiamGia());
                }
            }
        }
        return map;
    }

    public Map<Long, Map<String, Object>> getActiveVariantMap(Long excludeDotId) {
        List<ChiTietDotGiamGia> chiTiets = chiTietDotGiamGiaRepository.findActiveChiTiets(Instant.now(), excludeDotId);
        Map<Long, Map<String, Object>> map = new HashMap<>();
        for (ChiTietDotGiamGia ct : chiTiets) {
            if (ct.getIdChiTietSanPham() != null && ct.getIdDotGiamGia() != null) {
                Long variantId = ct.getIdChiTietSanPham().getId();
                BigDecimal currentPt = ct.getPhanTramGiamBienThe() != null ? ct.getPhanTramGiamBienThe() : ct.getIdDotGiamGia().getPhanTramGiam();
                if (currentPt == null) currentPt = BigDecimal.ZERO;

                // Nếu biến thể nằm trong nhiều đợt giảm giá đang chạy, tự động lấy đợt có % giảm lớn nhất (MAX discount)
                if (map.containsKey(variantId)) {
                    Map<String, Object> existing = map.get(variantId);
                    Object existingPtObj = existing.get("phanTramGiam");
                    BigDecimal existingPt = BigDecimal.ZERO;
                    if (existingPtObj instanceof BigDecimal) existingPt = (BigDecimal) existingPtObj;
                    else if (existingPtObj instanceof Number) existingPt = BigDecimal.valueOf(((Number) existingPtObj).doubleValue());
                    if (currentPt.compareTo(existingPt) <= 0) {
                        continue;
                    }
                }

                Map<String, Object> info = new HashMap<>();
                info.put("dotId", ct.getIdDotGiamGia().getId());
                info.put("maDotGiamGia", ct.getIdDotGiamGia().getMaDotGiamGia());
                info.put("tenDotGiamGia", ct.getIdDotGiamGia().getTenDotGiamGia());
                info.put("phanTramGiam", currentPt);
                info.put("loaiGiamGia", ct.getIdDotGiamGia().getLoaiGiamGia());
                info.put("giaTriGiam", ct.getIdDotGiamGia().getGiaTriGiam());
                info.put("giamToiDa", ct.getIdDotGiamGia().getGiamToiDa());
                info.put("ngayBatDau", ct.getIdDotGiamGia().getNgayBatDau());
                info.put("ngayKetThuc", ct.getIdDotGiamGia().getNgayKetThuc());
                map.put(variantId, info);
            }
        }
        return map;
    }

    public List<Long> getActiveProductIds(Long excludeDotId) {
        return new ArrayList<>(getActiveProductMap(excludeDotId).keySet());
    }

    public Optional<DotGiamGia> getActiveCampaignByProductId(Long sanPhamId) {
        List<ChiTietDotGiamGia> chiTiets = chiTietDotGiamGiaRepository.findActiveChiTiets(Instant.now(), null);
        for (ChiTietDotGiamGia ct : chiTiets) {
            if (ct.getIdChiTietSanPham() != null
                    && ct.getIdChiTietSanPham().getIdSanPham() != null
                    && ct.getIdChiTietSanPham().getIdSanPham().getId().equals(sanPhamId)) {
                return Optional.ofNullable(ct.getIdDotGiamGia());
            }
        }
        return Optional.empty();
    }

    private void saveChiTiet(DotGiamGia dotGiamGia, List<Long> chiTietSanPhamIds, List<Long> sanPhamIds) {
        Set<Long> finalCtspIds = new LinkedHashSet<>();
        // Ưu tiên lưu danh sách biến thể cụ thể đã được người dùng chọn
        if (chiTietSanPhamIds != null && !chiTietSanPhamIds.isEmpty()) {
            finalCtspIds.addAll(chiTietSanPhamIds);
        }
        if (sanPhamIds != null && !sanPhamIds.isEmpty()) {
            // Nếu người dùng chọn cả sản phẩm mà không chỉ định biến thể riêng lẻ
            for (Long spId : sanPhamIds) {
                List<ChiTietSanPham> variants = chiTietSanPhamRepository.findByIdSanPham_Id(spId);
                for (ChiTietSanPham v : variants) {
                    finalCtspIds.add(v.getId());
                }
            }
        }
        if (finalCtspIds.isEmpty()) return;
        List<ChiTietDotGiamGia> list = new ArrayList<>();
        for (Long ctspId : finalCtspIds) {
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
        saveChiTiet(saved, dotGiamGia.getChiTietSanPhamIds(), dotGiamGia.getSanPhamIds());
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
        if (moi.getChiTietSanPhamIds() != null || moi.getSanPhamIds() != null) {
            chiTietDotGiamGiaRepository.deleteByIdDotGiamGiaId(id);
            saveChiTiet(saved, moi.getChiTietSanPhamIds(), moi.getSanPhamIds());
        }
        return saved;
    }

    @Transactional
    public DotGiamGia toggleStatus(Long id) {
        DotGiamGia item = dotGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá với ID: " + id));

        Instant now = Instant.now();
        // Nếu đã quá hạn kết thúc thì không thể kích hoạt lại
        if (item.getNgayKetThuc() != null && now.isAfter(item.getNgayKetThuc())) {
            item.setTrangThai(0);
            return dotGiamGiaRepository.save(item);
        }

        // Tương tự PGG: Nếu đang hoạt động (1) -> chuyển sang Ngừng hoạt động (0).
        // Nếu đang ngừng hoạt động (0) và chưa hết hạn -> Kích hoạt lại (1).
        // (Không cần gửi mail cho khách hàng như PGG)
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
