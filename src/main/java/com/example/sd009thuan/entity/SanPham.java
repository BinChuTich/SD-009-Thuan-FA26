package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Entity
@Table(name = "san_pham")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chat_lieu")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ChatLieu idChatLieu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_co_ao")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CoAo idCoAo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tay_ao")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private TayAo idTayAo;

    @Size(max = 50)
    @Column(name = "ma_san_pham", length = 50)
    private String maSanPham;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "ten_san_pham", nullable = false)
    private String tenSanPham;

    @NotNull
    @ColumnDefault("getdate()")
    @Column(name = "ngay_tao", nullable = false)
    private Instant ngayTao;

    @Column(name = "ngay_cap_nhat")
    private Instant ngayCapNhat;

    @Size(max = 100)
    @Nationalized
    @Column(name = "nguoi_tao", length = 100)
    private String nguoiTao;

    @Size(max = 100)
    @Nationalized
    @Column(name = "nguoi_cap_nhat", length = 100)
    private String nguoiCapNhat;

    @Nationalized
    @Lob
    @Column(name = "mo_ta")
    private String moTa;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_xuat_xu")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private XuatXu idXuatXu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_thuong_hieu")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ThuongHieu idThuongHieu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_danh_muc")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DanhMuc idDanhMuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoa_tiet")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private HoaTiet idHoaTiet;

    public SanPham() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatLieu getIdChatLieu() {
        return idChatLieu;
    }

    public void setIdChatLieu(ChatLieu idChatLieu) {
        this.idChatLieu = idChatLieu;
    }

    public CoAo getIdCoAo() {
        return idCoAo;
    }

    public void setIdCoAo(CoAo idCoAo) {
        this.idCoAo = idCoAo;
    }

    public TayAo getIdTayAo() {
        return idTayAo;
    }

    public void setIdTayAo(TayAo idTayAo) {
        this.idTayAo = idTayAo;
    }

    public String getMaSanPham() {
        return maSanPham;
    }

    public void setMaSanPham(String maSanPham) {
        this.maSanPham = maSanPham;
    }

    public String getTenSanPham() {
        return tenSanPham;
    }

    public void setTenSanPham(String tenSanPham) {
        this.tenSanPham = tenSanPham;
    }

    public Instant getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(Instant ngayTao) {
        this.ngayTao = ngayTao;
    }

    public Instant getNgayCapNhat() {
        return ngayCapNhat;
    }

    public void setNgayCapNhat(Instant ngayCapNhat) {
        this.ngayCapNhat = ngayCapNhat;
    }

    public String getNguoiTao() {
        return nguoiTao;
    }

    public void setNguoiTao(String nguoiTao) {
        this.nguoiTao = nguoiTao;
    }

    public String getNguoiCapNhat() {
        return nguoiCapNhat;
    }

    public void setNguoiCapNhat(String nguoiCapNhat) {
        this.nguoiCapNhat = nguoiCapNhat;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public Integer getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Integer trangThai) {
        this.trangThai = trangThai;
    }

    public XuatXu getIdXuatXu() {
        return idXuatXu;
    }

    public void setIdXuatXu(XuatXu idXuatXu) {
        this.idXuatXu = idXuatXu;
    }

    public ThuongHieu getIdThuongHieu() {
        return idThuongHieu;
    }

    public void setIdThuongHieu(ThuongHieu idThuongHieu) {
        this.idThuongHieu = idThuongHieu;
    }

    public DanhMuc getIdDanhMuc() {
        return idDanhMuc;
    }

    public void setIdDanhMuc(DanhMuc idDanhMuc) {
        this.idDanhMuc = idDanhMuc;
    }

    public HoaTiet getIdHoaTiet() {
        return idHoaTiet;
    }

    public void setIdHoaTiet(HoaTiet idHoaTiet) {
        this.idHoaTiet = idHoaTiet;
    }

    @PrePersist
    public void prePersist() {
        if (this.ngayTao == null) {
            this.ngayTao = Instant.now();
        }
        if (this.trangThai == null) {
            this.trangThai = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.ngayCapNhat = Instant.now();
    }
}