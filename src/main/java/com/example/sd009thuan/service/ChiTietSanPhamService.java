package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ChiTietSanPhamRequest;
import com.example.sd009thuan.dto.ChiTietSanPhamResponse;
import com.example.sd009thuan.dto.PageResponse;
import com.example.sd009thuan.entity.ChiTietSanPham;
import com.example.sd009thuan.entity.KichCo;
import com.example.sd009thuan.entity.MauSac;
import com.example.sd009thuan.entity.SanPham;
import com.example.sd009thuan.entity.DotGiamGia;
import com.example.sd009thuan.entity.ChiTietDotGiamGia;
import com.example.sd009thuan.repository.ChiTietSanPhamRepository;
import com.example.sd009thuan.repository.ChiTietDotGiamGiaRepository;
import com.example.sd009thuan.repository.KichCoRepository;
import com.example.sd009thuan.repository.MauSacRepository;
import com.example.sd009thuan.repository.SanPhamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sd009thuan.entity.HinhAnh;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ChiTietSanPhamService {

    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final SanPhamRepository sanPhamRepository;
    private final KichCoRepository kichCoRepository;
    private final MauSacRepository mauSacRepository;
    private final com.example.sd009thuan.repository.HinhAnhRepository hinhAnhRepository;
    private final ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;

    public ChiTietSanPhamService(
            ChiTietSanPhamRepository chiTietSanPhamRepository,
            SanPhamRepository sanPhamRepository,
            KichCoRepository kichCoRepository,
            MauSacRepository mauSacRepository,
            com.example.sd009thuan.repository.HinhAnhRepository hinhAnhRepository,
            ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository
    ) {
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.kichCoRepository = kichCoRepository;
        this.mauSacRepository = mauSacRepository;
        this.hinhAnhRepository = hinhAnhRepository;
        this.chiTietDotGiamGiaRepository = chiTietDotGiamGiaRepository;
    }

    public List<ChiTietSanPhamResponse> getBySanPhamId(Long idSanPham) {
        return chiTietSanPhamRepository.findByIdSanPham_Id(idSanPham).stream()
                .map(this::convertToResponse)
                .toList();
    }

    public PageResponse<ChiTietSanPhamResponse> filterVariants(
            Long idSanPham,
            String keyword,
            Long idKichCo,
            Long idMauSac,
            BigDecimal minGia,
            BigDecimal maxGia,
            Integer trangThai,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<ChiTietSanPham> result = chiTietSanPhamRepository.filterVariants(
                idSanPham, (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null,
                idKichCo, idMauSac, minGia, maxGia, trangThai, pageable
        );

        List<ChiTietSanPhamResponse> dtoList = result.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        return PageResponse.<ChiTietSanPhamResponse>builder()
                .content(dtoList)
                .pageNumber(result.getNumber())
                .pageSize(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    public ChiTietSanPhamResponse getById(Long id) {
        ChiTietSanPham ct = chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể có ID: " + id));
        return convertToResponse(ct);
    }

    @Transactional
    public ChiTietSanPhamResponse create(ChiTietSanPhamRequest req) {
        if (req.getSoLuong() == null || req.getSoLuong() < 0 || req.getSoLuong() > 999999) {
            throw new IllegalArgumentException("Số lượng biến thể phải là số nguyên từ 0 đến 999,999!");
        }
        if (req.getGiaBan() == null || req.getGiaBan().compareTo(BigDecimal.ZERO) < 0 || req.getGiaBan().compareTo(new BigDecimal("1000000000")) > 0 || req.getGiaBan().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Đơn giá biến thể phải là số nguyên từ 0 đến 1,000,000,000 VNĐ!");
        }

        SanPham sp = sanPhamRepository.findById(req.getIdSanPham())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + req.getIdSanPham()));
        KichCo kc = kichCoRepository.findById(req.getIdKichCo())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kích cỡ có ID: " + req.getIdKichCo()));
        MauSac ms = mauSacRepository.findById(req.getIdMauSac())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc có ID: " + req.getIdMauSac()));

        List<ChiTietSanPham> existing = chiTietSanPhamRepository.findByIdSanPham_IdAndIdKichCo_IdAndIdMauSac_Id(
                req.getIdSanPham(), req.getIdKichCo(), req.getIdMauSac()
        );
        if (existing != null && !existing.isEmpty()) {
            throw new IllegalArgumentException("Biến thể với kích cỡ " + kc.getTenKichCo() + " và màu sắc " + ms.getTenMauSac() + " đã tồn tại cho sản phẩm này!");
        }

        if (req.getMaChiTietSanPham() == null || req.getMaChiTietSanPham().trim().isEmpty()) {
            req.setMaChiTietSanPham("CTSP" + System.currentTimeMillis());
        } else if (chiTietSanPhamRepository.existsByMaChiTietSanPham(req.getMaChiTietSanPham().trim())) {
            throw new IllegalArgumentException("Mã chi tiết sản phẩm đã tồn tại: " + req.getMaChiTietSanPham());
        }

        ChiTietSanPham ct = new ChiTietSanPham();
        ct.setIdSanPham(sp);
        ct.setIdKichCo(kc);
        ct.setIdMauSac(ms);
        ct.setMaChiTietSanPham(req.getMaChiTietSanPham().trim());
        ct.setSoLuong(req.getSoLuong());
        ct.setGiaBan(req.getGiaBan());
        ct.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : 1);
        ct.setNguoiTao(req.getNguoiThaoTac() != null ? req.getNguoiThaoTac() : "Admin");

        ChiTietSanPham saved = chiTietSanPhamRepository.save(ct);

        // Tự động liên kết với đợt giảm giá đang hoạt động của sản phẩm (nếu có)
        try {
            List<ChiTietDotGiamGia> activeDiscounts = chiTietDotGiamGiaRepository.findActiveDiscountsBySanPhamId(sp.getId());
            if (activeDiscounts != null && !activeDiscounts.isEmpty()) {
                ChiTietDotGiamGia sample = activeDiscounts.get(0);
                if (sample.getIdDotGiamGia() != null) {
                    ChiTietDotGiamGia newCd = new ChiTietDotGiamGia();
                    newCd.setIdDotGiamGia(sample.getIdDotGiamGia());
                    newCd.setIdChiTietSanPham(saved);
                    newCd.setPhanTramGiamBienThe(sample.getPhanTramGiamBienThe() != null ? sample.getPhanTramGiamBienThe() : sample.getIdDotGiamGia().getPhanTramGiam());
                    newCd.setTrangThai(1);
                    chiTietDotGiamGiaRepository.save(newCd);
                }
            }
        } catch (Exception ex) {
            log.warn("Không thể tự động liên kết đợt giảm giá cho CTSP {}: {}", saved.getId(), ex.getMessage());
        }

        return convertToResponse(saved);
    }

    @Transactional
    public ChiTietSanPhamResponse update(Long id, ChiTietSanPhamRequest req) {
        if (req.getSoLuong() == null || req.getSoLuong() < 0 || req.getSoLuong() > 999999) {
            throw new IllegalArgumentException("Số lượng biến thể phải là số nguyên từ 0 đến 999,999!");
        }
        if (req.getGiaBan() == null || req.getGiaBan().compareTo(BigDecimal.ZERO) < 0 || req.getGiaBan().compareTo(new BigDecimal("1000000000")) > 0 || req.getGiaBan().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Đơn giá biến thể phải là số nguyên từ 0 đến 1,000,000,000 VNĐ!");
        }

        ChiTietSanPham ct = chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể có ID: " + id));

        KichCo kc = kichCoRepository.findById(req.getIdKichCo())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kích cỡ có ID: " + req.getIdKichCo()));
        MauSac ms = mauSacRepository.findById(req.getIdMauSac())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc có ID: " + req.getIdMauSac()));

        // Kiểm tra xem biến thể có bị đổi sang (Kích cỡ, Màu sắc) khác với hiện tại của nó không
        boolean isColorOrSizeChanged = ct.getIdKichCo() == null || !ct.getIdKichCo().getId().equals(req.getIdKichCo())
                || ct.getIdMauSac() == null || !ct.getIdMauSac().getId().equals(req.getIdMauSac());

        if (isColorOrSizeChanged) {
            List<ChiTietSanPham> duplicates = chiTietSanPhamRepository.findByIdSanPham_IdAndIdKichCo_IdAndIdMauSac_Id(
                    ct.getIdSanPham().getId(), req.getIdKichCo(), req.getIdMauSac()
            );
            ChiTietSanPham other = duplicates != null ? duplicates.stream()
                    .filter(d -> !d.getId().equals(id))
                    .findFirst()
                    .orElse(null) : null;

            if (other != null) {
                // Nếu sản phẩm đã có biến thể khác cùng Màu sắc & Kích cỡ, tự động gộp số lượng và cập nhật giá, trạng thái
                other.setSoLuong((other.getSoLuong() != null ? other.getSoLuong() : 0) + (req.getSoLuong() != null ? req.getSoLuong() : 0));
                other.setGiaBan(req.getGiaBan());
                if (req.getTrangThai() != null) {
                    other.setTrangThai(req.getTrangThai());
                }
                other.setNguoiCapNhat(req.getNguoiThaoTac() != null ? req.getNguoiThaoTac() : "Admin");

                if (req.getTenSanPham() != null && !req.getTenSanPham().trim().isEmpty()) {
                    String newTenSp = req.getTenSanPham().trim();
                    SanPham sp = ct.getIdSanPham();
                    if (sp != null && !newTenSp.equalsIgnoreCase(sp.getTenSanPham())) {
                        sp.setTenSanPham(newTenSp);
                        sanPhamRepository.save(sp);
                    }
                }

                if (req.getAnhDaiDien() != null) {
                    updateVariantImage(ct.getIdSanPham(), ms.getId(), req.getAnhDaiDien().trim());
                }

                chiTietSanPhamRepository.delete(ct);
                ChiTietSanPham merged = chiTietSanPhamRepository.save(other);
                return convertToResponse(merged);
            }
        }

        if (req.getMaChiTietSanPham() != null && !req.getMaChiTietSanPham().trim().isEmpty()) {
            if (chiTietSanPhamRepository.existsByMaChiTietSanPhamAndIdNot(req.getMaChiTietSanPham().trim(), id)) {
                throw new IllegalArgumentException("Mã chi tiết sản phẩm đã được sử dụng: " + req.getMaChiTietSanPham());
            }
            ct.setMaChiTietSanPham(req.getMaChiTietSanPham().trim());
        }

        ct.setIdKichCo(kc);
        ct.setIdMauSac(ms);
        ct.setSoLuong(req.getSoLuong());
        ct.setGiaBan(req.getGiaBan());
        if (req.getTrangThai() != null) {
            ct.setTrangThai(req.getTrangThai());
        }
        ct.setNguoiCapNhat(req.getNguoiThaoTac() != null ? req.getNguoiThaoTac() : "Admin");

        // Cập nhật tên sản phẩm nếu có thay đổi
        if (req.getTenSanPham() != null && !req.getTenSanPham().trim().isEmpty()) {
            String newTenSp = req.getTenSanPham().trim();
            SanPham sp = ct.getIdSanPham();
            if (sp != null && !newTenSp.equalsIgnoreCase(sp.getTenSanPham())) {
                sp.setTenSanPham(newTenSp);
                sanPhamRepository.save(sp);
            }
        }

        // Cập nhật/thêm/xóa ảnh đại diện theo màu sắc của biến thể
        if (req.getAnhDaiDien() != null) {
            updateVariantImage(ct.getIdSanPham(), ms.getId(), req.getAnhDaiDien().trim());
        }

        ChiTietSanPham updated = chiTietSanPhamRepository.save(ct);
        return convertToResponse(updated);
    }

    private void updateVariantImage(SanPham sp, Long colorId, String imageInput) {
        if (sp == null || colorId == null) {
            return;
        }
        String targetTen = "COLOR_" + colorId;
        List<HinhAnh> imgs = hinhAnhRepository.findByIdSanPham_Id(sp.getId());

        // Nếu imageInput rỗng hoặc "REMOVE" -> Xóa ảnh của màu/biến thể này
        if (imageInput == null || imageInput.trim().isEmpty() || "REMOVE".equalsIgnoreCase(imageInput.trim())) {
            if (imgs != null) {
                for (HinhAnh img : imgs) {
                    if (targetTen.equals(img.getTenAnh())) {
                        hinhAnhRepository.delete(img);
                    }
                }
                // Nếu sản phẩm chỉ có duy nhất 1 ảnh chung và người dùng xóa ảnh ở biến thể
                if (imgs.size() == 1) {
                    HinhAnh single = imgs.get(0);
                    if (single.getTenAnh() == null || !single.getTenAnh().startsWith("COLOR_")) {
                        hinhAnhRepository.delete(single);
                    }
                }
            }
            return;
        }

        // Cập nhật hoặc thêm ảnh mới
        String processedUrl = processImage(imageInput.trim());
        if (processedUrl != null && !processedUrl.trim().isEmpty()) {
            HinhAnh targetImg = null;
            if (imgs != null) {
                for (HinhAnh img : imgs) {
                    if (targetTen.equals(img.getTenAnh())) {
                        targetImg = img;
                        break;
                    }
                }
            }
            if (targetImg != null) {
                targetImg.setDuongDan(processedUrl.trim());
                hinhAnhRepository.save(targetImg);
            } else {
                HinhAnh newImg = new HinhAnh();
                newImg.setIdSanPham(sp);
                newImg.setTenAnh(targetTen);
                newImg.setDuongDan(processedUrl.trim());
                newImg.setTrangThai(1);
                hinhAnhRepository.save(newImg);
            }
        }
    }

    private String processImage(String imageInput) {
        if (imageInput == null || imageInput.trim().isEmpty()) {
            return null;
        }
        String trimmed = imageInput.trim();
        if (trimmed.startsWith("data:image/")) {
            try {
                int commaIdx = trimmed.indexOf(",");
                if (commaIdx != -1) {
                    String metadata = trimmed.substring(0, commaIdx);
                    String base64Data = trimmed.substring(commaIdx + 1);

                    String extension = ".png";
                    if (metadata.contains("image/jpeg") || metadata.contains("image/jpg")) {
                        extension = ".jpg";
                    } else if (metadata.contains("image/webp")) {
                        extension = ".webp";
                    } else if (metadata.contains("image/gif")) {
                        extension = ".gif";
                    }

                    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);

                    java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads");
                    if (!java.nio.file.Files.exists(uploadDir)) {
                        java.nio.file.Files.createDirectories(uploadDir);
                    }

                    String filename = "sp_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + extension;
                    java.nio.file.Path filePath = uploadDir.resolve(filename);
                    java.nio.file.Files.write(filePath, imageBytes);

                    return "/uploads/" + filename;
                }
            } catch (Exception e) {
                return null;
            }
        }

        if (trimmed.length() > 500) {
            return trimmed.substring(0, 500);
        }
        return trimmed;
    }

    @Transactional
    public void changeStatus(Long id, Integer trangThai) {
        ChiTietSanPham ct = chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể có ID: " + id));
        ct.setTrangThai(trangThai);
        chiTietSanPhamRepository.save(ct);
    }

    @Transactional
    public void delete(Long id) {
        if (!chiTietSanPhamRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy biến thể có ID: " + id);
        }
        try {
            chiTietSanPhamRepository.deleteById(id);
            chiTietSanPhamRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Không thể xóa biến thể vì đã phát sinh giao dịch/hóa đơn trong hệ thống! Vui lòng đổi trạng thái sang 'Ngừng kinh doanh'.");
        }
    }

    @Transactional
    public List<ChiTietSanPhamResponse> saveBatch(List<ChiTietSanPhamRequest> requests) {
        List<ChiTietSanPhamResponse> list = new ArrayList<>();
        int counter = 0;
        for (ChiTietSanPhamRequest req : requests) {
            if (req.getId() != null) {
                list.add(update(req.getId(), req));
            } else {
                if (req.getMaChiTietSanPham() == null || req.getMaChiTietSanPham().trim().isEmpty()) {
                    req.setMaChiTietSanPham("CTSP" + System.currentTimeMillis() + (counter++));
                }
                list.add(create(req));
            }
        }
        return list;
    }

    private ChiTietSanPhamResponse convertToResponse(ChiTietSanPham ct) {
        String anhDaiDien = null;
        if (ct.getIdSanPham() != null) {
            List<HinhAnh> imgs = hinhAnhRepository.findByIdSanPham_Id(ct.getIdSanPham().getId());
            if (imgs != null && !imgs.isEmpty()) {
                if (ct.getIdMauSac() != null) {
                    String targetTen = "COLOR_" + ct.getIdMauSac().getId();
                    for (HinhAnh img : imgs) {
                        if (targetTen.equals(img.getTenAnh())) {
                            anhDaiDien = img.getDuongDan();
                            break;
                        }
                    }
                }
                // Chỉ fallback về ảnh chung nếu sản phẩm có ảnh chung và KHÔNG có bất kỳ ảnh riêng nào theo màu khác
                if (anhDaiDien == null) {
                    boolean hasAnyColorSpecificImg = imgs.stream()
                            .anyMatch(i -> i.getTenAnh() != null && i.getTenAnh().startsWith("COLOR_"));
                    if (!hasAnyColorSpecificImg) {
                        for (HinhAnh img : imgs) {
                            if (img.getTenAnh() == null || !img.getTenAnh().startsWith("COLOR_")) {
                                anhDaiDien = img.getDuongDan();
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Tính toán đợt giảm giá & giá bán sau giảm của biến thể
        Long idDotGiamGia = null;
        String tenDotGiamGia = null;
        BigDecimal phanTramGiam = null;
        BigDecimal giaSauGiam = ct.getGiaBan();
        boolean coGiamGia = false;

        List<ChiTietDotGiamGia> activeDiscounts = chiTietDotGiamGiaRepository.findActiveDiscountsByCtspId(ct.getId());
        if (activeDiscounts != null && !activeDiscounts.isEmpty()) {
            java.time.Instant now = java.time.Instant.now();
            for (ChiTietDotGiamGia cd : activeDiscounts) {
                DotGiamGia d = cd.getIdDotGiamGia();
                if (d != null && d.getTrangThai() != null && d.getTrangThai() == 1) {
                    boolean started = d.getNgayBatDau() == null || !now.isBefore(d.getNgayBatDau());
                    boolean notEnded = d.getNgayKetThuc() == null || !now.isAfter(d.getNgayKetThuc());
                    if (started && notEnded) {
                        idDotGiamGia = d.getId();
                        tenDotGiamGia = d.getTenDotGiamGia();
                        phanTramGiam = cd.getPhanTramGiamBienThe() != null ? cd.getPhanTramGiamBienThe() : d.getPhanTramGiam();
                        if (phanTramGiam != null && phanTramGiam.compareTo(BigDecimal.ZERO) > 0 && ct.getGiaBan() != null) {
                            coGiamGia = true;
                            BigDecimal factor = BigDecimal.valueOf(100).subtract(phanTramGiam).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
                            giaSauGiam = ct.getGiaBan().multiply(factor).setScale(0, java.math.RoundingMode.HALF_UP);
                        }
                        break;
                    }
                }
            }
        }

        return ChiTietSanPhamResponse.builder()
                .id(ct.getId())
                .idSanPham(ct.getIdSanPham() != null ? ct.getIdSanPham().getId() : null)
                .maSanPham(ct.getIdSanPham() != null ? ct.getIdSanPham().getMaSanPham() : null)
                .tenSanPham(ct.getIdSanPham() != null ? ct.getIdSanPham().getTenSanPham() : null)
                .idKichCo(ct.getIdKichCo() != null ? ct.getIdKichCo().getId() : null)
                .tenKichCo(ct.getIdKichCo() != null ? ct.getIdKichCo().getTenKichCo() : null)
                .idMauSac(ct.getIdMauSac() != null ? ct.getIdMauSac().getId() : null)
                .tenMauSac(ct.getIdMauSac() != null ? ct.getIdMauSac().getTenMauSac() : null)
                .maHex(ct.getIdMauSac() != null ? ct.getIdMauSac().getMaHex() : null)
                .maChiTietSanPham(ct.getMaChiTietSanPham())
                .soLuong(ct.getSoLuong())
                .giaBan(ct.getGiaBan())
                .trangThai(ct.getTrangThai())
                .ngayTao(ct.getNgayTao())
                .anhDaiDien(anhDaiDien)
                .idDotGiamGia(idDotGiamGia)
                .tenDotGiamGia(tenDotGiamGia)
                .phanTramGiam(phanTramGiam)
                .giaSauGiam(giaSauGiam)
                .coGiamGia(coGiamGia)
                .build();
    }

    public byte[] exportExcel(Long idSanPham, String keyword, Long idKichCo, Long idMauSac, Integer trangThai) {
        PageResponse<ChiTietSanPhamResponse> pageData = filterVariants(
                idSanPham, keyword, idKichCo, idMauSac, null, null, trangThai, 0, 10000
        );
        List<ChiTietSanPhamResponse> list = pageData.getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Biến thể sản phẩm");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0"));

            String[] headers = {
                    "STT", "Mã SP", "Tên sản phẩm", "Mã CTSP", "Màu sắc", "Kích cỡ", "Số lượng", "Giá bán (VNĐ)", "Trạng thái"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ChiTietSanPhamResponse ct : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(ct.getMaSanPham() != null ? ct.getMaSanPham() : "");
                row.createCell(2).setCellValue(ct.getTenSanPham() != null ? ct.getTenSanPham() : "");
                row.createCell(3).setCellValue(ct.getMaChiTietSanPham() != null ? ct.getMaChiTietSanPham() : "");
                row.createCell(4).setCellValue(ct.getTenMauSac() != null ? ct.getTenMauSac() : "");
                row.createCell(5).setCellValue(ct.getTenKichCo() != null ? ct.getTenKichCo() : "");

                Cell c6 = row.createCell(6);
                c6.setCellValue(ct.getSoLuong() != null ? ct.getSoLuong() : 0);
                c6.setCellStyle(numberStyle);

                Cell c7 = row.createCell(7);
                c7.setCellValue(ct.getGiaBan() != null ? ct.getGiaBan().doubleValue() : 0);
                c7.setCellStyle(numberStyle);

                row.createCell(8).setCellValue(ct.getTrangThai() != null && ct.getTrangThai() == 1 ? "Đang bán" : "Ngừng bán");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xuất file Excel: " + e.getMessage());
        }
    }
}
