package com.vela.velaCMS.config.property;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.stimulus")
public record StimulusProperty(
        @NotBlank String cookieName,
        @NotBlank String secret,
        @NotBlank String appender,
        @NotBlank String algorithm,
        @NotBlank int ttlDays
) {
}
