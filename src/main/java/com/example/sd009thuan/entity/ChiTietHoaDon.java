package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

@Entity
@Table(name = "chi_tiet_hoa_don")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChiTietHoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hoa_don", nullable = false)
    private HoaDon idHoaDon;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_chi_tiet_san_pham", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ChiTietSanPham idChiTietSanPham;

    @Size(max = 50)
    @Column(name = "ma_hoa_don_chi_tiet", length = 50)
    private String maHoaDonChiTiet;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "don_gia", nullable = false, precision = 18, scale = 2)
    private BigDecimal donGia;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "thanh_tien", nullable = false, precision = 18, scale = 2)
    private BigDecimal thanhTien;

    @Nationalized
    @Lob
    @Column(name = "ghi_chu")
    private String ghiChu;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai;

    @Transient
    @JsonProperty("anhDaiDien")
    private String anhDaiDien;

    public ChiTietHoaDon() {
    }

    public ChiTietHoaDon(Long id, HoaDon idHoaDon, ChiTietSanPham idChiTietSanPham, String maHoaDonChiTiet, BigDecimal donGia, Integer soLuong, BigDecimal thanhTien, String ghiChu, Integer trangThai, String anhDaiDien) {
        this.id = id;
        this.idHoaDon = idHoaDon;
        this.idChiTietSanPham = idChiTietSanPham;
        this.maHoaDonChiTiet = maHoaDonChiTiet;
        this.donGia = donGia;
        this.soLuong = soLuong;
        this.thanhTien = thanhTien;
        this.ghiChu = ghiChu;
        this.trangThai = trangThai;
        this.anhDaiDien = anhDaiDien;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public HoaDon getIdHoaDon() {
        return idHoaDon;
    }

    public void setIdHoaDon(HoaDon idHoaDon) {
        this.idHoaDon = idHoaDon;
    }

    public ChiTietSanPham getIdChiTietSanPham() {
        return idChiTietSanPham;
    }

    public void setIdChiTietSanPham(ChiTietSanPham idChiTietSanPham) {
        this.idChiTietSanPham = idChiTietSanPham;
    }

    public String getMaHoaDonChiTiet() {
        return maHoaDonChiTiet;
    }

    public void setMaHoaDonChiTiet(String maHoaDonChiTiet) {
        this.maHoaDonChiTiet = maHoaDonChiTiet;
    }

    public BigDecimal getDonGia() {
        return donGia;
    }

    public void setDonGia(BigDecimal donGia) {
        this.donGia = donGia;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }

    public BigDecimal getThanhTien() {
        return thanhTien;
    }

    public void setThanhTien(BigDecimal thanhTien) {
        this.thanhTien = thanhTien;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public Integer getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Integer trangThai) {
        this.trangThai = trangThai;
    }

    public String getAnhDaiDien() {
        return anhDaiDien;
    }

    public void setAnhDaiDien(String anhDaiDien) {
        this.anhDaiDien = anhDaiDien;
    }

    @JsonProperty("tenSanPham")
    public String getTenSanPham() {
        if (idChiTietSanPham != null && idChiTietSanPham.getIdSanPham() != null) {
            return idChiTietSanPham.getIdSanPham().getTenSanPham();
        }
        return null;
    }

    @JsonProperty("maSanPham")
    public String getMaSanPham() {
        if (idChiTietSanPham != null && idChiTietSanPham.getIdSanPham() != null) {
            return idChiTietSanPham.getIdSanPham().getMaSanPham();
        }
        return null;
    }

    @JsonProperty("maChiTietSanPham")
    public String getMaChiTietSanPham() {
        return idChiTietSanPham != null ? idChiTietSanPham.getMaChiTietSanPham() : null;
    }

    @JsonProperty("tenKichCo")
    public String getTenKichCo() {
        if (idChiTietSanPham != null && idChiTietSanPham.getIdKichCo() != null) {
            return idChiTietSanPham.getIdKichCo().getTenKichCo();
        }
        return null;
    }

    @JsonProperty("tenMauSac")
    public String getTenMauSac() {
        if (idChiTietSanPham != null && idChiTietSanPham.getIdMauSac() != null) {
            return idChiTietSanPham.getIdMauSac().getTenMauSac();
        }
        return null;
    }
}