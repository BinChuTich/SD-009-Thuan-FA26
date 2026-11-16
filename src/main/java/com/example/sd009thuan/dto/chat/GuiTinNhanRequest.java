package com.example.sd009thuan.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuiTinNhanRequest {
    private Long cuocHoiThoaiId;
    private Long khachHangId;
    private Long nhanVienId;
    private String tenNhanVien;
    private String tenKhachHang;
    private String soDienThoai;
    private String nguoiGui; // 'KHACH_HANG' | 'NHAN_VIEN' | 'BOT'

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String noiDung;
}

