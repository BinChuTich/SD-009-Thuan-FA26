package com.example.sd009thuan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "phieu_giam_gia")
public class PhieuGiamGia {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @Column(name = "ma_phieu_giam_gia", length = 50)
    private String maPhieuGiamGia;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "ten_phieu_giam_gia", nullable = false)
    private String tenPhieuGiamGia;

    @NotNull
    @Column(name = "loai_phieu_giam_gia", nullable = false)
    private Integer loaiPhieuGiamGia;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "gia_tri_giam_gia", nullable = false, precision = 18, scale = 2)
    private BigDecimal giaTriGiamGia;

    @Column(name = "giam_toi_da", precision = 18, scale = 2)
    private BigDecimal giamToiDa;

    @ColumnDefault("0")
    @Column(name = "hoa_don_toi_thieu", precision = 18, scale = 2)
    private BigDecimal hoaDonToiThieu;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "so_luong_su_dung", nullable = false)
    private Integer soLuongSuDung;

    @Column(name = "ngay_bat_dau")
    private Instant ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private Instant ngayKetThuc;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai;

}