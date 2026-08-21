package com.vela.velaCMS.websocket.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PongMessage {
    private String type;
}
