package com.example.sd009thuan.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@Entity
@Table(name = "dia_chi_khach_hang")
public class DiaChiKhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang idKhachHang;

    @Size(max = 50)
    @Column(name = "ma_dia_chi", length = 50)
    private String maDiaChi;

    @Size(max = 100)
    @Nationalized
    @Column(name = "thanh_pho", length = 100)
    private String thanhPho;

    @Size(max = 100)
    @Nationalized
    @Column(name = "huyen", length = 100)
    private String huyen;

    @Size(max = 100)
    @Nationalized
    @Column(name = "phuong", length = 100)
    private String phuong;

    @Nationalized
    @Lob
    @Column(name = "dia_chi_cu_the")
    private String diaChiCuThe;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "mac_dinh", nullable = false)
    private Boolean macDinh = false;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai;

    @Size(max = 20)
    @Column(name = "so_dien_thoai_nhan", length = 20)
    private String soDienThoaiNhan;
    @Size(max = 100)
    @Nationalized
    @Column(name = "ten_nguoi_nhan", length = 100)
    private String tenNguoiNhan;


}