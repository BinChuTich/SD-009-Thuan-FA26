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
public class ThongKeHoaDonItemDTO {
    private Long idHoaDon;
    private String maHoaDon;
    private String ngayTao;
    private String tenKhachHang;
    private String soDienThoai;
    private Integer loaiDon;
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
    private Integer trangThaiHoaDon;
    private String tenTrangThai;
}

