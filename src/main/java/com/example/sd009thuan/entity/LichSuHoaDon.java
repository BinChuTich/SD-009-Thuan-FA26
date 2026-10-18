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

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "lich_su_hoa_don")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LichSuHoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hoa_don", nullable = false)
    private HoaDon idHoaDon;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @NotNull
    @ColumnDefault("getdate()")
    @Column(name = "thoi_gian", nullable = false)
    private Instant thoiGian;

    @Nationalized
    @Lob
    @Column(name = "ghi_chu")
    private String ghiChu;

    @Size(max = 100)
    @Nationalized
    @Column(name = "nguoi_thuc_hien", length = 100)
    private String nguoiThucHien;

    @Size(max = 100)
    @Nationalized
    @Column(name = "vai_tro_nguoi_thuc_hien", length = 100)
    private String vaiTroNguoiThucHien;

    @Transient
    private String hanhDong;

    public String getHanhDong() {
        if (hanhDong != null && !hanhDong.trim().isEmpty()) {
            return hanhDong;
        }
        return ghiChu != null && !ghiChu.trim().isEmpty() ? ghiChu : "Thao tác đơn hàng";
    }

    public void setHanhDong(String hanhDong) {
        this.hanhDong = hanhDong;
        if (this.ghiChu == null || this.ghiChu.trim().isEmpty()) {
            this.ghiChu = hanhDong;
        }
    }

    public String getGhiChu() {
        if (ghiChu != null && !ghiChu.trim().isEmpty()) {
            return ghiChu;
        }
        return hanhDong != null && !hanhDong.trim().isEmpty() ? hanhDong : "Thao tác đơn hàng";
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
        if (this.hanhDong == null || this.hanhDong.trim().isEmpty()) {
            this.hanhDong = ghiChu;
        }
    }
}