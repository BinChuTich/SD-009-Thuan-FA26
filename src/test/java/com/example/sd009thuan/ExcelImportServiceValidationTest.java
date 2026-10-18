package com.example.sd009thuan;

import com.example.sd009thuan.dto.ExcelImportResult;
import com.example.sd009thuan.entity.VaiTro;
import com.example.sd009thuan.repository.NhanVienRepository;
import com.example.sd009thuan.repository.VaiTroRepository;
import com.example.sd009thuan.service.AdministrativeRegionService;
import com.example.sd009thuan.service.EmailService;
import com.example.sd009thuan.service.ExcelImportService;
import com.example.sd009thuan.service.NhanVienService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ExcelImportServiceValidationTest {

    private NhanVienRepository nhanVienRepo;
    private VaiTroRepository roleRepo;
    private NhanVienService nhanVienService;
    private EmailService emailService;
    private AdministrativeRegionService regionService;
    private ExcelImportService excelImportService;

    @BeforeEach
    void setUp() {
        nhanVienRepo = Mockito.mock(NhanVienRepository.class);
        roleRepo = Mockito.mock(VaiTroRepository.class);
        nhanVienService = Mockito.mock(NhanVienService.class);
        emailService = Mockito.mock(EmailService.class);

        regionService = new AdministrativeRegionService();
        regionService.init();

        VaiTro roleNV = new VaiTro();
        roleNV.setId(1L);
        roleNV.setMaVaiTro("NV");
        when(roleRepo.findByMaVaiTroIgnoreCaseAndTrangThai("NV", 1)).thenReturn(Optional.of(roleNV));

        excelImportService = new ExcelImportService(nhanVienRepo, roleRepo, nhanVienService, emailService, regionService);
    }

    @Test
    void testImportWithAddressMismatchAndCollectAllErrors() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employees");

            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("STT");
            header.createCell(1).setCellValue("Họ và tên (*)");
            header.createCell(2).setCellValue("Email (*)");
            header.createCell(3).setCellValue("Số điện thoại (*)");
            header.createCell(4).setCellValue("Giới tính");
            header.createCell(5).setCellValue("Ngày sinh");
            header.createCell(6).setCellValue("Tỉnh / Thành phố (*)");
            header.createCell(7).setCellValue("Phường / Xã (*)");

            // Dòng 2 (displayRow = 2): Phường Bến Nghé (thuộc HCM) gán cho Hà Nội -> Lỗi địa chỉ
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue(1);
            row1.createCell(1).setCellValue("Nguyễn Văn A");
            row1.createCell(2).setCellValue("nguyenvana@gmail.com");
            row1.createCell(3).setCellValue("0912345678");
            row1.createCell(4).setCellValue("Nam");
            row1.createCell(5).setCellValue("2000-01-01");
            row1.createCell(6).setCellValue("Thành phố Hà Nội");
            row1.createCell(7).setCellValue("Phường Bến Nghé"); // Không thuộc Hà Nội

            // Dòng 3 (displayRow = 3): Có nhiều lỗi cùng lúc: Tên rỗng, Email sai format, Ngày sinh < 18 tuổi, Tỉnh không tồn tại
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue(2);
            row2.createCell(1).setCellValue(""); // Lỗi: họ tên rỗng
            row2.createCell(2).setCellValue("email-sai-dinh-dang"); // Lỗi: sai email
            row2.createCell(3).setCellValue("0987654321");
            row2.createCell(4).setCellValue("Nữ");
            row2.createCell(5).setCellValue("2020-01-01"); // Lỗi: chưa đủ 18 tuổi
            row2.createCell(6).setCellValue("Tỉnh Không Tồn Tại"); // Lỗi: tỉnh không tồn tại
            row2.createCell(7).setCellValue("Phường A");

            workbook.write(out);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test_import.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );

            ExcelImportResult result = excelImportService.importEmployees(file);

            assertEquals(0, result.successCount(), "All-or-nothing: khi có lỗi thì không được lưu bất kỳ dòng nào");
            assertTrue(result.errorCount() >= 4, "Phải gom đầy đủ tất cả các lỗi từ các dòng, hiện tại có: " + result.errorCount());

            // Kiểm tra lỗi địa chỉ ở Dòng 2
            boolean foundAddressMismatchError = result.errors().stream().anyMatch(e ->
                    e.contains("Dòng 2") && e.contains("Phường Bến Nghé") && e.contains("không thuộc Tỉnh / Thành phố"));
            assertTrue(foundAddressMismatchError, "Phải có thông báo lỗi Phường Bến Nghé không thuộc Hà Nội ở Dòng 2. Danh sách: " + result.errors());

            // Kiểm tra các lỗi ở Dòng 3 được gom đầy đủ
            boolean foundNameError = result.errors().stream().anyMatch(e -> e.contains("Dòng 3") && e.contains("Họ tên"));
            boolean foundEmailError = result.errors().stream().anyMatch(e -> e.contains("Dòng 3") && e.contains("Email"));
            boolean foundDobError = result.errors().stream().anyMatch(e -> e.contains("Dòng 3") && e.contains("18 tuổi"));
            boolean foundProvinceError = result.errors().stream().anyMatch(e -> e.contains("Dòng 3") && e.contains("không tồn tại"));

            assertTrue(foundNameError, "Phải có lỗi họ tên ở Dòng 3");
            assertTrue(foundEmailError, "Phải có lỗi email ở Dòng 3");
            assertTrue(foundDobError, "Phải có lỗi ngày sinh ở Dòng 3");
            assertTrue(foundProvinceError, "Phải có lỗi tỉnh thành ở Dòng 3");
        }
    }
}

