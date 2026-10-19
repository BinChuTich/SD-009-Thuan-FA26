package com.example.sd009thuan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
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

    public SanPhamRequest() {
        super();
    }

    public SanPhamRequest(String maSanPham, String tenSanPham, String moTa, Long idXuatXu, Long idChatLieu, Long idThuongHieu, Long idDanhMuc, Long idCoAo, Long idTayAo, Long idHoaTiet, Integer trangThai, String nguoiThaoTac, List<String> hinhAnhs) {
        super();
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
    }
}
