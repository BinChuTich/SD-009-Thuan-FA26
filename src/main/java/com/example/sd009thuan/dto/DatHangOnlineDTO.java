package com.example.sd009thuan.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class DatHangOnlineDTO {
    private Long idKhachHang;
    private Long idPhieuGiamGia;
    private String tenKhachHang;
    private String soDienThoai;
    private String email;
    private String diaChiNhanHang;
    private String ghiChu;
    private String phuongThucThanhToan; // "COD", "VNPAY", "MANUAL"
    private String donViVanChuyen;
    private String maVanDon;
    private BigDecimal tongTien;
    private BigDecimal phiVanChuyen;
    private BigDecimal giamGia;
    private List<ItemChiTiet> chiTiet;

    @Getter
    @Setter
    public static class ItemChiTiet {
        private Long idChiTietSanPham;
        private Integer soLuong;
        private BigDecimal donGia;
    }
}
