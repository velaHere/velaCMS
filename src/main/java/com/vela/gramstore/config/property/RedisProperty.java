package com.vela.gramstore.config.property;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
public record RedisProperty(
        @NotBlank String hostName,
        @NotBlank int port,
        @NotBlank String password,
        @NotBlank String accessTokenKeyPrefix,
        @NotBlank String sessionHashKeyPrefix,
        @NotBlank String sessionHashUserIdField,
        @NotBlank String sessionHashSignaturePrefixField
) {
}
