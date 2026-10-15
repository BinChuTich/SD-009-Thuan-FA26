package com.example.sd009thuan.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class SanPhamResponse {
    private Long id;
    private String maSanPham;
    private String tenSanPham;
    private String moTa;
    private Integer trangThai;
    private Instant ngayTao;
    private Instant ngayCapNhat;
    private String nguoiTao;

    // Thuộc tính mở rộng
    private Long idXuatXu;
    private String tenXuatXu;

    private Long idChatLieu;
    private String tenChatLieu;

    private Long idThuongHieu;
    private String tenThuongHieu;

    private Long idDanhMuc;
    private String tenDanhMuc;

    private Long idCoAo;
    private String tenCoAo;

    private Long idTayAo;
    private String tenTayAo;

    private Long idHoaTiet;
    private String tenHoaTiet;

    // Tổng hợp từ chi tiết biến thể
    private BigDecimal minGia;
    private BigDecimal maxGia;
    private Integer tongSoLuong;
    private Integer soLuongBienThe;
    private Integer soMau;
    private Integer soKichCo;
    private String anhDaiDien;
    private List<String> hinhAnhs;

    // Thông tin đợt giảm giá liên kết
    private Boolean coGiamGia;
    private BigDecimal minGiaSauGiam;
    private BigDecimal maxGiaSauGiam;
    private BigDecimal phanTramGiam;
    private String tenDotGiamGia;

    public SanPhamResponse() {
    }

    public SanPhamResponse(Long id, String maSanPham, String tenSanPham, String moTa, Integer trangThai, Instant ngayTao, Instant ngayCapNhat, String nguoiTao, Long idXuatXu, String tenXuatXu, Long idChatLieu, String tenChatLieu, Long idThuongHieu, String tenThuongHieu, Long idDanhMuc, String tenDanhMuc, Long idCoAo, String tenCoAo, Long idTayAo, String tenTayAo, Long idHoaTiet, String tenHoaTiet, BigDecimal minGia, BigDecimal maxGia, Integer tongSoLuong, Integer soLuongBienThe, Integer soMau, Integer soKichCo, String anhDaiDien, List<String> hinhAnhs, Boolean coGiamGia, BigDecimal minGiaSauGiam, BigDecimal maxGiaSauGiam, BigDecimal phanTramGiam, String tenDotGiamGia) {
        this.id = id;
        this.maSanPham = maSanPham;
        this.tenSanPham = tenSanPham;
        this.moTa = moTa;
        this.trangThai = trangThai;
        this.ngayTao = ngayTao;
        this.ngayCapNhat = ngayCapNhat;
        this.nguoiTao = nguoiTao;
        this.idXuatXu = idXuatXu;
        this.tenXuatXu = tenXuatXu;
        this.idChatLieu = idChatLieu;
        this.tenChatLieu = tenChatLieu;
        this.idThuongHieu = idThuongHieu;
        this.tenThuongHieu = tenThuongHieu;
        this.idDanhMuc = idDanhMuc;
        this.tenDanhMuc = tenDanhMuc;
        this.idCoAo = idCoAo;
        this.tenCoAo = tenCoAo;
        this.idTayAo = idTayAo;
        this.tenTayAo = tenTayAo;
        this.idHoaTiet = idHoaTiet;
        this.tenHoaTiet = tenHoaTiet;
        this.minGia = minGia;
        this.maxGia = maxGia;
        this.tongSoLuong = tongSoLuong;
        this.soLuongBienThe = soLuongBienThe;
        this.soMau = soMau;
        this.soKichCo = soKichCo;
        this.anhDaiDien = anhDaiDien;
        this.hinhAnhs = hinhAnhs;
        this.coGiamGia = coGiamGia;
        this.minGiaSauGiam = minGiaSauGiam;
        this.maxGiaSauGiam = maxGiaSauGiam;
        this.phanTramGiam = phanTramGiam;
        this.tenDotGiamGia = tenDotGiamGia;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMaSanPham() { return maSanPham; }
    public void setMaSanPham(String maSanPham) { this.maSanPham = maSanPham; }

    public String getTenSanPham() { return tenSanPham; }
    public void setTenSanPham(String tenSanPham) { this.tenSanPham = tenSanPham; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public Integer getTrangThai() { return trangThai; }
    public void setTrangThai(Integer trangThai) { this.trangThai = trangThai; }

    public Instant getNgayTao() { return ngayTao; }
    public void setNgayTao(Instant ngayTao) { this.ngayTao = ngayTao; }

    public Instant getNgayCapNhat() { return ngayCapNhat; }
    public void setNgayCapNhat(Instant ngayCapNhat) { this.ngayCapNhat = ngayCapNhat; }

    public String getNguoiTao() { return nguoiTao; }
    public void setNguoiTao(String nguoiTao) { this.nguoiTao = nguoiTao; }

    public Long getIdXuatXu() { return idXuatXu; }
    public void setIdXuatXu(Long idXuatXu) { this.idXuatXu = idXuatXu; }

    public String getTenXuatXu() { return tenXuatXu; }
    public void setTenXuatXu(String tenXuatXu) { this.tenXuatXu = tenXuatXu; }

    public Long getIdChatLieu() { return idChatLieu; }
    public void setIdChatLieu(Long idChatLieu) { this.idChatLieu = idChatLieu; }

    public String getTenChatLieu() { return tenChatLieu; }
    public void setTenChatLieu(String tenChatLieu) { this.tenChatLieu = tenChatLieu; }

    public Long getIdThuongHieu() { return idThuongHieu; }
    public void setIdThuongHieu(Long idThuongHieu) { this.idThuongHieu = idThuongHieu; }

    public String getTenThuongHieu() { return tenThuongHieu; }
    public void setTenThuongHieu(String tenThuongHieu) { this.tenThuongHieu = tenThuongHieu; }

    public Long getIdDanhMuc() { return idDanhMuc; }
    public void setIdDanhMuc(Long idDanhMuc) { this.idDanhMuc = idDanhMuc; }

    public String getTenDanhMuc() { return tenDanhMuc; }
    public void setTenDanhMuc(String tenDanhMuc) { this.tenDanhMuc = tenDanhMuc; }

    public Long getIdCoAo() { return idCoAo; }
    public void setIdCoAo(Long idCoAo) { this.idCoAo = idCoAo; }

    public String getTenCoAo() { return tenCoAo; }
    public void setTenCoAo(String tenCoAo) { this.tenCoAo = tenCoAo; }

    public Long getIdTayAo() { return idTayAo; }
    public void setIdTayAo(Long idTayAo) { this.idTayAo = idTayAo; }

    public String getTenTayAo() { return tenTayAo; }
    public void setTenTayAo(String tenTayAo) { this.tenTayAo = tenTayAo; }

    public Long getIdHoaTiet() { return idHoaTiet; }
    public void setIdHoaTiet(Long idHoaTiet) { this.idHoaTiet = idHoaTiet; }

    public String getTenHoaTiet() { return tenHoaTiet; }
    public void setTenHoaTiet(String tenHoaTiet) { this.tenHoaTiet = tenHoaTiet; }

    public BigDecimal getMinGia() { return minGia; }
    public void setMinGia(BigDecimal minGia) { this.minGia = minGia; }

    public BigDecimal getMaxGia() { return maxGia; }
    public void setMaxGia(BigDecimal maxGia) { this.maxGia = maxGia; }

    public Integer getTongSoLuong() { return tongSoLuong; }
    public void setTongSoLuong(Integer tongSoLuong) { this.tongSoLuong = tongSoLuong; }

    public Integer getSoLuongBienThe() { return soLuongBienThe; }
    public void setSoLuongBienThe(Integer soLuongBienThe) { this.soLuongBienThe = soLuongBienThe; }

    public Integer getSoMau() { return soMau; }
    public void setSoMau(Integer soMau) { this.soMau = soMau; }

    public Integer getSoKichCo() { return soKichCo; }
    public void setSoKichCo(Integer soKichCo) { this.soKichCo = soKichCo; }

    public String getAnhDaiDien() { return anhDaiDien; }
    public void setAnhDaiDien(String anhDaiDien) { this.anhDaiDien = anhDaiDien; }

    public List<String> getHinhAnhs() { return hinhAnhs; }
    public void setHinhAnhs(List<String> hinhAnhs) { this.hinhAnhs = hinhAnhs; }

    public Boolean getCoGiamGia() { return coGiamGia; }
    public void setCoGiamGia(Boolean coGiamGia) { this.coGiamGia = coGiamGia; }

    public BigDecimal getMinGiaSauGiam() { return minGiaSauGiam; }
    public void setMinGiaSauGiam(BigDecimal minGiaSauGiam) { this.minGiaSauGiam = minGiaSauGiam; }

    public BigDecimal getMaxGiaSauGiam() { return maxGiaSauGiam; }
    public void setMaxGiaSauGiam(BigDecimal maxGiaSauGiam) { this.maxGiaSauGiam = maxGiaSauGiam; }

    public BigDecimal getPhanTramGiam() { return phanTramGiam; }
    public void setPhanTramGiam(BigDecimal phanTramGiam) { this.phanTramGiam = phanTramGiam; }

    public String getTenDotGiamGia() { return tenDotGiamGia; }
    public void setTenDotGiamGia(String tenDotGiamGia) { this.tenDotGiamGia = tenDotGiamGia; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String maSanPham;
        private String tenSanPham;
        private String moTa;
        private Integer trangThai;
        private Instant ngayTao;
        private Instant ngayCapNhat;
        private String nguoiTao;
        private Long idXuatXu;
        private String tenXuatXu;
        private Long idChatLieu;
        private String tenChatLieu;
        private Long idThuongHieu;
        private String tenThuongHieu;
        private Long idDanhMuc;
        private String tenDanhMuc;
        private Long idCoAo;
        private String tenCoAo;
        private Long idTayAo;
        private String tenTayAo;
        private Long idHoaTiet;
        private String tenHoaTiet;
        private BigDecimal minGia;
        private BigDecimal maxGia;
        private Integer tongSoLuong;
        private Integer soLuongBienThe;
        private Integer soMau;
        private Integer soKichCo;
        private String anhDaiDien;
        private List<String> hinhAnhs;
        private Boolean coGiamGia;
        private BigDecimal minGiaSauGiam;
        private BigDecimal maxGiaSauGiam;
        private BigDecimal phanTramGiam;
        private String tenDotGiamGia;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder maSanPham(String maSanPham) { this.maSanPham = maSanPham; return this; }
        public Builder tenSanPham(String tenSanPham) { this.tenSanPham = tenSanPham; return this; }
        public Builder moTa(String moTa) { this.moTa = moTa; return this; }
        public Builder trangThai(Integer trangThai) { this.trangThai = trangThai; return this; }
        public Builder ngayTao(Instant ngayTao) { this.ngayTao = ngayTao; return this; }
        public Builder ngayCapNhat(Instant ngayCapNhat) { this.ngayCapNhat = ngayCapNhat; return this; }
        public Builder nguoiTao(String nguoiTao) { this.nguoiTao = nguoiTao; return this; }
        public Builder idXuatXu(Long idXuatXu) { this.idXuatXu = idXuatXu; return this; }
        public Builder tenXuatXu(String tenXuatXu) { this.tenXuatXu = tenXuatXu; return this; }
        public Builder idChatLieu(Long idChatLieu) { this.idChatLieu = idChatLieu; return this; }
        public Builder tenChatLieu(String tenChatLieu) { this.tenChatLieu = tenChatLieu; return this; }
        public Builder idThuongHieu(Long idThuongHieu) { this.idThuongHieu = idThuongHieu; return this; }
        public Builder tenThuongHieu(String tenThuongHieu) { this.tenThuongHieu = tenThuongHieu; return this; }
        public Builder idDanhMuc(Long idDanhMuc) { this.idDanhMuc = idDanhMuc; return this; }
        public Builder tenDanhMuc(String tenDanhMuc) { this.tenDanhMuc = tenDanhMuc; return this; }
        public Builder idCoAo(Long idCoAo) { this.idCoAo = idCoAo; return this; }
        public Builder tenCoAo(String tenCoAo) { this.tenCoAo = tenCoAo; return this; }
        public Builder idTayAo(Long idTayAo) { this.idTayAo = idTayAo; return this; }
        public Builder tenTayAo(String tenTayAo) { this.tenTayAo = tenTayAo; return this; }
        public Builder idHoaTiet(Long idHoaTiet) { this.idHoaTiet = idHoaTiet; return this; }
        public Builder tenHoaTiet(String tenHoaTiet) { this.tenHoaTiet = tenHoaTiet; return this; }
        public Builder minGia(BigDecimal minGia) { this.minGia = minGia; return this; }
        public Builder maxGia(BigDecimal maxGia) { this.maxGia = maxGia; return this; }
        public Builder tongSoLuong(Integer tongSoLuong) { this.tongSoLuong = tongSoLuong; return this; }
        public Builder soLuongBienThe(Integer soLuongBienThe) { this.soLuongBienThe = soLuongBienThe; return this; }
        public Builder soMau(Integer soMau) { this.soMau = soMau; return this; }
        public Builder soKichCo(Integer soKichCo) { this.soKichCo = soKichCo; return this; }
        public Builder anhDaiDien(String anhDaiDien) { this.anhDaiDien = anhDaiDien; return this; }
        public Builder hinhAnhs(List<String> hinhAnhs) { this.hinhAnhs = hinhAnhs; return this; }
        public Builder coGiamGia(Boolean coGiamGia) { this.coGiamGia = coGiamGia; return this; }
        public Builder minGiaSauGiam(BigDecimal minGiaSauGiam) { this.minGiaSauGiam = minGiaSauGiam; return this; }
        public Builder maxGiaSauGiam(BigDecimal maxGiaSauGiam) { this.maxGiaSauGiam = maxGiaSauGiam; return this; }
        public Builder phanTramGiam(BigDecimal phanTramGiam) { this.phanTramGiam = phanTramGiam; return this; }
        public Builder tenDotGiamGia(String tenDotGiamGia) { this.tenDotGiamGia = tenDotGiamGia; return this; }

        public SanPhamResponse build() {
            return new SanPhamResponse(id, maSanPham, tenSanPham, moTa, trangThai, ngayTao, ngayCapNhat, nguoiTao, idXuatXu, tenXuatXu, idChatLieu, tenChatLieu, idThuongHieu, tenThuongHieu, idDanhMuc, tenDanhMuc, idCoAo, tenCoAo, idTayAo, tenTayAo, idHoaTiet, tenHoaTiet, minGia, maxGia, tongSoLuong, soLuongBienThe, soMau, soKichCo, anhDaiDien, hinhAnhs, coGiamGia, minGiaSauGiam, maxGiaSauGiam, phanTramGiam, tenDotGiamGia);
        }
    }
}
