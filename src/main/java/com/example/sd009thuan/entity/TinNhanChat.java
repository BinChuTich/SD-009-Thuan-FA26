package com.example.sd009thuan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Entity
@Table(name = "tin_nhan_chat")
public class TinNhanChat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cuoc_hoi_thoai", nullable = false)
    private CuocHoiThoai idCuocHoiThoai;

    @Column(name = "nguoi_gui", nullable = false, length = 20)
    private String nguoiGui;

    @Column(name = "noi_dung", nullable = false, columnDefinition = "nvarchar(max)")
    private String noiDung;

    @Column(name = "ngay_gui", nullable = false)
    private LocalDateTime ngayGui = LocalDateTime.now();

    @OneToMany(mappedBy = "idTinNhanChat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TepDinhKemChat> tepDinhKems = new ArrayList<>();
}
