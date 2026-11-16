package com.example.sd009thuan.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Vui lòng nhập địa chỉ email của bạn")
        @Email(message = "Địa chỉ email không đúng định dạng")
        String email
) {}

