package com.vela.velaCMS.config.property;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @NotBlank AccessTokenProperty accessToken,
        @NotBlank StimulusProperty stimulus,
        @NotBlank CorsProperties cors,
        @NotBlank OTPProperties otp
) {
}
