package com.example.sd009thuan.dto.chat;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhGiaCuocHoiThoaiDTO {
    private Long id;
    private String maDanhGia;
    private Long cuocHoiThoaiId;
    private Long khachHangId;
    private String tenKhachHang;
    private Long nhanVienId;
    private String tenNhanVien;
    private Integer diemDanhGia; // 1 đến 5 sao
    private String noiDungDanhGia;
    private Integer trangThai;
    private LocalDateTime ngayTao;
    private String phanLoai; // TICH_CUC | TRUNG_LAP | TIEU_CUC
    private String camXucNoiDung; // TICH_CUC | TRUNG_LAP | TIEU_CUC | HON_HOP | KHONG_XAC_DINH
    private List<String> chuDe;
    private Boolean mauThuanSaoNoiDung;
    private Boolean canXuLy;
    private Boolean daXuLy;
    private String nguoiXuLy;
    private LocalDateTime ngayXuLy;
}

