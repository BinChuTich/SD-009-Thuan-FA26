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

@Service
public class ChiTietSanPhamService {

    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final SanPhamRepository sanPhamRepository;
    private final KichCoRepository kichCoRepository;
    private final MauSacRepository mauSacRepository;
    private final com.example.sd009thuan.repository.HinhAnhRepository hinhAnhRepository;

    public ChiTietSanPhamService(
            ChiTietSanPhamRepository chiTietSanPhamRepository,
            SanPhamRepository sanPhamRepository,
            KichCoRepository kichCoRepository,
            MauSacRepository mauSacRepository,
            com.example.sd009thuan.repository.HinhAnhRepository hinhAnhRepository
    ) {
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.kichCoRepository = kichCoRepository;
        this.mauSacRepository = mauSacRepository;
        this.hinhAnhRepository = hinhAnhRepository;
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
            throw new RuntimeException("Biến thể với kích cỡ " + kc.getTenKichCo() + " và màu sắc " + ms.getTenMauSac() + " đã tồn tại cho sản phẩm này!");
        }

        if (req.getMaChiTietSanPham() == null || req.getMaChiTietSanPham().trim().isEmpty()) {
            req.setMaChiTietSanPham("CTSP" + System.currentTimeMillis());
        } else if (chiTietSanPhamRepository.existsByMaChiTietSanPham(req.getMaChiTietSanPham().trim())) {
            throw new RuntimeException("Mã chi tiết sản phẩm đã tồn tại: " + req.getMaChiTietSanPham());
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
        return convertToResponse(saved);
    }

    @Transactional
    public ChiTietSanPhamResponse update(Long id, ChiTietSanPhamRequest req) {
        ChiTietSanPham ct = chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể có ID: " + id));

        KichCo kc = kichCoRepository.findById(req.getIdKichCo())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kích cỡ có ID: " + req.getIdKichCo()));
        MauSac ms = mauSacRepository.findById(req.getIdMauSac())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc có ID: " + req.getIdMauSac()));

        if (req.getMaChiTietSanPham() != null && !req.getMaChiTietSanPham().trim().isEmpty()) {
            if (chiTietSanPhamRepository.existsByMaChiTietSanPhamAndIdNot(req.getMaChiTietSanPham().trim(), id)) {
                throw new RuntimeException("Mã chi tiết sản phẩm đã được sử dụng: " + req.getMaChiTietSanPham());
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
        return convertToResponse(updated);
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
        chiTietSanPhamRepository.deleteById(id);
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
                anhDaiDien = imgs.get(0).getDuongDan();
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
