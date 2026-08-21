package com.vela.velaCMS.dto.response;

public record AuthResponse(
        String accessToken,
        boolean verified
){
}
