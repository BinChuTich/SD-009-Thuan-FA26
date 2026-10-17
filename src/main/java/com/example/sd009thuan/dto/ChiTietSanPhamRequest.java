package com.example.sd009thuan.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ChiTietSanPhamRequest {
    private Long id;

    @NotNull(message = "Sản phẩm không được để trống")
    private Long idSanPham;

    @NotNull(message = "Kích cỡ không được để trống")
    private Long idKichCo;

    @NotNull(message = "Màu sắc không được để trống")
    private Long idMauSac;

    @Size(max = 50, message = "Mã chi tiết sản phẩm không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]*$", message = "Mã chi tiết sản phẩm chỉ được chứa chữ cái, chữ số và các ký tự _ - .")
    private String maChiTietSanPham;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng phải lớn hơn hoặc bằng 0")
    @Max(value = 999999, message = "Số lượng không được vượt quá 999.999")
    private Integer soLuong;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá bán phải lớn hơn hoặc bằng 0")
    @DecimalMax(value = "1000000000.0", inclusive = true, message = "Giá bán không được vượt quá 1.000.000.000 đ")
    private BigDecimal giaBan;

    @Min(value = 0, message = "Trạng thái chỉ có thể là 0 (Ngừng bán) hoặc 1 (Đang bán)")
    @Max(value = 1, message = "Trạng thái chỉ có thể là 0 (Ngừng bán) hoặc 1 (Đang bán)")
    private Integer trangThai;

    private String nguoiThaoTac;
    private String anhDaiDien;
    private String hinhAnh;

    public ChiTietSanPhamRequest() {
    }

    public ChiTietSanPhamRequest(Long id, Long idSanPham, Long idKichCo, Long idMauSac, String maChiTietSanPham, Integer soLuong, BigDecimal giaBan, Integer trangThai, String nguoiThaoTac, String anhDaiDien, String hinhAnh) {
        this.id = id;
        this.idSanPham = idSanPham;
        this.idKichCo = idKichCo;
        this.idMauSac = idMauSac;
        this.maChiTietSanPham = maChiTietSanPham;
        this.soLuong = soLuong;
        this.giaBan = giaBan;
        this.trangThai = trangThai;
        this.nguoiThaoTac = nguoiThaoTac;
        this.anhDaiDien = anhDaiDien;
        this.hinhAnh = hinhAnh;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdSanPham() {
        return idSanPham;
    }

    public void setIdSanPham(Long idSanPham) {
        this.idSanPham = idSanPham;
    }

    public Long getIdKichCo() {
        return idKichCo;
    }

    public void setIdKichCo(Long idKichCo) {
        this.idKichCo = idKichCo;
    }

    public Long getIdMauSac() {
        return idMauSac;
    }

    public void setIdMauSac(Long idMauSac) {
        this.idMauSac = idMauSac;
    }

    public String getMaChiTietSanPham() {
        return maChiTietSanPham;
    }

    public void setMaChiTietSanPham(String maChiTietSanPham) {
        this.maChiTietSanPham = maChiTietSanPham;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }

    public BigDecimal getGiaBan() {
        return giaBan;
    }

    public void setGiaBan(BigDecimal giaBan) {
        this.giaBan = giaBan;
    }

    public Integer getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Integer trangThai) {
        this.trangThai = trangThai;
    }

    public String getNguoiThaoTac() {
        return nguoiThaoTac;
    }

    public void setNguoiThaoTac(String nguoiThaoTac) {
        this.nguoiThaoTac = nguoiThaoTac;
    }

    public String getAnhDaiDien() {
        return anhDaiDien;
    }

    public void setAnhDaiDien(String anhDaiDien) {
        this.anhDaiDien = anhDaiDien;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(String hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long idSanPham;
        private Long idKichCo;
        private Long idMauSac;
        private String maChiTietSanPham;
        private Integer soLuong;
        private BigDecimal giaBan;
        private Integer trangThai;
        private String nguoiThaoTac;
        private String anhDaiDien;
        private String hinhAnh;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder idSanPham(Long idSanPham) { this.idSanPham = idSanPham; return this; }
        public Builder idKichCo(Long idKichCo) { this.idKichCo = idKichCo; return this; }
        public Builder idMauSac(Long idMauSac) { this.idMauSac = idMauSac; return this; }
        public Builder maChiTietSanPham(String maChiTietSanPham) { this.maChiTietSanPham = maChiTietSanPham; return this; }
        public Builder soLuong(Integer soLuong) { this.soLuong = soLuong; return this; }
        public Builder giaBan(BigDecimal giaBan) { this.giaBan = giaBan; return this; }
        public Builder trangThai(Integer trangThai) { this.trangThai = trangThai; return this; }
        public Builder nguoiThaoTac(String nguoiThaoTac) { this.nguoiThaoTac = nguoiThaoTac; return this; }
        public Builder anhDaiDien(String anhDaiDien) { this.anhDaiDien = anhDaiDien; return this; }
        public Builder hinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; return this; }

        public ChiTietSanPhamRequest build() {
            return new ChiTietSanPhamRequest(id, idSanPham, idKichCo, idMauSac, maChiTietSanPham, soLuong, giaBan, trangThai, nguoiThaoTac, anhDaiDien, hinhAnh);
        }
    }
}
