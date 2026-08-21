package com.vela.velaCMS.config;

import com.vela.velaCMS.config.property.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class SupabaseStorageConfig {

    @Bean
    public S3Client supabaseS3Client(AppProperties properties) {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(
                        properties.storage().supabase().accessKey(),
                        properties.storage().supabase().secretKey()
                );

        return S3Client.builder()
                .endpointOverride(URI.create(properties.storage().supabase().endpoint()))
                .region(Region.of(properties.storage().supabase().region()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .build();
    }
}
