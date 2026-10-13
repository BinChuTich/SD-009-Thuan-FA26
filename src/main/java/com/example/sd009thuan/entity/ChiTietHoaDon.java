package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Transient
    @JsonProperty("anhDaiDien")
    private String anhDaiDien;

    public String getAnhDaiDien() {
        return anhDaiDien;
    }

    public void setAnhDaiDien(String anhDaiDien) {
        this.anhDaiDien = anhDaiDien;
    }
}