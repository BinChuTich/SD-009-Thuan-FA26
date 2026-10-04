package com.example.sd009thuan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamResponse {
    private Long id;
    private String maSanPham;
    private String tenSanPham;
    private String moTa;
    private Integer trangThai;
    private Instant ngayTao;
    private Instant ngayCapNhat;
    private String nguoiTao;

    // Thuộc tính mở rộng
    private Long idXuatXu;
    private String tenXuatXu;

    private Long idChatLieu;
    private String tenChatLieu;

    private Long idThuongHieu;
    private String tenThuongHieu;

    private Long idDanhMuc;
    private String tenDanhMuc;

    private Long idCoAo;
    private String tenCoAo;

    private Long idTayAo;
    private String tenTayAo;

    private Long idHoaTiet;
    private String tenHoaTiet;

    // Tổng hợp từ chi tiết biến thể
    private BigDecimal minGia;
    private BigDecimal maxGia;
    private Integer tongSoLuong;
    private Integer soLuongBienThe;
    private String anhDaiDien;
}
