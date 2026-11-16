package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.ai.AiChatRequest;
import com.example.sd009thuan.dto.ai.AiChatResponse;
import com.example.sd009thuan.service.AiShoppingAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-chat")
public class AiChatController {
    private final AiShoppingAssistantService assistantService;

    public AiChatController(AiShoppingAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/message")
    public ResponseEntity<AiChatResponse> message(@Valid @RequestBody AiChatRequest request) {
        return ResponseEntity.ok(assistantService.reply(request.getMessage()));
    }
}
