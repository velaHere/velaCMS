package com.vela.gramstore.dto.response;

public record AuthResponse(
        String accessToken,
        boolean verified
){
}
