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
    private final AdministrativeRegionService administrativeRegionService;

    public ExcelImportService(NhanVienRepository repo, VaiTroRepository roleRepo,
                              NhanVienService nhanVienService, EmailService emailService,
                              AdministrativeRegionService administrativeRegionService) {
        this.repo = repo;
        this.roleRepo = roleRepo;
        this.nhanVienService = nhanVienService;
        this.emailService = emailService;
        this.administrativeRegionService = administrativeRegionService;
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
    private static class ParsedEmployeeRow {
        int displayRow;
        String fullName;
        String email;
        String phone;
        boolean gender;
        LocalDate dob;
        String queQuan;
        String phuong;
        String diaChiCuThe;
        VaiTro role;
    }

    /**
     * Nhập danh sách nhân viên từ file Excel theo cơ chế All-or-Nothing
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
        List<ParsedEmployeeRow> parsedRows = new ArrayList<>();
        int totalRows = 0;

        Map<String, Integer> emailToFirstRow = new HashMap<>();
        Map<String, Integer> phoneToFirstRow = new HashMap<>();

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

            // GIAI ĐOẠN 1: KIỂM TRA (VALIDATE) TOÀN BỘ CÁC DÒNG TRONG FILE EXCEL
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                totalRows++;
                int displayRow = i + 1;
                boolean rowHasError = false;

                String fullName = getCellValue(row.getCell(1)).trim();
                String email = getCellValue(row.getCell(2)).trim();
                String phoneRaw = getCellValue(row.getCell(3)).trim();
                String genderStr = getCellValue(row.getCell(4)).trim();
                String dobStr = getCellValue(row.getCell(5)).trim();
                String queQuan = getCellValue(row.getCell(6)).trim();
                String phuong = getCellValue(row.getCell(7)).trim();
                String diaChiCuThe = getCellValue(row.getCell(8)).trim();
                String roleStr = getCellValue(row.getCell(9)).trim();

                // 1. Kiểm tra Họ và tên
                if (fullName.isBlank()) {
                    errors.add(String.format("Dòng %d: Họ tên nhân viên không được để trống", displayRow));
                    rowHasError = true;
                } else if (!fullName.matches("^[\\p{L}\\s'.-]+$")) {
                    errors.add(String.format("Dòng %d: Họ tên '%s' không hợp lệ (không được chứa chữ số hoặc ký tự đặc biệt)", displayRow, fullName));
                    rowHasError = true;
                }

                // 2. Kiểm tra Email
                String emailLower = email.toLowerCase();
                if (email.isBlank()) {
                    errors.add(String.format("Dòng %d: Email không được để trống", displayRow));
                    rowHasError = true;
                } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    errors.add(String.format("Dòng %d: Email '%s' không đúng định dạng chuẩn (ví dụ: example@gmail.com)", displayRow, email));
                    rowHasError = true;
                } else {
                    // Kiểm tra trùng lặp nội bộ trong file Excel
                    if (emailToFirstRow.containsKey(emailLower)) {
                        errors.add(String.format("Dòng %d: Email '%s' bị trùng lặp với Dòng %d trong file Excel",
                                displayRow, email, emailToFirstRow.get(emailLower)));
                        rowHasError = true;
                    } else {
                        emailToFirstRow.put(emailLower, displayRow);
                    }

                    // Kiểm tra trùng lặp với Database
                    if (repo.existsByEmailIgnoreCase(emailLower)) {
                        errors.add(String.format("Dòng %d: Email '%s' đã tồn tại trong hệ thống", displayRow, email));
                        rowHasError = true;
                    }
                }

                // 3. Kiểm tra Số điện thoại
                String phone = normalizePhone(phoneRaw);
                if (phone.isBlank()) {
                    errors.add(String.format("Dòng %d: Số điện thoại không được để trống", displayRow));
                    rowHasError = true;
                } else if (!phone.matches("^0\\d{9,10}$")) {
                    errors.add(String.format("Dòng %d: Số điện thoại '%s' không đúng định dạng (phải từ 10 - 11 số và bắt đầu bằng số 0)", displayRow, phoneRaw));
                    rowHasError = true;
                } else {
                    // Kiểm tra trùng lặp nội bộ trong file Excel
                    if (phoneToFirstRow.containsKey(phone)) {
                        errors.add(String.format("Dòng %d: Số điện thoại '%s' bị trùng lặp với Dòng %d trong file Excel",
                                displayRow, phone, phoneToFirstRow.get(phone)));
                        rowHasError = true;
                    } else {
                        phoneToFirstRow.put(phone, displayRow);
                    }

                    // Kiểm tra trùng lặp với Database
                    if (repo.existsBySoDienThoai(phone)) {
                        errors.add(String.format("Dòng %d: Số điện thoại '%s' đã tồn tại trong hệ thống", displayRow, phone));
                        rowHasError = true;
                    }
                }

                // 4. Giới tính
                boolean gender = true;
                if (!genderStr.isBlank() && (genderStr.equalsIgnoreCase("Nữ") || genderStr.equalsIgnoreCase("Nu") || genderStr.equalsIgnoreCase("Female") || genderStr.equals("0"))) {
                    gender = false;
                }

                // 5. Ngày sinh
                LocalDate dob = null;
                if (dobStr.isBlank()) {
                    errors.add(String.format("Dòng %d: Ngày sinh không được để trống", displayRow));
                    rowHasError = true;
                } else {
                    dob = parseDate(dobStr);
                    if (dob == null) {
                        errors.add(String.format("Dòng %d: Ngày sinh '%s' không hợp lệ (định dạng chuẩn: yyyy-MM-dd hoặc dd/MM/yyyy)", displayRow, dobStr));
                        rowHasError = true;
                    } else if (dob.isAfter(LocalDate.now())) {
                        errors.add(String.format("Dòng %d: Ngày sinh (%s) không được lớn hơn ngày hiện tại", displayRow, dobStr));
                        rowHasError = true;
                    } else if (Period.between(dob, LocalDate.now()).getYears() < 18) {
                        errors.add(String.format("Dòng %d: Nhân viên phải từ đủ 18 tuổi trở lên (ngày sinh: %s)", displayRow, dobStr));
                        rowHasError = true;
                    }
                }

                // 6. Địa chỉ: Kiểm tra Tỉnh/Thành phố hợp lệ và Phường/Xã có thuộc Tỉnh/Thành phố đó không
                AdministrativeRegionService.AddressValidationResult addrResult = administrativeRegionService.validateAddress(queQuan, phuong);
                if (!addrResult.valid()) {
                    for (String addrErr : addrResult.errors()) {
                        errors.add(String.format("Dòng %d: %s", displayRow, addrErr));
                    }
                    rowHasError = true;
                } else {
                    // Chuẩn hóa tên theo danh mục chính thức sau sáp nhập 2025
                    if (addrResult.matchedProvince() != null) {
                        queQuan = addrResult.matchedProvince().name();
                    }
                    if (addrResult.matchedWard() != null) {
                        phuong = addrResult.matchedWard().name();
                    }
                }

                // 7. Chức vụ / Vai trò
                VaiTro role = roleNV;
                if (!roleStr.isBlank() && (roleStr.toLowerCase().contains("quản trị") || roleStr.equalsIgnoreCase("ADMIN") || roleStr.toLowerCase().contains("quản lý"))) {
                    role = roleAdmin != null ? roleAdmin : roleNV;
                }

                if (!rowHasError) {
                    ParsedEmployeeRow p = new ParsedEmployeeRow();
                    p.displayRow = displayRow;
                    p.fullName = fullName;
                    p.email = emailLower;
                    p.phone = phone;
                    p.gender = gender;
                    p.dob = dob;
                    p.queQuan = queQuan;
                    p.phuong = phuong;
                    p.diaChiCuThe = diaChiCuThe;
                    p.role = role;
                    parsedRows.add(p);
                }
            }

        } catch (Exception e) {
            log.error("Lỗi khi đọc file Excel: {}", e.getMessage(), e);
            throw new IllegalStateException("Lỗi khi xử lý file Excel: " + e.getMessage(), e);
        }

        if (totalRows == 0) {
            return new ExcelImportResult(0, 0, 0, List.of("File Excel không có dòng dữ liệu nào để nhập."), "File Excel trống.");
        }

        // GIAI ĐOẠN 2: NGUYÊN TẮC 'TẤT CẢ HOẶC KHÔNG CÓ GÌ' (ALL-OR-NOTHING)
        // Nếu chỉ cần 1 lỗi xuất hiện ở bất kỳ dòng nào, KHÔNG LƯU BẤT KỲ DÒNG NÀO vào database!
        if (!errors.isEmpty()) {
            log.warn("Import Excel bị từ chối do có {} lỗi: {}", errors.size(), errors);
            String summary = String.format("Nhập thất bại: Phát hiện %d lỗi trong file Excel. Toàn bộ file đã bị hủy, không có nhân viên nào được lưu vào hệ thống. Vui lòng sửa lại toàn bộ lỗi trước khi tải lên lại.", errors.size());
            return new ExcelImportResult(totalRows, 0, errors.size(), errors, summary);
        }

        // 100% CÁC DÒNG ĐỀU HỢP LỆ -> LƯU HÀNG LOẠT VÀO DATABASE
        List<NhanVien> entitiesToSave = new ArrayList<>();
        List<EmailService.AccountEmailDto> emailsToSend = new ArrayList<>();

        for (ParsedEmployeeRow r : parsedRows) {
            String code = nhanVienService.generateEmployeeCode(r.fullName);
            String account = code.toLowerCase();
            String password = "NV@" + (100000 + new Random().nextInt(900000));

            NhanVien nv = new NhanVien();
            nv.setIdVaiTro(r.role);
            nv.setMaNhanVien(code);
            nv.setTenTaiKhoan(account);
            nv.setMatKhau(password);
            nv.setTenNhanVien(r.fullName);
            nv.setEmail(r.email);
            nv.setSoDienThoai(r.phone);
            nv.setGioiTinh(r.gender);
            nv.setNgaySinh(r.dob);
            nv.setQueQuan(r.queQuan);
            nv.setPhuong(r.phuong);
            nv.setDiaChiCuThe(r.diaChiCuThe.isBlank() ? null : r.diaChiCuThe);
            nv.setTrangThai(1); // Mặc định Hoạt động
            nv.setNgayTao(Instant.now());
            nv.setNguoiTao("admin (import)");

            entitiesToSave.add(nv);
            emailsToSend.add(new EmailService.AccountEmailDto(nv.getEmail(), nv.getTenNhanVien(), code, account, password));
        }

        repo.saveAll(entitiesToSave);

        // Kích hoạt gửi email tài khoản bất đồng bộ (@Async) chạy ngầm để không làm nghẽn/treo giao diện
        emailService.sendBulkAccountInfoEmailsAsync(emailsToSend);

        String summary = String.format("Nhập thành công toàn bộ %d/%d nhân viên vào hệ thống!", entitiesToSave.size(), totalRows);
        return new ExcelImportResult(totalRows, entitiesToSave.size(), 0, Collections.emptyList(), summary);
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

