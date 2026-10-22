package com.example.sd009thuan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Entity
@Table(name = "cuoc_hoi_thoai")
public class CuocHoiThoai {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang idKhachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien idNhanVien;

    @Column(name = "che_do", nullable = false, length = 20)
    private String cheDo = "AI";

    @Column(name = "trang_thai", nullable = false, length = 20)
    private String trangThai = "MOI";

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @OneToMany(mappedBy = "idCuocHoiThoai", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ngayGui ASC, id ASC")
    private List<TinNhanChat> tinNhans = new ArrayList<>();
}
