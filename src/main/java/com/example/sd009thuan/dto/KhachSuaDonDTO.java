package com.example.sd009thuan.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class KhachSuaDonDTO {
    private String soDienThoaiXacThuc;
    private String tenNguoiNhan;
    private String soDienThoaiNhan;
    private String diaChiNhanHang; // Chuỗi địa chỉ hoàn chỉnh (Số nhà, đường, Phường, Xã, Quận/Huyện, Tỉnh/TP)
    private String ghiChu;
    private BigDecimal phiVanChuyenMoi;
    private String email;
}
