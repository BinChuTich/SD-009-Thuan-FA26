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

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "ca_lam")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CaLamViec {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @Column(name = "ma_ca", length = 50, unique = true)
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

    @Nationalized
    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "trang_thai")
    private Integer trangThai = 1; // 1: Đang hoạt động, 0: Ngừng hoạt động

    @Transient
    private LocalDateTime ngayTao;

    @Transient
    private LocalDateTime ngayCapNhat;

    public CaLamViec() {
        super();
    }

    @JsonProperty("startTime")
    public String getStartTime() {
        return gioBatDau != null ? gioBatDau.toString() : "00:00:00";
    }

    @JsonProperty("endTime")
    public String getEndTime() {
        return gioKetThuc != null ? gioKetThuc.toString() : "00:00:00";
    }
}

