package com.example.sd009thuan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class LichLamViecRequest {
    @NotNull(message = "Vui lòng chọn một nhân viên")
    private Long nhanVienId;

    @NotNull(message = "Vui lòng chọn ca làm việc")
    private Long caLamViecId;


    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate ngayLamViec;

    private List<LocalDate> danhSachNgay;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    private String ghiChu;

    private Integer trangThai = 1;

    public LichLamViecRequest() {
        super();
    }
}

