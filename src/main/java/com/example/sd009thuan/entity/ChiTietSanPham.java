package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "chi_tiet_san_pham")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChiTietSanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_san_pham", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private SanPham idSanPham;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_kich_co", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private KichCo idKichCo;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_mau_sac", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private MauSac idMauSac;

    @Size(max = 50)
    @Column(name = "ma_chi_tiet_san_pham", length = 50, unique = true)
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

    @JsonProperty("tenSanPham")
    public String getTenSanPham() {
        return idSanPham != null ? idSanPham.getTenSanPham() : null;
    }

    @JsonProperty("maSanPham")
    public String getMaSanPham() {
        return idSanPham != null ? idSanPham.getMaSanPham() : null;
    }

    @JsonProperty("tenKichCo")
    public String getTenKichCo() {
        return idKichCo != null ? idKichCo.getTenKichCo() : null;
    }

    @JsonProperty("tenMauSac")
    public String getTenMauSac() {
        return idMauSac != null ? idMauSac.getTenMauSac() : null;
    }
}