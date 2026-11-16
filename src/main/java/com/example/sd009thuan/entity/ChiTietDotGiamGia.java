package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Entity
@Table(name = "chi_tiet_dot_giam_gia")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChiTietDotGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dot_giam_gia", nullable = false)
    private DotGiamGia idDotGiamGia;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_chi_tiet_san_pham", nullable = false)
    private ChiTietSanPham idChiTietSanPham;

    @Column(name = "phan_tram_giam_bien_the", precision = 5, scale = 2)
    private BigDecimal phanTramGiamBienThe;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai;

    public ChiTietDotGiamGia() {
    }

    public ChiTietDotGiamGia(Long id, DotGiamGia idDotGiamGia, ChiTietSanPham idChiTietSanPham, BigDecimal phanTramGiamBienThe, Integer trangThai) {
        this.id = id;
        this.idDotGiamGia = idDotGiamGia;
        this.idChiTietSanPham = idChiTietSanPham;
        this.phanTramGiamBienThe = phanTramGiamBienThe;
        this.trangThai = trangThai;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DotGiamGia getIdDotGiamGia() {
        return idDotGiamGia;
    }

    public void setIdDotGiamGia(DotGiamGia idDotGiamGia) {
        this.idDotGiamGia = idDotGiamGia;
    }

    public ChiTietSanPham getIdChiTietSanPham() {
        return idChiTietSanPham;
    }

    public void setIdChiTietSanPham(ChiTietSanPham idChiTietSanPham) {
        this.idChiTietSanPham = idChiTietSanPham;
    }

    public BigDecimal getPhanTramGiamBienThe() {
        return phanTramGiamBienThe;
    }

    public void setPhanTramGiamBienThe(BigDecimal phanTramGiamBienThe) {
        this.phanTramGiamBienThe = phanTramGiamBienThe;
    }

    public Integer getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Integer trangThai) {
        this.trangThai = trangThai;
    }
}