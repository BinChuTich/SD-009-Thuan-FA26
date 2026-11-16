package com.example.sd009thuan.service;

import com.example.sd009thuan.dto.NhanVienResponse;
import com.example.sd009thuan.dto.KhachHangResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExcelExportService {
    public byte[] exportEmployees(List<NhanVienResponse> data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("NhanVien");
            String[] headers = {"STT", "Mã NV", "Họ tên", "Tài khoản", "Email", "Giới tính", "Ngày sinh", "SĐT", "Quê quán", "Phường", "Địa chỉ", "Vai trò", "Trạng thái", "Ảnh"};
            CellStyle headerStyle = headerStyle(workbook);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (NhanVienResponse x : data) {
                Row row = sheet.createRow(rowIndex);
                int c = 0;
                row.createCell(c++).setCellValue(rowIndex);
                row.createCell(c++).setCellValue(text(x.maNhanVien()));
                row.createCell(c++).setCellValue(text(x.tenNhanVien()));
                row.createCell(c++).setCellValue(text(x.tenTaiKhoan()));
                row.createCell(c++).setCellValue(text(x.email()));
                row.createCell(c++).setCellValue(gender(x.gioiTinh()));
                row.createCell(c++).setCellValue(date(x.ngaySinh()));
                row.createCell(c++).setCellValue(text(x.soDienThoai()));
                row.createCell(c++).setCellValue(text(x.queQuan()));
                row.createCell(c++).setCellValue(text(x.phuong()));
                row.createCell(c++).setCellValue(text(x.diaChiCuThe()));
                row.createCell(c++).setCellValue(text(x.tenVaiTro()));
                row.createCell(c++).setCellValue(x.trangThai() != null && x.trangThai() == 1 ? "Hoạt động" : "Ngừng hoạt động");
                row.createCell(c).setCellValue(text(x.anhNhanVien()));
                rowIndex++;
            }
            autoSize(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo file Excel nhân viên", e);
        }
    }

    public byte[] exportCustomers(List<KhachHangResponse> data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("KhachHang");
            String[] headers = {"STT", "Mã KH", "Tài khoản", "Họ tên", "Email", "SĐT", "Giới tính", "Ngày sinh", "Tỉnh/Thành phố", "Huyện", "Phường", "Địa chỉ", "Trạng thái"};
            CellStyle headerStyle = headerStyle(workbook);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (KhachHangResponse x : data) {
                Row row = sheet.createRow(rowIndex);
                int c = 0;
                row.createCell(c++).setCellValue(rowIndex);
                row.createCell(c++).setCellValue(text(x.maKhachHang()));
                row.createCell(c++).setCellValue(text(x.taiKhoan()));
                row.createCell(c++).setCellValue(text(x.tenKhachHang()));
                row.createCell(c++).setCellValue(text(x.email()));
                row.createCell(c++).setCellValue(text(x.soDienThoai()));
                row.createCell(c++).setCellValue(gender(x.gioiTinh()));
                row.createCell(c++).setCellValue(date(x.ngaySinh()));
                row.createCell(c++).setCellValue(text(x.thanhPho()));
                row.createCell(c++).setCellValue(text(x.huyen()));
                row.createCell(c++).setCellValue(text(x.phuong()));
                row.createCell(c++).setCellValue(text(x.diaChiCuThe()));
                row.createCell(c).setCellValue(x.trangThai() != null && x.trangThai() == 1 ? "Hoạt động" : "Ngừng hoạt động");
                rowIndex++;
            }
            autoSize(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo file Excel khách hàng", e);
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void autoSize(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) sheet.autoSizeColumn(i);
    }

    private String text(String value) { return value == null ? "" : value; }
    private String date(LocalDate value) { return value == null ? "" : value.toString(); }
    private String gender(Boolean value) { return value == null ? "" : (value ? "Nam" : "Nữ"); }
}
