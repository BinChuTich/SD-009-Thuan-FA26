package com.example.sd009thuan.dto.chat;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TinNhanDTO {
    private Long id;
    private Long cuocHoiThoaiId;
    private Long nhanVienId;
    private String tenNhanVien;
    private String nguoiGui;
    private String noiDung;
    private Boolean daDoc;
    private LocalDateTime ngayTao;
}
