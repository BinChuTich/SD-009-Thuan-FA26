package com.example.sd009thuan.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
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

    public SanPhamFilterRequest() {
        super();
        this.page = 0;
        this.size = 10;
        this.sortBy = "id";
        this.sortDir = "desc";
    }

    public SanPhamFilterRequest(String keyword, Long idThuongHieu, Long idXuatXu, Long idDanhMuc, Long idChatLieu, Long idCoAo, Long idTayAo, Long idHoaTiet, Long idKichCo, Long idMauSac, Integer trangThai, BigDecimal minGia, BigDecimal maxGia, int page, int size, String sortBy, String sortDir) {
        super();
        this.keyword = keyword;
        this.idThuongHieu = idThuongHieu;
        this.idXuatXu = idXuatXu;
        this.idDanhMuc = idDanhMuc;
        this.idChatLieu = idChatLieu;
        this.idCoAo = idCoAo;
        this.idTayAo = idTayAo;
        this.idHoaTiet = idHoaTiet;
        this.idKichCo = idKichCo;
        this.idMauSac = idMauSac;
        this.trangThai = trangThai;
        this.minGia = minGia;
        this.maxGia = maxGia;
        this.page = page;
        this.size = size > 0 ? size : 10;
        this.sortBy = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy : "id";
        this.sortDir = (sortDir != null && !sortDir.trim().isEmpty()) ? sortDir : "desc";
    }

    public String getSortBy() {
        return (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy : "id";
    }

    public String getSortDir() {
        return (sortDir != null && !sortDir.trim().isEmpty()) ? sortDir : "desc";
    }

    public int getPage() {
        return Math.max(page, 0);
    }

    public int getSize() {
        return size > 0 ? size : 10;
    }
}
