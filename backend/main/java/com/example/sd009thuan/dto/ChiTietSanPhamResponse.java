package com.example.sd009thuan.dto;

import java.math.BigDecimal;
import java.time.Instant;

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
    }

    public ChiTietSanPhamResponse(Long id, Long idSanPham, String maSanPham, String tenSanPham, Long idKichCo, String tenKichCo, Long idMauSac, String tenMauSac, String maHex, String maChiTietSanPham, Integer soLuong, BigDecimal giaBan, Integer trangThai, Instant ngayTao, String anhDaiDien, Boolean coGiamGia, BigDecimal giaSauGiam, BigDecimal phanTramGiam, String tenDotGiamGia) {
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdSanPham() { return idSanPham; }
    public void setIdSanPham(Long idSanPham) { this.idSanPham = idSanPham; }

    public String getMaSanPham() { return maSanPham; }
    public void setMaSanPham(String maSanPham) { this.maSanPham = maSanPham; }

    public String getTenSanPham() { return tenSanPham; }
    public void setTenSanPham(String tenSanPham) { this.tenSanPham = tenSanPham; }

    public Long getIdKichCo() { return idKichCo; }
    public void setIdKichCo(Long idKichCo) { this.idKichCo = idKichCo; }

    public String getTenKichCo() { return tenKichCo; }
    public void setTenKichCo(String tenKichCo) { this.tenKichCo = tenKichCo; }

    public Long getIdMauSac() { return idMauSac; }
    public void setIdMauSac(Long idMauSac) { this.idMauSac = idMauSac; }

    public String getTenMauSac() { return tenMauSac; }
    public void setTenMauSac(String tenMauSac) { this.tenMauSac = tenMauSac; }

    public String getMaHex() { return maHex; }
    public void setMaHex(String maHex) { this.maHex = maHex; }

    public String getMaChiTietSanPham() { return maChiTietSanPham; }
    public void setMaChiTietSanPham(String maChiTietSanPham) { this.maChiTietSanPham = maChiTietSanPham; }

    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }

    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }

    public Integer getTrangThai() { return trangThai; }
    public void setTrangThai(Integer trangThai) { this.trangThai = trangThai; }

    public Instant getNgayTao() { return ngayTao; }
    public void setNgayTao(Instant ngayTao) { this.ngayTao = ngayTao; }

    public String getAnhDaiDien() { return anhDaiDien; }
    public void setAnhDaiDien(String anhDaiDien) { this.anhDaiDien = anhDaiDien; }

    public Boolean getCoGiamGia() { return coGiamGia; }
    public void setCoGiamGia(Boolean coGiamGia) { this.coGiamGia = coGiamGia; }

    public BigDecimal getGiaSauGiam() { return giaSauGiam; }
    public void setGiaSauGiam(BigDecimal giaSauGiam) { this.giaSauGiam = giaSauGiam; }

    public BigDecimal getPhanTramGiam() { return phanTramGiam; }
    public void setPhanTramGiam(BigDecimal phanTramGiam) { this.phanTramGiam = phanTramGiam; }

    public String getTenDotGiamGia() { return tenDotGiamGia; }
    public void setTenDotGiamGia(String tenDotGiamGia) { this.tenDotGiamGia = tenDotGiamGia; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
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
        private Boolean coGiamGia;
        private BigDecimal giaSauGiam;
        private BigDecimal phanTramGiam;
        private String tenDotGiamGia;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder idSanPham(Long idSanPham) { this.idSanPham = idSanPham; return this; }
        public Builder maSanPham(String maSanPham) { this.maSanPham = maSanPham; return this; }
        public Builder tenSanPham(String tenSanPham) { this.tenSanPham = tenSanPham; return this; }
        public Builder idKichCo(Long idKichCo) { this.idKichCo = idKichCo; return this; }
        public Builder tenKichCo(String tenKichCo) { this.tenKichCo = tenKichCo; return this; }
        public Builder idMauSac(Long idMauSac) { this.idMauSac = idMauSac; return this; }
        public Builder tenMauSac(String tenMauSac) { this.tenMauSac = tenMauSac; return this; }
        public Builder maHex(String maHex) { this.maHex = maHex; return this; }
        public Builder maChiTietSanPham(String maChiTietSanPham) { this.maChiTietSanPham = maChiTietSanPham; return this; }
        public Builder soLuong(Integer soLuong) { this.soLuong = soLuong; return this; }
        public Builder giaBan(BigDecimal giaBan) { this.giaBan = giaBan; return this; }
        public Builder trangThai(Integer trangThai) { this.trangThai = trangThai; return this; }
        public Builder ngayTao(Instant ngayTao) { this.ngayTao = ngayTao; return this; }
        public Builder anhDaiDien(String anhDaiDien) { this.anhDaiDien = anhDaiDien; return this; }
        public Builder coGiamGia(Boolean coGiamGia) { this.coGiamGia = coGiamGia; return this; }
        public Builder giaSauGiam(BigDecimal giaSauGiam) { this.giaSauGiam = giaSauGiam; return this; }
        public Builder phanTramGiam(BigDecimal phanTramGiam) { this.phanTramGiam = phanTramGiam; return this; }
        public Builder tenDotGiamGia(String tenDotGiamGia) { this.tenDotGiamGia = tenDotGiamGia; return this; }

        public ChiTietSanPhamResponse build() {
            return new ChiTietSanPhamResponse(id, idSanPham, maSanPham, tenSanPham, idKichCo, tenKichCo, idMauSac, tenMauSac, maHex, maChiTietSanPham, soLuong, giaBan, trangThai, ngayTao, anhDaiDien, coGiamGia, giaSauGiam, phanTramGiam, tenDotGiamGia);
        }
    }
}
