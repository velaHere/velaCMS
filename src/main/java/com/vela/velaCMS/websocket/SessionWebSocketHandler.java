package com.vela.velaCMS.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vela.velaCMS.config.ExecutorConfig;
import com.vela.velaCMS.security.AccessTokenUtil;
import com.vela.velaCMS.websocket.request.AuthRequest;
import com.vela.velaCMS.websocket.request.WebSocketMessage;
import com.vela.velaCMS.websocket.response.AuthFailedResponse;
import com.vela.velaCMS.websocket.response.AuthSuccessResponse;
import com.vela.velaCMS.websocket.response.PongMessage;
import com.vela.velaCMS.websocket.service.WebSocketSessionService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class SessionWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final AccessTokenUtil accessTokenUtil;
    private final WebSocketSessionService service;
    private final ScheduledExecutorService scheduler;

    private final Map<WebSocketSession, ScheduledFuture<?>> authTimeouts = new ConcurrentHashMap<>();

    @Autowired
    public SessionWebSocketHandler(
            ObjectMapper objectMapper,
            AccessTokenUtil accessTokenUtil,
            WebSocketSessionService service,
            ExecutorConfig executorConfig
    ) {
        this.objectMapper = objectMapper;
        this.accessTokenUtil = accessTokenUtil;
        this.service = service;
        this.scheduler = executorConfig.webSocketScheduler();
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            Boolean check = (Boolean) session.getAttributes().get("authenticated");
            if(!Boolean.TRUE.equals(check))
                try {
                    session.close(CloseStatus.POLICY_VIOLATION);
                } catch (Exception ignored) {}
        }, 5, TimeUnit.SECONDS);

        this.authTimeouts.put(session, future);

        log.info("WebSocket Connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) throws Exception {
        String json = message.getPayload();
        WebSocketMessage wsMessage = objectMapper.readValue(json, WebSocketMessage.class);

        if(!wsMessage.getType().equals(WebSocketMessageType.AUTH)) {
            if(isAuthenticated(session)) {
                service.sendMessage(session, new AuthFailedResponse("AUTH_FAILED", "Authenticate first"));
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
        }

        switch (wsMessage.getType()) {

            case WebSocketMessageType.PING -> {
                session.sendMessage(
                        new TextMessage(objectMapper.writeValueAsString(new PongMessage(WebSocketMessageType.PONG)))
                );
            }

            case WebSocketMessageType.AUTH -> {
                AuthRequest authRequest = objectMapper.readValue(json, AuthRequest.class);
                if(accessTokenUtil.isTokenExpiredOrInvalid(authRequest.getToken())) {
                    service.sendMessage(session, new AuthFailedResponse("AUTH_FAILED", "Invalid or expired token"));
                    session.close(CloseStatus.NOT_ACCEPTABLE);
                    return;
                }
                String username = accessTokenUtil.verifyAndExtractUsername(authRequest.getToken());
                session.getAttributes().put("username", username);
                service.registerSession(username, session);

                session.getAttributes().put("authenticated", true);
                ScheduledFuture<?> future = authTimeouts.remove(session);
                if(future!=null) future.cancel(false);

                service.sendMessage(session, new AuthSuccessResponse("AUTH_SUCCESS"));
            }

            default -> log.warn("Unknown message type: {}", wsMessage.getType());
        }
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {

        service.unregisterSession(session);
        ScheduledFuture<?> future = authTimeouts.remove(session);
        if(future != null) future.cancel(false);

        log.info("WebSocket disconnected: {} with status: {}", session.getId(), status.getReason());
    }

    private boolean isAuthenticated(WebSocketSession session) {
        return Boolean.TRUE.equals(session.getAttributes().get("authenticated"));
    }
}
