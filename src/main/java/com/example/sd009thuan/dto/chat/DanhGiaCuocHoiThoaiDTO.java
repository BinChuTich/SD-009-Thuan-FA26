package com.example.sd009thuan.dto.chat;

import lombok.*;

import java.time.LocalDateTime;

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
}

