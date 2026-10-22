package com.example.sd009thuan.dto.chat;
import java.time.LocalDateTime;
public record ChatMessageResponse(Long id, String role, String text, String time, LocalDateTime sentAt) { }
