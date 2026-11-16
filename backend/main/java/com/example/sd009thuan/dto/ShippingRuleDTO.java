package com.example.sd009thuan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRuleDTO {
    private Long id;
    private Long giaTriTu = 0L;
    private Long giaTriDen = 100000L;
    private Double khoangCachTu = 0.0;
    private Double khoangCachDen = 1.0;
    private String hinhThucTinhPhi = "TINH_PHI"; // TINH_PHI | MIEN_PHI
    private Long phiVanChuyen = 10000L;
    private Integer trangThai = 1; // 1: Đang hoạt động, 0: Ngưng
}
