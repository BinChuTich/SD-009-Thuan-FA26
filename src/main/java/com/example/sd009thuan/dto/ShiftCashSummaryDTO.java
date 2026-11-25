package com.example.sd009thuan.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    // Thông tin ca tiếp theo và người nhận bàn giao tự động
    private Long nhanVienNhanDuKienId;
    private String tenNhanVienNhanDuKien;
    private String maNhanVienNhanDuKien;
    private String tenCaTiepTheo;
    private String thongTinBaoGiao;
    private List<ShiftInvoiceDTO> danhSachHoaDon = new ArrayList<>();

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

    public void setSoHoaDon(long soHoaDon) {
        this.soHoaDon = soHoaDon;
    }

    public BigDecimal getTienChiTrongCa() {
        return tienChiTrongCa;
    }

    public void setTienChiTrongCa(BigDecimal tienChiTrongCa) {
        this.tienChiTrongCa = tienChiTrongCa != null ? tienChiTrongCa : BigDecimal.ZERO;
    }

    public List<ShiftInvoiceDTO> getDanhSachHoaDon() {
        return danhSachHoaDon;
    }

    public void setDanhSachHoaDon(List<ShiftInvoiceDTO> danhSachHoaDon) {
        this.danhSachHoaDon = danhSachHoaDon != null ? danhSachHoaDon : new ArrayList<>();
    }

    public Long getNhanVienNhanDuKienId() {
        return nhanVienNhanDuKienId;
    }

    public void setNhanVienNhanDuKienId(Long nhanVienNhanDuKienId) {
        this.nhanVienNhanDuKienId = nhanVienNhanDuKienId;
    }

    public String getTenNhanVienNhanDuKien() {
        return tenNhanVienNhanDuKien;
    }

    public void setTenNhanVienNhanDuKien(String tenNhanVienNhanDuKien) {
        this.tenNhanVienNhanDuKien = tenNhanVienNhanDuKien;
    }

    public String getMaNhanVienNhanDuKien() {
        return maNhanVienNhanDuKien;
    }

    public void setMaNhanVienNhanDuKien(String maNhanVienNhanDuKien) {
        this.maNhanVienNhanDuKien = maNhanVienNhanDuKien;
    }

    public String getTenCaTiepTheo() {
        return tenCaTiepTheo;
    }

    public void setTenCaTiepTheo(String tenCaTiepTheo) {
        this.tenCaTiepTheo = tenCaTiepTheo;
    }

    public String getThongTinBaoGiao() {
        return thongTinBaoGiao;
    }

    public void setThongTinBaoGiao(String thongTinBaoGiao) {
        this.thongTinBaoGiao = thongTinBaoGiao;
    }

    public static class ShiftInvoiceDTO {
        private Long id;
        private String maHoaDon;
        private String tenKhachHang;
        private String soDienThoaiKhachHang;
        private BigDecimal tongTien;
        private String hinhThucThanhToan;
        private Instant ngayTao;
        private Integer loaiDon;
        private Integer trangThai;
        private Integer trangThaiThanhToan;

        public ShiftInvoiceDTO() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getMaHoaDon() { return maHoaDon; }
        public void setMaHoaDon(String maHoaDon) { this.maHoaDon = maHoaDon; }
        public String getTenKhachHang() { return tenKhachHang; }
        public void setTenKhachHang(String tenKhachHang) { this.tenKhachHang = tenKhachHang; }
        public String getSoDienThoaiKhachHang() { return soDienThoaiKhachHang; }
        public void setSoDienThoaiKhachHang(String soDienThoaiKhachHang) { this.soDienThoaiKhachHang = soDienThoaiKhachHang; }
        public BigDecimal getTongTien() { return tongTien; }
        public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
        public String getHinhThucThanhToan() { return hinhThucThanhToan; }
        public void setHinhThucThanhToan(String hinhThucThanhToan) { this.hinhThucThanhToan = hinhThucThanhToan; }
        public Instant getNgayTao() { return ngayTao; }
        public void setNgayTao(Instant ngayTao) { this.ngayTao = ngayTao; }
        public Integer getLoaiDon() { return loaiDon; }
        public void setLoaiDon(Integer loaiDon) { this.loaiDon = loaiDon; }
        public Integer getTrangThai() { return trangThai; }
        public void setTrangThai(Integer trangThai) { this.trangThai = trangThai; }
        public Integer getTrangThaiThanhToan() { return trangThaiThanhToan; }
        public void setTrangThaiThanhToan(Integer trangThaiThanhToan) { this.trangThaiThanhToan = trangThaiThanhToan; }
    }
}
