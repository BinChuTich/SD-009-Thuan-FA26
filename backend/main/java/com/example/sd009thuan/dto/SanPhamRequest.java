package com.example.sd009thuan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class SanPhamRequest {

    @Size(max = 50, message = "Mã sản phẩm không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]*$", message = "Mã sản phẩm chỉ được chứa chữ cái, chữ số và các ký tự _ - .")
    private String maSanPham;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 1, max = 255, message = "Tên sản phẩm phải từ 1 đến 255 ký tự")
    private String tenSanPham;

    @Size(max = 2000, message = "Mô tả sản phẩm không được vượt quá 2000 ký tự")
    private String moTa;

    private Long idXuatXu;
    private Long idChatLieu;
    private Long idThuongHieu;
    private Long idDanhMuc;
    private Long idCoAo;
    private Long idTayAo;
    private Long idHoaTiet;

    private Integer trangThai;
    private String nguoiThaoTac;
    private List<String> hinhAnhs;
    private Boolean xacNhanGop;

    public SanPhamRequest() {
    }

    public SanPhamRequest(String maSanPham, String tenSanPham, String moTa, Long idXuatXu,
                         Long idChatLieu, Long idThuongHieu, Long idDanhMuc, Long idCoAo,
                         Long idTayAo, Long idHoaTiet, Integer trangThai, String nguoiThaoTac,
                         List<String> hinhAnhs, Boolean xacNhanGop) {
        this.maSanPham = maSanPham;
        this.tenSanPham = tenSanPham;
        this.moTa = moTa;
        this.idXuatXu = idXuatXu;
        this.idChatLieu = idChatLieu;
        this.idThuongHieu = idThuongHieu;
        this.idDanhMuc = idDanhMuc;
        this.idCoAo = idCoAo;
        this.idTayAo = idTayAo;
        this.idHoaTiet = idHoaTiet;
        this.trangThai = trangThai;
        this.nguoiThaoTac = nguoiThaoTac;
        this.hinhAnhs = hinhAnhs;
        this.xacNhanGop = xacNhanGop;
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

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public Long getIdXuatXu() {
        return idXuatXu;
    }

    public void setIdXuatXu(Long idXuatXu) {
        this.idXuatXu = idXuatXu;
    }

    public Long getIdChatLieu() {
        return idChatLieu;
    }

    public void setIdChatLieu(Long idChatLieu) {
        this.idChatLieu = idChatLieu;
    }

    public Long getIdThuongHieu() {
        return idThuongHieu;
    }

    public void setIdThuongHieu(Long idThuongHieu) {
        this.idThuongHieu = idThuongHieu;
    }

    public Long getIdDanhMuc() {
        return idDanhMuc;
    }

    public void setIdDanhMuc(Long idDanhMuc) {
        this.idDanhMuc = idDanhMuc;
    }

    public Long getIdCoAo() {
        return idCoAo;
    }

    public void setIdCoAo(Long idCoAo) {
        this.idCoAo = idCoAo;
    }

    public Long getIdTayAo() {
        return idTayAo;
    }

    public void setIdTayAo(Long idTayAo) {
        this.idTayAo = idTayAo;
    }

    public Long getIdHoaTiet() {
        return idHoaTiet;
    }

    public void setIdHoaTiet(Long idHoaTiet) {
        this.idHoaTiet = idHoaTiet;
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

    public List<String> getHinhAnhs() {
        return hinhAnhs;
    }

    public void setHinhAnhs(List<String> hinhAnhs) {
        this.hinhAnhs = hinhAnhs;
    }

    public Boolean getXacNhanGop() {
        return xacNhanGop;
    }

    public Boolean isXacNhanGop() {
        return xacNhanGop;
    }

    public void setXacNhanGop(Boolean xacNhanGop) {
        this.xacNhanGop = xacNhanGop;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String maSanPham;
        private String tenSanPham;
        private String moTa;
        private Long idXuatXu;
        private Long idChatLieu;
        private Long idThuongHieu;
        private Long idDanhMuc;
        private Long idCoAo;
        private Long idTayAo;
        private Long idHoaTiet;
        private Integer trangThai;
        private String nguoiThaoTac;
        private List<String> hinhAnhs;
        private Boolean xacNhanGop;

        public Builder maSanPham(String maSanPham) {
            this.maSanPham = maSanPham;
            return this;
        }

        public Builder tenSanPham(String tenSanPham) {
            this.tenSanPham = tenSanPham;
            return this;
        }

        public Builder moTa(String moTa) {
            this.moTa = moTa;
            return this;
        }

        public Builder idXuatXu(Long idXuatXu) {
            this.idXuatXu = idXuatXu;
            return this;
        }

        public Builder idChatLieu(Long idChatLieu) {
            this.idChatLieu = idChatLieu;
            return this;
        }

        public Builder idThuongHieu(Long idThuongHieu) {
            this.idThuongHieu = idThuongHieu;
            return this;
        }

        public Builder idDanhMuc(Long idDanhMuc) {
            this.idDanhMuc = idDanhMuc;
            return this;
        }

        public Builder idCoAo(Long idCoAo) {
            this.idCoAo = idCoAo;
            return this;
        }

        public Builder idTayAo(Long idTayAo) {
            this.idTayAo = idTayAo;
            return this;
        }

        public Builder idHoaTiet(Long idHoaTiet) {
            this.idHoaTiet = idHoaTiet;
            return this;
        }

        public Builder trangThai(Integer trangThai) {
            this.trangThai = trangThai;
            return this;
        }

        public Builder nguoiThaoTac(String nguoiThaoTac) {
            this.nguoiThaoTac = nguoiThaoTac;
            return this;
        }

        public Builder hinhAnhs(List<String> hinhAnhs) {
            this.hinhAnhs = hinhAnhs;
            return this;
        }

        public Builder xacNhanGop(Boolean xacNhanGop) {
            this.xacNhanGop = xacNhanGop;
            return this;
        }

        public SanPhamRequest build() {
            return new SanPhamRequest(maSanPham, tenSanPham, moTa, idXuatXu, idChatLieu, idThuongHieu,
                    idDanhMuc, idCoAo, idTayAo, idHoaTiet, trangThai, nguoiThaoTac, hinhAnhs, xacNhanGop);
        }
    }
}
