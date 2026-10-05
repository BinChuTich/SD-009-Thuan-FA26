package com.example.sd009thuan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChiTietSanPhamRequest {
    private Long id;
    @NotNull(message = "Sản phẩm không được để trống")
    private Long idSanPham;

    @NotNull(message = "Kích cỡ không được để trống")
    private Long idKichCo;

    @NotNull(message = "Màu sắc không được để trống")
    private Long idMauSac;

    private String maChiTietSanPham;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng phải lớn hơn hoặc bằng 0")
    private Integer soLuong;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá bán phải lớn hơn hoặc bằng 0")
    private BigDecimal giaBan;

    private Integer trangThai;
    private String nguoiThaoTac;
}
