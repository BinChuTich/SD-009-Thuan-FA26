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
public class GhnFeeResponseDTO {
    private boolean success;
    private BigDecimal fee;
    private String currency;
    private String source; // "GHN_API" hoặc "REGIONAL_POLICY"
    private String regionDescription; // e.g. "Nội thành", "Ngoại thành", "Liên tỉnh"
    private String message;
}

