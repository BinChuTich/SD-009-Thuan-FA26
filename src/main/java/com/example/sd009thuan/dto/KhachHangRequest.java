package com.example.sd009thuan.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record KhachHangRequest(
        String maKhachHang,
        String taiKhoan,
        @NotBlank(message = "Họ tên khách hàng không được để trống") String tenKhachHang,
        @Email(message = "Email khách hàng không hợp lệ") String email,
        String matKhau,
        @Pattern(regexp = "^$|^0\\d{9,10}$", message = "Số điện thoại phải gồm 10-11 số và bắt đầu bằng 0") String soDienThoai,
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
