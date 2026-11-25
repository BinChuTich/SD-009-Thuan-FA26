package com.example.sd009thuan.dto.chat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KhoiTaoChatRequest {
    private Long cuocHoiThoaiId;
    private Long khachHangId;
    private String tenKhachHang;
    private String soDienThoai;
}

