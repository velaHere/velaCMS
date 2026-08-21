package com.vela.velaCMS.config.property;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.storage.supabase")
public record SupabaseStorageProperty(
        @NotBlank String endpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String region,
        @NotBlank String bucket
) {
}
