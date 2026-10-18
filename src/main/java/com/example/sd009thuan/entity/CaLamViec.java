package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "ca_lam_viec")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CaLamViec {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @Column(name = "ma_ca", length = 50, nullable = false, unique = true)
    private String maCa;

    @Size(max = 100)
    @NotNull
    @Nationalized
    @Column(name = "ten_ca", nullable = false, length = 100)
    private String tenCa;

    @NotNull
    @JsonFormat(pattern = "HH:mm:ss")
    @Column(name = "gio_bat_dau", nullable = false)
    private LocalTime gioBatDau;

    @NotNull
    @JsonFormat(pattern = "HH:mm:ss")
    @Column(name = "gio_ket_thuc", nullable = false)
    private LocalTime gioKetThuc;

    @Size(max = 255)
    @Nationalized
    @Column(name = "mo_ta", length = 255)
    private String moTa;

    @Column(name = "trang_thai")
    private Integer trangThai = 1; // 1: Đang hoạt động, 0: Ngừng hoạt động

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    public CaLamViec() {
        super();
    }
}
