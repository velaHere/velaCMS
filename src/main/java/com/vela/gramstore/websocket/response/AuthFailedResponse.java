package com.vela.gramstore.websocket.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthFailedResponse {
    private String type;
    private String message;
}
