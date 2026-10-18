package com.example.sd009thuan.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class KhachHangDTO {
    private Long id;
    private String maKhachHang;
    private String tenKhachHang;
    private String soDienThoai;
    private String email;
    private LocalDate ngaySinh;
    private Boolean gioiTinh; // true: Nam, false: Nu
    private Long tongDonMua;
    private Instant ngayMuaGanNhat;
    private Integer trangThai;

    public KhachHangDTO() {
        super();
    }

    public KhachHangDTO(Long id, String maKhachHang, String tenKhachHang, String soDienThoai, String email, LocalDate ngaySinh, Boolean gioiTinh, Long tongDonMua, Instant ngayMuaGanNhat, Integer trangThai) {
        super();
        this.id = id;
        this.maKhachHang = maKhachHang;
        this.tenKhachHang = tenKhachHang;
        this.soDienThoai = soDienThoai;
        this.email = email;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.tongDonMua = tongDonMua;
        this.ngayMuaGanNhat = ngayMuaGanNhat;
        this.trangThai = trangThai;
    }
}
