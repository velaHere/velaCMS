package com.vela.velaCMS.config.property;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank SecurityProperties security,
        @NotBlank RedisProperty redis
) {
}
