package com.example.sd009thuan.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class SanPhamResponse {
    private Long id;
    private String maSanPham;
    private String tenSanPham;
    private String moTa;
    private Integer trangThai;
    private Instant ngayTao;
    private Instant ngayCapNhat;
    private String nguoiTao;

    // Thuộc tính mở rộng
    private Long idXuatXu;
    private String tenXuatXu;

    private Long idChatLieu;
    private String tenChatLieu;

    private Long idThuongHieu;
    private String tenThuongHieu;

    private Long idDanhMuc;
    private String tenDanhMuc;

    private Long idCoAo;
    private String tenCoAo;

    private Long idTayAo;
    private String tenTayAo;

    private Long idHoaTiet;
    private String tenHoaTiet;

    // Tổng hợp từ chi tiết biến thể
    private BigDecimal minGia;
    private BigDecimal maxGia;
    private Integer tongSoLuong;
    private Integer soLuongBienThe;
    private Integer soMau;
    private Integer soKichCo;
    private String anhDaiDien;
    private List<String> hinhAnhs;

    // Thông tin đợt giảm giá liên kết
    private Boolean coGiamGia;
    private BigDecimal minGiaSauGiam;
    private BigDecimal maxGiaSauGiam;
    private BigDecimal phanTramGiam;
    private String tenDotGiamGia;

    public SanPhamResponse() {
        super();
    }

    public SanPhamResponse(Long id, String maSanPham, String tenSanPham, String moTa, Integer trangThai, Instant ngayTao, Instant ngayCapNhat, String nguoiTao, Long idXuatXu, String tenXuatXu, Long idChatLieu, String tenChatLieu, Long idThuongHieu, String tenThuongHieu, Long idDanhMuc, String tenDanhMuc, Long idCoAo, String tenCoAo, Long idTayAo, String tenTayAo, Long idHoaTiet, String tenHoaTiet, BigDecimal minGia, BigDecimal maxGia, Integer tongSoLuong, Integer soLuongBienThe, Integer soMau, Integer soKichCo, String anhDaiDien, List<String> hinhAnhs, Boolean coGiamGia, BigDecimal minGiaSauGiam, BigDecimal maxGiaSauGiam, BigDecimal phanTramGiam, String tenDotGiamGia) {
        super();
        this.id = id;
        this.maSanPham = maSanPham;
        this.tenSanPham = tenSanPham;
        this.moTa = moTa;
        this.trangThai = trangThai;
        this.ngayTao = ngayTao;
        this.ngayCapNhat = ngayCapNhat;
        this.nguoiTao = nguoiTao;
        this.idXuatXu = idXuatXu;
        this.tenXuatXu = tenXuatXu;
        this.idChatLieu = idChatLieu;
        this.tenChatLieu = tenChatLieu;
        this.idThuongHieu = idThuongHieu;
        this.tenThuongHieu = tenThuongHieu;
        this.idDanhMuc = idDanhMuc;
        this.tenDanhMuc = tenDanhMuc;
        this.idCoAo = idCoAo;
        this.tenCoAo = tenCoAo;
        this.idTayAo = idTayAo;
        this.tenTayAo = tenTayAo;
        this.idHoaTiet = idHoaTiet;
        this.tenHoaTiet = tenHoaTiet;
        this.minGia = minGia;
        this.maxGia = maxGia;
        this.tongSoLuong = tongSoLuong;
        this.soLuongBienThe = soLuongBienThe;
        this.soMau = soMau;
        this.soKichCo = soKichCo;
        this.anhDaiDien = anhDaiDien;
        this.hinhAnhs = hinhAnhs;
        this.coGiamGia = coGiamGia;
        this.minGiaSauGiam = minGiaSauGiam;
        this.maxGiaSauGiam = maxGiaSauGiam;
        this.phanTramGiam = phanTramGiam;
        this.tenDotGiamGia = tenDotGiamGia;
    }
}
