package com.example.sd009thuan.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

public class AiChatRequest {
    @NotBlank(message = "Tin nhắn không được để trống")
    @Size(max = 1000, message = "Tin nhắn tối đa 1000 ký tự")
    private String message;

    @Valid
    @Size(max = 10, message = "Lịch sử hội thoại tối đa 10 lượt")
    private List<AiChatTurn> history = new ArrayList<>();

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AiChatTurn> getHistory() { return history; }
    public void setHistory(List<AiChatTurn> history) { this.history = history; }
}
