package com.vela.velaCMS.websocket.request;

import lombok.Data;

@Data
public class AuthRequest {
    private String type;
    private String token;
}
