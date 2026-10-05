package com.example.sd009thuan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamFilterRequest {
    private String keyword;
    private Long idThuongHieu;
    private Long idXuatXu;
    private Long idDanhMuc;
    private Long idChatLieu;
    private Long idCoAo;
    private Long idTayAo;
    private Long idHoaTiet;
    private Long idKichCo;
    private Long idMauSac;
    private Integer trangThai;

    private BigDecimal minGia;
    private BigDecimal maxGia;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 10;

    @Builder.Default
    private String sortBy = "id";

    @Builder.Default
    private String sortDir = "desc";
}
