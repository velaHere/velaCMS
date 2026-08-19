package com.vela.gramstore.utility;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

class HmacUtilTests {

    private final byte [] bytes = new byte[6];

    @Test
    void justTesting(){
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(bytes);
        System.out.println(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }
}
