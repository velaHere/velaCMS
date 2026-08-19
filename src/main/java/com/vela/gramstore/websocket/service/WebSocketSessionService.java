package com.vela.gramstore.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vela.gramstore.websocket.response.LogoutResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WebSocketSessionService {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public WebSocketSessionService(ObjectMapper mapper) {
        this.objectMapper = mapper;
    }

    public void registerSession(String username, WebSocketSession session) {
        sessions.put(username, session);
    }

    public void sendMessage(WebSocketSession session, Object message) throws IOException {
        if(session == null || message == null) return;
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }

    public void unregisterSession(WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");
        if(username == null) return;
        sessions.remove(username, session);
    }

    public void logoutUser(String username) {
        WebSocketSession session = sessions.get(username);
        if(session == null) return;
        try {
            sendMessage(session, new LogoutResponse("LOGOUT", "Your session has expired"));
        } catch (IOException e) {
            log.warn("Failed to send logout message to {}", username, e);
        } finally {
            try {
                session.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                log.warn("Failed to close websocket for {}", username, e);
            }
        }
    }
}
