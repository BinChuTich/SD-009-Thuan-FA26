package com.example.sd009thuan.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "dia_chi_khach_hang")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DiaChiKhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang idKhachHang;

    @Size(max = 50)
    @Column(name = "ma_dia_chi", length = 50)
    private String maDiaChi;

    @Size(max = 100)
    @Nationalized
    @Column(name = "thanh_pho", length = 100)
    private String thanhPho;

    @Size(max = 100)
    @Nationalized
    @Column(name = "huyen", length = 100)
    private String huyen;

    @Size(max = 100)
    @Nationalized
    @Column(name = "phuong", length = 100)
    private String phuong;

    @Nationalized
    @Lob
    @Column(name = "dia_chi_cu_the")
    private String diaChiCuThe;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "mac_dinh", nullable = false)
    private Boolean macDinh = false;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai;

    @Size(max = 20)
    @Column(name = "so_dien_thoai_nhan", length = 20)
    private String soDienThoaiNhan;

    @Size(max = 100)
    @Nationalized
    @Column(name = "ten_nguoi_nhan", length = 100)
    private String tenNgươiNhan;

    public DiaChiKhachHang() {
    }

    public DiaChiKhachHang(Long id, KhachHang idKhachHang, String maDiaChi, String thanhPho, String huyen, String phuong, String diaChiCuThe, Boolean macDinh, Integer trangThai, String soDienThoaiNhan, String tenNguoiNhan) {
        this.id = id;
        this.idKhachHang = idKhachHang;
        this.maDiaChi = maDiaChi;
        this.thanhPho = thanhPho;
        this.huyen = huyen;
        this.phuong = phuong;
        this.diaChiCuThe = diaChiCuThe;
        this.macDinh = macDinh;
        this.trangThai = trangThai;
        this.soDienThoaiNhan = soDienThoaiNhan;
        this.tenNgươiNhan = tenNguoiNhan;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public KhachHang getIdKhachHang() {
        return idKhachHang;
    }

    public void setIdKhachHang(KhachHang idKhachHang) {
        this.idKhachHang = idKhachHang;
    }

    public String getMaDiaChi() {
        return maDiaChi;
    }

    public void setMaDiaChi(String maDiaChi) {
        this.maDiaChi = maDiaChi;
    }

    public String getThanhPho() {
        return thanhPho;
    }

    public void setThanhPho(String thanhPho) {
        this.thanhPho = thanhPho;
    }

    public String getHuyen() {
        return huyen;
    }

    public void setHuyen(String huyen) {
        this.huyen = huyen;
    }

    public String getPhuong() {
        return phuong;
    }

    public void setPhuong(String phuong) {
        this.phuong = phuong;
    }

    public String getDiaChiCuThe() {
        return diaChiCuThe;
    }

    public void setDiaChiCuThe(String diaChiCuThe) {
        this.diaChiCuThe = diaChiCuThe;
    }

    public Boolean getMacDinh() {
        return macDinh;
    }

    public void setMacDinh(Boolean macDinh) {
        this.macDinh = macDinh;
    }

    public Integer getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Integer trangThai) {
        this.trangThai = trangThai;
    }

    public String getSoDienThoaiNhan() {
        return soDienThoaiNhan;
    }

    public void setSoDienThoaiNhan(String soDienThoaiNhan) {
        this.soDienThoaiNhan = soDienThoaiNhan;
    }

    public String getTenNguoiNhan() {
        return tenNgươiNhan;
    }

    public void setTenNguoiNhan(String tenNguoiNhan) {
        this.tenNgươiNhan = tenNguoiNhan;
    }
}