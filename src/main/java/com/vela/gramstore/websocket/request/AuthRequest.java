package com.vela.gramstore.websocket.request;

import lombok.Data;

@Data
public class AuthRequest {
    private String type;
    private String token;
}
