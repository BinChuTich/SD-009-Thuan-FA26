package com.example.sd009thuan.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record NhanVienRequest(
        String maNhanVien,
        String tenTaiKhoan,
        @NotBlank(message = "Họ tên không được để trống") String tenNhanVien,
        String matKhau,
        String email,
        String soDienThoai,
        String anhNhanVien,
        Boolean gioiTinh,
        LocalDate ngaySinh,
        String queQuan,
        String phuong,
        String diaChiCuThe,
        Long idVaiTro,
        Integer trangThai
) {}
