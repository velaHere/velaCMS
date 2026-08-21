package com.vela.velaCMS.config.property;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.accessToken")
public record AccessTokenProperty(
        @NotBlank String secret,
        @NotBlank int ttlMins,
        @NotBlank String issuer
) {
}
