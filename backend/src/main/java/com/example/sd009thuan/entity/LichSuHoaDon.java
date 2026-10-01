package com.example.sd009thuan.entity;

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
public class LichSuHoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hoa_don", nullable = false)
    private HoaDon idHoaDon;

    @Size(max = 255)
    @Nationalized
    @Column(name = "hanh_dong")
    private String hanhDong;

    @NotNull
    @ColumnDefault("getdate()")
    @Column(name = "thoi_gian", nullable = false)
    private Instant thoiGian;

    @Size(max = 100)
    @Nationalized
    @Column(name = "nguoi_thuc_hien", length = 100)
    private String nguoiThucHien;

    @Size(max = 100)
    @Nationalized
    @Column(name = "vai_tro_nguoi_thuc_hien", length = 100)
    private String vaiTroNguoiThucHien;

}