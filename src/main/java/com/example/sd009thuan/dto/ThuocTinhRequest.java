package com.example.sd009thuan.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThuocTinhRequest {

    @Size(max = 50, message = "Mã thuộc tính không được vượt quá 50 ký tự!")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]*$", message = "Mã thuộc tính chỉ được chứa chữ cái, chữ số và các ký tự _ - .")
    private String ma;

    @NotBlank(message = "Tên thuộc tính không được để trống!")
    @Size(min = 1, max = 255, message = "Tên thuộc tính phải từ 1 đến 255 ký tự!")
    private String ten;

    @Pattern(regexp = "^(#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3}))?$", message = "Mã màu HEX không hợp lệ (Ví dụ: #FF0000 hoặc #FFF)!")
    private String maHex;

    @Min(value = 0, message = "Trạng thái chỉ có thể là 0 (Ngừng dùng) hoặc 1 (Đang dùng)!")
    @Max(value = 1, message = "Trạng thái chỉ có thể là 0 (Ngừng dùng) hoặc 1 (Đang dùng)!")
    private Integer trangThai;
}
