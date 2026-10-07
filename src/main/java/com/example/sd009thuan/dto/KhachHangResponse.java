package com.example.sd009thuan.dto;

import java.time.LocalDate;

public record KhachHangResponse(
        Long id,
        String maKhachHang,
        String taiKhoan,
        String tenKhachHang,
        String email,
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
