package com.example.sd009thuan.entity;

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
public class ChiTietHoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hoa_don", nullable = false)
    private HoaDon idHoaDon;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_chi_tiet_san_pham", nullable = false)
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


}