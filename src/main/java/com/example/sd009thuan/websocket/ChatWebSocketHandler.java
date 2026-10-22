package com.example.sd009thuan.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Set<WebSocketSession> SESSIONS = new CopyOnWriteArraySet<>();
    private final ObjectMapper mapper;
    public ChatWebSocketHandler(ObjectMapper mapper) { this.mapper = mapper; }
    @Override public void afterConnectionEstablished(WebSocketSession session) { SESSIONS.add(session); }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { SESSIONS.remove(session); }
    public void broadcast(Object event) {
        try {
            String payload = mapper.writeValueAsString(event);
            for (WebSocketSession s : SESSIONS) {
                if (s.isOpen()) synchronized (s) { s.sendMessage(new TextMessage(payload)); }
            }
        } catch (Exception ignored) { /* websocket client can reconnect; database remains source of truth */ }
    }
}
