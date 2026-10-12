package com.example.sd009thuan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThuocTinhResponseDTO {
    // Both plural with List and short names for 100% frontend compatibility
    private List<ThuocTinhItemDTO> thuongHieu;
    private List<ThuocTinhItemDTO> thuongHieuList;

    private List<ThuocTinhItemDTO> xuatXu;
    private List<ThuocTinhItemDTO> xuatXuList;

    private List<ThuocTinhItemDTO> danhMuc;
    private List<ThuocTinhItemDTO> danhMucList;

    private List<ThuocTinhItemDTO> chatLieu;
    private List<ThuocTinhItemDTO> chatLieuList;

    private List<ThuocTinhItemDTO> coAo;
    private List<ThuocTinhItemDTO> coAoList;

    private List<ThuocTinhItemDTO> tayAo;
    private List<ThuocTinhItemDTO> tayAoList;

    private List<ThuocTinhItemDTO> hoaTiet;
    private List<ThuocTinhItemDTO> hoaTietList;

    private List<ThuocTinhItemDTO> mauSac;
    private List<ThuocTinhItemDTO> mauSacList;

    private List<ThuocTinhItemDTO> kichCo;
    private List<ThuocTinhItemDTO> kichCoList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThuocTinhItemDTO {
        private Long id;
        private String ma;
        private String ten;

        // Specific property names to match various frontend templates
        private String tenThuongHieu;
        private String tenXuatXu;
        private String tenDanhMuc;
        private String tenChatLieu;
        private String tenCoAo;
        private String tenTayAo;
        private String tenHoaTiet;
        private String tenMauSac;
        private String tenKichCo;

        private String maHex;
        private Integer trangThai;
    }
}
