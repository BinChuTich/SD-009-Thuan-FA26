package com.example.sd009thuan.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Tên tài khoản không được để trống")
        String tenTaiKhoan,

        @NotBlank(message = "Mật khẩu không được để trống")
        String matKhau
) {}

