package com.example.sd009thuan.dto;

import java.math.BigDecimal;

public class ShiftCashSummaryDTO {
    private Long id;
    private Long nhanVienId;
    private String tenNhanVien;
    private Long caLamViecId;
    private String tenCa;
    private BigDecimal tienMatDauCa = BigDecimal.ZERO;
    private BigDecimal tienMatHeThong = BigDecimal.ZERO;
    private BigDecimal tienChuyenKhoanHeThong = BigDecimal.ZERO;
    private BigDecimal tongTienHeThong = BigDecimal.ZERO;
    private BigDecimal tienChiTrongCa = BigDecimal.ZERO;
    private BigDecimal tienMatDuKien = BigDecimal.ZERO;
    private long soHoaDon = 0;

    public ShiftCashSummaryDTO() {
        super();
    }

    public ShiftCashSummaryDTO(Long id, Long nhanVienId, String tenNhanVien, Long caLamViecId, String tenCa,
                               BigDecimal tienMatDauCa, BigDecimal tienMatHeThong, BigDecimal tienChuyenKhoanHeThong,
                               BigDecimal tongTienHeThong, BigDecimal tienMatDuKien, long soHoaDon) {
        this.id = id;
        this.nhanVienId = nhanVienId;
        this.tenNhanVien = tenNhanVien;
        this.caLamViecId = caLamViecId;
        this.tenCa = tenCa;
        this.tienMatDauCa = tienMatDauCa != null ? tienMatDauCa : BigDecimal.ZERO;
        this.tienMatHeThong = tienMatHeThong != null ? tienMatHeThong : BigDecimal.ZERO;
        this.tienChuyenKhoanHeThong = tienChuyenKhoanHeThong != null ? tienChuyenKhoanHeThong : BigDecimal.ZERO;
        this.tongTienHeThong = tongTienHeThong != null ? tongTienHeThong : BigDecimal.ZERO;
        this.tienMatDuKien = tienMatDuKien != null ? tienMatDuKien : BigDecimal.ZERO;
        this.soHoaDon = soHoaDon;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNhanVienId() {
        return nhanVienId;
    }

    public void setNhanVienId(Long nhanVienId) {
        this.nhanVienId = nhanVienId;
    }

    public String getTenNhanVien() {
        return tenNhanVien;
    }

    public void setTenNhanVien(String tenNhanVien) {
        this.tenNhanVien = tenNhanVien;
    }

    public Long getCaLamViecId() {
        return caLamViecId;
    }

    public void setCaLamViecId(Long caLamViecId) {
        this.caLamViecId = caLamViecId;
    }

    public String getTenCa() {
        return tenCa;
    }

    public void setTenCa(String tenCa) {
        this.tenCa = tenCa;
    }

    public BigDecimal getTienMatDauCa() {
        return tienMatDauCa;
    }

    public void setTienMatDauCa(BigDecimal tienMatDauCa) {
        this.tienMatDauCa = tienMatDauCa;
    }

    public BigDecimal getTienMatHeThong() {
        return tienMatHeThong;
    }

    public void setTienMatHeThong(BigDecimal tienMatHeThong) {
        this.tienMatHeThong = tienMatHeThong;
    }

    public BigDecimal getTienChuyenKhoanHeThong() {
        return tienChuyenKhoanHeThong;
    }

    public void setTienChuyenKhoanHeThong(BigDecimal tienChuyenKhoanHeThong) {
        this.tienChuyenKhoanHeThong = tienChuyenKhoanHeThong;
    }

    public BigDecimal getTongTienHeThong() {
        return tongTienHeThong;
    }

    public void setTongTienHeThong(BigDecimal tongTienHeThong) {
        this.tongTienHeThong = tongTienHeThong;
    }

    public BigDecimal getTienMatDuKien() {
        return tienMatDuKien;
    }

    public void setTienMatDuKien(BigDecimal tienMatDuKien) {
        this.tienMatDuKien = tienMatDuKien;
    }

    public long getSoHoaDon() {
        return soHoaDon;
    }

    public BigDecimal getTienChiTrongCa() {
        return tienChiTrongCa;
    }

    public void setTienChiTrongCa(BigDecimal tienChiTrongCa) {
        this.tienChiTrongCa = tienChiTrongCa != null ? tienChiTrongCa : BigDecimal.ZERO;
    }

    public void setSoHoaDon(long soHoaDon) {
        this.soHoaDon = soHoaDon;
    }
}
