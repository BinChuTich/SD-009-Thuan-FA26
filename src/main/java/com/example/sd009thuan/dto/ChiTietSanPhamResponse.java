package com.example.sd009thuan.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ChiTietSanPhamResponse {
    private Long id;
    private Long idSanPham;
    private String maSanPham;
    private String tenSanPham;

    private Long idKichCo;
    private String tenKichCo;

    private Long idMauSac;
    private String tenMauSac;
    private String maHex;

    private String maChiTietSanPham;
    private Integer soLuong;
    private BigDecimal giaBan;
    private Integer trangThai;
    private Instant ngayTao;
    private String anhDaiDien;

    // Thông tin giảm giá liên kết
    private Boolean coGiamGia;
    private BigDecimal giaSauGiam;
    private BigDecimal phanTramGiam;
    private String tenDotGiamGia;

    public ChiTietSanPhamResponse() {
        super();
    }

    public ChiTietSanPhamResponse(Long id, Long idSanPham, String maSanPham, String tenSanPham, Long idKichCo, String tenKichCo, Long idMauSac, String tenMauSac, String maHex, String maChiTietSanPham, Integer soLuong, BigDecimal giaBan, Integer trangThai, Instant ngayTao, String anhDaiDien, Boolean coGiamGia, BigDecimal giaSauGiam, BigDecimal phanTramGiam, String tenDotGiamGia) {
        super();
        this.id = id;
        this.idSanPham = idSanPham;
        this.maSanPham = maSanPham;
        this.tenSanPham = tenSanPham;
        this.idKichCo = idKichCo;
        this.tenKichCo = tenKichCo;
        this.idMauSac = idMauSac;
        this.tenMauSac = tenMauSac;
        this.maHex = maHex;
        this.maChiTietSanPham = maChiTietSanPham;
        this.soLuong = soLuong;
        this.giaBan = giaBan;
        this.trangThai = trangThai;
        this.ngayTao = ngayTao;
        this.anhDaiDien = anhDaiDien;
        this.coGiamGia = coGiamGia;
        this.giaSauGiam = giaSauGiam;
        this.phanTramGiam = phanTramGiam;
        this.tenDotGiamGia = tenDotGiamGia;
    }
}
