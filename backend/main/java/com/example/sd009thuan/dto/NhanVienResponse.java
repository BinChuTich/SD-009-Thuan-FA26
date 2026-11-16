package com.example.sd009thuan.dto;

import java.time.LocalDate;

public record NhanVienResponse(
        Long id,
        String maNhanVien,
        String tenTaiKhoan,
        String tenNhanVien,
        String email,
        String soDienThoai,
        String anhNhanVien,
        Boolean gioiTinh,
        LocalDate ngaySinh,
        String queQuan,
        String phuong,
        String diaChiCuThe,
        Long idVaiTro,
        String maVaiTro,
        String tenVaiTro,
        Integer trangThai
) {}
