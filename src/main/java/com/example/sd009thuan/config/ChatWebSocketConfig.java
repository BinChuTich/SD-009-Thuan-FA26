package com.example.sd009thuan.config;

import com.example.sd009thuan.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketHandler handler;
    public ChatWebSocketConfig(ChatWebSocketHandler handler) { this.handler = handler; }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/chat").setAllowedOriginPatterns("*");
    }
}
