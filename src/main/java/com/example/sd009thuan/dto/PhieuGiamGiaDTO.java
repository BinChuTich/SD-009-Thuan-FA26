package com.example.sd009thuan.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class PhieuGiamGiaDTO {
    private Long id;
    private String maPhieuGiamGia;
    private String tenPhieuGiamGia;
    private Integer loaiPhieuGiamGia;
    private BigDecimal giaTriGiamGia;
    private BigDecimal giamToiDa;
    private BigDecimal hoaDonToiThieu;
    private Integer soLuongSuDung;
    private Instant ngayBatDau;
    private Instant ngayKetThuc;
    private Integer trangThai;


    private String hinhThuc; 
    private Integer soKhachHang; 
    private List<String> danhSachKhachHang; 
    private List<Long> idKhachHangList;

    public PhieuGiamGiaDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMaPhieuGiamGia() {
        return maPhieuGiamGia;
    }

    public void setMaPhieuGiamGia(String maPhieuGiamGia) {
        this.maPhieuGiamGia = maPhieuGiamGia;
    }

    public String getTenPhieuGiamGia() {
        return tenPhieuGiamGia;
    }

    public void setTenPhieuGiamGia(String tenPhieuGiamGia) {
        this.tenPhieuGiamGia = tenPhieuGiamGia;
    }

    public Integer getLoaiPhieuGiamGia() {
        return loaiPhieuGiamGia;
    }

    public void setLoaiPhieuGiamGia(Integer loaiPhieuGiamGia) {
        this.loaiPhieuGiamGia = loaiPhieuGiamGia;
    }

    public BigDecimal getGiaTriGiamGia() {
        return giaTriGiamGia;
    }

    public void setGiaTriGiamGia(BigDecimal giaTriGiamGia) {
        this.giaTriGiamGia = giaTriGiamGia;
    }

    public BigDecimal getGiamToiDa() {
        return giamToiDa;
    }

    public void setGiamToiDa(BigDecimal giamToiDa) {
        this.giamToiDa = giamToiDa;
    }

    public BigDecimal getHoaDonToiThieu() {
        return hoaDonToiThieu;
    }

    public void setHoaDonToiThieu(BigDecimal hoaDonToiThieu) {
        this.hoaDonToiThieu = hoaDonToiThieu;
    }

    public Integer getSoLuongSuDung() {
        return soLuongSuDung;
    }

    public void setSoLuongSuDung(Integer soLuongSuDung) {
        this.soLuongSuDung = soLuongSuDung;
    }

    public Instant getNgayBatDau() {
        return ngayBatDau;
    }

    public void setNgayBatDau(Instant ngayBatDau) {
        this.ngayBatDau = ngayBatDau;
    }

    public Instant getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(Instant ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }

    public Integer getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Integer trangThai) {
        this.trangThai = trangThai;
    }

    public String getHinhThuc() {
        return hinhThuc;
    }

    public void setHinhThuc(String hinhThuc) {
        this.hinhThuc = hinhThuc;
    }

    public Integer getSoKhachHang() {
        return soKhachHang;
    }

    public void setSoKhachHang(Integer soKhachHang) {
        this.soKhachHang = soKhachHang;
    }

    public List<String> getDanhSachKhachHang() {
        return danhSachKhachHang;
    }

    public void setDanhSachKhachHang(List<String> danhSachKhachHang) {
        this.danhSachKhachHang = danhSachKhachHang;
    }

    public List<Long> getIdKhachHangList() {
        return idKhachHangList;
    }

    public void setIdKhachHangList(List<Long> idKhachHangList) {
        this.idKhachHangList = idKhachHangList;
    }
}
