package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.ExcelImportResult;
import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.repository.*;
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
import java.math.BigDecimal;
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

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamService sanPhamService;
    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final HinhAnhRepository hinhAnhRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final ChatLieuRepository chatLieuRepository;
    private final CoAoRepository coAoRepository;
    private final TayAoRepository tayAoRepository;
    private final XuatXuRepository xuatXuRepository;
    private final HoaTietRepository hoaTietRepository;
    private final MauSacRepository mauSacRepository;
    private final KichCoRepository kichCoRepository;

    public ExcelImportService(NhanVienRepository repo, VaiTroRepository roleRepo,
                              NhanVienService nhanVienService, EmailService emailService,
                              com.example.sd009thuan.service.AdministrativeRegionService regionService) {
        this(repo, roleRepo, nhanVienService, emailService, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ExcelImportService(NhanVienRepository repo, VaiTroRepository roleRepo,
                              NhanVienService nhanVienService, EmailService emailService,
                              SanPhamRepository sanPhamRepository, SanPhamService sanPhamService,
                              ChiTietSanPhamRepository chiTietSanPhamRepository, HinhAnhRepository hinhAnhRepository,
                              DanhMucRepository danhMucRepository, ThuongHieuRepository thuongHieuRepository,
                              ChatLieuRepository chatLieuRepository, CoAoRepository coAoRepository,
                              TayAoRepository tayAoRepository, XuatXuRepository xuatXuRepository,
                              HoaTietRepository hoaTietRepository, MauSacRepository mauSacRepository,
                              KichCoRepository kichCoRepository) {
        this.repo = repo;
        this.roleRepo = roleRepo;
        this.nhanVienService = nhanVienService;
        this.emailService = emailService;
        this.sanPhamRepository = sanPhamRepository;
        this.sanPhamService = sanPhamService;
        this.chiTietSanPhamRepository = chiTietSanPhamRepository;
        this.hinhAnhRepository = hinhAnhRepository;
        this.danhMucRepository = danhMucRepository;
        this.thuongHieuRepository = thuongHieuRepository;
        this.chatLieuRepository = chatLieuRepository;
        this.coAoRepository = coAoRepository;
        this.tayAoRepository = tayAoRepository;
        this.xuatXuRepository = xuatXuRepository;
        this.hoaTietRepository = hoaTietRepository;
        this.mauSacRepository = mauSacRepository;
        this.kichCoRepository = kichCoRepository;
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
                }

                // 2. Kiểm tra Email
                String emailLower = email.toLowerCase();
                if (email.isBlank()) {
                    errors.add(String.format("Dòng %d: Email không được để trống", displayRow));
                    rowHasError = true;
                } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    errors.add(String.format("Dòng %d: Email '%s' không đúng định dạng chuẩn", displayRow, email));
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
                LocalDate dob = parseDate(dobStr);
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

                // 6. Địa chỉ
                if (queQuan.isBlank()) {
                    errors.add(String.format("Dòng %d: Tỉnh / Thành phố không được để trống", displayRow));
                    rowHasError = true;
                }
                if (phuong.isBlank()) {
                    errors.add(String.format("Dòng %d: Phường / Xã không được để trống", displayRow));
                    rowHasError = true;
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

    // ==========================================
    // PHẦN NHẬP & XUẤT EXCEL SẢN PHẨM & BIẾN THỂ
    // ==========================================

    /**
     * Tạo file Excel mẫu chuẩn nhập danh sách sản phẩm và các biến thể kèm link ảnh
     */
    public byte[] generateProductTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("SanPhamVaBienThe");

            String[] headers = {
                    "STT", "Tên sản phẩm (*)", "Danh mục (*)", "Thương hiệu (*)",
                    "Chất liệu (*)", "Cổ áo (*)", "Tay áo (*)", "Xuất xứ (*)", "Họa tiết (*)",
                    "Mô tả sản phẩm", "Màu sắc (*)", "Kích cỡ (*)", "Giá bán (*)",
                    "Số lượng (*)", "Link ảnh biến thể"
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
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(30);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Cell styles cho data
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            textStyle.setBorderBottom(BorderStyle.THIN);
            textStyle.setBorderTop(BorderStyle.THIN);
            textStyle.setBorderLeft(BorderStyle.THIN);
            textStyle.setBorderRight(BorderStyle.THIN);

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.cloneStyleFrom(textStyle);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(textStyle);
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0"));

            // 15 dòng dữ liệu mẫu cho 3 sản phẩm (mỗi sản phẩm 5 biến thể)
            Object[][] sampleData = {
                // Sản phẩm 1: Áo Polo Nam Phối Viền Cổ Dệt (5 biến thể)
                {1, "Áo Polo Nam Phối Viền Cổ Dệt", "Áo Polo", "Teelab", "Cotton Compact", "Cổ bẻ", "Tay ngắn", "Việt Nam", "Phối viền", "Áo polo nam cao cấp co giãn 4 chiều thoáng mát", "Đen", "M", 250000, 50, "https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=500"},
                {2, "Áo Polo Nam Phối Viền Cổ Dệt", "Áo Polo", "Teelab", "Cotton Compact", "Cổ bẻ", "Tay ngắn", "Việt Nam", "Phối viền", "Áo polo nam cao cấp co giãn 4 chiều thoáng mát", "Đen", "L", 250000, 60, "https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=500"},
                {3, "Áo Polo Nam Phối Viền Cổ Dệt", "Áo Polo", "Teelab", "Cotton Compact", "Cổ bẻ", "Tay ngắn", "Việt Nam", "Phối viền", "Áo polo nam cao cấp co giãn 4 chiều thoáng mát", "Trắng", "M", 250000, 40, "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500"},
                {4, "Áo Polo Nam Phối Viền Cổ Dệt", "Áo Polo", "Teelab", "Cotton Compact", "Cổ bẻ", "Tay ngắn", "Việt Nam", "Phối viền", "Áo polo nam cao cấp co giãn 4 chiều thoáng mát", "Trắng", "L", 250000, 70, "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500"},
                {5, "Áo Polo Nam Phối Viền Cổ Dệt", "Áo Polo", "Teelab", "Cotton Compact", "Cổ bẻ", "Tay ngắn", "Việt Nam", "Phối viền", "Áo polo nam cao cấp co giãn 4 chiều thoáng mát", "Xanh Navy", "XL", 260000, 35, "https://images.unsplash.com/photo-1586790170083-2f9ceadc732d?w=500"},

                // Sản phẩm 2: Áo Sơ Mi Nam Oxford Dài Tay (5 biến thể)
                {6, "Áo Sơ Mi Nam Oxford Dài Tay", "Áo Sơ Mi", "Owen", "Vải Oxford", "Cổ đức", "Tay dài", "Việt Nam", "Trơn", "Sơ mi nam form slimfit chống nhăn lịch lãm", "Trắng", "M", 380000, 30, "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=500"},
                {7, "Áo Sơ Mi Nam Oxford Dài Tay", "Áo Sơ Mi", "Owen", "Vải Oxford", "Cổ đức", "Tay dài", "Việt Nam", "Trơn", "Sơ mi nam form slimfit chống nhăn lịch lãm", "Trắng", "L", 380000, 45, "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=500"},
                {8, "Áo Sơ Mi Nam Oxford Dài Tay", "Áo Sơ Mi", "Owen", "Vải Oxford", "Cổ đức", "Tay dài", "Việt Nam", "Trơn", "Sơ mi nam form slimfit chống nhăn lịch lãm", "Xanh Nhạt", "M", 380000, 25, "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=500"},
                {9, "Áo Sơ Mi Nam Oxford Dài Tay", "Áo Sơ Mi", "Owen", "Vải Oxford", "Cổ đức", "Tay dài", "Việt Nam", "Trơn", "Sơ mi nam form slimfit chống nhăn lịch lãm", "Xanh Nhạt", "L", 380000, 35, "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=500"},
                {10, "Áo Sơ Mi Nam Oxford Dài Tay", "Áo Sơ Mi", "Owen", "Vải Oxford", "Cổ đức", "Tay dài", "Việt Nam", "Trơn", "Sơ mi nam form slimfit chống nhăn lịch lãm", "Xanh Nhạt", "XL", 395000, 20, "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=500"},

                // Sản phẩm 3: Áo Thun Cổ Tròn Unisex Basic (5 biến thể)
                {11, "Áo Thun Cổ Tròn Unisex Basic", "Áo Thun", "Coolmate", "100% Cotton", "Cổ tròn", "Tay lỡ", "Việt Nam", "Trơn", "Áo phông unisex cotton tự nhiên mềm mại thoáng khí", "Đen", "S", 180000, 60, "https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=500"},
                {12, "Áo Thun Cổ Tròn Unisex Basic", "Áo Thun", "Coolmate", "100% Cotton", "Cổ tròn", "Tay lỡ", "Việt Nam", "Trơn", "Áo phông unisex cotton tự nhiên mềm mại thoáng khí", "Đen", "M", 180000, 80, "https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=500"},
                {13, "Áo Thun Cổ Tròn Unisex Basic", "Áo Thun", "Coolmate", "100% Cotton", "Cổ tròn", "Tay lỡ", "Việt Nam", "Trơn", "Áo phông unisex cotton tự nhiên mềm mại thoáng khí", "Đen", "L", 180000, 90, "https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=500"},
                {14, "Áo Thun Cổ Tròn Unisex Basic", "Áo Thun", "Coolmate", "100% Cotton", "Cổ tròn", "Tay lỡ", "Việt Nam", "Trơn", "Áo phông unisex cotton tự nhiên mềm mại thoáng khí", "Be", "M", 180000, 50, "https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=500"},
                {15, "Áo Thun Cổ Tròn Unisex Basic", "Áo Thun", "Coolmate", "100% Cotton", "Cổ tròn", "Tay lỡ", "Việt Nam", "Trơn", "Áo phông unisex cotton tự nhiên mềm mại thoáng khí", "Be", "L", 180000, 50, "https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=500"}
            };

            for (int r = 0; r < sampleData.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.setHeightInPoints(22);
                Object[] rowData = sampleData[r];
                for (int c = 0; c < rowData.length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = rowData[c];
                    if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                        if (c == 0) {
                            cell.setCellStyle(centerStyle);
                        } else {
                            cell.setCellStyle(numberStyle);
                        }
                    } else {
                        cell.setCellValue(val != null ? val.toString() : "");
                        if (c == 10 || c == 11) {
                            cell.setCellStyle(centerStyle);
                        } else {
                            cell.setCellStyle(textStyle);
                        }
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1400, 3800));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo file mẫu Excel sản phẩm", e);
        }
    }

    private static class ParsedProductRow {
        int displayRow;
        String tenSanPham;
        String tenDanhMuc;
        String tenThuongHieu;
        String tenChatLieu;
        String tenCoAo;
        String tenTayAo;
        String tenXuatXu;
        String hoaTiet;
        String moTa;
        String tenMauSac;
        String tenKichCo;
        BigDecimal giaBan;
        Integer soLuong;
        String linkAnh;
    }

    private BigDecimal parsePrice(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = raw.trim().replace(".", "").replace(",", "").replace("đ", "").replace("VND", "").replace("₫", "").trim();
        try {
            BigDecimal bd = new BigDecimal(clean);
            return bd.compareTo(BigDecimal.ZERO) >= 0 ? bd : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseQuantity(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = raw.trim().replace(".", "").replace(",", "").trim();
        try {
            int q = Integer.parseInt(clean);
            return q >= 0 ? q : null;
        } catch (Exception e) {
            return null;
        }
    }

    private DanhMuc getOrCreateDanhMuc(String name, Map<String, DanhMuc> map) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase();
        if (map.containsKey(key)) return map.get(key);
        DanhMuc entity = new DanhMuc();
        entity.setMaDanhMuc("DM" + (System.currentTimeMillis() % 1000000) + (int)(Math.random() * 100));
        entity.setTenDanhMuc(name.trim());
        entity.setTrangThai(1);
        DanhMuc saved = danhMucRepository.save(entity);
        map.put(key, saved);
        return saved;
    }

    private ThuongHieu getOrCreateThuongHieu(String name, Map<String, ThuongHieu> map) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase();
        if (map.containsKey(key)) return map.get(key);
        ThuongHieu entity = new ThuongHieu();
        entity.setMaThuongHieu("TH" + (System.currentTimeMillis() % 1000000) + (int)(Math.random() * 100));
        entity.setTenThuongHieu(name.trim());
        entity.setTrangThai(1);
        ThuongHieu saved = thuongHieuRepository.save(entity);
        map.put(key, saved);
        return saved;
    }

    private ChatLieu getOrCreateChatLieu(String name, Map<String, ChatLieu> map) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase();
        if (map.containsKey(key)) return map.get(key);
        ChatLieu entity = new ChatLieu();
        entity.setMaChatLieu("CL" + (System.currentTimeMillis() % 1000000) + (int)(Math.random() * 100));
        entity.setTenChatLieu(name.trim());
        entity.setTrangThai(1);
        ChatLieu saved = chatLieuRepository.save(entity);
        map.put(key, saved);
        return saved;
    }

    private CoAo getOrCreateCoAo(String name, Map<String, CoAo> map) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase();
        if (map.containsKey(key)) return map.get(key);
        CoAo entity = new CoAo();
        entity.setMaCoAo("CA" + (System.currentTimeMillis() % 1000000) + (int)(Math.random() * 100));
        entity.setTenCoAo(name.trim());
        entity.setTrangThai(1);
        CoAo saved = coAoRepository.save(entity);
        map.put(key, saved);
        return saved;
    }

    private TayAo getOrCreateTayAo(String name, Map<String, TayAo> map) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase();
        if (map.containsKey(key)) return map.get(key);
        TayAo entity = new TayAo();
        entity.setMaTayAo("TA" + (System.currentTimeMillis() % 1000000) + (int)(Math.random() * 100));
        entity.setTenTayAo(name.trim());
        entity.setTrangThai(1);
        TayAo saved = tayAoRepository.save(entity);
        map.put(key, saved);
        return saved;
    }

    private XuatXu getOrCreateXuatXu(String name, Map<String, XuatXu> map) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase();
        if (map.containsKey(key)) return map.get(key);
        XuatXu entity = new XuatXu();
        entity.setMaXuatXu("XX" + (System.currentTimeMillis() % 1000000) + (int)(Math.random() * 100));
        entity.setTenXuatXu(name.trim());
        entity.setTrangThai(1);
        XuatXu saved = xuatXuRepository.save(entity);
        map.put(key, saved);
        return saved;
    }

    private HoaTiet getOrCreateHoaTiet(String name, Map<String, HoaTiet> map) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase();
        if (map.containsKey(key)) return map.get(key);
        HoaTiet entity = new HoaTiet();
        entity.setMaHoaTiet("HT" + (System.currentTimeMillis() % 1000000) + (int)(Math.random() * 100));
        entity.setTenHoaTiet(name.trim());
        entity.setTrangThai(1);
        HoaTiet saved = hoaTietRepository.save(entity);
        map.put(key, saved);
        return saved;
    }

    private MauSac getOrCreateMauSac(String name, Map<String, MauSac> map) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase();
        if (map.containsKey(key)) return map.get(key);
        MauSac entity = new MauSac();
        entity.setMaMauSac("MS" + (System.currentTimeMillis() % 1000000) + (int)(Math.random() * 100));
        entity.setTenMauSac(name.trim());
        entity.setMaHex("#496883");
        entity.setTrangThai(1);
        MauSac saved = mauSacRepository.save(entity);
        map.put(key, saved);
        return saved;
    }

    private KichCo getOrCreateKichCo(String name, Map<String, KichCo> map) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase();
        if (map.containsKey(key)) return map.get(key);
        KichCo entity = new KichCo();
        entity.setMaKichCo("KC" + (System.currentTimeMillis() % 1000000) + (int)(Math.random() * 100));
        entity.setTenKichCo(name.trim());
        entity.setTrangThai(1);
        KichCo saved = kichCoRepository.save(entity);
        map.put(key, saved);
        return saved;
    }

    private String getNextProductCode(Set<String> usedCodes) {
        String candidate = sanPhamService.nextCode();
        while (usedCodes.contains(candidate.toUpperCase()) || sanPhamRepository.existsByMaSanPhamIgnoreCase(candidate)) {
            int numPart = 1;
            try {
                numPart = Integer.parseInt(candidate.substring(2)) + 1;
            } catch (Exception ignored) {}
            candidate = String.format("SP%03d", numPart);
        }
        usedCodes.add(candidate.toUpperCase());
        return candidate;
    }

    private String getNextCtspCode(SanPham sp, MauSac ms, KichCo kc, Set<String> usedCtspCodes) {
        String candidate;
        do {
            candidate = "CTSP" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        } while (usedCtspCodes.contains(candidate) || chiTietSanPhamRepository.existsByMaChiTietSanPham(candidate));
        usedCtspCodes.add(candidate);
        return candidate;
    }

    public ExcelImportResult importProducts(MultipartFile file) {
        return importProducts(file, false);
    }

    /**
     * Nhập danh sách sản phẩm và các biến thể từ file Excel theo cơ chế All-or-Nothing
     * Có kiểm tra trùng lặp toàn diện:
     * 1. Nhất quán thuộc tính giữa các dòng trong cùng 1 sản phẩm.
     * 2. Trùng lặp biến thể (Màu sắc + Kích cỡ) trong cùng sản phẩm.
     * 3. Trùng cả tên và toàn bộ 7 thuộc tính với sản phẩm đã tồn tại trong hệ thống.
     */
    @Transactional
    public ExcelImportResult importProducts(MultipartFile file, boolean xacNhanGop) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file Excel để nhập dữ liệu");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ. Vui lòng chọn file có định dạng .xlsx hoặc .xls");
        }

        List<String> errors = new ArrayList<>();
        List<ParsedProductRow> parsedRows = new ArrayList<>();
        int totalRows = 0;

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                return new ExcelImportResult(0, 0, 1, List.of("File Excel không chứa dòng dữ liệu nào!"), "File rỗng");
            }

            String currentProductName = null;
            String currentDanhMuc = null;
            String currentThuongHieu = null;
            String currentChatLieu = null;
            String currentCoAo = null;
            String currentTayAo = null;
            String currentXuatXu = null;
            String currentHoaTiet = null;
            String currentMoTa = null;

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;

                totalRows++;
                int displayRow = r + 1;

                String rawTenSp = getCellValue(row.getCell(1));
                String rawDanhMuc = getCellValue(row.getCell(2));
                String rawThuongHieu = getCellValue(row.getCell(3));
                String rawChatLieu = getCellValue(row.getCell(4));
                String rawCoAo = getCellValue(row.getCell(5));
                String rawTayAo = getCellValue(row.getCell(6));
                String rawXuatXu = getCellValue(row.getCell(7));
                String rawHoaTiet = getCellValue(row.getCell(8));
                String rawMoTa = getCellValue(row.getCell(9));
                String rawMauSac = getCellValue(row.getCell(10));
                String rawKichCo = getCellValue(row.getCell(11));
                String rawGiaBan = getCellValue(row.getCell(12));
                String rawSoLuong = getCellValue(row.getCell(13));
                String rawLinkAnh = getCellValue(row.getCell(14));

                // Kế thừa thông tin chung của sản phẩm nếu dòng dưới để trống
                if (!rawTenSp.isBlank()) {
                    currentProductName = rawTenSp.trim();
                    currentDanhMuc = rawDanhMuc.trim();
                    currentThuongHieu = rawThuongHieu.trim();
                    currentChatLieu = rawChatLieu.trim();
                    currentCoAo = rawCoAo.trim();
                    currentTayAo = rawTayAo.trim();
                    currentXuatXu = rawXuatXu.trim();
                    currentHoaTiet = rawHoaTiet.trim();
                    currentMoTa = rawMoTa.trim();
                }

                if (currentProductName == null || currentProductName.isBlank()) {
                    errors.add(String.format("Dòng %d: Tên sản phẩm không được để trống", displayRow));
                }
                if (currentDanhMuc == null || currentDanhMuc.isBlank()) {
                    errors.add(String.format("Dòng %d: Danh mục không được để trống", displayRow));
                }
                if (currentThuongHieu == null || currentThuongHieu.isBlank()) {
                    errors.add(String.format("Dòng %d: Thương hiệu không được để trống", displayRow));
                }
                if (currentChatLieu == null || currentChatLieu.isBlank()) {
                    errors.add(String.format("Dòng %d: Chất liệu không được để trống", displayRow));
                }
                if (currentCoAo == null || currentCoAo.isBlank()) {
                    errors.add(String.format("Dòng %d: Cổ áo không được để trống", displayRow));
                }
                if (currentTayAo == null || currentTayAo.isBlank()) {
                    errors.add(String.format("Dòng %d: Tay áo không được để trống", displayRow));
                }
                if (currentXuatXu == null || currentXuatXu.isBlank()) {
                    errors.add(String.format("Dòng %d: Xuất xứ không được để trống", displayRow));
                }
                if (currentHoaTiet == null || currentHoaTiet.isBlank()) {
                    errors.add(String.format("Dòng %d: Họa tiết không được để trống", displayRow));
                }

                if (rawMauSac.isBlank()) {
                    errors.add(String.format("Dòng %d: Màu sắc biến thể không được để trống", displayRow));
                }
                if (rawKichCo.isBlank()) {
                    errors.add(String.format("Dòng %d: Kích cỡ biến thể không được để trống", displayRow));
                }

                BigDecimal giaBan = parsePrice(rawGiaBan);
                if (giaBan == null) {
                    errors.add(String.format("Dòng %d: Giá bán phải là số hợp lệ không âm (Nhập: '%s')", displayRow, rawGiaBan));
                }

                Integer soLuong = parseQuantity(rawSoLuong);
                if (soLuong == null) {
                    errors.add(String.format("Dòng %d: Số lượng phải là số nguyên không âm (Nhập: '%s')", displayRow, rawSoLuong));
                }

                ParsedProductRow parsed = new ParsedProductRow();
                parsed.displayRow = displayRow;
                parsed.tenSanPham = currentProductName;
                parsed.tenDanhMuc = currentDanhMuc;
                parsed.tenThuongHieu = currentThuongHieu;
                parsed.tenChatLieu = currentChatLieu;
                parsed.tenCoAo = currentCoAo;
                parsed.tenTayAo = currentTayAo;
                parsed.tenXuatXu = currentXuatXu;
                parsed.hoaTiet = currentHoaTiet;
                parsed.moTa = currentMoTa;
                parsed.tenMauSac = rawMauSac.trim();
                parsed.tenKichCo = rawKichCo.trim();
                parsed.giaBan = giaBan != null ? giaBan : BigDecimal.ZERO;
                parsed.soLuong = soLuong != null ? soLuong : 0;
                parsed.linkAnh = rawLinkAnh.trim();
                parsedRows.add(parsed);
            }
        } catch (Exception e) {
            log.error("Lỗi khi đọc file Excel sản phẩm", e);
            return new ExcelImportResult(0, 0, 1, List.of("Không thể đọc file Excel: " + e.getMessage()), "Lỗi đọc file");
        }

        if (totalRows == 0) {
            return new ExcelImportResult(0, 0, 1, List.of("File Excel không chứa dòng dữ liệu nào!"), "File rỗng");
        }

        // KIỂM TRA TÍNH NHẤT QUÁN THUỘC TÍNH CỦA CÙNG SẢN PHẨM & TRÙNG BIẾN THỂ TRONG FILE
        Map<String, ParsedProductRow> firstRowOfProduct = new HashMap<>();
        Map<String, Set<String>> productVariantKeys = new HashMap<>();

        for (ParsedProductRow row : parsedRows) {
            if (row.tenSanPham == null) continue;
            String pKey = row.tenSanPham.trim().toLowerCase();
            ParsedProductRow first = firstRowOfProduct.get(pKey);
            if (first == null) {
                firstRowOfProduct.put(pKey, row);
            } else {
                if (row.tenDanhMuc != null && first.tenDanhMuc != null && !first.tenDanhMuc.equalsIgnoreCase(row.tenDanhMuc)) {
                    errors.add(String.format("Dòng %d: Sản phẩm '%s' có Danh mục ('%s') không đồng nhất với dòng %d ('%s')!",
                            row.displayRow, row.tenSanPham, row.tenDanhMuc, first.displayRow, first.tenDanhMuc));
                }
                if (row.tenThuongHieu != null && first.tenThuongHieu != null && !first.tenThuongHieu.equalsIgnoreCase(row.tenThuongHieu)) {
                    errors.add(String.format("Dòng %d: Sản phẩm '%s' có Thương hiệu ('%s') không đồng nhất với dòng %d ('%s')!",
                            row.displayRow, row.tenSanPham, row.tenThuongHieu, first.displayRow, first.tenThuongHieu));
                }
                if (row.tenChatLieu != null && first.tenChatLieu != null && !first.tenChatLieu.equalsIgnoreCase(row.tenChatLieu)) {
                    errors.add(String.format("Dòng %d: Sản phẩm '%s' có Chất liệu ('%s') không đồng nhất với dòng %d ('%s')!",
                            row.displayRow, row.tenSanPham, row.tenChatLieu, first.displayRow, first.tenChatLieu));
                }
                if (row.tenCoAo != null && first.tenCoAo != null && !first.tenCoAo.equalsIgnoreCase(row.tenCoAo)) {
                    errors.add(String.format("Dòng %d: Sản phẩm '%s' có Cổ áo ('%s') không đồng nhất với dòng %d ('%s')!",
                            row.displayRow, row.tenSanPham, row.tenCoAo, first.displayRow, first.tenCoAo));
                }
                if (row.tenTayAo != null && first.tenTayAo != null && !first.tenTayAo.equalsIgnoreCase(row.tenTayAo)) {
                    errors.add(String.format("Dòng %d: Sản phẩm '%s' có Tay áo ('%s') không đồng nhất với dòng %d ('%s')!",
                            row.displayRow, row.tenSanPham, row.tenTayAo, first.displayRow, first.tenTayAo));
                }
                if (row.tenXuatXu != null && first.tenXuatXu != null && !first.tenXuatXu.equalsIgnoreCase(row.tenXuatXu)) {
                    errors.add(String.format("Dòng %d: Sản phẩm '%s' có Xuất xứ ('%s') không đồng nhất với dòng %d ('%s')!",
                            row.displayRow, row.tenSanPham, row.tenXuatXu, first.displayRow, first.tenXuatXu));
                }
                if (row.hoaTiet != null && first.hoaTiet != null && !first.hoaTiet.equalsIgnoreCase(row.hoaTiet)) {
                    errors.add(String.format("Dòng %d: Sản phẩm '%s' có Họa tiết ('%s') không đồng nhất với dòng %d ('%s')!",
                            row.displayRow, row.tenSanPham, row.hoaTiet, first.displayRow, first.hoaTiet));
                }
            }

            // Kiểm tra trùng biến thể (Màu sắc + Kích cỡ) trong cùng sản phẩm
            Set<String> variantsInProd = productVariantKeys.computeIfAbsent(pKey, k -> new HashSet<>());
            String vKey = (row.tenMauSac != null ? row.tenMauSac.trim().toLowerCase() : "") + "###"
                    + (row.tenKichCo != null ? row.tenKichCo.trim().toLowerCase() : "");
            if (variantsInProd.contains(vKey)) {
                errors.add(String.format("Dòng %d: Biến thể màu sắc '%s' và kích cỡ '%s' bị trùng lặp trong cùng sản phẩm '%s'!",
                        row.displayRow, row.tenMauSac, row.tenKichCo, row.tenSanPham));
            } else {
                variantsInProd.add(vKey);
            }
        }

        // Tải trước và cache các thuộc tính hiện có
        Map<String, DanhMuc> danhMucMap = new HashMap<>();
        danhMucRepository.findAll().forEach(item -> danhMucMap.put(item.getTenDanhMuc().trim().toLowerCase(), item));

        Map<String, ThuongHieu> thuongHieuMap = new HashMap<>();
        thuongHieuRepository.findAll().forEach(item -> thuongHieuMap.put(item.getTenThuongHieu().trim().toLowerCase(), item));

        Map<String, ChatLieu> chatLieuMap = new HashMap<>();
        chatLieuRepository.findAll().forEach(item -> chatLieuMap.put(item.getTenChatLieu().trim().toLowerCase(), item));

        Map<String, CoAo> coAoMap = new HashMap<>();
        coAoRepository.findAll().forEach(item -> coAoMap.put(item.getTenCoAo().trim().toLowerCase(), item));

        Map<String, TayAo> tayAoMap = new HashMap<>();
        tayAoRepository.findAll().forEach(item -> tayAoMap.put(item.getTenTayAo().trim().toLowerCase(), item));

        Map<String, XuatXu> xuatXuMap = new HashMap<>();
        xuatXuRepository.findAll().forEach(item -> xuatXuMap.put(item.getTenXuatXu().trim().toLowerCase(), item));

        Map<String, HoaTiet> hoaTietMap = new HashMap<>();
        hoaTietRepository.findAll().forEach(item -> hoaTietMap.put(item.getTenHoaTiet().trim().toLowerCase(), item));

        Map<String, MauSac> mauSacMap = new HashMap<>();
        mauSacRepository.findAll().forEach(item -> mauSacMap.put(item.getTenMauSac().trim().toLowerCase(), item));

        Map<String, KichCo> kichCoMap = new HashMap<>();
        kichCoRepository.findAll().forEach(item -> kichCoMap.put(item.getTenKichCo().trim().toLowerCase(), item));

        // Gom nhóm theo Sản phẩm
        Map<String, List<ParsedProductRow>> groupedByProduct = new LinkedHashMap<>();
        for (ParsedProductRow r : parsedRows) {
            groupedByProduct.computeIfAbsent(r.tenSanPham.trim(), k -> new ArrayList<>()).add(r);
        }

        // KIỂM TRA TRÙNG SẢN PHẨM VỚI DATABASE (Trùng CẢ TÊN VÀ TẤT CẢ 7 THUỘC TÍNH)
        List<String> duplicateProductList = new ArrayList<>();
        for (Map.Entry<String, List<ParsedProductRow>> entry : groupedByProduct.entrySet()) {
            String prodName = entry.getKey();
            List<ParsedProductRow> variantRows = entry.getValue();
            ParsedProductRow firstRow = variantRows.get(0);

            DanhMuc dm = getOrCreateDanhMuc(firstRow.tenDanhMuc, danhMucMap);
            ThuongHieu th = getOrCreateThuongHieu(firstRow.tenThuongHieu, thuongHieuMap);
            ChatLieu cl = getOrCreateChatLieu(firstRow.tenChatLieu, chatLieuMap);
            CoAo ca = getOrCreateCoAo(firstRow.tenCoAo, coAoMap);
            TayAo ta = getOrCreateTayAo(firstRow.tenTayAo, tayAoMap);
            XuatXu xx = getOrCreateXuatXu(firstRow.tenXuatXu, xuatXuMap);
            HoaTiet ht = getOrCreateHoaTiet(firstRow.hoaTiet, hoaTietMap);

            Optional<SanPham> existingIdentical = sanPhamService.findIdenticalProduct(
                    prodName,
                    dm != null ? dm.getId() : null,
                    th != null ? th.getId() : null,
                    cl != null ? cl.getId() : null,
                    ca != null ? ca.getId() : null,
                    ta != null ? ta.getId() : null,
                    xx != null ? xx.getId() : null,
                    ht != null ? ht.getId() : null
            );

            if (existingIdentical.isPresent()) {
                SanPham existingSp = existingIdentical.get();
                duplicateProductList.add(String.format("Sản phẩm '%s' (Mã: %s)", prodName, existingSp.getMaSanPham()));
            }
        }

        // Kiểm tra nguyên tắc All-or-Nothing đối với lỗi dữ liệu trong file Excel
        if (!errors.isEmpty()) {
            log.warn("Import Excel sản phẩm bị từ chối do có {} lỗi dữ liệu: {}", errors.size(), errors);
            String summary = String.format("Nhập thất bại: Phát hiện %d lỗi trong file Excel. Vui lòng sửa lại toàn bộ lỗi trước khi tải lên lại.", errors.size());
            return new ExcelImportResult(totalRows, 0, errors.size(), errors, summary, duplicateProductList);
        }

        // Nếu file hợp lệ nhưng có sản phẩm trùng và người dùng chưa xác nhận gộp
        if (!duplicateProductList.isEmpty() && !xacNhanGop) {
            log.info("Phát hiện {} sản phẩm trùng trong hệ thống, yêu cầu xác nhận gộp từ người dùng.", duplicateProductList.size());
            return new ExcelImportResult(
                    totalRows,
                    0,
                    duplicateProductList.size(),
                    Collections.emptyList(),
                    "DUPLICATE_PRODUCTS_NEED_CONFIRM",
                    duplicateProductList
            );
        }

        Set<String> usedProductCodes = new HashSet<>();
        Set<String> usedCtspCodes = new HashSet<>();
        int savedVariantsCount = 0;

        for (Map.Entry<String, List<ParsedProductRow>> entry : groupedByProduct.entrySet()) {
            String prodName = entry.getKey();
            List<ParsedProductRow> variantRows = entry.getValue();
            ParsedProductRow firstRow = variantRows.get(0);

            DanhMuc dm = getOrCreateDanhMuc(firstRow.tenDanhMuc, danhMucMap);
            ThuongHieu th = getOrCreateThuongHieu(firstRow.tenThuongHieu, thuongHieuMap);
            ChatLieu cl = getOrCreateChatLieu(firstRow.tenChatLieu, chatLieuMap);
            CoAo ca = getOrCreateCoAo(firstRow.tenCoAo, coAoMap);
            TayAo ta = getOrCreateTayAo(firstRow.tenTayAo, tayAoMap);
            XuatXu xx = getOrCreateXuatXu(firstRow.tenXuatXu, xuatXuMap);
            HoaTiet ht = getOrCreateHoaTiet(firstRow.hoaTiet, hoaTietMap);

            // Tìm sản phẩm trùng CẢ TÊN VÀ TOÀN BỘ 7 THUỘC TÍNH
            Optional<SanPham> existingIdentical = sanPhamService.findIdenticalProduct(
                    prodName,
                    dm != null ? dm.getId() : null,
                    th != null ? th.getId() : null,
                    cl != null ? cl.getId() : null,
                    ca != null ? ca.getId() : null,
                    ta != null ? ta.getId() : null,
                    xx != null ? xx.getId() : null,
                    ht != null ? ht.getId() : null
            );

            SanPham sp;
            if (existingIdentical.isPresent()) {
                // Đã tồn tại trùng cả tên và tất cả thuộc tính, người dùng đã xác nhận gộp
                sp = existingIdentical.get();
                if (firstRow.moTa != null && !firstRow.moTa.isBlank()) {
                    sp.setMoTa(firstRow.moTa.trim());
                }
                sp.setTrangThai(1);
                sp.setNgayCapNhat(Instant.now());
                sp.setNguoiCapNhat("admin (import excel)");
                sp = sanPhamRepository.saveAndFlush(sp);
                usedProductCodes.add(sp.getMaSanPham().trim().toUpperCase());
            } else {
                // Sản phẩm mới hoàn toàn (hoặc có cùng tên nhưng bộ thuộc tính khác)
                String candidateCode = getNextProductCode(usedProductCodes);
                sp = new SanPham();
                sp.setMaSanPham(candidateCode);
                sp.setTenSanPham(prodName);
                sp.setMoTa(firstRow.moTa != null && !firstRow.moTa.isBlank() ? firstRow.moTa.trim() : null);
                sp.setIdDanhMuc(dm);
                sp.setIdThuongHieu(th);
                sp.setIdChatLieu(cl);
                sp.setIdCoAo(ca);
                sp.setIdTayAo(ta);
                sp.setIdXuatXu(xx);
                sp.setIdHoaTiet(ht);
                sp.setTrangThai(1);
                sp.setNgayTao(Instant.now());
                sp.setNguoiTao("admin (import excel)");
                sp = sanPhamRepository.saveAndFlush(sp);
            }

            for (ParsedProductRow vr : variantRows) {
                MauSac ms = getOrCreateMauSac(vr.tenMauSac, mauSacMap);
                KichCo kc = getOrCreateKichCo(vr.tenKichCo, kichCoMap);

                Optional<ChiTietSanPham> existingCtspOpt = chiTietSanPhamRepository
                        .findByIdSanPham_IdAndIdKichCo_IdAndIdMauSac_Id(sp.getId(), kc.getId(), ms.getId());

                ChiTietSanPham ctsp;
                if (existingCtspOpt.isPresent()) {
                    ctsp = existingCtspOpt.get();
                    ctsp.setSoLuong(ctsp.getSoLuong() + vr.soLuong);
                    ctsp.setGiaBan(vr.giaBan);
                    ctsp.setNgayCapNhat(Instant.now());
                    ctsp.setNguoiCapNhat("admin (import excel)");
                    ctsp = chiTietSanPhamRepository.saveAndFlush(ctsp);
                } else {
                    ctsp = new ChiTietSanPham();
                    ctsp.setIdSanPham(sp);
                    ctsp.setIdMauSac(ms);
                    ctsp.setIdKichCo(kc);
                    String candidateCtspCode = getNextCtspCode(sp, ms, kc, usedCtspCodes);
                    ctsp.setMaChiTietSanPham(candidateCtspCode);
                    ctsp.setSoLuong(vr.soLuong);
                    ctsp.setGiaBan(vr.giaBan);
                    ctsp.setTrangThai(1);
                    ctsp.setNgayTao(Instant.now());
                    ctsp.setNguoiTao("admin (import excel)");
                    ctsp = chiTietSanPhamRepository.saveAndFlush(ctsp);
                }
                savedVariantsCount++;

                // Lưu đường dẫn ảnh biến thể vào bảng hinh_anh
                if (vr.linkAnh != null && !vr.linkAnh.isBlank()) {
                    String imgUrl = vr.linkAnh.trim();
                    String targetTen = "CTSP_" + ctsp.getId();
                    List<HinhAnh> existingImgs = hinhAnhRepository.findByIdSanPham_Id(sp.getId());
                    HinhAnh found = null;
                    if (existingImgs != null) {
                        for (HinhAnh ha : existingImgs) {
                            if (targetTen.equals(ha.getTenAnh())) {
                                found = ha;
                                break;
                            }
                        }
                    }
                    if (found != null) {
                        found.setDuongDan(imgUrl);
                        hinhAnhRepository.save(found);
                    } else {
                        HinhAnh ha = new HinhAnh();
                        ha.setIdSanPham(sp);
                        ha.setTenAnh(targetTen);
                        ha.setDuongDan(imgUrl);
                        ha.setTrangThai(1);
                        hinhAnhRepository.save(ha);
                    }

                    // Đảm bảo có ảnh chung đại diện cho sản phẩm
                    boolean hasGeneral = false;
                    if (existingImgs != null) {
                        for (HinhAnh ha : existingImgs) {
                            if (ha.getTenAnh() != null && !ha.getTenAnh().startsWith("CTSP_") && !ha.getTenAnh().startsWith("COLOR_")) {
                                hasGeneral = true;
                                break;
                            }
                        }
                    }
                    if (!hasGeneral) {
                        HinhAnh generalHa = new HinhAnh();
                        generalHa.setIdSanPham(sp);
                        generalHa.setTenAnh(sp.getTenSanPham());
                        generalHa.setDuongDan(imgUrl);
                        generalHa.setTrangThai(1);
                        hinhAnhRepository.save(generalHa);
                    }
                }
            }
        }

        String summary = String.format("Nhập thành công toàn bộ %d sản phẩm với %d biến thể vào hệ thống!", groupedByProduct.size(), savedVariantsCount);
        return new ExcelImportResult(totalRows, savedVariantsCount, 0, Collections.emptyList(), summary);
    }
}

