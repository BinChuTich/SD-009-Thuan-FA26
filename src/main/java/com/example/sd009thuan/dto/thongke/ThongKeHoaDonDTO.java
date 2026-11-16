package com.example.sd009thuan.dto.thongke;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThongKeHoaDonDTO {
    private Long id;
    private String maHoaDon;
    private String ngayTao;
    private String tenKhachHang;
    private String soDienThoai;
    private Integer loaiDon;
    private String tenLoaiDon;
    private String phuongThucThanhToan;
    private BigDecimal tongTien;
    private BigDecimal tienSauGiamGia;
    private BigDecimal phiVanChuyen;
    private Integer trangThai;
    private String tenTrangThai;
    private Integer trangThaiThanhToan;
    private String tenTrangThaiThanhToan;
    private Integer tongSoLuongSanPham;
    @Builder.Default
    private List<ThongKeSanPhamItemDTO> sanPhams = new ArrayList<>();
}

