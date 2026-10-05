package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "hoa_don")
public class HoaDon {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien idNhanVien;

    @Size(max = 50)
    @Column(name = "ma_hoa_don", length = 50)
    private String maHoaDon;

    @NotNull
    @Column(name = "loai_don", nullable = false)
    private Integer loaiDon;

    @NotNull
    @ColumnDefault("0")
    @Column(
            name = "phi_ship",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal phiShip;

    @NotNull
    @ColumnDefault("0")
    @Column(
            name = "tong_tien",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal tongTien;

    @NotNull
    @ColumnDefault("0")
    @Column(
            name = "tong_tien_giam_gia",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal tongTienGiamGia;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ten_khach_hang")
    private String tenKhachHang;

    @Size(max = 20)
    @Column(
            name = "so_dien_thoai_khach_hang",
            length = 20
    )
    private String soDienThoaiKhachHang;

    @Nationalized
    @Lob
    @Column(name = "dia_chi_nhan_hang")
    private String diaChiNhanHang;

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


    @NotNull
    @ColumnDefault("1")
    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai;

    @Nationalized
    @Lob
    @Column(name = "ghi_chu")
    private String ghiChu;

}