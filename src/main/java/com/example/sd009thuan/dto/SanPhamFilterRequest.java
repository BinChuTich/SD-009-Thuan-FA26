package com.example.sd009thuan.dto;

import java.math.BigDecimal;

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

    private int page = 0;
    private int size = 10;
    private String sortBy = "id";
    private String sortDir = "desc";

    public SanPhamFilterRequest() {
    }

    public SanPhamFilterRequest(String keyword, Long idThuongHieu, Long idXuatXu, Long idDanhMuc, Long idChatLieu, Long idCoAo, Long idTayAo, Long idHoaTiet, Long idKichCo, Long idMauSac, Integer trangThai, BigDecimal minGia, BigDecimal maxGia, int page, int size, String sortBy, String sortDir) {
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
        this.size = size;
        this.sortBy = sortBy;
        this.sortDir = sortDir;
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Long getIdThuongHieu() { return idThuongHieu; }
    public void setIdThuongHieu(Long idThuongHieu) { this.idThuongHieu = idThuongHieu; }

    public Long getIdXuatXu() { return idXuatXu; }
    public void setIdXuatXu(Long idXuatXu) { this.idXuatXu = idXuatXu; }

    public Long getIdDanhMuc() { return idDanhMuc; }
    public void setIdDanhMuc(Long idDanhMuc) { this.idDanhMuc = idDanhMuc; }

    public Long getIdChatLieu() { return idChatLieu; }
    public void setIdChatLieu(Long idChatLieu) { this.idChatLieu = idChatLieu; }

    public Long getIdCoAo() { return idCoAo; }
    public void setIdCoAo(Long idCoAo) { this.idCoAo = idCoAo; }

    public Long getIdTayAo() { return idTayAo; }
    public void setIdTayAo(Long idTayAo) { this.idTayAo = idTayAo; }

    public Long getIdHoaTiet() { return idHoaTiet; }
    public void setIdHoaTiet(Long idHoaTiet) { this.idHoaTiet = idHoaTiet; }

    public Long getIdKichCo() { return idKichCo; }
    public void setIdKichCo(Long idKichCo) { this.idKichCo = idKichCo; }

    public Long getIdMauSac() { return idMauSac; }
    public void setIdMauSac(Long idMauSac) { this.idMauSac = idMauSac; }

    public Integer getTrangThai() { return trangThai; }
    public void setTrangThai(Integer trangThai) { this.trangThai = trangThai; }

    public BigDecimal getMinGia() { return minGia; }
    public void setMinGia(BigDecimal minGia) { this.minGia = minGia; }

    public BigDecimal getMaxGia() { return maxGia; }
    public void setMaxGia(BigDecimal maxGia) { this.maxGia = maxGia; }

    public void setPage(int page) { this.page = page; }
    public void setSize(int size) { this.size = size; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setSortDir(String sortDir) { this.sortDir = sortDir; }

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
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
        private int page = 0;
        private int size = 10;
        private String sortBy = "id";
        private String sortDir = "desc";

        public Builder keyword(String keyword) { this.keyword = keyword; return this; }
        public Builder idThuongHieu(Long idThuongHieu) { this.idThuongHieu = idThuongHieu; return this; }
        public Builder idXuatXu(Long idXuatXu) { this.idXuatXu = idXuatXu; return this; }
        public Builder idDanhMuc(Long idDanhMuc) { this.idDanhMuc = idDanhMuc; return this; }
        public Builder idChatLieu(Long idChatLieu) { this.idChatLieu = idChatLieu; return this; }
        public Builder idCoAo(Long idCoAo) { this.idCoAo = idCoAo; return this; }
        public Builder idTayAo(Long idTayAo) { this.idTayAo = idTayAo; return this; }
        public Builder idHoaTiet(Long idHoaTiet) { this.idHoaTiet = idHoaTiet; return this; }
        public Builder idKichCo(Long idKichCo) { this.idKichCo = idKichCo; return this; }
        public Builder idMauSac(Long idMauSac) { this.idMauSac = idMauSac; return this; }
        public Builder trangThai(Integer trangThai) { this.trangThai = trangThai; return this; }
        public Builder minGia(BigDecimal minGia) { this.minGia = minGia; return this; }
        public Builder maxGia(BigDecimal maxGia) { this.maxGia = maxGia; return this; }
        public Builder page(int page) { this.page = page; return this; }
        public Builder size(int size) { this.size = size; return this; }
        public Builder sortBy(String sortBy) { this.sortBy = sortBy; return this; }
        public Builder sortDir(String sortDir) { this.sortDir = sortDir; return this; }

        public SanPhamFilterRequest build() {
            return new SanPhamFilterRequest(keyword, idThuongHieu, idXuatXu, idDanhMuc, idChatLieu, idCoAo, idTayAo, idHoaTiet, idKichCo, idMauSac, trangThai, minGia, maxGia, page, size, sortBy, sortDir);
        }
    }
}
