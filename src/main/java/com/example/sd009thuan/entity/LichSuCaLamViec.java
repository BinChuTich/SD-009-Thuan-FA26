package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "lich_su_ca_lam_viec")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LichSuCaLamViec {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nhan_vien", nullable = false)
    private NhanVien nhanVien;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_ca_lam_viec", nullable = false)
    private CaLamViec caLamViec;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "thoi_gian_mo_ca", nullable = false)
    private LocalDateTime thoiGianMoCa;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "thoi_gian_dong_ca")
    private LocalDateTime thoiGianDongCa;

    @Column(name = "tien_mat_dau_ca", precision = 18, scale = 2)
    private BigDecimal tienMatDauCa = BigDecimal.ZERO;

    @Column(name = "tien_mat_cuoi_ca", precision = 18, scale = 2)
    private BigDecimal tienMatCuoiCa;

    @Column(name = "tien_chuyen_khoan", precision = 18, scale = 2)
    private BigDecimal tienChuyenKhoan = BigDecimal.ZERO;

    @Column(name = "tong_tien_trong_ca", precision = 18, scale = 2)
    private BigDecimal tongTienTrongCa = BigDecimal.ZERO;

    @Column(name = "chenh_lech", precision = 18, scale = 2)
    private BigDecimal chenhLech = BigDecimal.ZERO;

    @Size(max = 500)
    @Nationalized
    @Column(name = "ly_do_chenh_lech", length = 500)
    private String lyDoChenhLech;

    @Column(name = "trang_thai")
    private Integer trangThai = 1; // 1: Đang mở, 2: Đã đóng

    @Size(max = 500)
    @Nationalized
    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    public LichSuCaLamViec() {
        super();
    }
}

