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
public class PaymentInfoResponse {
    private String maHoaDon;
    private BigDecimal soTien;
    private String nganHang;
    private String maNganHang;
    private String soTaiKhoan;
    private String tenChuTaiKhoan;
    private String noiDungChuyenKhoan;
    private String maGiaoDich;
    private String qrCodeUrl;
    private String vnpayUrl;
    private Boolean daThanhToan;
    private Instant thoiGianTao;
}

