package com.example.sd009thuan.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "kich_co")
public class KichCo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @Column(name = "ma_kich_co", length = 50)
    private String maKichCo;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "ten_kich_co", nullable = false)
    private String tenKichCo;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai;

    @OneToMany(mappedBy = "idKichCo")
    private Set<ChiTietSanPham> chiTietSanPhams = new LinkedHashSet<>();

}