package com.example.sd009thuan.dto.ghn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhnFeeRequestDTO {
    private Integer fromDistrictId;
    private String fromWardCode;
    private Integer toDistrictId;
    private String toWardCode;
    private String provinceName;
    private String districtName;
    private String wardName;
    private Integer weight; // gram
    private Integer length; // cm
    private Integer width;  // cm
    private Integer height; // cm
    private BigDecimal insuranceValue; // Giá trị bảo hiểm đơn hàng (VNĐ)
}

