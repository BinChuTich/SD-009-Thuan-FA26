package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "kich_co")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class KichCo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @Column(name = "ma_kich_co", length = 50, unique = true)
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
}

