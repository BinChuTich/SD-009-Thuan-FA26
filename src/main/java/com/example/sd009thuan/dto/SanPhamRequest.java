package com.example.sd009thuan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamRequest {
    private String maSanPham;

    @NotBlank(message = "Tên sản phẩm không được để trống")
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
}
