package com.vela.velaCMS.config.property;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.storage")
public record StorageProperty(
        @NotBlank String root,
        @NotBlank SupabaseStorageProperty supabase
) {
}
