package com.example.sd009thuan.dto;

import jakarta.validation.constraints.*;
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
}
