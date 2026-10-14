package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ChiTietSanPhamRequest;
import com.example.sd009thuan.dto.ChiTietSanPhamResponse;
import com.example.sd009thuan.dto.PageResponse;
import com.example.sd009thuan.entity.ChiTietSanPham;
import com.example.sd009thuan.entity.KichCo;
import com.example.sd009thuan.entity.MauSac;
import com.example.sd009thuan.entity.SanPham;
import com.example.sd009thuan.repository.ChiTietSanPhamRepository;
import com.example.sd009thuan.repository.KichCoRepository;
import com.example.sd009thuan.repository.MauSacRepository;
import com.example.sd009thuan.repository.SanPhamRepository;
import com.example.sd009thuan.repository.ChiTietDotGiamGiaRepository;
import com.example.sd009thuan.repository.DotGiamGiaRepository;
import com.example.sd009thuan.entity.ChiTietDotGiamGia;
import com.example.sd009thuan.entity.DotGiamGia;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ChiTietSanPhamService {

    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final SanPhamRepository sanPhamRepository;
    private final KichCoRepository kichCoRepository;
    private final MauSacRepository mauSacRepository;
    private final com.example.sd009thuan.repository.HinhAnhRepository hinhAnhRepository;
    private final ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;
    private final DotGiamGiaRepository dotGiamGiaRepository;

    public ChiTietSanPhamService(
            ChiTietSanPhamRepository chiTietSanPhamRepository,
            SanPhamRepository sanPhamRepository,
            KichCoRepository kichCoRepository,
            MauSacRepository mauSacRepository,
            com.example.sd009thuan.repository.HinhAnhRepository hinhAnhRepository,
            ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository,
            DotGiamGiaRepository dotGiamGiaRepository
    ) {
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.kichCoRepository = kichCoRepository;
        this.mauSacRepository = mauSacRepository;
        this.hinhAnhRepository = hinhAnhRepository;
        this.chiTietDotGiamGiaRepository = chiTietDotGiamGiaRepository;
        this.dotGiamGiaRepository = dotGiamGiaRepository;
    }

    @Transactional
    public List<ChiTietSanPhamResponse> getBySanPhamId(Long idSanPham) {
        return chiTietSanPhamRepository.findByIdSanPham_Id(idSanPham).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional
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

        Optional<ChiTietSanPham> existing = chiTietSanPhamRepository.findByIdSanPham_IdAndIdKichCo_IdAndIdMauSac_Id(
                req.getIdSanPham(), req.getIdKichCo(), req.getIdMauSac()
        );
        if (existing.isPresent()) {
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

        // Lưu ảnh riêng cho biến thể nếu có
        String imgInput = req.getAnhDaiDien() != null ? req.getAnhDaiDien() : req.getHinhAnh();
        if (imgInput != null && !imgInput.trim().isEmpty()) {
            String processed = processImage(imgInput);
            if (processed != null && !processed.trim().isEmpty()) {
                HinhAnh ha = new HinhAnh();
                ha.setIdSanPham(saved.getIdSanPham());
                ha.setTenAnh("CTSP_" + saved.getId());
                ha.setDuongDan(processed.trim());
                ha.setTrangThai(1);
                hinhAnhRepository.save(ha);
            }
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

        Optional<ChiTietSanPham> duplicate = chiTietSanPhamRepository.findByIdSanPham_IdAndIdKichCo_IdAndIdMauSac_Id(
                ct.getIdSanPham().getId(), req.getIdKichCo(), req.getIdMauSac()
        );
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new IllegalArgumentException("Biến thể với kích cỡ " + kc.getTenKichCo() + " và màu sắc " + ms.getTenMauSac() + " đã tồn tại ở sản phẩm này!");
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

        ChiTietSanPham updated = chiTietSanPhamRepository.save(ct);

        // Lưu / cập nhật / gỡ ảnh riêng cho biến thể nếu có
        String imgInput = req.getAnhDaiDien() != null ? req.getAnhDaiDien() : req.getHinhAnh();
        if (imgInput != null) {
            String targetTen = "CTSP_" + id;
            List<HinhAnh> existingImgs = hinhAnhRepository.findByIdSanPham_Id(ct.getIdSanPham().getId());
            HinhAnh found = null;
            if (existingImgs != null) {
                for (HinhAnh ha : existingImgs) {
                    if (targetTen.equals(ha.getTenAnh())) {
                        found = ha;
                        break;
                    }
                }
            }

            if (imgInput.trim().isEmpty()) {
                // Người dùng gỡ ảnh riêng của biến thể
                if (found != null) {
                    hinhAnhRepository.delete(found);
                }
            } else {
                String processed = processImage(imgInput);
                if (processed != null && !processed.trim().isEmpty()) {
                    if (found != null) {
                        found.setDuongDan(processed.trim());
                        hinhAnhRepository.save(found);
                    } else {
                        HinhAnh ha = new HinhAnh();
                        ha.setIdSanPham(ct.getIdSanPham());
                        ha.setTenAnh(targetTen);
                        ha.setDuongDan(processed.trim());
                        ha.setTrangThai(1);
                        hinhAnhRepository.save(ha);
                    }
                }
            }
        }

        return convertToResponse(updated);
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

                    String filename = "ctsp_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + extension;
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
        ChiTietSanPham ct = chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể có ID: " + id));
        try {
            if (ct.getIdSanPham() != null) {
                List<HinhAnh> imgs = hinhAnhRepository.findByIdSanPham_Id(ct.getIdSanPham().getId());
                if (imgs != null) {
                    String ctspTarget = "CTSP_" + id;
                    for (HinhAnh img : imgs) {
                        if (ctspTarget.equals(img.getTenAnh())) {
                            hinhAnhRepository.delete(img);
                        }
                    }
                }
            }
            chiTietSanPhamRepository.delete(ct);
            chiTietSanPhamRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Không thể xóa biến thể vì đã phát sinh giao dịch/hóa đơn trong hệ thống! Vui lòng đổi trạng thái sang 'Ngừng kinh doanh'.");
        }
    }

    @Transactional
    public List<ChiTietSanPhamResponse> saveBatch(List<ChiTietSanPhamRequest> requests) {
        List<ChiTietSanPhamResponse> list = new ArrayList<>();
        for (ChiTietSanPhamRequest req : requests) {
            if (req.getId() != null) {
                list.add(update(req.getId(), req));
            } else {
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
                // 1. Ưu tiên ảnh riêng theo biến thể CTSP
                String ctspTarget = "CTSP_" + ct.getId();
                for (HinhAnh img : imgs) {
                    if (ctspTarget.equals(img.getTenAnh())) {
                        anhDaiDien = img.getDuongDan();
                        break;
                    }
                }
                // 2. Nếu chưa có ảnh riêng biến thể, lấy ảnh theo ĐÚNG màu sắc của biến thể này
                if (anhDaiDien == null && ct.getIdMauSac() != null) {
                    String colorTarget = "COLOR_" + ct.getIdMauSac().getId();
                    for (HinhAnh img : imgs) {
                        if (colorTarget.equals(img.getTenAnh())) {
                            anhDaiDien = img.getDuongDan();
                            break;
                        }
                    }
                }
                // 3. Nếu vẫn chưa có ảnh riêng và chưa có ảnh theo màu, chỉ lấy ảnh CHUNG của sản phẩm
                // (chỉ áp dụng nếu ảnh chung không thuộc bất kỳ màu nào: tên ảnh không bắt đầu bằng COLOR_ hoặc CTSP_)
                if (anhDaiDien == null) {
                    for (HinhAnh img : imgs) {
                        String tenAnh = img.getTenAnh();
                        if (tenAnh != null && !tenAnh.startsWith("COLOR_") && !tenAnh.startsWith("CTSP_")) {
                            anhDaiDien = img.getDuongDan();
                            break;
                        }
                    }
                }
            }
        }

        // Kiểm tra giảm giá liên kết (theo biến thể hoặc theo sản phẩm cha)
        Instant now = Instant.now();
        List<ChiTietDotGiamGia> activeDiscounts = chiTietDotGiamGiaRepository.findActiveDiscountsByVariantId(ct.getId(), now);
        if ((activeDiscounts == null || activeDiscounts.isEmpty()) && ct.getIdSanPham() != null && ct.getIdSanPham().getId() != null) {
            activeDiscounts = chiTietDotGiamGiaRepository.findActiveDiscountsBySanPhamId(ct.getIdSanPham().getId(), now);
        }
        Boolean coGiamGia = false;
        BigDecimal giaSauGiam = null;
        BigDecimal phanTramGiam = null;
        String tenDotGiamGia = null;

        if (activeDiscounts != null && !activeDiscounts.isEmpty()) {
            coGiamGia = true;
            ChiTietDotGiamGia dgg = activeDiscounts.get(0);
            if (dgg.getIdDotGiamGia() != null) {
                tenDotGiamGia = dgg.getIdDotGiamGia().getTenDotGiamGia();
                phanTramGiam = dgg.getPhanTramGiamBienThe() != null 
                        ? dgg.getPhanTramGiamBienThe() 
                        : dgg.getIdDotGiamGia().getPhanTramGiam();
            }
            if (ct.getGiaBan() != null && phanTramGiam != null && phanTramGiam.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal factor = BigDecimal.valueOf(100).subtract(phanTramGiam).max(BigDecimal.ZERO);
                giaSauGiam = ct.getGiaBan().multiply(factor).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
            } else {
                giaSauGiam = ct.getGiaBan();
            }
        } else {
            giaSauGiam = ct.getGiaBan();
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
                .coGiamGia(coGiamGia)
                .giaSauGiam(giaSauGiam)
                .phanTramGiam(phanTramGiam)
                .tenDotGiamGia(tenDotGiamGia)
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
