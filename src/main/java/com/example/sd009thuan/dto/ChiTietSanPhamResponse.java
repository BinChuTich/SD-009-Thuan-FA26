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
public class ChiTietSanPhamResponse {
    private Long id;
    private Long idSanPham;
    private String maSanPham;
    private String tenSanPham;

    private Long idKichCo;
    private String tenKichCo;

    private Long idMauSac;
    private String tenMauSac;
    private String maHex;

    private String maChiTietSanPham;
    private Integer soLuong;
    private BigDecimal giaBan;
    private Integer trangThai;
    private Instant ngayTao;
    private String anhDaiDien;
}
