package com.example.sd009thuan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class CaLamViecRequest {
    @NotBlank(message = "Tên ca làm việc không được để trống")
    @Size(min = 2, max = 50, message = "Tên ca làm việc phải từ 2 đến 50 ký tự")
    private String tenCa;

    @NotNull(message = "Vui lòng chọn Giờ bắt đầu")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime gioBatDau;

    @NotNull(message = "Vui lòng chọn Giờ kết thúc")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime gioKetThuc;

    @Size(max = 255, message = "Mô tả chi tiết tối đa 255 ký tự")
    private String moTa;

    private Integer trangThai = 1;

    public CaLamViecRequest() {
        super();
    }
}
