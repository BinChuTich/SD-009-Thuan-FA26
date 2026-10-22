package com.example.sd009thuan.controller;

import com.example.sd009thuan.dto.chat.*;
import com.example.sd009thuan.service.ChatService;
import com.example.sd009thuan.service.ChatKnowledgeService;
import com.example.sd009thuan.websocket.ChatWebSocketHandler;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final ChatKnowledgeService knowledgeService;
    private final ChatWebSocketHandler socket;
    public ChatController(ChatService chatService, ChatKnowledgeService knowledgeService, ChatWebSocketHandler socket) {
        this.chatService = chatService; this.knowledgeService = knowledgeService; this.socket = socket;
    }
    private void publish(Long id) {
        socket.broadcast(Map.of("type", "conversation-updated", "conversationId", id));
    }
    @PostMapping("/knowledge/ask")
    public Map<String, String> askKnowledge(@RequestBody Map<String, String> request) {
        String question = request.getOrDefault("question", "");
        if (question.isBlank()) throw new IllegalArgumentException("Vui lòng nhập câu hỏi.");
        return Map.of("answer", knowledgeService.answer(question));
    }

    // Admin/nhân viên: danh sách cuộc hội thoại; status nhận active, history hoặc all.
    @GetMapping("/admin/conversations")
    public List<ConversationResponse> adminConversations(@RequestParam(required = false) String status) {
        return chatService.getAdminConversations(status);
    }

    @GetMapping("/conversations/{id}")
    public ConversationResponse getConversation(@PathVariable Long id) { return chatService.getConversation(id); }

    @GetMapping("/conversations/{id}/messages")
    public List<ChatMessageResponse> messages(@PathVariable Long id) {
        return chatService.getConversation(id).messages();
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse create(@Valid @RequestBody CreateConversationRequest request) {
        return chatService.createConversation(request);
    }

    @GetMapping("/customer/{customerId}/conversations")
    public List<ConversationResponse> customerConversations(@PathVariable Long customerId) {
        return chatService.getCustomerConversations(customerId);
    }

    @PostMapping("/conversations/{id}/messages")
    public ConversationResponse employeeMessage(@PathVariable Long id,
            @RequestParam(required = false) Long employeeId,
            @Valid @RequestBody ChatMessageRequest request) {
        var response = chatService.sendEmployeeMessage(id, employeeId, request.text()); publish(id); return response;
    }

    @PostMapping("/conversations/{id}/customer-messages")
    public ConversationResponse customerMessage(@PathVariable Long id, @Valid @RequestBody ChatMessageRequest request) {
        var response = chatService.sendCustomerMessage(id, request.text()); publish(id); return response;
    }

    @PostMapping("/conversations/{id}/knowledge-messages")
    public ConversationResponse knowledgeMessage(@PathVariable Long id, @Valid @RequestBody ChatMessageRequest request) {
        var response = chatService.sendKnowledgeMessage(id, request.text()); publish(id); return response;
    }

    @PostMapping("/conversations/{id}/ai-messages")
    public ConversationResponse aiMessage(@PathVariable Long id, @Valid @RequestBody ChatMessageRequest request) {
        var response = chatService.sendAiMessage(id, request.text()); publish(id); return response;
    }

    @PostMapping("/conversations/{id}/request-human")
    public ConversationResponse requestHuman(@PathVariable Long id) { var response = chatService.requestHuman(id); publish(id); return response; }

    @PostMapping("/conversations/{id}/assign")
    public ConversationResponse assign(@PathVariable Long id, @Valid @RequestBody AssignEmployeeRequest request) {
        var response = chatService.assignEmployee(id, request.employeeId()); publish(id); return response;
    }

    @PostMapping("/conversations/{id}/close")
    public ConversationResponse close(@PathVariable Long id) { var response = chatService.closeConversation(id); publish(id); return response; }
}
