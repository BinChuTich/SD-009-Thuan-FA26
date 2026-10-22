package com.example.sd009thuan.dto.chat;
import java.time.LocalDateTime;
import java.util.List;
public record ConversationResponse(Long id, Long customerId, String name, String phone,
        Long employeeId, String employeeName, String mode, String status,
        String time, String startTime, LocalDateTime updatedAt, List<ChatMessageResponse> messages) { }
