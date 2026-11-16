package com.example.sd009thuan.dto.payment;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ConfirmPaymentRequest {
    private String maHoaDon;
    private BigDecimal soTien;
    private String maGiaoDich;
    private String ghiChu;
    private String phuongThuc;
}

