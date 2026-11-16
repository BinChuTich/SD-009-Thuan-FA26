package com.example.sd009thuan.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {
    private String maHoaDon;
    private Boolean daThanhToan;
    private Integer trangThaiThanhToan;
    private BigDecimal soTienDaNhan;
    private String maGiaoDich;
    private Instant thoiGianThanhToan;
    private String message;
}

