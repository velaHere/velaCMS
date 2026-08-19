package com.vela.gramstore.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OTPGenerator {

    public final SecureRandom random;

    public OTPGenerator() {
        this.random = new SecureRandom();
    }

    public String generateOTP() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
