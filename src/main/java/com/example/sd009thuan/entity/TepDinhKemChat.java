package com.example.sd009thuan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "tep_dinh_kem_chat")
public class TepDinhKemChat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tin_nhan_chat", nullable = false)
    private TinNhanChat idTinNhanChat;

    @Column(name = "ten_tep", nullable = false, length = 255)
    private String tenTep;

    @Column(name = "duong_dan", nullable = false, length = 1000)
    private String duongDan;

    @Column(name = "loai_tep", length = 100)
    private String loaiTep;
}
