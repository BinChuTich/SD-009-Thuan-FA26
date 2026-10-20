package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@Table(name = "giao_ca")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LichSuCaLamViec {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nhan_vien_giao", nullable = false)
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nhan_vien_nhan")
    private NhanVien nhanVienNhan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_lich_lam_viec")
    private LichLamViec lichLamViec;

    @Size(max = 50)
    @Column(name = "ma_giao_ca", length = 50)
    private String maGiaoCa;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "thoi_gian_nhan_ca", nullable = false)
    private LocalDateTime thoiGianMoCa;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "thoi_gian_ket_ca")
    private LocalDateTime thoiGianDongCa;

    @Column(name = "tien_mat_dau_ca", precision = 18, scale = 2)
    private BigDecimal tienMatDauCa = BigDecimal.ZERO;

    @Column(name = "tien_thuc_te_kiem_duoc", precision = 18, scale = 2)
    private BigDecimal tienMatCuoiCa;

    @Column(name = "tien_chuyen_khoan", precision = 18, scale = 2)
    private BigDecimal tienChuyenKhoan = BigDecimal.ZERO;

    @Column(name = "tien_mat_thu_trong_ca", precision = 18, scale = 2)
    private BigDecimal tongTienTrongCa = BigDecimal.ZERO;

    @Column(name = "tien_chi_trong_ca", precision = 18, scale = 2)
    private BigDecimal tienChiTrongCa = BigDecimal.ZERO;

    @Column(name = "tien_chenh_lech", precision = 18, scale = 2)
    private BigDecimal chenhLech = BigDecimal.ZERO;

    @Size(max = 500)
    @Nationalized
    @Column(name = "ly_do_chenh_lech", length = 500)
    private String lyDoChenhLech;

    @Size(max = 500)
    @Nationalized
    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    @Column(name = "trang_thai")
    private Integer trangThai = 1; // 1: Đang mở (Đang trong ca), 0: Đã đóng (Đã kết ca / giao ca)

    @Transient
    private CaLamViec caLamViec;

    @Transient
    private LocalDateTime ngayTao;

    public LichSuCaLamViec() {
        super();
    }

    @JsonProperty("caLamViec")
    public CaLamViec getCaLamViec() {
        if (caLamViec != null) {
            return caLamViec;
        }
        if (lichLamViec != null && lichLamViec.getCaLamViec() != null) {
            return lichLamViec.getCaLamViec();
        }
        return null;
    }

    @JsonProperty("nhanVienGiao")
    public NhanVien getNhanVienGiao() {
        return nhanVien;
    }

    @JsonProperty("thoiGianNhanCa")
    public LocalDateTime getThoiGianNhanCa() {
        return thoiGianMoCa;
    }

    @JsonProperty("thoiGianKetCa")
    public LocalDateTime getThoiGianKetCa() {
        return thoiGianDongCa;
    }

    @JsonProperty("tienThucTeKiemDuoc")
    public BigDecimal getTienThucTeKiemDuoc() {
        return tienMatCuoiCa;
    }

    @JsonProperty("tienMatThuTrongCa")
    public BigDecimal getTienMatThuTrongCa() {
        return tongTienTrongCa;
    }

    @JsonProperty("tienChenhLech")
    public BigDecimal getTienChenhLech() {
        return chenhLech;
    }
}
