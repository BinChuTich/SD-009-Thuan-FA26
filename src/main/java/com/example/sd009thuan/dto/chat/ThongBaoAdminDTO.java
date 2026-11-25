package com.example.sd009thuan.dto.chat;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThongBaoAdminDTO {
    private long tongSoThongBao;
    private long soTinChuaDoc;
    private long soDonHangMoi;
    private List<ThongBaoItemDTO> danhSachThongBao;
    private List<TinNhanThongBaoDTO> danhSachMoi;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ThongBaoItemDTO {
        private Long id;
        private String loai; // "DON_HANG" hoặc "CHAT"
        private String tieuDe;
        private String noiDung;
        private String thoiGian;
        private boolean daDoc;
        private String maDonHang;
        private Long cuocHoiThoaiId;
        private String lienKet;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TinNhanThongBaoDTO {
        private Long tinNhanId;
        private Long cuocHoiThoaiId;
        private String tenKhachHang;
        private String noiDung;
        private String thoiGian;
        private boolean daDoc;
    }
}
