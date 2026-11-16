package com.example.sd009thuan.dto.payment;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentInfoRequest {
    private String maHoaDon;
    private BigDecimal soTien;
    private String noiDung;
    private String bankId;
    private String accountNo;
    private String accountName;
}

