package com.vela.gramstore.core.result;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public enum FailureType {
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    INVALID_ARGUMENTS(HttpStatus.BAD_REQUEST, "Invalid Request Arguments"),
    SESSION_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Session not found"),
    MALFORMED_TOKEN(HttpStatus.BAD_REQUEST, "Malformed token"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Content not found"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "User already exists"),
    TAMPERED_TOKEN(HttpStatus.UNAUTHORIZED, "Tampered token"),
    OTP_EXPIRED(HttpStatus.GONE, "OTP expired"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");

    @Getter
    private final HttpStatus status;
    private final String message;

    FailureType(HttpStatus status, String message){
        this.status=status;
        this.message=message;
    }

    public String getDefaultMessage(){
        return this.message;
    }
}
