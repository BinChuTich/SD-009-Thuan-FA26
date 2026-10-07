package com.example.sd009thuan.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import java.math.BigDecimal;
import java.time.Instant;
@Getter
@Setter
@Entity
@Table(name = "hinh_thuc_thanh_toan")
public class HinhThucThanhToan {
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
    @JoinColumn(name = "id_phuong_thuc", nullable = false)
    private PhuongThucThanhToan idPhuongThuc;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "so_tien", nullable = false, precision = 18, scale = 2)
    private BigDecimal soTien;

    @Size(max = 100)
    @Column(name = "ma_giao_dich", length = 100)
    private String maGiaoDich;

    @NotNull
    @ColumnDefault("getdate()")
    @Column(name = "thoi_gian", nullable = false)
    private Instant thoiGian;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai;
}