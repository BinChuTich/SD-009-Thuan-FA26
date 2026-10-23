package com.example.sd009thuan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DongMoCaRequest {
    private Long id; // ID của lượt giao ca (khi đóng ca)


    private Long nhanVienId;


    private Long caLamViecId;

    private Long nhanVienNhanId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Số tiền mặt đầu ca phải lớn hơn hoặc bằng 0")
    private BigDecimal tienMatDauCa;

    @DecimalMin(value = "0.0", inclusive = true, message = "Số tiền mặt cuối ca phải lớn hơn hoặc bằng 0")
    private BigDecimal tienMatCuoiCa;

    private BigDecimal tienChiTrongCa;

    private BigDecimal tienChuyenKhoan;

    private BigDecimal tongTienTrongCa;

    private BigDecimal chenhLech;

    private String lyDoChenhLech;

    private String ghiChu;

    public DongMoCaRequest() {
        super();
    }
}

