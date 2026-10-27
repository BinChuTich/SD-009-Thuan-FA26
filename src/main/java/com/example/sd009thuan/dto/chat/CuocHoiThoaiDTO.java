package com.example.sd009thuan.dto.chat;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuocHoiThoaiDTO {
    private Long id;
    private Long khachHangId;
    private String tenKhachHang;
    private String soDienThoai;
    private Long nhanVienId;
    private String tenNhanVien;
    private Integer trangThai;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private String tinNhanCuoi;
    private LocalDateTime thoiGianTinNhanCuoi;
    private Long soTinChuaDoc;
}
