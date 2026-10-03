package com.example.sd009thuan.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record KhachHangRequest(
        String maKhachHang,
        String taiKhoan,
        @NotBlank(message = "Họ tên không được để trống") String tenKhachHang,
        @Email(message = "Email không hợp lệ") String email,
        String matKhau,
        String soDienThoai,
        LocalDate ngaySinh,
        Boolean gioiTinh,
        Integer trangThai,
        String thanhPho,
        String huyen,
        String phuong,
        String diaChiCuThe,
        String nguoiNhan,
        String soDienThoaiNhan
) {}

