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
@Table(name = "xuat_xu")
public class XuatXu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @Column(name = "ma_xuat_xu", length = 50)
    private String maXuatXu;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "ten_xuat_xu", nullable = false)
    private String tenXuatXu;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "idXuatXu")
    private Set<SanPham> sanPhams = new LinkedHashSet<>();

}