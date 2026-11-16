package com.example.sd009thuan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShippingConfigDTO {
    private String cheDoTinhPhi = "REGION_TABLE";
    private Long phiNoiThanhHaNoi = 20000L;
    private Long phiNgoaiThanhHaNoi = 30000L;
    private Long phiMienBac = 35000L;
    private Long phiMienTrung = 40000L;
    private Long phiMienNam = 45000L;
    private Long phiMacDinhToanQuoc = 45000L;
    private Boolean apDungMienPhiShip = true;
    private Long nguongMienPhiShip = 500000L;
    private Boolean apDungFreeshipPos = true, apDungFreeshipOnline = true;
    private List<ShippingRuleDTO> danhSachCauHinh = new ArrayList<>();
    private String nguoiCapNhat;
    private String ngayCapNhat;
}
