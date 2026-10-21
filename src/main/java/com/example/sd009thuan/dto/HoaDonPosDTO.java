package com.example.sd009thuan.dto;

import java.math.BigDecimal;
import java.util.List;

public class HoaDonPosDTO {
    private String maHoaDon;
    private Long idKhachHang;
    private Long idNhanVien;
    private Long idGiaoCa;
    private Long idPhieuGiamGia;
    private Integer loaiDon; // 1: Tại quầy, 2: Giao hàng
    private String tenKhachHang;
    private String soDienThoaiKhachHang;
    private String diaChiNhanHang;
    private BigDecimal tongTien;
    private BigDecimal tongTienGiamGia;
    private BigDecimal phiShip;
    private Integer trangThai; // 5: Hoàn thành, 1: Chờ xử lý
    private Integer trangThaiThanhToan; // 1: Đã thanh toán
    private String phuongThucThanhToan;
    private BigDecimal tienMat;
    private BigDecimal tienChuyenKhoan;
    private String ghiChu;
    private List<ItemChiTietDTO> chiTietList;

    public HoaDonPosDTO() {
    }

    public String getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(String maHoaDon) { this.maHoaDon = maHoaDon; }

    public Long getIdKhachHang() { return idKhachHang; }
    public void setIdKhachHang(Long idKhachHang) { this.idKhachHang = idKhachHang; }

    public Long getIdGiaoCa() { return idGiaoCa; }
    public void setIdGiaoCa(Long idGiaoCa) { this.idGiaoCa = idGiaoCa; }

    public Long getIdNhanVien() { return idNhanVien; }
    public void setIdNhanVien(Long idNhanVien) { this.idNhanVien = idNhanVien; }

    public Long getIdPhieuGiamGia() { return idPhieuGiamGia; }
    public void setIdPhieuGiamGia(Long idPhieuGiamGia) { this.idPhieuGiamGia = idPhieuGiamGia; }

    public Integer getLoaiDon() { return loaiDon; }
    public void setLoaiDon(Integer loaiDon) { this.loaiDon = loaiDon; }

    public String getTenKhachHang() { return tenKhachHang; }
    public void setTenKhachHang(String tenKhachHang) { this.tenKhachHang = tenKhachHang; }

    public String getSoDienThoaiKhachHang() { return soDienThoaiKhachHang; }
    public void setSoDienThoaiKhachHang(String soDienThoaiKhachHang) { this.soDienThoaiKhachHang = soDienThoaiKhachHang; }

    public String getDiaChiNhanHang() { return diaChiNhanHang; }
    public void setDiaChiNhanHang(String diaChiNhanHang) { this.diaChiNhanHang = diaChiNhanHang; }

    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }

    public BigDecimal getTongTienGiamGia() { return tongTienGiamGia; }
    public void setTongTienGiamGia(BigDecimal tongTienGiamGia) { this.tongTienGiamGia = tongTienGiamGia; }

    public BigDecimal getPhiShip() { return phiShip; }
    public void setPhiShip(BigDecimal phiShip) { this.phiShip = phiShip; }

    public Integer getTrangThai() { return trangThai; }
    public void setTrangThai(Integer trangThai) { this.trangThai = trangThai; }

    public Integer getTrangThaiThanhToan() { return trangThaiThanhToan; }
    public void setTrangThaiThanhToan(Integer trangThaiThanhToan) { this.trangThaiThanhToan = trangThaiThanhToan; }

    public String getPhuongThucThanhToan() { return phuongThucThanhToan; }
    public void setPhuongThucThanhToan(String phuongThucThanhToan) { this.phuongThucThanhToan = phuongThucThanhToan; }

    public BigDecimal getTienMat() { return tienMat; }
    public void setTienMat(BigDecimal tienMat) { this.tienMat = tienMat; }

    public BigDecimal getTienChuyenKhoan() { return tienChuyenKhoan; }
    public void setTienChuyenKhoan(BigDecimal tienChuyenKhoan) { this.tienChuyenKhoan = tienChuyenKhoan; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public List<ItemChiTietDTO> getChiTietList() { return chiTietList; }
    public void setChiTietList(List<ItemChiTietDTO> chiTietList) { this.chiTietList = chiTietList; }

    public static class ItemChiTietDTO {
        private Long sanPhamId; // id của SanPhamChiTiet
        private Long idChiTietSanPham;
        private Integer soLuong;
        private BigDecimal giaDuocTinh;
        private BigDecimal donGia;

        public ItemChiTietDTO() {
        }

        public Long getSanPhamId() { return sanPhamId; }
        public void setSanPhamId(Long sanPhamId) { this.sanPhamId = sanPhamId; }

        public Long getIdChiTietSanPham() { return idChiTietSanPham; }
        public void setIdChiTietSanPham(Long idChiTietSanPham) { this.idChiTietSanPham = idChiTietSanPham; }

        public Integer getSoLuong() { return soLuong; }
        public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }

        public BigDecimal getGiaDuocTinh() { return giaDuocTinh; }
        public void setGiaDuocTinh(BigDecimal giaDuocTinh) { this.giaDuocTinh = giaDuocTinh; }

        public BigDecimal getDonGia() { return donGia; }
        public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }

        public Long getResolvedVariantId() {
            return idChiTietSanPham != null ? idChiTietSanPham : sanPhamId;
        }

        public BigDecimal getResolvedPrice() {
            return giaDuocTinh != null ? giaDuocTinh : (donGia != null ? donGia : BigDecimal.ZERO);
        }
    }
}