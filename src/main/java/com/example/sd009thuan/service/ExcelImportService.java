package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ExcelImportResult;
import com.example.sd009thuan.entity.NhanVien;
import com.example.sd009thuan.entity.VaiTro;
import com.example.sd009thuan.repository.NhanVienRepository;
import com.example.sd009thuan.repository.VaiTroRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExcelImportService {
    private static final Logger log = LoggerFactory.getLogger(ExcelImportService.class);

    private final NhanVienRepository repo;
    private final VaiTroRepository roleRepo;
    private final NhanVienService nhanVienService;
    private final EmailService emailService;

    public ExcelImportService(NhanVienRepository repo, VaiTroRepository roleRepo,
                              NhanVienService nhanVienService, EmailService emailService) {
        this.repo = repo;
        this.roleRepo = roleRepo;
        this.nhanVienService = nhanVienService;
        this.emailService = emailService;
    }

    /**
     * Tạo file Excel mẫu chuẩn để người dùng tải về nhập danh sách nhân viên
     */
    public byte[] generateEmployeeTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("MauNhapNhanVien");

            String[] headers = {
                    "STT", "Họ và tên (*)", "Email (*)", "Số điện thoại (*)",
                    "Giới tính (Nam/Nữ)", "Ngày sinh (yyyy-MM-dd)",
                    "Tỉnh / Thành phố (*)", "Phường / Xã (*)", "Địa chỉ cụ thể", "Chức vụ (Nhân viên/Quản trị viên)"
            };

            // Style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Dòng dữ liệu mẫu 1
            Row sampleRow1 = sheet.createRow(1);
            sampleRow1.createCell(0).setCellValue(1);
            sampleRow1.createCell(1).setCellValue("Nguyễn Văn An");
            sampleRow1.createCell(2).setCellValue("nguyenvanan.sample@gmail.com");
            sampleRow1.createCell(3).setCellValue("0912345678");
            sampleRow1.createCell(4).setCellValue("Nam");
            sampleRow1.createCell(5).setCellValue("2000-05-15");
            sampleRow1.createCell(6).setCellValue("Thành phố Hà Nội");
            sampleRow1.createCell(7).setCellValue("Phường Ba Đình");
            sampleRow1.createCell(8).setCellValue("Số 15 Đội Cấn");
            sampleRow1.createCell(9).setCellValue("Nhân viên");

            // Dòng dữ liệu mẫu 2
            Row sampleRow2 = sheet.createRow(2);
            sampleRow2.createCell(0).setCellValue(2);
            sampleRow2.createCell(1).setCellValue("Trần Thị Hoa");
            sampleRow2.createCell(2).setCellValue("tranthihoa.sample@gmail.com");
            sampleRow2.createCell(3).setCellValue("0987654321");
            sampleRow2.createCell(4).setCellValue("Nữ");
            sampleRow2.createCell(5).setCellValue("1998-10-20");
            sampleRow2.createCell(6).setCellValue("Thành phố Bắc Ninh");
            sampleRow2.createCell(7).setCellValue("Phường Bắc Giang");
            sampleRow2.createCell(8).setCellValue("Số 88 Lê Lợi");
            sampleRow2.createCell(9).setCellValue("Nhân viên");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3600));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo file mẫu Excel nhân viên", e);
        }
    }

    /**
     * Nhập danh sách nhân viên từ file Excel
     */
    @Transactional
    public ExcelImportResult importEmployees(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file Excel để nhập dữ liệu");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ. Vui lòng chọn file có định dạng .xlsx hoặc .xls");
        }

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows = 0;

        Set<String> processedEmailsInFile = new HashSet<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("File Excel không có dữ liệu trang tính.");
            }

            int lastRowNum = sheet.getLastRowNum();
            if (lastRowNum < 1) {
                return new ExcelImportResult(0, 0, 0, List.of("File Excel không có dòng dữ liệu nào."), "File Excel trống.");
            }

            // Lấy vai trò mặc định
            VaiTro roleNV = roleRepo.findByMaVaiTroIgnoreCaseAndTrangThai("NV", 1).orElse(null);
            VaiTro roleAdmin = roleRepo.findByMaVaiTroIgnoreCaseAndTrangThai("ADMIN", 1).orElse(null);

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                totalRows++;
                int displayRow = i + 1;

                String fullName = getCellValue(row.getCell(1));
                String email = getCellValue(row.getCell(2));
                String phone = getCellValue(row.getCell(3));
                String genderStr = getCellValue(row.getCell(4));
                String dobStr = getCellValue(row.getCell(5));
                String queQuan = getCellValue(row.getCell(6));
                String phuong = getCellValue(row.getCell(7));
                String diaChiCuThe = getCellValue(row.getCell(8));
                String roleStr = getCellValue(row.getCell(9));

                // 1. Kiểm tra họ tên
                if (fullName.isBlank()) {
                    errors.add(String.format("Dòng %d: Họ tên nhân viên không được để trống", displayRow));
                    continue;
                }

                // 2. Kiểm tra email
                if (email.isBlank()) {
                    errors.add(String.format("Dòng %d (%s): Email không được để trống", displayRow, fullName));
                    continue;
                }
                if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    errors.add(String.format("Dòng %d (%s): Email không đúng định dạng (%s)", displayRow, fullName, email));
                    continue;
                }
                String emailLower = email.toLowerCase().trim();
                if (processedEmailsInFile.contains(emailLower)) {
                    errors.add(String.format("Dòng %d (%s): Email bị trùng lặp trong file Excel (%s)", displayRow, fullName, email));
                    continue;
                }
                if (repo.findAll().stream().anyMatch(e -> emailLower.equalsIgnoreCase(e.getEmail()))) {
                    errors.add(String.format("Dòng %d (%s): Email đã tồn tại trong hệ thống (%s)", displayRow, fullName, email));
                    continue;
                }

                // 3. Kiểm tra số điện thoại
                phone = normalizePhone(phone);
                if (phone.isBlank()) {
                    errors.add(String.format("Dòng %d (%s): Số điện thoại không được để trống", displayRow, fullName));
                    continue;
                }
                if (!phone.matches("^0\\d{9,10}$")) {
                    errors.add(String.format("Dòng %d (%s): Số điện thoại phải từ 10-11 số và bắt đầu bằng số 0 (%s)", displayRow, fullName, phone));
                    continue;
                }

                // 4. Giới tính
                boolean gender = true; // Mặc định Nam
                if (genderStr != null && (genderStr.equalsIgnoreCase("Nữ") || genderStr.equalsIgnoreCase("Nu") || genderStr.equalsIgnoreCase("Female"))) {
                    gender = false;
                }

                // 5. Ngày sinh
                LocalDate dob = parseDate(dobStr);
                if (dob == null) {
                    errors.add(String.format("Dòng %d (%s): Ngày sinh không hợp lệ (%s). Định dạng đúng: yyyy-MM-dd hoặc dd/MM/yyyy", displayRow, fullName, dobStr));
                    continue;
                }
                if (dob.isAfter(LocalDate.now())) {
                    errors.add(String.format("Dòng %d (%s): Ngày sinh không được lớn hơn ngày hiện tại", displayRow, fullName));
                    continue;
                }
                if (Period.between(dob, LocalDate.now()).getYears() < 18) {
                    errors.add(String.format("Dòng %d (%s): Nhân viên phải từ đủ 18 tuổi trở lên", displayRow, fullName));
                    continue;
                }

                // 6. Địa chỉ
                if (queQuan.isBlank()) {
                    errors.add(String.format("Dòng %d (%s): Tỉnh / Thành phố không được để trống", displayRow, fullName));
                    continue;
                }
                if (phuong.isBlank()) {
                    errors.add(String.format("Dòng %d (%s): Phường / Xã không được để trống", displayRow, fullName));
                    continue;
                }

                // 7. Chức vụ
                VaiTro role = roleNV;
                if (roleStr != null && (roleStr.toLowerCase().contains("quản trị") || roleStr.equalsIgnoreCase("ADMIN"))) {
                    role = roleAdmin != null ? roleAdmin : roleNV;
                }

                // 8. Tạo mới nhân viên
                try {
                    String code = nhanVienService.generateEmployeeCode(fullName);
                    String account = code.toLowerCase();
                    String password = "NV@" + (100000 + new Random().nextInt(900000));

                    NhanVien nv = new NhanVien();
                    nv.setIdVaiTro(role);
                    nv.setMaNhanVien(code);
                    nv.setTenTaiKhoan(account);
                    nv.setMatKhau(password);
                    nv.setTenNhanVien(fullName);
                    nv.setEmail(emailLower);
                    nv.setSoDienThoai(phone);
                    nv.setGioiTinh(gender);
                    nv.setNgaySinh(dob);
                    nv.setQueQuan(queQuan);
                    nv.setPhuong(phuong);
                    nv.setDiaChiCuThe(diaChiCuThe.isBlank() ? null : diaChiCuThe);
                    nv.setTrangThai(1); // Mặc định Hoạt động
                    nv.setNgayTao(Instant.now());
                    nv.setNguoiTao("admin (import)");

                    repo.save(nv);
                    processedEmailsInFile.add(emailLower);
                    successCount++;

                    // Gửi email thông tin tài khoản cho nhân viên mới
                    emailService.sendAccountInfoEmail(nv.getEmail(), nv.getTenNhanVien(), code, account, password);
                } catch (Exception ex) {
                    log.error("Lỗi khi lưu nhân viên dòng {}: {}", displayRow, ex.getMessage(), ex);
                    errors.add(String.format("Dòng %d (%s): Lỗi hệ thống khi lưu - %s", displayRow, fullName, ex.getMessage()));
                }
            }

        } catch (Exception e) {
            log.error("Lỗi khi đọc file Excel: {}", e.getMessage(), e);
            throw new IllegalStateException("Lỗi khi xử lý file Excel: " + e.getMessage(), e);
        }

        String summary = String.format("Đã nhập thành công %d/%d nhân viên. %s",
                successCount, totalRows,
                errors.isEmpty() ? "Tất cả các bản ghi đều hợp lệ." : "Có " + errors.size() + " lỗi được ghi nhận.");

        return new ExcelImportResult(totalRows, successCount, errors.size(), errors, summary);
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    } else {
                        double val = cell.getNumericCellValue();
                        if (val == Math.floor(val)) {
                            return String.format("%.0f", val);
                        }
                        return String.valueOf(val);
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return cell.getStringCellValue().trim();
                    } catch (Exception e) {
                        return String.format("%.0f", cell.getNumericCellValue());
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizePhone(String raw) {
        if (raw == null) return "";
        String p = raw.replaceAll("[^0-9]", "");
        if (p.length() == 9 && !p.startsWith("0")) {
            p = "0" + p;
        }
        return p;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String val = raw.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );
        for (DateTimeFormatter dtf : formatters) {
            try {
                return LocalDate.parse(val, dtf);
            } catch (Exception ignored) {}
        }
        return null;
    }
}

