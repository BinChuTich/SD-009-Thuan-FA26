package com.example.sd009thuan.dto.chat;
import jakarta.validation.constraints.NotBlank;
public record ChatMessageRequest(@NotBlank(message = "Nội dung tin nhắn không được để trống") String text) { }
