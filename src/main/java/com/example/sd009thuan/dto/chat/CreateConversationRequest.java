package com.example.sd009thuan.dto.chat;
import jakarta.validation.constraints.NotNull;
public record CreateConversationRequest(@NotNull Long customerId, String mode) { }
