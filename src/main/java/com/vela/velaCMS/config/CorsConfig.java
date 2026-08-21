package com.vela.velaCMS.config;

import com.vela.velaCMS.config.property.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(properties.security().cors().allowedOrigins());

        configuration.setAllowedMethods(List.of(
                "GET","POST","PUT","PATCH","DELETE","OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Public endpoints: allow all origins, no restrictions
        CorsConfiguration openConfig = new CorsConfiguration();
        openConfig.setAllowedOriginPatterns(List.of("*")); // works with credentials
        openConfig.setAllowedMethods(List.of("GET","OPTIONS"));
        openConfig.setAllowedHeaders(List.of("*"));
        openConfig.setAllowCredentials(true);

        // Register only the endpoints you want open
        source.registerCorsConfiguration("/healthy-vela", openConfig);
        source.registerCorsConfiguration("/image/**", openConfig);
        source.registerCorsConfiguration("/*/posts/**", openConfig);
        source.registerCorsConfiguration("/*/post/**", openConfig);

        // Everything else uses your stricter config
        CorsConfiguration restrictedConfig = new CorsConfiguration();
        restrictedConfig.setAllowedOrigins(properties.security().cors().allowedOrigins());
        restrictedConfig.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        restrictedConfig.setAllowedHeaders(List.of("*"));
        restrictedConfig.setAllowCredentials(true);

        source.registerCorsConfiguration("/**", restrictedConfig);

        return source;
    }

    /*
    If you use allowCredentials(true), you generally cannot use allowedOrigins("*")(wildcard).
    You must specify exact origins (which you did).Also, if you’re storing JWT in localStorage and
    sending it in the Authorization header, allowCredentials(true) is not strictly required—but it’s
    often used when you move to cookie-based auth.
    */
}
