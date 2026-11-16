package com.example.sd009thuan.dto.thongke;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThongKeSanPhamItemDTO {
    private Long idChiTietHoaDon;
    private Long idSanPham;
    private Long idChiTietSanPham;
    private String maSanPham;
    private String tenSanPham;
    private String tenThuongHieu;
    private String tenMauSac;
    private String tenKichCo;
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
    private String anhDaiDien;
}

