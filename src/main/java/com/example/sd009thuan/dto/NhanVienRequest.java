package com.example.sd009thuan.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record NhanVienRequest(
        String maNhanVien,
        String tenTaiKhoan,
        @NotBlank(message = "Họ tên nhân viên không được để trống") String tenNhanVien,
        String matKhau,
        @Email(message = "Email nhân viên không hợp lệ") String email,
        @Pattern(regexp = "^$|^0\\d{9,10}$", message = "Số điện thoại phải gồm 10-11 số và bắt đầu bằng 0") String soDienThoai,
        String anhNhanVien,
        Boolean gioiTinh,
        LocalDate ngaySinh,
        String queQuan,
        String phuong,
        String diaChiCuThe,
        @NotNull(message = "Vui lòng chọn vai trò") Long idVaiTro,
        Integer trangThai
) {}
