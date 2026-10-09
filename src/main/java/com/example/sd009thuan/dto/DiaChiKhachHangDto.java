package com.example.sd009thuan.dto;

public record DiaChiKhachHangDto(
        Long id,
        Long idKhachHang,
        String maDiaChi,
        String thanhPho,
        String huyen,
        String phuong,
        String diaChiCuThe,
        Boolean macDinh,
        Integer trangThai
) {}
