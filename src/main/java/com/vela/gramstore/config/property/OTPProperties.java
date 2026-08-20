package com.vela.gramstore.config.property;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.otp")
public record OTPProperties(
        @NotBlank String cacheKeyPrefix,
        @NotBlank String hashedOtpField,
        @NotBlank String attemptsField
) {
}
