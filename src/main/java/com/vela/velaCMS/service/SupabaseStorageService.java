package com.vela.velaCMS.service;

import com.vela.velaCMS.config.property.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class SupabaseStorageService {

    private final S3Client client;
    private final String bucket;

    @Autowired
    public SupabaseStorageService(S3Client client, AppProperties properties) {
        this.client = client;
        this.bucket = properties.storage().supabase().bucket();
    }

    public void put(
            String key,
            String content,
            String contentType
    ) {

        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build();

        client.putObject(
                request,
                RequestBody.fromString(
                        content,
                        StandardCharsets.UTF_8
                )
        );
    }

    public String get(String key) {

        GetObjectRequest request =
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();

        return client
                .getObjectAsBytes(request)
                .asUtf8String();
    }

    public void delete(String key) {

        DeleteObjectRequest request =
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();

        client.deleteObject(request);
    }

    public void put(
            String key,
            InputStream inputStream,
            long size,
            String contentType
    ) {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(size)
                .build();

        client.putObject(
                request,
                RequestBody.fromInputStream(inputStream, size)
        );
    }

    public byte[] getBytes(String key) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> response =
                client.getObjectAsBytes(request);

        return response.asByteArray();
    }
}
