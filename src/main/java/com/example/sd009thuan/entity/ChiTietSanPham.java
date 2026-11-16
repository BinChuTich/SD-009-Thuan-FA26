package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "chi_tiet_san_pham")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChiTietSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "idChatLieu", "idCoAo", "idTayAo", "idXuatXu", "idThuongHieu", "idDanhMuc", "idHoaTiet"})
    private SanPham idSanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_kich_co", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private KichCo idKichCo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mau_sac", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private MauSac idMauSac;

    @Size(max = 50)
    @Column(name = "ma_chi_tiet_san_pham", length = 50)
    private String maChiTietSanPham;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "gia_ban", nullable = false, precision = 18, scale = 2)
    private BigDecimal giaBan;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai;

    @NotNull
    @ColumnDefault("getdate()")
    @Column(name = "ngay_tao", nullable = false)
    private Instant ngayTao;

    @Size(max = 100)
    @Nationalized
    @Column(name = "nguoi_tao", length = 100)
    private String nguoiTao;

    @Column(name = "ngay_cap_nhat")
    private Instant ngayCapNhat;

    @Size(max = 100)
    @Nationalized
    @Column(name = "nguoi_cap_nhat", length = 100)
    private String nguoiCapNhat;

    public ChiTietSanPham() {
    }

    public ChiTietSanPham(Long id, SanPham idSanPham, KichCo idKichCo, MauSac idMauSac, String maChiTietSanPham, Integer soLuong, BigDecimal giaBan, Integer trangThai, Instant ngayTao, String nguoiTao, Instant ngayCapNhat, String nguoiCapNhat) {
        this.id = id;
        this.idSanPham = idSanPham;
        this.idKichCo = idKichCo;
        this.idMauSac = idMauSac;
        this.maChiTietSanPham = maChiTietSanPham;
        this.soLuong = soLuong;
        this.giaBan = giaBan;
        this.trangThai = trangThai;
        this.ngayTao = ngayTao;
        this.nguoiTao = nguoiTao;
        this.ngayCapNhat = ngayCapNhat;
        this.nguoiCapNhat = nguoiCapNhat;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SanPham getIdSanPham() {
        return idSanPham;
    }

    public void setIdSanPham(SanPham idSanPham) {
        this.idSanPham = idSanPham;
    }

    public KichCo getIdKichCo() {
        return idKichCo;
    }

    public void setIdKichCo(KichCo idKichCo) {
        this.idKichCo = idKichCo;
    }

    public MauSac getIdMauSac() {
        return idMauSac;
    }

    public void setIdMauSac(MauSac idMauSac) {
        this.idMauSac = idMauSac;
    }

    public String getMaChiTietSanPham() {
        return maChiTietSanPham;
    }

    public void setMaChiTietSanPham(String maChiTietSanPham) {
        this.maChiTietSanPham = maChiTietSanPham;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }

    public BigDecimal getGiaBan() {
        return giaBan;
    }

    public void setGiaBan(BigDecimal giaBan) {
        this.giaBan = giaBan;
    }

    public Integer getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Integer trangThai) {
        this.trangThai = trangThai;
    }

    public Instant getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(Instant ngayTao) {
        this.ngayTao = ngayTao;
    }

    public String getNguoiTao() {
        return nguoiTao;
    }

    public void setNguoiTao(String nguoiTao) {
        this.nguoiTao = nguoiTao;
    }

    public Instant getNgayCapNhat() {
        return ngayCapNhat;
    }

    public void setNgayCapNhat(Instant ngayCapNhat) {
        this.ngayCapNhat = ngayCapNhat;
    }

    public String getNguoiCapNhat() {
        return nguoiCapNhat;
    }

    public void setNguoiCapNhat(String nguoiCapNhat) {
        this.nguoiCapNhat = nguoiCapNhat;
    }

    @PrePersist
    public void prePersist() {
        if (this.ngayTao == null) {
            this.ngayTao = Instant.now();
        }
        if (this.trangThai == null) {
            this.trangThai = 1;
        }
        if (this.soLuong == null) {
            this.soLuong = 0;
        }
        if (this.giaBan == null) {
            this.giaBan = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.ngayCapNhat = Instant.now();
    }
}