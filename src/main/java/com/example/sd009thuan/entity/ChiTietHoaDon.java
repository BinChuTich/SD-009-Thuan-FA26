package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "chi_tiet_hoa_don")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChiTietHoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // THÊM @JsonIgnore ĐỂ CHẶN JACKSON ĐỌC PROXY HÓA ĐƠN
    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hoa_don", nullable = false)
    private HoaDon idHoaDon;

    // Bổ sung @JsonIgnoreProperties cho biến thể sản phẩm
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

    @com.fasterxml.jackson.annotation.JsonProperty("tenSanPham")
    public String getTenSanPham() {
        return idChiTietSanPham != null ? idChiTietSanPham.getTenSanPham() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("maSanPham")
    public String getMaSanPham() {
        return idChiTietSanPham != null ? idChiTietSanPham.getMaSanPham() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("maChiTietSanPham")
    public String getMaChiTietSanPham() {
        return idChiTietSanPham != null ? idChiTietSanPham.getMaChiTietSanPham() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("tenKichCo")
    public String getTenKichCo() {
        return idChiTietSanPham != null ? idChiTietSanPham.getTenKichCo() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("tenMauSac")
    public String getTenMauSac() {
        return idChiTietSanPham != null ? idChiTietSanPham.getTenMauSac() : null;
    }
}